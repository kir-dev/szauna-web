package hu.kirdev.szaunaweb.test

import hu.kirdev.szaunaweb.opening.IntervalStatus
import hu.kirdev.szaunaweb.opening.OpeningEntity
import hu.kirdev.szaunaweb.opening.OpeningEntityRepository
import hu.kirdev.szaunaweb.opening.OpeningIntervalEntity
import hu.kirdev.szaunaweb.opening.OpeningStatus
import hu.kirdev.szaunaweb.opening.OpeningTypeEntity
import hu.kirdev.szaunaweb.opening.OpeningTypeEntityRepository
import hu.kirdev.szaunaweb.user.UserEntity
import hu.kirdev.szaunaweb.user.UserEntityRepository
import hu.kirdev.szaunaweb.user.UserRole
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Component
@Profile("test")
class TestDataInitializer(
    private val userRepository: UserEntityRepository,
    private val openingTypeRepository: OpeningTypeEntityRepository,
    private val openingRepository: OpeningEntityRepository,
) : CommandLineRunner {

    @Transactional
    override fun run(vararg args: String) {
        if (userRepository.count() > 0) return

        val adminUser = userRepository.save(
            UserEntity(
                authSub = "test-admin-sub",
                email = "admin@example.com",
                displayName = "Teszt Admin",
                role = UserRole.ADMIN,
                balance = 10000,
                requestReminders = true,
                isBanned = false
            )
        )

        val me = userRepository.save(
            UserEntity(
                authSub = "YOUR_AUTH_SUB",
                email = "me@example.com",
                displayName = "M E",
                role = UserRole.ADMIN,
                balance = 10000,
                requestReminders = true,
                isBanned = false
            )
        )

        val regularUser = userRepository.save(
            UserEntity(
                authSub = "test-user-sub",
                email = "user@example.com",
                displayName = "Teszt Elek",
                role = UserRole.USER,
                balance = 5000,
                requestReminders = true,
                isBanned = false
            )
        )

        val defaultType = openingTypeRepository.save(
            OpeningTypeEntity(
                name = "Normál Nyitás",
                description = "Általános hétköznapi nyitás",
                defaultPrice = 1500,
                active = true
            )
        )

        val specialType = openingTypeRepository.save(
            OpeningTypeEntity(
                name = "VIP / Zártkörű",
                description = "Privát szaunaest",
                defaultPrice = 3000,
                active = true
            )
        )

        val legacyType = openingTypeRepository.save(
            OpeningTypeEntity(
                name = "Régi Nyitás",
                description = "Már nem használt nyitástípus",
                defaultPrice = 1200,
                active = false
            )
        )

        val baseTime = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0).truncatedTo(ChronoUnit.MINUTES)

        val opening = OpeningEntity(
            openingStart = baseTime,
            openingEnd = baseTime.plusHours(3),
            isPrivate = false,
            price = defaultType.defaultPrice,
            status = OpeningStatus.SCHEDULED,
            hostedBy = adminUser,
            openingType = defaultType
        )

        val interval1 = OpeningIntervalEntity(
            start = baseTime,
            end = baseTime.plusHours(1),
            opening = opening,
            participantLimit = 8
        )

        val interval2 = OpeningIntervalEntity(
            start = baseTime.plusHours(1),
            end = baseTime.plusHours(2),
            opening = opening,
            participantLimit = 8
        )

        val interval3 = OpeningIntervalEntity(
            start = baseTime.plusHours(2),
            end = baseTime.plusHours(3),
            opening = opening,
            participantLimit = 8
        )

        opening.intervals.addAll(listOf(interval1, interval2, interval3))

        openingRepository.save(opening)

        val hosts = listOf(adminUser, me, adminUser, me, regularUser)
        val participantLimits = listOf(4, 6, 8, 8, 10, 12)
        val startHours = listOf(16, 17, 18, 19)
        val durations = listOf(90L, 120L, 180L, 240L)
        val today = LocalDate.now()
        val generated = mutableListOf<OpeningEntity>()

        (30L downTo 2L step 2).forEachIndexed { i, daysAgo ->
            val status = when {
                i % 5 == 4 -> OpeningStatus.CANCELLED
                i % 7 == 6 -> OpeningStatus.DELETED
                i % 6 == 3 -> OpeningStatus.SCHEDULED // elmúlt, de le nem zárt nyitás
                else -> OpeningStatus.COMPLETED
            }
            val type = when {
                i % 10 == 0 -> legacyType
                i % 3 == 2 -> specialType
                else -> defaultType
            }
            val duration = durations[i % durations.size]
            val start = today.minusDays(daysAgo).atTime(startHours[i % startHours.size], 0)
            val slots = slotLayout(i, duration)

            generated += buildOpening(
                start = start,
                durationMinutes = duration,
                host = hosts[i % hosts.size],
                type = type,
                status = status,
                isPrivate = type == specialType || i % 4 == 3,
                price = priceFor(i, type),
            ).withIntervals(slots, participantLimits[i % participantLimits.size], intervalStatusFor(status, i, slots.size))
        }

        val currentStart = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).minusHours(1)
        generated += buildOpening(
            start = currentStart,
            durationMinutes = 180,
            host = adminUser,
            type = defaultType,
            status = OpeningStatus.SCHEDULED,
        ).withIntervals(equalSlots(180, 3), 8)

        (2L..45L).forEachIndexed { i, daysAhead ->
            val day = today.plusDays(daysAhead)
            val status = when {
                i % 8 == 5 -> OpeningStatus.CANCELLED
                i % 11 == 10 -> OpeningStatus.DELETED
                else -> OpeningStatus.SCHEDULED
            }
            val type = if (i % 3 == 2) specialType else defaultType
            val duration = durations[(i + 1) % durations.size]
            val start = day.atTime(startHours[(i * 3) % startHours.size], if (i % 2 == 0) 0 else 30)
            val slots = slotLayout(i, duration)

            generated += buildOpening(
                start = start,
                durationMinutes = duration,
                host = hosts[i % hosts.size],
                type = type,
                status = status,
                isPrivate = type == specialType || i % 7 == 3,
                price = priceFor(i, type),
            ).withIntervals(slots, participantLimits[i % participantLimits.size], intervalStatusFor(status, i, slots.size))

            if (i % 5 == 0) {
                generated += buildOpening(
                    start = day.atTime(8, 0),
                    durationMinutes = 120,
                    host = hosts[(i + 1) % hosts.size],
                    type = defaultType,
                    status = OpeningStatus.SCHEDULED,
                    price = 1000,
                ).withIntervals(equalSlots(120, 2), 6)
            }

            if (i % 10 == 7) {
                val ghostStatus = if (i % 20 == 7) OpeningStatus.CANCELLED else OpeningStatus.DELETED
                generated += buildOpening(
                    start = start.minusMinutes(30),
                    durationMinutes = duration,
                    host = adminUser,
                    type = defaultType,
                    status = ghostStatus,
                ).withIntervals(equalSlots(duration, 3), 8, intervalStatusFor(ghostStatus, i, 3))
            }
        }

        openingRepository.saveAll(generated)
    }

    private fun buildOpening(
        start: LocalDateTime,
        durationMinutes: Long,
        host: UserEntity,
        type: OpeningTypeEntity,
        status: OpeningStatus,
        isPrivate: Boolean = false,
        price: Int = type.defaultPrice,
    ): OpeningEntity = OpeningEntity(
        openingStart = start.truncatedTo(ChronoUnit.MINUTES),
        openingEnd = start.plusMinutes(durationMinutes).truncatedTo(ChronoUnit.MINUTES),
        isPrivate = isPrivate,
        price = price,
        status = status,
        hostedBy = host,
        openingType = type,
    )

    private fun OpeningEntity.withIntervals(
        slots: List<Pair<Long, Long>>,
        participantLimit: Int,
        statusFor: (index: Int) -> IntervalStatus = { IntervalStatus.ACTIVE },
    ): OpeningEntity {
        slots.forEachIndexed { idx, (from, to) ->
            val interval = OpeningIntervalEntity(
                start = openingStart.plusMinutes(from),
                end = openingStart.plusMinutes(to),
                opening = this,
                participantLimit = participantLimit,
            )
            interval.status = statusFor(idx)
            intervals.add(interval)
        }
        return this
    }

    private fun equalSlots(durationMinutes: Long, count: Int): List<Pair<Long, Long>> {
        require(durationMinutes % count == 0L) { "$durationMinutes perc nem osztható $count részre" }
        val len = durationMinutes / count
        return (0 until count).map { it * len to (it + 1) * len }
    }

    private fun gapSlots(durationMinutes: Long): List<Pair<Long, Long>> {
        val third = durationMinutes / 3
        return listOf(0L to third, 2 * third to durationMinutes)
    }

    private fun slotLayout(index: Int, durationMinutes: Long): List<Pair<Long, Long>> = when (index % 6) {
        0, 1, 2 -> equalSlots(durationMinutes, 3)
        3 -> equalSlots(durationMinutes, 2)
        4 -> if (durationMinutes % 4 == 0L) equalSlots(durationMinutes, 4) else equalSlots(durationMinutes, 3)
        else -> if (index % 12 == 5) emptyList() else gapSlots(durationMinutes) // intervallum nélküli nyitás is legyen
    }

    private fun intervalStatusFor(openingStatus: OpeningStatus, index: Int, slotCount: Int): (Int) -> IntervalStatus =
        when (openingStatus) {
            OpeningStatus.CANCELLED -> { _ -> IntervalStatus.CANCELLED }
            OpeningStatus.DELETED -> { _ -> IntervalStatus.DELETED }
            OpeningStatus.COMPLETED -> { _ -> IntervalStatus.ACTIVE }
            OpeningStatus.SCHEDULED -> { idx ->
                when {
                    index % 6 == 1 && idx == slotCount - 1 -> IntervalStatus.CANCELLED
                    index % 9 == 2 && slotCount >= 3 && idx == 1 -> IntervalStatus.DELETED
                    else -> IntervalStatus.ACTIVE
                }
            }
        }

    private fun priceFor(index: Int, type: OpeningTypeEntity): Int = when {
        index % 9 == 4 -> 0                      // ingyenes nyitás
        index % 7 == 2 -> type.defaultPrice + 500 // egyedi ár
        else -> type.defaultPrice
    }

}