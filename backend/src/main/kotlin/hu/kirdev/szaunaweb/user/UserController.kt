package hu.kirdev.szaunaweb.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user")
class UserController(
    private val userService: UserService
) {

    @Operation(summary = "Get authenticated user details")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
                content = [Content(schema = Schema(implementation = MeResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Missing jwt token cookie or unable to resolve token claims.",
                content = [Content(schema = Schema(implementation = ProblemDetail::class))]
            )

        ]
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SAUNA_MASTER', 'TRAINEE', 'USER')")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/me")
    fun me(request: HttpServletRequest): MeResponse {
        return userService.me(request);
    }

}