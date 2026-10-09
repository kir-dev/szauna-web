package hu.kirdev.szaunaweb.opening

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

class OpeningControllerTest {

    private val openingService: OpeningService = mockk()
    private lateinit var mockMvc: MockMvc

    private val dummyOpeningId = UUID.randomUUID()
    private val dummyIntervalId = UUID.randomUUID()
    private val authSub = "test-auth-sub"

    @BeforeEach
    fun setup() {
        val principalResolver = object : HandlerMethodArgumentResolver {
            override fun supportsParameter(parameter: MethodParameter): Boolean {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal::class.java)
            }
            override fun resolveArgument(
                parameter: MethodParameter,
                mavContainer: ModelAndViewContainer?,
                webRequest: NativeWebRequest,
                binderFactory: WebDataBinderFactory?
            ): Any {
                return authSub
            }
        }

        mockMvc = MockMvcBuilders.standaloneSetup(OpeningController(openingService))
            .setCustomArgumentResolvers(principalResolver, PageableHandlerMethodArgumentResolver())
            .build()
    }

    @Nested
    @DisplayName("1. GET /api/v1/opening/{publicId}")
    inner class FindByPublicIdTests {
        @Test
        fun `find by public id returns 200`() {
            val mockResponse = mockk<OpeningResponse>(relaxed = true)
            every { openingService.getOpeningByPublicId(authSub, dummyOpeningId) } returns mockResponse

            mockMvc.perform(get("/api/v1/opening/$dummyOpeningId"))
                .andExpect(status().isOk)

            verify(exactly = 1) { openingService.getOpeningByPublicId(authSub, dummyOpeningId) }
        }
    }

    @Nested
    @DisplayName("2. GET /api/v1/opening")
    inner class FindOpeningsTests {

        // PageImpl-t adunk vissza relaxed mock helyett, mert a Jackson egy mockolt
        // Page-et megbízhatatlanul szerializál.
        @Test
        fun `find openings returns 200 with page body`() {
            every { openingService.getOpenings(authSub, any(), any(), any(), any()) } returns
                    PageImpl(emptyList<OpeningResponse>(), PageRequest.of(0, 10), 0)
            mockMvc.perform(
                get("/api/v1/opening")
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content").isEmpty)

            verify(exactly = 1) { openingService.getOpenings(authSub, any(), null, null, null) }
        }

        @Test
        fun `find openings passes status and date filters to the service`() {
            val pageableSlot = slot<Pageable>()
            every {
                openingService.getOpenings(authSub, capture(pageableSlot), any(), any(), any())
            } returns PageImpl(emptyList<OpeningResponse>(), PageRequest.of(2, 5), 0)

            mockMvc.perform(
                get("/api/v1/opening")
                    .param("status", "SCHEDULED")
                    .param("from", "2026-10-01T00:00:00")
                    .param("to", "2026-10-31T23:59:59")
                    .param("page", "2")
                    .param("size", "5")
            )
                .andExpect(status().isOk)

            verify(exactly = 1) {
                openingService.getOpenings(
                    authSub,
                    any(),
                    OpeningStatus.SCHEDULED,
                    LocalDateTime.of(2026, 10, 1, 0, 0, 0),
                    LocalDateTime.of(2026, 10, 31, 23, 59, 59)
                )
            }
            assertEquals(2, pageableSlot.captured.pageNumber)
            assertEquals(5, pageableSlot.captured.pageSize)
        }

        @Test
        fun `find openings with invalid status returns 400`() {
            mockMvc.perform(get("/api/v1/opening").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest)

            verify(exactly = 0) { openingService.getOpenings(any(), any(), any(), any(), any()) }
        }
    }


    @Nested
    @DisplayName("3. GET /api/v1/opening/up-coming")
    inner class GetUpComingTests {
        @Test
        fun `get up-coming returns 200`() {
            val mockResponse = mockk<OpeningResponse>(relaxed = true)
            every { openingService.getCurrentOpening() } returns mockResponse

            mockMvc.perform(get("/api/v1/opening/up-coming"))
                .andExpect(status().isOk)

            verify(exactly = 1) { openingService.getCurrentOpening() }
        }
    }

    @Nested
    @DisplayName("4. POST /api/v1/opening")
    inner class CreateOpeningTests {
        @Test
        fun `create opening returns 201`() {
            val mockResponse = mockk<OpeningResponse>(relaxed = true)
            every { openingService.createOpening(authSub, any()) } returns mockResponse

            val requestJson = """
                {
                    "openingStart": "2026-10-10T10:00:00",
                    "openingEnd": "2026-10-10T14:00:00",
                    "openingTypeId": 1,
                    "generateDefaultIntervals": true
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/opening")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isCreated)

            verify(exactly = 1) { openingService.createOpening(authSub, any()) }
        }
    }

    @Nested
    @DisplayName("5. POST /api/v1/opening/{openingId}/interval")
    inner class CreateIntervalTests {
        @Test
        fun `create interval returns 201`() {
            val mockResponse = mockk<OpeningResponse>(relaxed = true)
            every { openingService.createInterval(authSub, any(), dummyOpeningId) } returns mockResponse

            val requestJson = """
                {
                    "intervalStart": "2026-10-10T10:00:00",
                    "intervalEnd": "2026-10-10T11:00:00",
                    "participantLimit": 10
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/opening/$dummyOpeningId/interval")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isCreated)

            verify(exactly = 1) { openingService.createInterval(authSub, any(), dummyOpeningId) }
        }
    }

    @Nested
    @DisplayName("6. PUT /api/v1/opening/{openingId}")
    inner class UpdateOpeningTests {
        @Test
        fun `update opening returns 200`() {
            val mockResponse = mockk<OpeningResponse>(relaxed = true)
            every { openingService.updateOpening(authSub, any(), dummyOpeningId) } returns mockResponse

            val requestJson = """
                {
                    "openingStart": "2026-10-10T12:00:00",
                    "openingEnd": "2026-10-10T16:00:00",
                    "openingTypeId": 2,
                    "price": 3500
                }
            """.trimIndent()

            mockMvc.perform(
                put("/api/v1/opening/$dummyOpeningId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)

            verify(exactly = 1) { openingService.updateOpening(authSub, any(), dummyOpeningId) }
        }
    }

    @Nested
    @DisplayName("7. PUT /api/v1/opening/{openingId}/interval/{intervalId}")
    inner class UpdateIntervalTests {
        @Test
        fun `update interval returns 200`() {
            val mockResponse = mockk<OpeningResponse>(relaxed = true)
            every { openingService.updateInterval(authSub, any(), dummyOpeningId, dummyIntervalId) } returns mockResponse

            val requestJson = """
                {
                    "intervalStart": "2026-10-10T12:00:00",
                    "intervalEnd": "2026-10-10T13:00:00",
                    "participantLimit": 15
                }
            """.trimIndent()

            mockMvc.perform(
                put("/api/v1/opening/$dummyOpeningId/interval/$dummyIntervalId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)

            verify(exactly = 1) { openingService.updateInterval(authSub, any(), dummyOpeningId, dummyIntervalId) }
        }
    }

    @Nested
    @DisplayName("8. PATCH /api/v1/opening/{openingId}")
    inner class UpdateOpeningStatusTests {
        @Test
        fun `update opening status returns 200`() {
            val mockResponse = mockk<OpeningResponse>(relaxed = true)
            every { openingService.updateOpeningStatus(authSub, any(), dummyOpeningId) } returns mockResponse

            val requestJson = """
                {
                    "status": "COMPLETED"
                }
            """.trimIndent()

            mockMvc.perform(
                patch("/api/v1/opening/$dummyOpeningId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)

            verify(exactly = 1) { openingService.updateOpeningStatus(authSub, any(), dummyOpeningId) }
        }
    }

    @Nested
    @DisplayName("9. PATCH /api/v1/opening/{openingId}/interval/{intervalId}")
    inner class UpdateIntervalStatusTests {
        @Test
        fun `update interval status returns 200`() {
            val mockResponse = mockk<OpeningResponse>(relaxed = true)
            every { openingService.updateIntervalStatus(authSub, any(), dummyOpeningId, dummyIntervalId) } returns mockResponse

            val requestJson = """
                {
                    "status": "CANCELLED"
                }
            """.trimIndent()

            mockMvc.perform(
                patch("/api/v1/opening/$dummyOpeningId/interval/$dummyIntervalId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)

            verify(exactly = 1) { openingService.updateIntervalStatus(authSub, any(), dummyOpeningId, dummyIntervalId) }
        }
    }

    @Nested
    @DisplayName("10. DELETE /api/v1/opening/{openingId}")
    inner class DeleteOpeningTests {
        @Test
        fun `delete opening returns 204`() {
            every { openingService.deleteOpening(authSub, dummyOpeningId) } returns Unit

            mockMvc.perform(delete("/api/v1/opening/$dummyOpeningId"))
                .andExpect(status().isNoContent)

            verify(exactly = 1) { openingService.deleteOpening(authSub, dummyOpeningId) }
        }
    }

    @Nested
    @DisplayName("11. DELETE /api/v1/opening/{openingId}/interval/{intervalId}")
    inner class DeleteIntervalTests {
        @Test
        fun `delete interval returns 200`() {
            val mockResponse = mockk<OpeningResponse>(relaxed = true)
            every { openingService.deleteInterval(authSub, dummyOpeningId, dummyIntervalId) } returns mockResponse

            mockMvc.perform(delete("/api/v1/opening/$dummyOpeningId/interval/$dummyIntervalId"))
                .andExpect(status().isOk)

            verify(exactly = 1) { openingService.deleteInterval(authSub, dummyOpeningId, dummyIntervalId) }
        }
    }

    @Nested
    @DisplayName("12. Invalid requests")
    inner class InvalidRequestTests {

        @Test
        fun `find by public id with malformed uuid returns 400`() {
            mockMvc.perform(get("/api/v1/opening/not-a-uuid"))
                .andExpect(status().isBadRequest)

            verify(exactly = 0) { openingService.getOpeningByPublicId(any(), any()) }
        }

        @Test
        fun `create opening with malformed json returns 400`() {
            mockMvc.perform(
                post("/api/v1/opening")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ this is not json")
            )
                .andExpect(status().isBadRequest)

            verify(exactly = 0) { openingService.createOpening(any(), any()) }
        }

        // Ez akkor ad 400-at, ha a DTO mezői non-null Kotlin típusúak (a Jackson Kotlin
        // modul hiányzó paramétert dob) vagy van rajtuk @NotNull. Ha a DTO-d mezői nullable-ök
        // és nincs Bean Validation annotáció, ezt a tesztet a DTO-hoz igazítsd.
        @Test
        fun `create opening with empty body returns 400`() {
            mockMvc.perform(
                post("/api/v1/opening")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
                .andExpect(status().isBadRequest)

            verify(exactly = 0) { openingService.createOpening(any(), any()) }
        }

        @Test
        fun `create opening with wrong content type returns 415`() {
            mockMvc.perform(
                post("/api/v1/opening")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("hello")
            )
                .andExpect(status().isUnsupportedMediaType)
        }

        @Test
        fun `update interval with malformed interval id returns 400`() {
            mockMvc.perform(
                put("/api/v1/opening/$dummyOpeningId/interval/not-a-uuid")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"intervalStart":"2026-10-10T12:00:00","intervalEnd":"2026-10-10T13:00:00","participantLimit":15}""")
            )
                .andExpect(status().isBadRequest)
        }
    }

}