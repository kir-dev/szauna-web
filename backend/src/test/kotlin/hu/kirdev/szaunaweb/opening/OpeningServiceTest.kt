package hu.kirdev.szaunaweb.opening

import hu.kirdev.szaunaweb.exception.*
import hu.kirdev.szaunaweb.user.UserEntity
import hu.kirdev.szaunaweb.user.UserRole
import hu.kirdev.szaunaweb.user.UserService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpeningServiceTest {
    private val userService: UserService = mockk()
    private val openingTypeEntityRepository: OpeningTypeEntityRepository = mockk()
    private val openingEntityRepository: OpeningEntityRepository = mockk()
    private val adminAuthSub: String = "admin-auth-sub"

    private val service = OpeningService(
        userService = userService,
        openingRepository = openingEntityRepository,
        openingTypeRepository = openingTypeEntityRepository,
        admins = listOf(adminAuthSub),
    )

    private val dummyOpeningId = UUID.randomUUID()

    private fun createDummyUser(
        publicId: UUID = UUID.randomUUID(),
        authSub: String = "user-auth-sub",
        email: String = "teszt.elek@example.com",
        displayName: String = "Teszt Elek",
        role: UserRole = UserRole.USER
    ): UserEntity {
        return UserEntity(
            publicId = publicId,
            email = email,
            displayName = displayName,
            role = role,
            authSub = authSub
        )
    }

    private fun createDummyOpeningType(active: Boolean = true): OpeningTypeEntity {
        return OpeningTypeEntity(
            name = "Normál nyitás",
            active = active,
            defaultPrice = 2500,
            description = "Lorem ipsum dolor sit amet",
        )
    }

    private fun createDummyOpening(
        host: UserEntity,
        isPrivate: Boolean = false,
        status: OpeningStatus = OpeningStatus.SCHEDULED,
        start: LocalDateTime = LocalDateTime.now().plusDays(1),
        end: LocalDateTime = LocalDateTime.now().plusDays(1).plusHours(3)
    ): OpeningEntity {
        return OpeningEntity(
            publicId = dummyOpeningId,
            hostedBy = host,
            openingType = createDummyOpeningType(),
            price = 2500,
            isPrivate = isPrivate,
            status = status,
            openingStart = start,
            openingEnd = end,
            intervals = mutableListOf()
        )
    }

    private fun createDummyInterval(
        opening: OpeningEntity,
        publicId: UUID = UUID.randomUUID(),
        start: LocalDateTime,
        end: LocalDateTime,
        limit: Int = 8,
        status: IntervalStatus = IntervalStatus.ACTIVE,
        bookings: MutableList<OpeningBookingEntity> = mutableListOf()
    ): OpeningIntervalEntity {
        val interval = mockk<OpeningIntervalEntity>(relaxed = true)
        every { interval.publicId } returns publicId
        every { interval.intervalStart } returns start
        every { interval.intervalEnd } returns end
        every { interval.participantLimit } returns limit
        every { interval.status } returns status
        every { interval.bookings } returns bookings
        every { interval.overlapsWith(any(), any()) } answers {
            val otherStart = firstArg<LocalDateTime>()
            val otherEnd = secondArg<LocalDateTime>()
            start.isBefore(otherEnd) && end.isAfter(otherStart)
        }
        return interval
    }

    @Nested
    @DisplayName("getOpeningByPublicId")
    inner class GetOpeningByPublicIdTests {

        @Test
        @DisplayName("Anonymous user can view the public opening. (authSub == null)")
        fun `get public opening as anonymous user`() {
            val host = createDummyUser()
            val opening = createDummyOpening(host, isPrivate = false)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening

            val response = service.getOpeningByPublicId(authSub = null, publicId = dummyOpeningId)

            assertNotNull(response)
            assertEquals(dummyOpeningId, response.publicId)
            assertFalse(response.isPrivate)
            verify(exactly = 0) { userService.findByAuthSub(any()) }
        }

        @Test
        @DisplayName("Private opening with authSub == null, expected OpeningPermissionException")
        fun `get private opening without authSub`() {
            val host = createDummyUser()
            val opening = createDummyOpening(host, isPrivate = true)
            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            val ex = assertThrows<OpeningPermissionException> {
                service.getOpeningByPublicId(authSub = null, publicId = dummyOpeningId)
            }

            assertTrue(ex.message!!.contains("This is a private opening, please login to verify your access!"))
        }

        @Test
        @DisplayName("Private opening with normal USER role.")
        fun `get private opening with normal user`() {
            val host = createDummyUser(authSub = "host-sub")
            val normalUser = createDummyUser(authSub = "normal-sub", role = UserRole.USER)
            val opening = createDummyOpening(host, isPrivate = true)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("normal-sub") } returns normalUser

            val ex = assertThrows<OpeningPermissionException> {
                service.getOpeningByPublicId(authSub = "normal-sub", publicId = dummyOpeningId)
            }

            assertTrue(ex.message!!.contains("You do not have permission to access this private opening!"))
        }

        @Test
        @DisplayName("Private opening with SAUNA_MASTER role.")
        fun `get private opening with SAUNA_MASTER role`() {
            val host = createDummyUser(authSub = "host-sub")
            val saunaMaster = createDummyUser(authSub = "sauna-master-sub", role = UserRole.SAUNA_MASTER)
            val opening = createDummyOpening(host, isPrivate = true)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("sauna-master-sub") } returns saunaMaster

            val response = service.getOpeningByPublicId(authSub = "sauna-master-sub", publicId = dummyOpeningId)

            assertNotNull(response)
            assertTrue(response.isPrivate)
        }

        @Test
        @DisplayName("Private opening with ADMIN role.")
        fun `get private opening with ADMIN role`() {
            val host = createDummyUser(authSub = "host-sub")
            val saunaMaster = createDummyUser(authSub = adminAuthSub, role = UserRole.USER)
            val opening = createDummyOpening(host, isPrivate = true)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub(adminAuthSub) } returns saunaMaster

            val response = service.getOpeningByPublicId(authSub = adminAuthSub, publicId = dummyOpeningId)

            assertNotNull(response)
            assertTrue(response.isPrivate)
        }

        @Test
        @DisplayName("Opening not found, expected OpeningNotFoundException")
        fun `opening not found`() {
            every { openingEntityRepository.findByPublicId(any()) } returns null
            assertThrows<OpeningNotFoundException> {
                service.getOpeningByPublicId(authSub = null, publicId = dummyOpeningId)
            }
        }

    }

    @Nested
    @DisplayName("Get openings with Pageable")
    inner class GetOpeningsTest {

        @Test
        @DisplayName("Anonymous user with hasAccessAll = false.")
        fun `anonymous user with hasAccessAll false`() {
            val pageable = PageRequest.of(0, 10)
            val emptyPage = PageImpl<OpeningEntity>(emptyList())

            every {
                openingEntityRepository.findOpeningWithFilter(
                    status = any(),
                    from = any(),
                    to = any(),
                    pageable = pageable,
                    hasAccessToAll = false
                )
            } returns emptyPage

            val result = service.getOpenings(
                authSub = null,
                pageable = pageable,
                status = null,
                from = null,
                to = null
            )

            assertNotNull(result)
            verify(exactly = 1) {
                openingEntityRepository.findOpeningWithFilter(null, null, null, pageable, false)
            }
        }

        @Test
        @DisplayName("Admin user with hasAccessAll = true.")
        fun `admin search with hasAccessToAll true`() {
            val pageable = PageRequest.of(0, 10)
            val adminUser = createDummyUser(authSub = adminAuthSub, role = UserRole.USER)
            val emptyPage = PageImpl<OpeningEntity>(emptyList())

            every { userService.findByAuthSub(adminAuthSub) } returns adminUser
            every {
                openingEntityRepository.findOpeningWithFilter(
                    status = any(),
                    from = any(),
                    to = any(),
                    pageable = pageable,
                    hasAccessToAll = true
                )
            } returns emptyPage

            val result = service.getOpenings(
                authSub = adminAuthSub,
                pageable = pageable,
                status = null,
                from = null,
                to = null
            )

            assertNotNull(result)
            verify(exactly = 1) {
                openingEntityRepository.findOpeningWithFilter(null, null, null, pageable, true)
            }
        }

    }

    @Nested
    @DisplayName("getCurrentOpening")
    inner class GetCurrentOpeningTests {

        @Test
        @DisplayName("Returns the current or next opening")
        fun `get current or next opening`() {
            val host = createDummyUser()
            val opening = createDummyOpening(host = host)
            every { openingEntityRepository.findCurrentOrNextOpening(any()) } returns opening

            val response = service.getCurrentOpening()

            assertNotNull(response)
            assertEquals(dummyOpeningId, response?.publicId)
            verify(exactly = 1) { openingEntityRepository.findCurrentOrNextOpening(any()) }
        }

        @Test
        @DisplayName("Returns null if no opening is found")
        fun `no current opening found`() {
            every { openingEntityRepository.findCurrentOrNextOpening(any()) } returns null

            val response = service.getCurrentOpening()

            assertNull(response)
        }
    }

    @Nested
    @DisplayName("createOpening")
    inner class CreateOpeningTest {

        @Test
        @DisplayName("Duration not divisible by 3, expected OpeningException")
        fun `duration not divisible into default intervals`() {
            val start = LocalDateTime.now().plusDays(2)
            val end = start.plusMinutes(100) // 100 % 3 != 0
            val dto = CreateOpeningRequest(
                openingStart = start,
                openingEnd = end,
                openingTypeId = 1L,
                generateDefaultIntervals = true
            )

            every { userService.findByAuthSub("hostAuthSub") } returns createDummyUser(authSub = "hostAuthSub")
            every {
                openingEntityRepository.existsByStatusNotInAndOpeningStartLessThanAndOpeningEndGreaterThan(any(), any(), any())
            } returns false
            every { openingTypeEntityRepository.findById(1L) } returns Optional.of(createDummyOpeningType(active = true))

            assertThrows<OpeningException> {
                service.createOpening("hostAuthSub", dto)
            }
            verify(exactly = 0) { openingEntityRepository.saveAndFlush(any()) }
        }

        @Test
        @DisplayName("Opening type not found, expected OpeningNotFoundException")
        fun `opening type not found`() {
            val start = LocalDateTime.now().plusDays(2)
            val dto = CreateOpeningRequest(
                openingStart = start,
                openingEnd = start.plusHours(3),
                openingTypeId = 99L,
                generateDefaultIntervals = false
            )

            every { userService.findByAuthSub("hostAuthSub") } returns createDummyUser(authSub = "hostAuthSub")
            every {
                openingEntityRepository.existsByStatusNotInAndOpeningStartLessThanAndOpeningEndGreaterThan(any(), any(), any())
            } returns false
            every { openingTypeEntityRepository.findById(99L) } returns Optional.empty()

            assertThrows<OpeningNotFoundException> {
                service.createOpening("hostAuthSub", dto)
            }
        }

        // A meglévő "create successfully with default intervals" teszt HELYETT (az intervallumok
// tényleges időpontjait is ellenőrzi, nem csak a darabszámot):
        @Test
        @DisplayName("Successful creation (generateDefaultIntervals = true) splits the range into 3 equal slots")
        fun `create successfully with default intervals`() {
            val start = LocalDateTime.now().plusDays(2)
            val end = start.plusMinutes(180)
            val dto = CreateOpeningRequest(
                openingStart = start,
                openingEnd = end,
                openingTypeId = 1L,
                generateDefaultIntervals = true
            )
            val host = createDummyUser(authSub = "hostAuthSub")

            every { userService.findByAuthSub("hostAuthSub") } returns host
            every {
                openingEntityRepository.existsByStatusNotInAndOpeningStartLessThanAndOpeningEndGreaterThan(any(), any(), any())
            } returns false
            every { openingTypeEntityRepository.findById(1L) } returns Optional.of(createDummyOpeningType(active = true))

            var savedEntity: OpeningEntity? = null
            every { openingEntityRepository.saveAndFlush(any()) } answers {
                val entity = firstArg<OpeningEntity>()
                entity.publicId = dummyOpeningId
                entity.intervals.forEach { interval ->
                    if (interval.publicId == null) interval.publicId = UUID.randomUUID()
                }
                savedEntity = entity
                entity
            }

            val response = service.createOpening("hostAuthSub", dto)

            assertNotNull(response)
            assertEquals(3, response.intervals.size)

            val intervals = savedEntity!!.intervals
            assertEquals(start, intervals[0].intervalStart)
            assertEquals(start.plusMinutes(60), intervals[0].intervalEnd)
            assertEquals(start.plusMinutes(60), intervals[1].intervalStart)
            assertEquals(start.plusMinutes(120), intervals[1].intervalEnd)
            assertEquals(start.plusMinutes(120), intervals[2].intervalStart)
            assertEquals(end, intervals[2].intervalEnd)
            intervals.forEach { assertEquals(OpeningService.DEFAULT_PARTICIPANT_LIMIT, it.participantLimit) }
        }


        @Test
        @DisplayName("Start in the past, expected InvalidTimeRangeException")
        fun `start in the past with exception`() {
            val dto = CreateOpeningRequest(
                openingStart = LocalDateTime.now().minusHours(2),
                openingEnd = LocalDateTime.now().plusHours(2),
                openingTypeId = 1L
            )

            assertThrows<InvalidTimeRangeException> {
                service.createOpening("hostAuthSub", dto)
            }
        }

        @Test
        @DisplayName("The opening ends before it starts.")
        fun `ends before it starts with exception`() {
            val now = LocalDateTime.now().plusDays(1)
            val dto = CreateOpeningRequest(
                openingStart = now.plusHours(4),
                openingEnd = now.plusHours(2),
                openingTypeId = 1L
            )

            assertThrows<InvalidTimeRangeException> {
                service.createOpening("hostAuthSub", dto)
            }
        }

        @Test
        @DisplayName("Overlap with other opening, expected TimeRangeConflictException")
        fun `overlapped opening with exception`() {
            val start = LocalDateTime.now().plusDays(2)
            val end = start.plusHours(3)
            val dto = CreateOpeningRequest(
                openingStart = start,
                openingEnd = end,
                openingTypeId = 1L
            )

            every { userService.findByAuthSub("hostAuthSub") } returns createDummyUser(authSub = "hostAuthSub")
            every {
                openingEntityRepository.existsByStatusNotInAndOpeningStartLessThanAndOpeningEndGreaterThan(
                    statuses = OpeningService.IGNORED_STATUSES_FOR_OVERLAP,
                    end = end,
                    start = start
                )
            } returns true

            assertThrows<TimeRangeConflictException> {
                service.createOpening("hostAuthSub", dto)
            }
        }

        @Test
        @DisplayName("Inactive opening type, expected OpeningException")
        fun `inactive opening type with exception`() {
            val start = LocalDateTime.now().plusDays(2)
            val end = start.plusHours(3)
            val dto = CreateOpeningRequest(
                openingStart = start,
                openingEnd = end,
                openingTypeId = 1L,
                generateDefaultIntervals = false
            )

            every { userService.findByAuthSub("hostAuthSub") } returns createDummyUser(authSub = "hostAuthSub")
            every {
                openingEntityRepository.existsByStatusNotInAndOpeningStartLessThanAndOpeningEndGreaterThan(
                    any(),
                    any(),
                    any()
                )
            } returns false
            every { openingTypeEntityRepository.findById(1L) } returns Optional.of(createDummyOpeningType(active = false))

            assertThrows<OpeningException> {
                service.createOpening("hostAuthSub", dto)
            }
        }

        @Test
        @DisplayName("Successful creation (generateDefaultIntervals = false)")
        fun `create successfully without default intervals`() {
            val start = LocalDateTime.now().plusDays(2)
            val end = start.plusHours(3)
            val dto = CreateOpeningRequest(
                openingStart = start,
                openingEnd = end,
                openingTypeId = 1L,
                generateDefaultIntervals = false
            )
            val host = createDummyUser(authSub = "hostAuthSub")
            val openingType = createDummyOpeningType(active = true)

            every { userService.findByAuthSub("hostAuthSub") } returns host
            every {
                openingEntityRepository.existsByStatusNotInAndOpeningStartLessThanAndOpeningEndGreaterThan(
                    any(),
                    any(),
                    any()
                )
            } returns false
            every { openingTypeEntityRepository.findById(1L) } returns Optional.of(openingType)
            every { openingEntityRepository.saveAndFlush(any()) } answers {
                val entity = firstArg<OpeningEntity>()
                entity.publicId = dummyOpeningId
                entity
            }

            val response = service.createOpening("hostAuthSub", dto)

            assertNotNull(response)
            assertEquals(dummyOpeningId, response.publicId)
            verify(exactly = 1) { openingEntityRepository.saveAndFlush(any()) }
        }


    }

    @Nested
    @DisplayName("deleteOpening")
    inner class DeleteOpening {


        @Test
        @DisplayName("Delete opening marks intervals as DELETED and cancels active bookings")
        fun `delete opening cascades to intervals and bookings`() {
            val adminUser = createDummyUser(authSub = adminAuthSub)
            val opening = createDummyOpening(host = adminUser, status = OpeningStatus.SCHEDULED)
            val booking = mockk<OpeningBookingEntity>(relaxed = true) {
                every { status } returns BookingStatus.ACTIVE
            }
            val interval = createDummyInterval(
                opening = opening,
                start = opening.openingStart,
                end = opening.openingStart.plusMinutes(60),
                bookings = mutableListOf(booking)
            )
            opening.intervals.add(interval)

            every { userService.findByAuthSub(adminAuthSub) } returns adminUser
            every { openingEntityRepository.findByPublicId(any()) } returns opening
            every { openingEntityRepository.save(any()) } answers { firstArg() }

            service.deleteOpening(adminAuthSub, dummyOpeningId)

            verify(exactly = 1) { interval.status = IntervalStatus.DELETED }
            verify(exactly = 1) { booking.status = BookingStatus.CANCELLED }
            assertEquals(OpeningStatus.DELETED, opening.status)
        }


        @Test
        @DisplayName("Not admin user delete opening (OpeningPermissionException)")
        fun `not admin user delete opening`() {
            val regularUser = createDummyUser(authSub = "regularAuthSub")
            every { userService.findByAuthSub("regularAuthSub") } returns regularUser

            assertThrows<OpeningPermissionException> {
                service.deleteOpening("regularAuthSub", dummyOpeningId)
            }
        }

        @Test
        @DisplayName("Delete already completed opening")
        fun `already completed opening deletion`() {
            val adminUser = createDummyUser(authSub = adminAuthSub)
            val opening = createDummyOpening(host = adminUser, status = OpeningStatus.COMPLETED)

            every { userService.findByAuthSub(adminAuthSub) } returns adminUser
            every { openingEntityRepository.findByPublicId(any()) } returns opening

            assertThrows<OpeningException> {
                service.deleteOpening(adminAuthSub, dummyOpeningId)
            }
        }

        @Test
        @DisplayName("Successfully deleted opening")
        fun `successfully deleted opening by admin`() {
            val adminUser = createDummyUser(authSub = adminAuthSub)
            val opening = createDummyOpening(host = adminUser, status = OpeningStatus.SCHEDULED)

            every { userService.findByAuthSub(adminAuthSub) } returns adminUser
            every { openingEntityRepository.findByPublicId(any()) } returns opening
            every { openingEntityRepository.save(any()) } answers { firstArg() }

            service.deleteOpening(adminAuthSub, dummyOpeningId)

            assertEquals(OpeningStatus.DELETED, opening.status)
            verify(exactly = 1) { openingEntityRepository.save(opening) }
        }

    }

    @Nested
    @DisplayName("createInterval")
    inner class CreateIntervalTests {


        @Test
        @DisplayName("Interval ending exactly at the opening end is valid")
        fun `interval can end exactly at opening end`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host
            every { openingEntityRepository.saveAndFlush(any<OpeningEntity>()) } answers {
                val entity = firstArg<OpeningEntity>()
                entity.intervals.forEach { if (it.publicId == null) it.publicId = UUID.randomUUID() }
                entity
            }

            val request = CreateIntervalRequest(
                intervalStart = opening.openingEnd.minusMinutes(60),
                intervalEnd = opening.openingEnd,
                participantLimit = 8
            )

            service.createInterval("hostAuthSub", request, dummyOpeningId)

            assertEquals(1, opening.intervals.size)
        }

        @Test
        @DisplayName("CANCELLED interval does not count as overlap")
        fun `cancelled interval does not block new interval`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)
            opening.intervals.add(
                createDummyInterval(
                    opening = opening,
                    start = opening.openingStart,
                    end = opening.openingStart.plusMinutes(60),
                    status = IntervalStatus.CANCELLED
                )
            )

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host
            every { openingEntityRepository.saveAndFlush(any<OpeningEntity>()) } answers {
                val entity = firstArg<OpeningEntity>()
                entity.intervals.forEach { if (it.publicId == null) it.publicId = UUID.randomUUID() }
                entity
            }

            val request = CreateIntervalRequest(
                intervalStart = opening.openingStart.plusMinutes(10),
                intervalEnd = opening.openingStart.plusMinutes(50),
                participantLimit = 8
            )

            service.createInterval("hostAuthSub", request, dummyOpeningId)

            assertEquals(2, opening.intervals.size)
        }


        @Test
        @DisplayName("Create interval without admin or  host permission.")
        fun `without admin or host permission create interval`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val intruder = createDummyUser(authSub = "regularAuthSub")
            val opening = createDummyOpening(host = host)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("regularAuthSub") } returns intruder

            val request = CreateIntervalRequest(
                intervalStart = opening.openingStart.plusMinutes(10),
                intervalEnd = opening.openingStart.plusMinutes(70),
                participantLimit = 8
            )

            assertThrows<OpeningPermissionException> {
                service.createInterval("regularAuthSub", request, dummyOpeningId)
            }
        }

        @Test
        @DisplayName("Interval is out of opening range")
        fun `interval is out of opening range`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host

            val invalidRequest = CreateIntervalRequest(
                intervalStart = opening.openingStart.minusMinutes(30),
                intervalEnd = opening.openingStart.plusMinutes(30),
                participantLimit = 8
            )

            assertThrows<InvalidTimeRangeException> {
                service.createInterval("hostAuthSub", invalidRequest, dummyOpeningId)
            }
        }

        @Test
        @DisplayName("Interval overlaps with other intervals")
        fun `overlapped interval`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)
            val existingInterval = createDummyInterval(
                opening = opening,
                start = opening.openingStart,
                end = opening.openingStart.plusMinutes(60)
            )
            opening.intervals.add(existingInterval)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host

            val overlappingRequest = CreateIntervalRequest(
                intervalStart = opening.openingStart.plusMinutes(30),
                intervalEnd = opening.openingStart.plusMinutes(90),
                participantLimit = 8
            )

            assertThrows<TimeRangeConflictException> {
                service.createInterval("hostAuthSub", overlappingRequest, dummyOpeningId)
            }
        }

        @Test
        @DisplayName("Successfully created interval")
        fun `interval created successfully`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host

            every { openingEntityRepository.saveAndFlush(any<OpeningEntity>()) } answers {
                val entity = firstArg<OpeningEntity>()
                entity.intervals.forEach { interval ->
                    if (interval.publicId == null) {
                        interval.publicId = UUID.randomUUID()
                    }
                }
                entity
            }

            val validRequest = CreateIntervalRequest(
                intervalStart = opening.openingStart.plusMinutes(10),
                intervalEnd = opening.openingStart.plusMinutes(70),
                participantLimit = 8
            )

            val response = service.createInterval("hostAuthSub", validRequest, dummyOpeningId)

            assertNotNull(response)
            assertEquals(1, opening.intervals.size)
            verify(exactly = 1) { openingEntityRepository.saveAndFlush(opening) }
        }
    }

    @Nested
    @DisplayName("updateOpening")
    inner class UpdateOpeningTests {

        @Test
        @DisplayName("Not host or admin tries to update, throws OpeningPermissionException")
        fun `unauthorized update throws exception`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val intruder = createDummyUser(authSub = "intruderAuthSub")
            val opening = createDummyOpening(host = host)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("intruderAuthSub") } returns intruder

            val request = UpdateOpeningRequest(
                openingStart = opening.openingStart,
                openingEnd = opening.openingEnd,
                openingTypeId = 1L,
                price = null
            )

            assertThrows<OpeningPermissionException> {
                service.updateOpening("intruderAuthSub", request, dummyOpeningId)
            }
            verify(exactly = 0) { openingEntityRepository.saveAndFlush(any()) }
        }

        @Test
        @DisplayName("Update with start in the past, expected InvalidTimeRangeException")
        fun `update with past start`() {
            val request = UpdateOpeningRequest(
                openingStart = LocalDateTime.now().minusHours(1),
                openingEnd = LocalDateTime.now().plusHours(2),
                openingTypeId = 1L,
                price = null
            )

            assertThrows<InvalidTimeRangeException> {
                service.updateOpening("hostAuthSub", request, dummyOpeningId)
            }
        }

        @Test
        @DisplayName("Cannot delay opening start past the first active interval")
        fun `cannot delay opening start after first interval`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)
            opening.intervals.add(
                createDummyInterval(
                    opening = opening,
                    start = opening.openingStart,
                    end = opening.openingStart.plusMinutes(60)
                )
            )

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host
            every {
                openingEntityRepository.existsByPublicIdNotAndStatusNotInAndOpeningStartLessThanAndOpeningEndGreaterThan(any(), any(), any(), any())
            } returns false

            val request = UpdateOpeningRequest(
                openingStart = opening.openingStart.plusMinutes(10), // késleltetés
                openingEnd = opening.openingEnd,
                openingTypeId = 1L,
                price = null
            )

            assertThrows<TimeRangeConflictException> {
                service.updateOpening("hostAuthSub", request, dummyOpeningId)
            }
        }

        @Test
        @DisplayName("Cannot shorten opening end before the last active interval")
        fun `cannot shorten opening end before last interval`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)
            opening.intervals.add(
                createDummyInterval(
                    opening = opening,
                    start = opening.openingEnd.minusMinutes(60),
                    end = opening.openingEnd
                )
            )

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host
            every {
                openingEntityRepository.existsByPublicIdNotAndStatusNotInAndOpeningStartLessThanAndOpeningEndGreaterThan(any(), any(), any(), any())
            } returns false

            val request = UpdateOpeningRequest(
                openingStart = opening.openingStart,
                openingEnd = opening.openingEnd.minusMinutes(10), // rövidítés
                openingTypeId = 1L,
                price = null
            )

            assertThrows<TimeRangeConflictException> {
                service.updateOpening("hostAuthSub", request, dummyOpeningId)
            }
        }

        @Test
        @DisplayName("Overlap with another opening on time change, expected TimeRangeConflictException")
        fun `update overlapping with another opening`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every {
                openingEntityRepository.existsByPublicIdNotAndStatusNotInAndOpeningStartLessThanAndOpeningEndGreaterThan(any(), any(), any(), any())
            } returns true

            val request = UpdateOpeningRequest(
                openingStart = opening.openingStart.plusHours(1),
                openingEnd = opening.openingEnd.plusHours(1),
                openingTypeId = 1L,
                price = null
            )

            assertThrows<TimeRangeConflictException> {
                service.updateOpening("hostAuthSub", request, dummyOpeningId)
            }
        }

        @Test
        @DisplayName("Inactive new opening type, expected OpeningException")
        fun `update to inactive opening type`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host
            every { openingTypeEntityRepository.findById(2L) } returns Optional.of(createDummyOpeningType(active = false))

            val request = UpdateOpeningRequest(
                openingStart = opening.openingStart,
                openingEnd = opening.openingEnd,
                openingTypeId = 2L,
                price = null
            )

            assertThrows<OpeningException> {
                service.updateOpening("hostAuthSub", request, dummyOpeningId)
            }
        }

        @Test
        @DisplayName("price == null resets the price to the opening type default")
        fun `update with null price falls back to default price`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)
            opening.price = 3000 // eltér a típus default árától (2500)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host
            every { openingTypeEntityRepository.findById(any()) } returns Optional.of(createDummyOpeningType(active = true))
            every { openingEntityRepository.saveAndFlush(any()) } answers { firstArg() }

            val request = UpdateOpeningRequest(
                openingStart = opening.openingStart,
                openingEnd = opening.openingEnd,
                openingTypeId = 1L,
                price = null
            )

            service.updateOpening("hostAuthSub", request, dummyOpeningId)

            assertEquals(2500, opening.price)
        }

        @Test
        @DisplayName("Update completed opening throws OpeningException")
        fun `update completed opening throws exception`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host, status = OpeningStatus.COMPLETED)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host

            val request = UpdateOpeningRequest(
                openingStart = LocalDateTime.now().plusDays(2),
                openingEnd = LocalDateTime.now().plusDays(2).plusHours(2),
                openingTypeId = 1L,
                price = null
            )

            assertThrows<OpeningException> {
                service.updateOpening("hostAuthSub", request, dummyOpeningId)
            }
        }

        @Test
        @DisplayName("Successfully update opening details")
        fun `successful update opening`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)
            val newType = createDummyOpeningType(active = true)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host
            every {
                openingEntityRepository.existsByPublicIdNotAndStatusNotInAndOpeningStartLessThanAndOpeningEndGreaterThan(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns false
            every { openingTypeEntityRepository.findById(2L) } returns Optional.of(newType)
            every { openingEntityRepository.saveAndFlush(any()) } answers { firstArg() }

            val request = UpdateOpeningRequest(
                openingStart = LocalDateTime.now().plusDays(3),
                openingEnd = LocalDateTime.now().plusDays(3).plusHours(4),
                openingTypeId = 2L,
                price = 3000
            )

            val response = service.updateOpening("hostAuthSub", request, dummyOpeningId)

            assertNotNull(response)
            assertEquals(3000, opening.price)
            assertEquals(newType, opening.openingType)
            verify(exactly = 1) { openingEntityRepository.saveAndFlush(opening) }
        }
    }

    @Nested
    @DisplayName("updateOpeningStatus")
    inner class UpdateOpeningStatusTests {


        @Test
        @DisplayName("Re-scheduling a cancelled opening re-activates its cancelled intervals")
        fun `schedule opening reactivates cancelled intervals`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host, status = OpeningStatus.CANCELLED)
            val interval = createDummyInterval(
                opening = opening,
                start = opening.openingStart,
                end = opening.openingStart.plusMinutes(60),
                status = IntervalStatus.CANCELLED
            )
            opening.intervals.add(interval)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host
            every { openingEntityRepository.saveAndFlush(any()) } answers { firstArg() }

            service.updateOpeningStatus("hostAuthSub", UpdateOpeningStatusRequest(status = OpeningStatus.SCHEDULED), dummyOpeningId)

            assertEquals(OpeningStatus.SCHEDULED, opening.status)
            verify(exactly = 1) { interval.status = IntervalStatus.ACTIVE }
        }

        @Test
        @DisplayName("Invalid target status (DELETED) throws OpeningException")
        fun `update opening status to deleted is rejected`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host

            assertThrows<OpeningException> {
                service.updateOpeningStatus("hostAuthSub", UpdateOpeningStatusRequest(status = OpeningStatus.DELETED), dummyOpeningId)
            }
            verify(exactly = 0) { openingEntityRepository.saveAndFlush(any()) }
        }


        @Test
        @DisplayName("Cancel opening cancels all active intervals and bookings")
        fun `cancel opening cancels intervals`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)
            val booking = mockk<OpeningBookingEntity>(relaxed = true) {
                every { status } returns BookingStatus.ACTIVE
            }
            val interval = createDummyInterval(
                opening = opening,
                start = opening.openingStart,
                end = opening.openingStart.plusMinutes(60),
                status = IntervalStatus.ACTIVE,
                bookings = mutableListOf(booking)
            )
            opening.intervals.add(interval)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host
            every { openingEntityRepository.saveAndFlush(any()) } answers { firstArg() }

            val request = UpdateOpeningStatusRequest(status = OpeningStatus.CANCELLED)

            service.updateOpeningStatus("hostAuthSub", request, dummyOpeningId)

            assertEquals(OpeningStatus.CANCELLED, opening.status)
            verify(exactly = 1) { interval.status = IntervalStatus.CANCELLED }
            verify(exactly = 1) { booking.status = BookingStatus.CANCELLED }
        }

        @Test
        @DisplayName("Complete opening updates status correctly")
        fun `complete opening`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host
            every { openingEntityRepository.saveAndFlush(any()) } answers { firstArg() }

            val request = UpdateOpeningStatusRequest(status = OpeningStatus.COMPLETED)

            service.updateOpeningStatus("hostAuthSub", request, dummyOpeningId)

            assertEquals(OpeningStatus.COMPLETED, opening.status)
            verify(exactly = 1) { openingEntityRepository.saveAndFlush(opening) }
        }
    }

    @Nested
    @DisplayName("updateInterval")
    inner class UpdateIntervalTests {

        private val intervalId = UUID.randomUUID()


        @Test
        @DisplayName("Interval not found, expected IntervalNotFoundException")
        fun `update non existing interval`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host

            val request = UpdateIntervalRequest(
                intervalStart = opening.openingStart,
                intervalEnd = opening.openingStart.plusMinutes(60),
                participantLimit = 8
            )

            assertThrows<IntervalNotFoundException> {
                service.updateInterval("hostAuthSub", request, dummyOpeningId, intervalId)
            }
        }

        @Test
        @DisplayName("New participant limit equal to booked seats is allowed (boundary)")
        fun `new participant limit equal to booked seats`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            val activeBooking = mockk<OpeningBookingEntity>(relaxed = true) {
                every { status } returns BookingStatus.ACTIVE
                every { seatCount } returns 5
            }
            val interval = createDummyInterval(
                opening = opening,
                publicId = intervalId,
                start = opening.openingStart,
                end = opening.openingStart.plusMinutes(60),
                limit = 8,
                bookings = mutableListOf(activeBooking)
            )
            opening.intervals.add(interval)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host
            every { openingEntityRepository.saveAndFlush(any()) } answers { firstArg() }

            val request = UpdateIntervalRequest(
                intervalStart = opening.openingStart,
                intervalEnd = opening.openingStart.plusMinutes(60),
                participantLimit = 5 // pontosan annyi, mint a foglalt helyek
            )

            service.updateInterval("hostAuthSub", request, dummyOpeningId, intervalId)

            verify(exactly = 1) { interval.participantLimit = 5 }
        }


        @Test
        @DisplayName("Modify completed opening")
        fun `completed opening status cannot modified`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host, status = OpeningStatus.COMPLETED)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host

            val request = UpdateIntervalRequest(
                intervalStart = opening.openingStart,
                intervalEnd = opening.openingStart.plusMinutes(60),
                participantLimit = 10
            )

            assertThrows<OpeningException> {
                service.updateInterval("hostAuthSub", request, dummyOpeningId, intervalId)
            }
        }

        @Test
        @DisplayName("New participant limit is less then the booked seats")
        fun `new participant limit is less then the booked seats`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            val activeBooking = mockk<OpeningBookingEntity> {
                every { status } returns BookingStatus.ACTIVE
                every { seatCount } returns 5
            }
            val interval = createDummyInterval(
                opening = opening,
                publicId = intervalId,
                start = opening.openingStart,
                end = opening.openingStart.plusMinutes(60),
                limit = 8,
                bookings = mutableListOf(activeBooking)
            )
            opening.intervals.add(interval)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host

            val request = UpdateIntervalRequest(
                intervalStart = opening.openingStart,
                intervalEnd = opening.openingStart.plusMinutes(60),
                participantLimit = 4
            )

            assertThrows<OpeningException> {
                service.updateInterval("hostAuthSub", request, dummyOpeningId, intervalId)
            }
        }

        @Test
        @DisplayName("Modified interval overlaps with other active intervals.")
        fun `modified interval overlaps`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            val intervalToUpdate = createDummyInterval(
                opening = opening,
                publicId = intervalId,
                start = opening.openingStart,
                end = opening.openingStart.plusMinutes(60)
            )
            val anotherInterval = createDummyInterval(
                opening = opening,
                publicId = UUID.randomUUID(),
                start = opening.openingStart.plusMinutes(60),
                end = opening.openingStart.plusMinutes(120)
            )
            opening.intervals.addAll(listOf(intervalToUpdate, anotherInterval))

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host

            val request = UpdateIntervalRequest(
                intervalStart = opening.openingStart.plusMinutes(30),
                intervalEnd = opening.openingStart.plusMinutes(90),
                participantLimit = 8
            )

            assertThrows<TimeRangeConflictException> {
                service.updateInterval("hostAuthSub", request, dummyOpeningId, intervalId)
            }
        }
    }


    @Nested
    @DisplayName("updateIntervalStatus and deleteInterval")
    inner class StatusAndDeletionTests {

        private val intervalId = UUID.randomUUID()

        @Test
        @DisplayName("After the intervals became cancelled, the bookings also become cancelled")
        fun `intervals and bookings became cancelled`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            val booking = mockk<OpeningBookingEntity>(relaxed = true) {
                every { status } returns BookingStatus.ACTIVE
            }
            val interval = createDummyInterval(
                opening = opening,
                publicId = intervalId,
                start = opening.openingStart,
                end = opening.openingStart.plusMinutes(60),
                status = IntervalStatus.ACTIVE,
                bookings = mutableListOf(booking)
            )
            opening.intervals.add(interval)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host
            every { openingEntityRepository.saveAndFlush(any<OpeningEntity>()) } answers { firstArg() }

            val request = UpdateIntervalStatusRequest(status = IntervalStatus.CANCELLED)

            service.updateIntervalStatus("hostAuthSub", request, dummyOpeningId, intervalId)

            verify(exactly = 1) { interval.status = IntervalStatus.CANCELLED }
            verify(exactly = 1) { booking.status = BookingStatus.CANCELLED }
        }

        @Test
        @DisplayName("Delete interval which has active bookings")
        fun `interval with active bookings cannot deleted`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            val activeBooking = mockk<OpeningBookingEntity> {
                every { status } returns BookingStatus.ACTIVE
            }
            val interval = createDummyInterval(
                opening = opening,
                publicId = intervalId,
                start = opening.openingStart,
                end = opening.openingStart.plusMinutes(60),
                bookings = mutableListOf(activeBooking)
            )
            opening.intervals.add(interval)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host

            assertThrows<OpeningException> {
                service.deleteInterval("hostAuthSub", dummyOpeningId, intervalId)
            }
        }

        @Test
        @DisplayName("Successfully delete interval which has no active bookings")
        fun `successfully delete empty interval`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            val interval = createDummyInterval(
                opening = opening,
                publicId = intervalId,
                start = opening.openingStart,
                end = opening.openingStart.plusMinutes(60),
                status = IntervalStatus.ACTIVE,
                bookings = mutableListOf()
            )
            opening.intervals.add(interval)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host
            every { openingEntityRepository.saveAndFlush(any<OpeningEntity>()) } answers { firstArg() }

            service.deleteInterval("hostAuthSub", dummyOpeningId, intervalId)

            verify(exactly = 1) { interval.status = IntervalStatus.DELETED }
            verify(exactly = 1) { openingEntityRepository.saveAndFlush(opening) }
        }


        @Test
        @DisplayName("Re-activating a cancelled interval")
        fun `cancelled interval can be reactivated`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)
            val interval = createDummyInterval(
                opening = opening,
                publicId = intervalId,
                start = opening.openingStart,
                end = opening.openingStart.plusMinutes(60),
                status = IntervalStatus.CANCELLED
            )
            opening.intervals.add(interval)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host
            every { openingEntityRepository.saveAndFlush(any<OpeningEntity>()) } answers { firstArg() }

            service.updateIntervalStatus("hostAuthSub", UpdateIntervalStatusRequest(status = IntervalStatus.ACTIVE), dummyOpeningId, intervalId)

            verify(exactly = 1) { interval.status = IntervalStatus.ACTIVE }
        }

        @Test
        @DisplayName("Activating an interval that is not cancelled, expected IntervalNotFoundException")
        fun `activate interval that is not cancelled`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)
            opening.intervals.add(
                createDummyInterval(
                    opening = opening,
                    publicId = intervalId,
                    start = opening.openingStart,
                    end = opening.openingStart.plusMinutes(60),
                    status = IntervalStatus.ACTIVE
                )
            )

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host

            assertThrows<IntervalNotFoundException> {
                service.updateIntervalStatus("hostAuthSub", UpdateIntervalStatusRequest(status = IntervalStatus.ACTIVE), dummyOpeningId, intervalId)
            }
        }

        @Test
        @DisplayName("Delete non existing interval, expected IntervalNotFoundException")
        fun `delete non existing interval`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host

            assertThrows<IntervalNotFoundException> {
                service.deleteInterval("hostAuthSub", dummyOpeningId, intervalId)
            }
        }

        @Test
        @DisplayName("Deleting an already DELETED interval is idempotent (no save)")
        fun `delete already deleted interval is idempotent`() {
            val host = createDummyUser(authSub = "hostAuthSub")
            val opening = createDummyOpening(host = host)
            opening.intervals.add(
                createDummyInterval(
                    opening = opening,
                    publicId = intervalId,
                    start = opening.openingStart,
                    end = opening.openingStart.plusMinutes(60),
                    status = IntervalStatus.DELETED
                )
            )

            every { openingEntityRepository.findByPublicId(dummyOpeningId) } returns opening
            every { userService.findByAuthSub("hostAuthSub") } returns host

            val response = service.deleteInterval("hostAuthSub", dummyOpeningId, intervalId)

            assertNotNull(response)
            verify(exactly = 0) { openingEntityRepository.saveAndFlush(any<OpeningEntity>()) }
        }

    }

}