package hu.kirdev.szaunaweb.opening

import hu.kirdev.szaunaweb.exception.OpeningException
import hu.kirdev.szaunaweb.user.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Service
class OpeningService(
    val userService: UserService,
    val openingTypeRepository: OpeningTypeEntityRepository,
    val openingRepository: OpeningEntityRepository,
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


    //NOT PRIVATE OPENING
    @Transactional(readOnly = true)
    fun getCurrentOpening(): OpeningResponse? {
        return openingRepository.findCurrentOrNextOpening(LocalDateTime.now())?.let { OpeningResponse(it) }
    }


    @Transactional
    fun updateOpening(dto: UpdateOpeningRequest): OpeningResponse {
        if (!dto.openingStart.isBefore(dto.openingEnd)) throw OpeningException("Opening start must be before end!")

        val opening = openingRepository.findByPublicId(dto.publicId)
            ?: throw OpeningException("No open type found for ${dto.publicId}")

        val activeIntervals = opening.intervals.filter { !it.cancelled }

        if (opening.intervals.isNotEmpty()) {
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

}