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
    @DisplayName("createOpening")
    inner class CreateOpeningTest {

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
    @DisplayName("updateInterval")
    inner class UpdateIntervalTests {

        private val intervalId = UUID.randomUUID()

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
    }

}