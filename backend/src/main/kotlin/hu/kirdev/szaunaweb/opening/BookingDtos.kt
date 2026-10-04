package hu.kirdev.szaunaweb.opening

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class BookingRequest(
    val userId: UUID? = null,
    @field:NotBlank
    val openingId: UUID,
    @field:NotBlank
    val intervalId: UUID,
    val seatCount: Int = 1,
)

data class BookingUpdateRequest(
    @field:NotBlank
    val openingId: UUID,
    @field:NotBlank
    val intervalId: UUID,
    @field:NotBlank
    val seatCount: Int,
    @field:NotBlank
    val status: BookingStatus
)