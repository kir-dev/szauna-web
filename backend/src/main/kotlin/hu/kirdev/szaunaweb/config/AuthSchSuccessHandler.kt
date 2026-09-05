package hu.kirdev.szaunaweb.config

import hu.kirdev.szaunaweb.user.UserEntity
import hu.kirdev.szaunaweb.user.UserEntityRepository
import hu.kirdev.szaunaweb.user.UserRole
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class AuthSchSuccessHandler(
    @Value("\${szaunaWeb.frontendUrl}")
    private val frontendUrl: String,

    @Value("\${szaunaWeb.szaunaKorId}")
    private val szaunaKorId: Int,

    @Value("\${szaunaWeb.admins:}")
    private val admins: List<String> = emptyList(),

    private val jwtService: JwtService,
    private val userEntityRepository: UserEntityRepository

) : SimpleUrlAuthenticationSuccessHandler() {

    private fun resolveSzaunaMembership(memberships: List<Map<String, Any>>): List<String> {
        return memberships.firstOrNull { it["id"].toString() == szaunaKorId.toString() }?.let { memberships ->
            (memberships["title"] as? List<*>)?.filterIsInstance<String>()
        } ?: emptyList()
    }

    private fun resolveRole(memberships: List<Map<String, Any>>, internalId: String): UserRole {
        if (internalId in admins) return UserRole.ADMIN
        val titles = resolveSzaunaMembership(memberships)

        if ("körvezető" in titles) return UserRole.ADMIN

        if ("tag" in titles) return UserRole.SAUNA_MASTER

        if ("újonc" in titles) return UserRole.TRAINEE //TODO:: Change to "próbás"???

        return UserRole.USER
    }


    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauth2User = authentication.principal as OAuth2User

        val internalId = oauth2User.name
        val email = oauth2User.attributes["mail"] as? String
        val name = oauth2User.attributes["displayName"] as? String
        val rawMemberships = oauth2User.attributes["eduPersonEntitlement"] as? List<*>



        if (email == null || name == null || rawMemberships == null) {
            redirectStrategy.sendRedirect(request, response, "$frontendUrl/login?error=forbidden")
            return
        }

        val membershipsList = (rawMemberships).filterIsInstance<Map<String, Any>>()
        val role = resolveRole(membershipsList, internalId)

        val user = userEntityRepository.findByAuthSub(internalId)?.apply {
            this.email = email
            this.displayName = name
            this.role = role
        } ?: UserEntity(
            authSub = internalId,
            email = email,
            displayName = name,
            role = role,
        )

        val saved = userEntityRepository.save(user)

        val token = jwtService.generateAuthToken(saved)

        val cookie = jwtService.generateAuthTokenCookie(token)

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())

        redirectStrategy.sendRedirect(request, response, frontendUrl)
    }


}