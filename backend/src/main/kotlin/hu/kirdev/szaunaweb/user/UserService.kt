package hu.kirdev.szaunaweb.user

import hu.kirdev.szaunaweb.config.JwtService
import hu.kirdev.szaunaweb.exception.JwtException
import hu.kirdev.szaunaweb.exception.UserNotFoundException
import io.jsonwebtoken.Claims
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService(
    private val userEntityRepository: UserEntityRepository,
    private val jwtService: JwtService,
    @Value("\${szaunaWeb.jwt.authTokenCookieName}")
    private val cookieName: String,
) {

    fun findByPublicId(userId: UUID): UserEntity {
        return userEntityRepository.findByPublicId(userId)
            ?: throw UserNotFoundException("User not found with id $userId")
    }

    fun findByAuthSub(authSub: String): UserEntity {
        return userEntityRepository.findByAuthSub(authSub)
            ?: throw UserNotFoundException("User not found with authSub $authSub")
    }

    fun me(request: HttpServletRequest): MeResponse {
        val authCookie = request.cookies?.firstOrNull { it.name == cookieName }
        if (authCookie == null) throw JwtException("User token is missing!")
        val claims: Claims =
            jwtService.getClaims(authCookie.value) ?: throw JwtException("User token claims are missing!")
        return MeResponse(claims)
    }

}