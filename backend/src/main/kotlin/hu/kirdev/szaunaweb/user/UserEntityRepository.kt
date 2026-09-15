package hu.kirdev.szaunaweb.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserEntityRepository : JpaRepository<UserEntity, Long> {

    fun findByAuthSub(auth: String): UserEntity?

    fun findByPublicId(userId: UUID): UserEntity?

}