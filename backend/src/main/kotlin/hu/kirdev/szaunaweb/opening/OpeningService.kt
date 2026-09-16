package hu.kirdev.szaunaweb.opening

import hu.kirdev.szaunaweb.exception.OpeningException
import hu.kirdev.szaunaweb.user.UserEntity
import hu.kirdev.szaunaweb.user.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.awt.print.Book
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import kotlin.concurrent.thread

@Service
class OpeningService(
    val userService: UserService,
    val openingTypeRepository: OpeningTypeEntityRepository,
    val openingRepository: OpeningEntityRepository,

    @Value("\${szaunaWeb.admins:}")
    private val admins: List<String> = emptyList(),
) {

    companion object {
        const val DEFAULT_PARTICIPANT_LIMIT = 8
        const val DEFAULT_INTERVAL_NUMBERS = 3 //3 equal slot in duration
    }

    @Transactional
    fun createOpening(userId: UUID, dto: CreateOpeningRequest): OpeningResponse {
        if (dto.openingStart >= dto.openingEnd) throw OpeningException("Opening start must be greater than end!")
        val user = userService.findByPublicId(userId)
        val openingType = findOpeningType(dto.openingTypeId)

        if (!openingType.active) throw OpeningException("Opening type $openingType is not active!")

        val opening = OpeningEntity(user, dto, openingType)

        if (dto.generateDefaultIntervals) {
            opening.intervals = generateDefaultIntervals(dto.openingStart, dto.openingEnd, opening)
        }

        val saved = openingRepository.saveAndFlush(opening)

        return OpeningResponse(saved)
    }

    private fun findOpeningType(id: Long): OpeningTypeEntity {
        return openingTypeRepository.findById(id).orElseThrow { OpeningException("No open type found for id $id") }
    }

    private fun generateDefaultIntervals(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        opening: OpeningEntity
    ): MutableList<OpeningIntervalEntity> {
        require(endTime.isAfter(startTime)) { "End time must be after the start and end time" }

        val totalMinutes = Duration.between(startTime, endTime).toMinutes()

        val slotMinutes = totalMinutes / DEFAULT_INTERVAL_NUMBERS

        require(totalMinutes % DEFAULT_INTERVAL_NUMBERS == 0L) {
            "Total duration ($totalMinutes) cannot be divided equally into $DEFAULT_INTERVAL_NUMBERS slots"
        }

        val intervals = mutableListOf<OpeningIntervalEntity>()
        var currentStart = startTime

        for (i in 1..DEFAULT_INTERVAL_NUMBERS) {
            val currentEnd = currentStart.plusMinutes(slotMinutes)
            intervals.add(
                OpeningIntervalEntity(
                    currentStart, currentEnd, opening, DEFAULT_PARTICIPANT_LIMIT
                )
            )

            currentStart = currentEnd
        }

        return intervals
    }

    private fun checkOpeningPermission(user: UserEntity, opening: OpeningEntity) {
        if (opening.hostedBy != user && (user.authSub !in admins)) throw OpeningException("To update opening, you have to host it or must be a admin!")
    }

    private fun findOpening(publicId: UUID): OpeningEntity {
        return openingRepository.findByPublicId(publicId)
            ?: throw OpeningException("No open found for $publicId")

    }


    //NOT PRIVATE OPENING
    @Transactional(readOnly = true)
    fun getCurrentOpening(): OpeningResponse? {
        return openingRepository.findCurrentOrNextOpening(LocalDateTime.now())?.let { OpeningResponse(it) }
    }


    @Transactional
    fun updateOpening(userId: UUID, dto: UpdateOpeningRequest): OpeningResponse {
        if (!dto.openingStart.isBefore(dto.openingEnd)) throw OpeningException("Opening start must be before end!")


        val opening = findOpening(dto.publicId)

        val user = userService.findByPublicId(userId)

        checkOpeningPermission(user, opening)

        val activeIntervals =
            opening.intervals.filter { it.status == IntervalStatus.ACTIVE }

        if (activeIntervals.isNotEmpty()) {
            val earliestIntervalStart = activeIntervals.minOf { it.intervalStart }
            val latestIntervalEnd = activeIntervals.maxOf { it.intervalEnd }

            if (dto.openingStart.isAfter(earliestIntervalStart)) {
                throw OpeningException("Cannot delay opening start: the first active interval begins at $earliestIntervalStart.")
            }

            if (dto.openingEnd.isBefore(latestIntervalEnd)) {
                throw OpeningException("Cannot shorten opening end: the last active interval ends at $latestIntervalEnd.")
            }
        }

        opening.openingStart = dto.openingStart
        opening.openingEnd = dto.openingEnd

        val priceChanged = opening.price != dto.price

        if (opening.openingType.id != dto.openingTypeId) {
            val newOpeningType = findOpeningType(dto.openingTypeId)
            if (!newOpeningType.active) throw OpeningException("Opening type ${newOpeningType.name} is not active!")
            opening.openingType = newOpeningType
            opening.price = newOpeningType.defaultPrice //TODO:: How to change the price if the type changed
        }

        if (priceChanged) {
            opening.price = dto.price ?: opening.openingType.defaultPrice
        }

        val saved = openingRepository.saveAndFlush(opening)

        return OpeningResponse(saved)
    }

    @Transactional
    fun updateInterval(userId: UUID, dto: UpdateIntervalRequest): OpeningResponse {
        val opening = findOpening(dto.openingPublicId)

        val user = userService.findByPublicId(userId)

        checkOpeningPermission(user, opening)

        val interval = opening.intervals.find { it.publicId == dto.intervalPublicId }
            ?: throw OpeningException("No interval found for ${dto.intervalPublicId}")

        val intervals =
            opening.intervals.filter { it.status == IntervalStatus.ACTIVE && it.publicId != dto.intervalPublicId }

        if (interval.participantLimit <= dto.participantLimit) {
            interval.participantLimit = dto.participantLimit
        } else {

            val bookedSeats = interval.bookings.filter { it.status == BookingStatus.ACTIVE }.sumOf { it.seatCount }

            if (bookedSeats > dto.participantLimit) {
                throw OpeningException("There is too much booked seats, the participant limit cannot be lower then the booked seats!")
            }

            interval.participantLimit = dto.participantLimit
        }

        if (!dto.intervalStart.isBefore(dto.intervalEnd)) {
            throw OpeningException("Interval start must be before interval end!")
        }

        val openingRange = opening.openingStart..opening.openingEnd

        if (dto.intervalStart !in openingRange || dto.intervalEnd !in openingRange) {
            throw OpeningException(
                "Interval (${dto.intervalStart} - ${dto.intervalEnd}) must be within opening hours (${opening.openingStart} - ${opening.openingEnd})!"
            )
        }

        val hasOverlap = intervals.any { other ->
            other.overlapsWith(dto.intervalStart, dto.intervalEnd)
        }

        if (hasOverlap) {
            throw OpeningException("The modified interval overlaps with an other active interval!")
        }

        interval.intervalStart = dto.intervalStart
        interval.intervalEnd = dto.intervalEnd

        val saved = openingRepository.saveAndFlush(opening)

        return OpeningResponse(saved)

    }

    @Transactional
    fun updateOpeningStatus(userId: UUID, dto: UpdateOpeningStatusRequest): OpeningResponse {
        val opening = findOpening(dto.publicId)

        val user = userService.findByPublicId(userId)

        checkOpeningPermission(user, opening)

        when (dto.status) {
            OpeningStatus.CANCELLED -> {
                if (opening.intervals.isNotEmpty()) {
                    opening.intervals
                        .filter { it.status == IntervalStatus.ACTIVE }
                        .forEach { interval ->
                            interval.status = IntervalStatus.CANCELLED
                            interval.bookings.filter { it.status == BookingStatus.ACTIVE }
                                .forEach { booking ->
                                    booking.status = BookingStatus.CANCELLED
                                }
                        }
                }
                opening.status = OpeningStatus.CANCELLED
            }

            OpeningStatus.SCHEDULED -> {
                if (opening.intervals.isNotEmpty()) {
                    opening.intervals
                        .filter { it.status == IntervalStatus.CANCELLED }
                        .forEach { interval ->
                            interval.status = IntervalStatus.ACTIVE
                        }
                }
                opening.status = OpeningStatus.SCHEDULED
            }

            else -> {
                throw OpeningException("The opening status is invalid!")
            }

        }

        val saved = openingRepository.saveAndFlush(opening)
        return OpeningResponse(saved)
    }

    @Transactional
    fun updateIntervalStatus(userId: UUID, dto: UpdateIntervalStatusRequest): OpeningResponse {
        val opening = findOpening(dto.openingPublicId)

        val user = userService.findByPublicId(userId)

        checkOpeningPermission(user, opening)

        when (dto.status) {
            IntervalStatus.ACTIVE -> {
                val interval =
                    opening.intervals.find { it.status == IntervalStatus.CANCELLED && it.publicId == dto.intervalPublicId }
                        ?: throw OpeningException("No interval found for ${dto.intervalPublicId} or not canceled!")

                interval.status = IntervalStatus.ACTIVE
            }

            IntervalStatus.CANCELLED -> {
                val interval =
                    opening.intervals.find { it.status == IntervalStatus.ACTIVE && it.publicId == dto.intervalPublicId }
                        ?: throw OpeningException("No interval found for ${dto.intervalPublicId} or not active!")

                interval.status = IntervalStatus.CANCELLED
                interval.bookings.filter { it.status == BookingStatus.ACTIVE }
                    .forEach { booking ->
                        booking.status = BookingStatus.CANCELLED
                    }

            }

            else -> {

            }
        }

        val saved = openingRepository.saveAndFlush(opening)

        return OpeningResponse(saved)
    }

    @Transactional
    fun deleteOpening(userId: UUID, openingId: UUID) {
        val user = userService.findByPublicId(userId)
        if (user.authSub !in admins) throw OpeningException("To delete an opening, you need admin permission!")

        val opening = findOpening(openingId)

        opening.intervals.forEach { interval ->
            interval.status = IntervalStatus.DELETED
            interval.bookings.filter { it.status == BookingStatus.ACTIVE }
                .forEach { booking ->
                    booking.status = BookingStatus.CANCELLED
                }
        }
        opening.status = OpeningStatus.DELETED
        openingRepository.save(opening)
    }

    @Transactional
    fun deleteInterval(userId: UUID, openingId: UUID, intervalId: UUID): OpeningResponse {

        val opening = findOpening(openingId)

        val user = userService.findByPublicId(userId)

        checkOpeningPermission(user, opening)

        val interval = opening.intervals.find { it.publicId == intervalId }
            ?: throw OpeningException("Interval: $intervalId not found!")

        if (interval.status == IntervalStatus.DELETED) {
            return OpeningResponse(opening)
        }

        val hasActiveBooking = interval.bookings.any { it.status == BookingStatus.ACTIVE }

        if (hasActiveBooking) {
            throw OpeningException("Cannot delete interval: active booking still exists on this time slot!")
        }

        interval.status = IntervalStatus.DELETED

        val saved = openingRepository.saveAndFlush(opening)

        return OpeningResponse(saved)
    }

}