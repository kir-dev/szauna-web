package hu.kirdev.szaunaweb.persistence

import hu.kirdev.szaunaweb.inventory.InventoryEntity
import hu.kirdev.szaunaweb.inventory.InventoryHistoryEntity
import hu.kirdev.szaunaweb.inventory.InventoryHistoryType
import hu.kirdev.szaunaweb.opening.BookingStatus
import hu.kirdev.szaunaweb.opening.OpeningBookingEntity
import hu.kirdev.szaunaweb.opening.OpeningEntity
import hu.kirdev.szaunaweb.opening.OpeningIntervalEntity
import hu.kirdev.szaunaweb.opening.OpeningStatus
import hu.kirdev.szaunaweb.opening.OpeningTypeEntity
import hu.kirdev.szaunaweb.user.BalanceChangeType
import hu.kirdev.szaunaweb.user.BalanceHistoryEntity
import hu.kirdev.szaunaweb.user.UserEntity
import hu.kirdev.szaunaweb.user.UserRole
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest
class EntityPersistenceTest @Autowired constructor(
    private val entityManager: EntityManager,
) {

    @Test
    fun unsavedEntitiesWithUnsetIdsAreNotEqual() {
        val a = user("auth-a", "a@example.com", "A")
        val b = user("auth-b", "b@example.com", "B")
        assertNotEquals(a, b)
        assertEquals(a, a)
        assertEquals(0L, a.id)
        assertEquals(0L, b.id)
    }

    @Test
    fun persistsOpeningGraphAndReloadsByPublicId() {
        val master = persistMaster()
        val guest = persistUser()
        val type = persistType()
        val start = LocalDateTime.of(2026, 9, 10, 17, 0)
        val opening = persistOpening(master, type, start)
        val firstHour = opening.intervals.first()

        val booking = OpeningBookingEntity(
            orderedBy = guest,
            createdBy = guest,
            openingInterval = firstHour,
            seatCount = 2,
        )
        firstHour.bookings.add(booking)
        entityManager.persist(booking)
        entityManager.flush()

        assertNotNull(opening.publicId)
        assertNotNull(firstHour.publicId)
        assertNotNull(booking.publicId)
        assertEquals(8, firstHour.participantLimit)
        assertEquals(OpeningStatus.SCHEDULED, opening.status)
        assertEquals(type.defaultPrice, opening.price)

        val openingToString = opening.toString()
        assertTrue(openingToString.contains("id=${opening.id}"))
        assertFalse(openingToString.contains("intervals"))
        val userToString = guest.toString()
        assertFalse(userToString.contains("bookings"))
        assertFalse(userToString.contains("balanceHistory"))

        entityManager.clear()

        val reloaded = entityManager.createQuery(
            "select o from OpeningEntity o where o.publicId = :id",
            OpeningEntity::class.java,
        ).setParameter("id", opening.publicId).singleResult

        assertEquals(master.displayName, reloaded.hostedBy.displayName)
        assertEquals("Jeges", reloaded.openingType.name)
        assertEquals(3, reloaded.intervals.size)
        assertEquals(2, reloaded.intervals.first().bookings.first().seatCount)
    }

    @Test
    fun cancelledBookingCanBeReactivatedOnTheSameInterval() {
        val master = persistMaster()
        val guest = persistUser()
        val opening = persistOpening(master, persistType(), LocalDateTime.of(2026, 9, 11, 17, 0))
        val interval = opening.intervals.first()

        val booking = OpeningBookingEntity(
            orderedBy = guest,
            createdBy = guest,
            openingInterval = interval,
            seatCount = 2,
        )
        entityManager.persist(booking)
        entityManager.flush()

        booking.status = BookingStatus.CANCELLED
        entityManager.flush()

        booking.status = BookingStatus.ACTIVE
        booking.seatCount = 3
        booking.createdBy = master
        entityManager.flush()
        entityManager.clear()

        val reloaded = entityManager.find(OpeningBookingEntity::class.java, booking.id)
        assertEquals(BookingStatus.ACTIVE, reloaded.status)
        assertEquals(3, reloaded.seatCount)
        assertEquals(master.id, reloaded.createdBy.id)
    }

    @Test
    fun duplicateBookingForSameUserAndIntervalIsRejected() {
        val master = persistMaster()
        val guest = persistUser()
        val opening = persistOpening(master, persistType(), LocalDateTime.of(2026, 9, 12, 17, 0))
        val interval = opening.intervals.first()

        entityManager.persist(
            OpeningBookingEntity(
                orderedBy = guest,
                createdBy = guest,
                openingInterval = interval,
                seatCount = 1,
            )
        )
        entityManager.flush()

        assertFails {
            entityManager.persist(
                OpeningBookingEntity(
                    orderedBy = guest,
                    createdBy = master,
                    openingInterval = interval,
                    seatCount = 1,
                    status = BookingStatus.CANCELLED,
                )
            )
            entityManager.flush()
        }
    }

    @Test
    fun chargeWritesLedgerAndMarksBookingPaid() {
        val master = persistMaster()
        val guest = persistUser()
        val opening = persistOpening(master, persistType(), LocalDateTime.of(2026, 9, 13, 17, 0))
        val booking = OpeningBookingEntity(
            orderedBy = guest,
            createdBy = guest,
            openingInterval = opening.intervals.first(),
            seatCount = 2,
        )
        entityManager.persist(booking)

        guest.balance = 2000
        val charge = opening.price * booking.seatCount
        guest.balance -= charge
        booking.chargedAmount = charge
        booking.chargedAt = Instant.now()

        entityManager.persist(
            BalanceHistoryEntity(
                type = BalanceChangeType.CHARGE,
                valueChange = -charge,
                message = "jelentkezés",
                balanceAfter = guest.balance,
                user = guest,
                createdBy = master,
                booking = booking,
            )
        )
        entityManager.flush()
        entityManager.clear()

        val reloadedUser = entityManager.find(UserEntity::class.java, guest.id)
        val reloadedBooking = entityManager.find(OpeningBookingEntity::class.java, booking.id)
        val history = entityManager.createQuery(
            "select h from BalanceHistoryEntity h where h.user.id = :userId",
            BalanceHistoryEntity::class.java,
        ).setParameter("userId", guest.id).singleResult

        assertEquals(2000 - charge, reloadedUser.balance)
        assertNotNull(reloadedBooking.chargedAt)
        assertEquals(charge, reloadedBooking.chargedAmount)
        assertEquals(BalanceChangeType.CHARGE, history.type)
        assertEquals(booking.id, history.booking?.id)
        assertEquals(master.id, history.createdBy.id)
    }

    @Test
    fun unbanKeepsLastBanReasonAndActor() {
        val master = persistMaster()
        val guest = persistUser()
        val bannedAt = Instant.parse("2026-09-01T10:00:00Z")

        guest.isBanned = true
        guest.banReason = "no-show"
        guest.bannedBy = master
        guest.bannedAt = bannedAt
        entityManager.flush()

        guest.isBanned = false
        entityManager.flush()
        entityManager.clear()

        val reloaded = entityManager.find(UserEntity::class.java, guest.id)
        assertFalse(reloaded.isBanned)
        assertEquals("no-show", reloaded.banReason)
        assertEquals(master.id, reloaded.bannedBy?.id)
        assertEquals(bannedAt, reloaded.bannedAt)
    }

    @Test
    fun deletingOpeningTypeWithOpeningsDoesNotCascade() {
        val master = persistMaster()
        val type = persistType()
        persistOpening(master, type, LocalDateTime.of(2026, 9, 14, 17, 0))

        entityManager.remove(type)
        assertFails { entityManager.flush() }
    }

    @Test
    fun inventoryHistoryRecordsBoughtAndAdjustment() {
        val master = persistMaster()
        val oil = InventoryEntity(name = "Eukaliptusz", quantity = 0)
        entityManager.persist(oil)

        entityManager.persist(
            InventoryHistoryEntity(
                type = InventoryHistoryType.BOUGHT,
                quantity = 10,
                totalCost = 5000,
                inventory = oil,
                createdBy = master,
            )
        )
        oil.quantity = 10
        entityManager.persist(
            InventoryHistoryEntity(
                type = InventoryHistoryType.ADJUSTMENT,
                quantity = 8,
                message = "count correction",
                inventory = oil,
                createdBy = master,
            )
        )
        oil.quantity = 8
        entityManager.flush()
        entityManager.clear()

        val reloaded = entityManager.find(InventoryEntity::class.java, oil.id)
        val history = entityManager.createQuery(
            "select h from InventoryHistoryEntity h where h.inventory.id = :id order by h.id",
            InventoryHistoryEntity::class.java,
        ).setParameter("id", oil.id).resultList

        assertEquals(8, reloaded.quantity)
        assertEquals(2, history.size)
        assertEquals(InventoryHistoryType.BOUGHT, history[0].type)
        assertEquals(InventoryHistoryType.ADJUSTMENT, history[1].type)
        assertEquals(master.id, history[0].createdBy.id)
        assertNull(history[1].totalCost)
    }

    private fun persistMaster(): UserEntity {
        val master = user("auth-master", "master@example.com", "Mester", UserRole.SAUNA_MASTER)
        entityManager.persist(master)
        entityManager.flush()
        return master
    }

    private fun persistUser(): UserEntity {
        val guest = user("auth-user", "user@example.com", "Vendég")
        entityManager.persist(guest)
        entityManager.flush()
        return guest
    }

    private fun persistType(): OpeningTypeEntity {
        val type = OpeningTypeEntity(name = "Jeges", description = "jég", defaultPrice = 800)
        entityManager.persist(type)
        entityManager.flush()
        return type
    }

    private fun persistOpening(
        master: UserEntity,
        type: OpeningTypeEntity,
        start: LocalDateTime,
    ): OpeningEntity {
        val opening = OpeningEntity(
            openingStart = start,
            openingEnd = start.plusHours(3),
            price = type.defaultPrice,
            hostedBy = master,
            openingType = type,
        )
        repeat(3) { index ->
            val interval = OpeningIntervalEntity(
                intervalStart = start.plusHours(index.toLong()),
                intervalEnd = start.plusHours(index + 1L),
                opening = opening,
            )
            opening.intervals.add(interval)
        }
        entityManager.persist(opening)
        entityManager.flush()
        return opening
    }

    private fun user(
        authSub: String,
        email: String,
        displayName: String,
        role: UserRole = UserRole.USER,
    ) = UserEntity(
        authSub = authSub,
        email = email,
        displayName = displayName,
        role = role,
    )
}
