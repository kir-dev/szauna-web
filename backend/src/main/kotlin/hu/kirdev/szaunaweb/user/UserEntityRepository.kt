package hu.kirdev.szaunaweb.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserEntityRepository : JpaRepository<UserEntity, Long> {

    fun findByAuthSub(auth: String): UserEntity?

}