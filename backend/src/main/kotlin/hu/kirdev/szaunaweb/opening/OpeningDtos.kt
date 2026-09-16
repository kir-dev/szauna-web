package hu.kirdev.szaunaweb.opening

import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime
import java.util.UUID

data class CreateOpeningRequest(
    @field:NotBlank
    val openingStart: LocalDateTime,
    @field:NotBlank
    val openingEnd: LocalDateTime,
    val isPrivate: Boolean = false,
    val price: Int? = null,
    @field:NotBlank
    val openingTypeId: Long,
    val generateDefaultIntervals: Boolean = true,
)

data class UpdateOpeningRequest(
    @field:NotBlank
    val publicId: UUID,
    @field:NotBlank
    val openingStart: LocalDateTime,
    @field:NotBlank
    val openingEnd: LocalDateTime,
    val price: Int? = null,
    @field:NotBlank
    val openingTypeId: Long,
)

data class UpdateIntervalRequest(
    @field:NotBlank
    val openingPublicId: UUID,
    @field:NotBlank
    val intervalPublicId: UUID,
    @field:NotBlank
    val intervalStart: LocalDateTime,
    @field:NotBlank
    val intervalEnd: LocalDateTime,
    val participantLimit: Int,
)

data class UpdateOpeningStatusRequest(
    @field:NotBlank
    val publicId: UUID,
    val status: OpeningStatus,
)

data class UpdateIntervalStatusRequest(
    @field:NotBlank
    val openingPublicId: UUID,
    @field:NotBlank
    val intervalPublicId: UUID,
    val status: IntervalStatus,
)

data class OpeningResponse(
    val publicId: UUID,
    val openingStart: LocalDateTime,
    val openingEnd: LocalDateTime,
    val isPrivate: Boolean,
    val price: Int,
    val hostedByName: String,
    val typeName: String,
    val status: OpeningStatus,
    val intervals: MutableList<IntervalResponse>
) {
    constructor(opening: OpeningEntity) : this(
        publicId = opening.publicId!!,
        openingStart = opening.openingStart,
        openingEnd = opening.openingEnd,
        isPrivate = opening.isPrivate,
        price = opening.price,
        hostedByName = opening.hostedBy.displayName,
        typeName = opening.openingType.name,
        status = opening.status,
        intervals = opening.intervals.map { IntervalResponse(it) }.toMutableList()
    )
}

data class IntervalResponse(
    val publicId: UUID,
    val intervalStart: LocalDateTime,
    val intervalEnd: LocalDateTime,
    val participantLimit: Int,
    val availableSeats: Int,
    val status: IntervalStatus,
) {
    constructor(interval: OpeningIntervalEntity) : this(
        publicId = interval.publicId!!,
        intervalStart = interval.intervalStart,
        intervalEnd = interval.intervalEnd,
        participantLimit = interval.participantLimit,
        status = interval.status,
        availableSeats = interval.participantLimit - interval.bookings.filter { it.status == BookingStatus.ACTIVE }
            .sumOf { it.seatCount }
    )
}