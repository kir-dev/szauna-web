package hu.kirdev.szaunaweb.config

import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val authSchSuccessHandler: AuthSchSuccessHandler,
    private val customOncePerRequestFilter: CustomOncePerRequestFilter
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowCredentials = true
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedOriginPatterns = listOf(
            "http://localhost:3000", //TODO:: Add frontend url
        )
        configuration.allowedHeaders = listOf("*")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtService: JwtService): SecurityFilterChain {
        http
            .cors { corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()) }

            .csrf { it.disable() } //TODO:: Implementing csrf

            .authorizeHttpRequests { authRequest ->
                authRequest
                    .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/login/**",
                        "/oauth2/**",
                    ).permitAll()
                    .anyRequest().authenticated()
            }

            .formLogin { it.disable() }
            .httpBasic { it.disable() }

            .sessionManagement { sessionManager -> sessionManager.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

            .oauth2Login { oauth2 ->
                oauth2.successHandler(authSchSuccessHandler)

            }

            .exceptionHandling { exceptionHandling ->
                exceptionHandling.authenticationEntryPoint { _, response, _ ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                }
            }

            .logout { logout ->
                logout
                    .logoutUrl("/api/v1/auth/logout")
                    .addLogoutHandler { _, response, _ ->
                        val clearCookie = jwtService.clearAuthTokenCookie()
                        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString())
                    }
                    .logoutSuccessHandler { _, response, _ ->
                        response.status = HttpServletResponse.SC_OK
                    }
            }
            .addFilterBefore(customOncePerRequestFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }


}