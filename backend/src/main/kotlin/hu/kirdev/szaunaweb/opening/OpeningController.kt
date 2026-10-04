package hu.kirdev.szaunaweb.opening

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api/v1/opening")
class OpeningController(
    private val openingService: OpeningService
) {

    @Operation(
        summary = "Get Opening by public id",
        description = "To get **PRIVATE** opening, you need `ADMIN`, `SAUNA_MASTER` or `TRAINEE` role."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
                content = [Content(schema = Schema(implementation = OpeningResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request: if you try to visit a private opening without permission or the opening not found!",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found!",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            )
        ]
    )
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{publicId}")
    fun findByPublicId(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: UUID?,

        @Parameter(description = "Opening public id")
        @PathVariable
        publicId: UUID
    ): OpeningResponse {
        return openingService.getOpeningByPublicId(userId, publicId)
    }


    @Operation(
        summary = "Load Opening by pages",
        description = "To get **PRIVATE** opening, you need `ADMIN`, `SAUNA_MASTER` or `TRAINEE` role."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
            )
        ]
    )
    @GetMapping
    fun findOpenings(
        @AuthenticationPrincipal
        @Parameter(hidden = true)
        userId: UUID?,

        @Parameter(description = "Filter by status", example = "SCHEDULED")
        @RequestParam(required = false)
        status: OpeningStatus?,

        @Parameter(description = "Filter by opening start date-time")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: LocalDateTime?,

        @Parameter(description = "Filter by opening end date-time")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: LocalDateTime?,

        @ParameterObject
        @PageableDefault(size = 10, sort = ["openingStart"], direction = Sort.Direction.DESC)
        pageable: Pageable
    ): Page<OpeningResponse> {
        return openingService.getOpenings(userId, pageable, status, from, to)
    }

    @Operation(
        summary = "Get up coming opening",
        description = "This endpoint exclude all private openings."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
            )
        ]
    )
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/up-coming")
    fun getUpComing(): OpeningResponse? {
        return openingService.getCurrentOpening()
    }

    @Operation(
        summary = "Create new Opening",
        description = "**Required role:** `ADMIN` or `SAUNA_MASTER`"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Opening successfully created",
                content = [Content(schema = Schema(implementation = OpeningResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request: opening range is invalid, overlap between opening ranges, opening type is inactive or not found.",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized request",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found!",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            )
        ]
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SAUNA_MASTER')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun createOpening(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: UUID,
        @Valid
        @RequestBody
        dto: CreateOpeningRequest
    ): OpeningResponse {
        return openingService.createOpening(userId, dto)
    }

    @Operation(
        summary = "Create new interval",
        description = "**Required role:** `ADMIN` or `SAUNA_MASTER`"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Opening successfully created",
                content = [Content(schema = Schema(implementation = OpeningResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request: invalid interval range, overlap with other interval, opening not found, or do not have permissions.",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized request",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found!",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            )
        ]
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SAUNA_MASTER')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/interval")
    fun createInterval(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: UUID,

        @Valid
        @RequestBody
        dto: CreateIntervalRequest
    ): OpeningResponse {
        return openingService.createInterval(userId, dto)
    }

    @Operation(
        summary = "Update Opening",
        description = "To update the opening you must have `ADMIN` privileges or you need to be the host."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request: if the opening range are invalid, overlapped, try to set the past, already marked as `COMPLETED`, or the new range collides with any interval, or the Opening Type is inactive.",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized request",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found!",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            )
        ]
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SAUNA_MASTER')")
    @ResponseStatus(HttpStatus.OK)
    @PutMapping
    fun updateOpening(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: UUID,

        @RequestBody
        dto: UpdateOpeningRequest

    ): OpeningResponse {
        return openingService.updateOpening(userId, dto)
    }


    @Operation(
        summary = "Update opening interval",
        description = "To update the interval you need to be the host or have `ADMIN` privileges."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request: if the opening already marked as `COMPLETED`, or the interval not found or the seat limit is invalid, or the time range is invalid.",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized request",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found!",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            )
        ]
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SAUNA_MASTER')")
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/interval")
    fun updateInterval(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: UUID,

        @RequestBody
        dto: UpdateIntervalRequest

    ): OpeningResponse {
        return openingService.updateInterval(userId, dto)
    }

    @Operation(
        summary = "Update opening status",
        description = "To update the opening status you need to be the host or have `ADMIN` privileges."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request: if the status is invalid or do not have privileges.",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized request",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found!",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            )
        ]
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SAUNA_MASTER')")
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping
    fun updateOpeningStatus(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: UUID,

        @RequestBody
        dto: UpdateOpeningStatusRequest
    ): OpeningResponse {
        return openingService.updateOpeningStatus(userId, dto)
    }

    @Operation(
        summary = "Update opening interval status",
        description = "To update the opening status you need to be the host or have `ADMIN` privileges."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request: if the status is invalid or do not have privileges or the opening not found or the opening already marked as `COMPLETED` or not interval found for the status.",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized request",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found!",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            )
        ]
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SAUNA_MASTER')")
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/interval")
    fun updateIntervalStatus(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: UUID,

        @RequestBody
        dto: UpdateIntervalStatusRequest
    ): OpeningResponse {
        return openingService.updateIntervalStatus(userId, dto)
    }


    @Operation(
        summary = "Delete opening",
        description = "To delete an opening you need to be an admin!"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "OK - NO CONTENT",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request: if you are not an admin or the opening already marked as `COMPLETED` or the opening not found.",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized request",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found!",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            )
        ]
    )
    @PreAuthorize("hasAnyRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{publicId}")
    fun deleteOpeningByPublicId(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: UUID,

        @Parameter(description = "Public ID of the opening")
        @PathVariable
        publicId: UUID
    ) {
        openingService.deleteOpening(userId, publicId)
    }

    @Operation(
        summary = "Delete opening interval",
        description = "To delete an opening you need to be an admin or the host of the opening!"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad request: if you are not an admin or host of the opening, or the opening already marked as `COMPLETED` or the interval ha `ACTIVE` bookings or the opening not found.",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized request",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found!",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            )
        ]
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SAUNA_MASTER')")
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/{publicId}/interval/{intervalId}")
    fun deleteInterval(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        userId: UUID,

        @Parameter(description = "Public ID of the opening")
        @PathVariable
        publicId: UUID,

        @Parameter(description = "Public ID of the interval")
        @PathVariable
        intervalId: UUID
    ): OpeningResponse {
        return openingService.deleteInterval(userId, publicId, intervalId)
    }


}