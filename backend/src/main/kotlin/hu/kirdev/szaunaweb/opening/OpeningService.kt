package hu.kirdev.szaunaweb.opening

import hu.kirdev.szaunaweb.exception.*
import hu.kirdev.szaunaweb.user.UserEntity
import hu.kirdev.szaunaweb.user.UserRole
import hu.kirdev.szaunaweb.user.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

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
        val IGNORED_STATUSES_FOR_OVERLAP = listOf(
            OpeningStatus.CANCELLED,
            OpeningStatus.DELETED
        )
    }

    @Transactional(readOnly = true)
    fun getOpeningByPublicId(userId: UUID?, publicId: UUID): OpeningResponse {
        val opening = findOpening(publicId)
        if (opening.isPrivate) {
            if (userId == null) {
                throw OpeningPermissionException("This is a private opening, please login to verify your access!")
            }

            val hasAccess = hasAccessForPrivateOpening(userId)

            if (!hasAccess) {
                throw OpeningPermissionException("You do not have permission to access this private opening!")
            }

        }
        return OpeningResponse(opening)
    }

    @Transactional(readOnly = true)
    fun getOpenings(
        userId: UUID?,
        pageable: Pageable,
        status: OpeningStatus?,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): Page<OpeningResponse> {
        var hasAccessToAll = false
        if (userId != null) {
            hasAccessToAll = hasAccessForPrivateOpening(userId)
        }

        val openings = openingRepository.findOpeningWithFilter(status, from, to, pageable, hasAccessToAll)

        return openings.map { OpeningResponse(it) }
    }


    //NOT PRIVATE OPENING
    @Transactional(readOnly = true)
    fun getCurrentOpening(): OpeningResponse? {
        return openingRepository.findCurrentOrNextOpening(LocalDateTime.now())?.let { OpeningResponse(it) }
    }

    @Transactional
    fun createOpening(userId: UUID, dto: CreateOpeningRequest): OpeningResponse {
        if (dto.openingStart >= dto.openingEnd) throw InvalidTimeRangeException("Opening start must be greater than end!")
        if (dto.openingStart.isBefore(LocalDateTime.now())) throw InvalidTimeRangeException("Cannot create an opening in the past!")
        val user = userService.findByPublicId(userId)
        val hasOverlap = openingRepository.existsByStatusNotInAndOpeningStartLessThanAndOpeningEndGreaterThan(
            statuses = IGNORED_STATUSES_FOR_OVERLAP,
            end = dto.openingEnd,
            start = dto.openingStart
        )

        if (hasOverlap) {
            throw TimeRangeConflictException("There is already an active opening scheduled in this time range!")
        }

        val openingType = findOpeningType(dto.openingTypeId)

        if (!openingType.active) throw OpeningException("Opening type $openingType is not active!")

        val opening = OpeningEntity(user, dto, openingType)

        if (dto.generateDefaultIntervals) {
            opening.intervals = generateDefaultIntervals(dto.openingStart, dto.openingEnd, opening)
        }

        val saved = openingRepository.saveAndFlush(opening)

        return OpeningResponse(saved)
    }

    @Transactional
    fun createInterval(userId: UUID, dto: CreateIntervalRequest, openingId: UUID): OpeningResponse {
        val opening = findOpening(openingId)
        val user = userService.findByPublicId(userId)
        checkOpeningPermission(user, opening)

        if (opening.openingStart.isAfter(dto.intervalStart) || opening.openingEnd.isBefore(dto.intervalEnd)) {
            throw InvalidTimeRangeException("The interval does not fit into the opening time range!")
        }

        val intervals = opening.intervals

        val hasOverlap = intervals.any { other ->
            other.overlapsWith(dto.intervalStart, dto.intervalEnd)
        }

        if (hasOverlap) {
            throw TimeRangeConflictException("Cannot create interval: the interval range are overlap with an other interval!")
        }

        val newInterval = OpeningIntervalEntity(
            dto.intervalStart,
            dto.intervalEnd,
            opening,
            dto.participantLimit
        )

        opening.intervals.add(newInterval)

        val saved = openingRepository.saveAndFlush(opening)

        return OpeningResponse(saved)
    }

    @Transactional
    fun updateOpening(userId: UUID, dto: UpdateOpeningRequest, openingId: UUID): OpeningResponse {
        if (!dto.openingStart.isBefore(dto.openingEnd)) throw InvalidTimeRangeException("Opening start must be before end!")

        if (dto.openingStart.isBefore(LocalDateTime.now())) throw InvalidTimeRangeException("Cannot create an opening in the past!")

        val opening = findOpening(openingId)

        val completed = isOpeningCompleted(opening)

        if (completed) {
            throw OpeningException("After the opening marked as completed, you are unable to modify it!")
        }

        if (opening.openingStart != dto.openingStart || opening.openingEnd != dto.openingEnd) {
            val hasOverlap =
                openingRepository.existsByPublicIdNotAndStatusNotInAndOpeningStartLessThanAndOpeningEndGreaterThan(
                    openingId,
                    IGNORED_STATUSES_FOR_OVERLAP,
                    dto.openingEnd,
                    dto.openingStart,
                )

            if (hasOverlap) {
                throw TimeRangeConflictException("The modified opening range overlaps with an other active opening!")
            }
        }

        val user = userService.findByPublicId(userId)

        checkOpeningPermission(user, opening)

        val activeIntervals =
            opening.intervals.filter { it.status == IntervalStatus.ACTIVE }

        if (activeIntervals.isNotEmpty()) {
            val earliestIntervalStart = activeIntervals.minOf { it.intervalStart }
            val latestIntervalEnd = activeIntervals.maxOf { it.intervalEnd }

            if (dto.openingStart.isAfter(earliestIntervalStart)) {
                throw TimeRangeConflictException("Cannot delay opening start: the first active interval begins at $earliestIntervalStart.")
            }

            if (dto.openingEnd.isBefore(latestIntervalEnd)) {
                throw TimeRangeConflictException("Cannot shorten opening end: the last active interval ends at $latestIntervalEnd.")
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
    fun updateInterval(userId: UUID, dto: UpdateIntervalRequest, openingId: UUID, intervalId: UUID): OpeningResponse {
        val opening = findOpening(openingId)

        val user = userService.findByPublicId(userId)

        checkOpeningPermission(user, opening)

        val completed = isOpeningCompleted(opening)

        if (completed) {
            throw OpeningException("After the opening marked as completed, you are unable to modify it!")
        }


        val interval = opening.intervals.find { it.publicId == intervalId }
            ?: throw IntervalNotFoundException("No interval found for $intervalId")

        val intervals =
            opening.intervals.filter { it.status == IntervalStatus.ACTIVE && it.publicId != intervalId }

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
            throw InvalidTimeRangeException("Interval start must be before interval end!")
        }

        val openingRange = opening.openingStart..opening.openingEnd

        if (dto.intervalStart !in openingRange || dto.intervalEnd !in openingRange) {
            throw InvalidTimeRangeException(
                "Interval (${dto.intervalStart} - ${dto.intervalEnd}) must be within opening hours (${opening.openingStart} - ${opening.openingEnd})!"
            )
        }

        val hasOverlap = intervals.any { other ->
            other.overlapsWith(dto.intervalStart, dto.intervalEnd)
        }

        if (hasOverlap) {
            throw TimeRangeConflictException("The modified interval overlaps with an other active interval!")
        }

        interval.intervalStart = dto.intervalStart
        interval.intervalEnd = dto.intervalEnd

        val saved = openingRepository.saveAndFlush(opening)

        return OpeningResponse(saved)

    }

    @Transactional
    fun updateOpeningStatus(userId: UUID, dto: UpdateOpeningStatusRequest, openingId: UUID): OpeningResponse {
        val opening = findOpening(openingId)

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

            OpeningStatus.COMPLETED -> {
                opening.status = OpeningStatus.COMPLETED
            }

            else -> {
                throw OpeningException("The opening status is invalid!")
            }

        }

        val saved = openingRepository.saveAndFlush(opening)
        return OpeningResponse(saved)
    }

    @Transactional
    fun updateIntervalStatus(
        userId: UUID,
        dto: UpdateIntervalStatusRequest,
        openingId: UUID,
        intervalId: UUID
    ): OpeningResponse {
        val opening = findOpening(openingId)

        val user = userService.findByPublicId(userId)

        checkOpeningPermission(user, opening)

        val completed = isOpeningCompleted(opening)

        if (completed) {
            throw OpeningException("After the opening marked as completed, you are unable to modify it!")
        }

        when (dto.status) {
            IntervalStatus.ACTIVE -> {
                val interval =
                    opening.intervals.find { it.status == IntervalStatus.CANCELLED && it.publicId == intervalId }
                        ?: throw IntervalNotFoundException("No interval found for $intervalId or not canceled!")

                interval.status = IntervalStatus.ACTIVE
            }

            IntervalStatus.CANCELLED -> {
                val interval =
                    opening.intervals.find { it.status == IntervalStatus.ACTIVE && it.publicId == intervalId }
                        ?: throw IntervalNotFoundException("No interval found for $intervalId or not active!")

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
        if (user.authSub !in admins) throw OpeningPermissionException("To delete an opening, you need admin permission!")

        val opening = findOpening(openingId)

        val completed = isOpeningCompleted(opening)

        if (completed) {
            throw OpeningException("After the opening marked as completed, you are unable to modify it!")
        }


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

        val completed = isOpeningCompleted(opening)

        if (completed) {
            throw OpeningException("After the opening marked as completed, you are unable to modify it!")
        }

        val interval = opening.intervals.find { it.publicId == intervalId }
            ?: throw IntervalNotFoundException("Interval: $intervalId not found!")

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

    private fun findOpeningType(id: Long): OpeningTypeEntity {
        return openingTypeRepository.findById(id)
            .orElseThrow { OpeningNotFoundException("No open type found for id $id") }
    }

    private fun generateDefaultIntervals(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        opening: OpeningEntity
    ): MutableList<OpeningIntervalEntity> {
        if (endTime.isAfter(startTime)) {
            throw InvalidTimeRangeException("End time must be after the start and end time")
        }

        val totalMinutes = Duration.between(startTime, endTime).toMinutes()

        val slotMinutes = totalMinutes / DEFAULT_INTERVAL_NUMBERS

        if (totalMinutes % DEFAULT_INTERVAL_NUMBERS == 0L) {
            throw OpeningException("Total duration ($totalMinutes) cannot be divided equally into $DEFAULT_INTERVAL_NUMBERS slots")
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
        if (opening.hostedBy != user && (user.authSub !in admins)) throw OpeningPermissionException("To update opening, you have to host it or must be a admin!")
    }

    private fun findOpening(publicId: UUID): OpeningEntity {
        return openingRepository.findByPublicId(publicId)
            ?: throw OpeningNotFoundException("No open found for $publicId")

    }

    private fun hasAccessForPrivateOpening(userId: UUID): Boolean {
        val user = userService.findByPublicId(userId)
        if (user.authSub in admins) return true
        if (user.role != UserRole.USER) return true
        return false
    }

    private fun isOpeningCompleted(opening: OpeningEntity): Boolean {
        return opening.status == OpeningStatus.COMPLETED
    }

}