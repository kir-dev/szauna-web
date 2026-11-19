package hu.kirdev.szaunaweb.user

import io.jsonwebtoken.Claims

data class MeResponse(
    val email: String,
    val authSub: String,
    val publicId: String,
    val banReason: String,
    val banned: Boolean,
    val role: UserRole,
    val displayName: String
){
    constructor(claims: Claims): this(
        email = claims["email"] as String,
        authSub = claims["sub"] as String,
        publicId = claims["publicId"] as String,
        banReason = claims["banReason"] as String,
        banned = claims["banned"] as Boolean,
        role = UserRole.valueOf(claims["role"] as String),
        displayName = claims["displayName"] as String
    )
}