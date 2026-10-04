package hu.kirdev.szaunaweb.opening

import hu.kirdev.szaunaweb.exception.BookingException
import hu.kirdev.szaunaweb.exception.OpeningException
import hu.kirdev.szaunaweb.user.UserEntity
import hu.kirdev.szaunaweb.user.UserRole
import hu.kirdev.szaunaweb.user.UserService
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BookingService(
    private val openingRepository: OpeningEntityRepository,
    private val userService: UserService,

    @Value("\${szaunaWeb.admins:}")
    private val admins: List<String> = emptyList()

) {


    @Transactional
    fun createBooking(userId: UUID, dto: BookingRequest) {
        val opening = findOpening(dto.openingId)
        val createdBy = userService.findByPublicId(userId)

        if (opening.isPrivate) {
            if (!checkUserHasAdminOrSaunaMasterRole(createdBy)) {
                throw BookingException("User does not have enough permission to create a booking for a private opening!")
            }
        }

        val interval = opening.intervals.find { it.publicId == dto.intervalId }

        if (interval == null) {
            throw BookingException("Interval does not exist!")
        }

        var orderedBy: UserEntity? = null
        if (userId != dto.userId) {
            orderedBy = userService.findByPublicId(userId)
        }

        val newBooking = OpeningBookingEntity(
            orderedBy = orderedBy ?: createdBy,
            createdBy = createdBy,
            openingInterval = interval,
            seatCount = dto.seatCount,
        )

        interval.bookings.add(newBooking)

        val saved = openingRepository.saveAndFlush(opening)

    }

    private fun findOpening(publicId: UUID): OpeningEntity {
        return openingRepository.findByPublicId(publicId)
            ?: throw OpeningException("No open found for $publicId")

    }

    private fun checkUserHasAdminOrSaunaMasterRole(user: UserEntity): Boolean {
        if (user.authSub in admins) {
            return true
        }

        if (user.role == UserRole.SAUNA_MASTER || user.role == UserRole.ADMIN) {
            return true
        }

        return false;
    }


}