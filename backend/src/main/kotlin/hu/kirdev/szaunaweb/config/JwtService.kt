package hu.kirdev.szaunaweb.config

import hu.kirdev.szaunaweb.user.UserEntity
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${szaunaWeb.jwt.secret}")
    private val secret: String,

    @Value("\${szaunaWeb.jwt.experation}")
    private val expiration: Long,

    @Value("\${szaunaWeb.jwt.issuer}")
    private val issuer: String,

    @Value("\${szaunaWeb.jwt.authTokenCookieName}")
    private val authTokenCookieName: String
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))

    fun generateAuthToken(user: UserEntity): String {

        val claims = mapOf(
            "email" to user.email,
            "name" to user.displayName,
            "role" to user.role.name,
        )

        return Jwts.builder()
            .claims(claims)
            .subject(user.authSub)
            .issuer(issuer)
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(secretKey)
            .compact()
    }

    fun generateAuthTokenCookie(token: String): ResponseCookie {
        return ResponseCookie.from(authTokenCookieName, token)
            .httpOnly(true)
            .sameSite("Lax") //TODO:: Strict
            .secure(false) //TODO:: Set true in production (https)
            .path("/")
            .maxAge(expiration / 1000)
            .build()
    }

    fun getClaims(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (_: Exception) {
            null
        }
    }

    fun clearAuthTokenCookie(): ResponseCookie {
        return ResponseCookie.from(authTokenCookieName, "").httpOnly(true).path("/").maxAge(0).build()
    }
}