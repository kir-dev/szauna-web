package hu.kirdev.szaunaweb.test

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
import java.time.LocalDateTime

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

        val baseTime = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0)

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
    }

}