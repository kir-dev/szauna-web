package hu.kirdev.szaunaweb.config

import io.jsonwebtoken.Claims
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CustomOncePerRequestFilter(

    @Value("\${szaunaWeb.jwt.authTokenCookieName}")
    private val cookieName: String,
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val authCookie = request.cookies?.firstOrNull { it.name == cookieName }

        if (authCookie != null) {
            val claims: Claims? = jwtService.getClaims(authCookie.value)
            if (claims != null && SecurityContextHolder.getContext().authentication == null) {
                val sub = claims.subject
                val role = claims["role"] as? String ?: "USER"
                val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))
                val auth = UsernamePasswordAuthenticationToken(sub, null, authorities)
                auth.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = auth

            }
        }

        filterChain.doFilter(request, response)

    }

}