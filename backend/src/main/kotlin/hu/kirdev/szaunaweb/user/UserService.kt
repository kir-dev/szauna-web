package hu.kirdev.szaunaweb.user

import hu.kirdev.szaunaweb.exception.UserNotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService (
    val userEntityRepository: UserEntityRepository
){

    fun findByPublicId(userId: UUID): UserEntity{
        return userEntityRepository.findByPublicId(userId)?: throw UserNotFoundException("User not found with id $userId")
    }

}