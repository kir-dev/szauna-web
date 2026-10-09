package hu.kirdev.szaunaweb.exception

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UserAuthorityException::class)
    fun handleUserAuthorityException(e: UserAuthorityException): ProblemDetail {
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.message ?: "Unauthorized")
        problemDetail.title = "AUTHORITY_EXCEPTION"
        return problemDetail
    }

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFoundException(e: UserNotFoundException): ProblemDetail {
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message ?: "Not Found")
        problemDetail.title = "USER_NOT_FOUND"
        return problemDetail
    }

    @ExceptionHandler(OpeningException::class)
    fun handleOpeningException(e: OpeningException): ProblemDetail {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.message ?: "Something went wrong")
        problemDetail.title = "OPENING_EXCEPTION"
        return problemDetail
    }

    @ExceptionHandler(InvalidTimeRangeException::class)
    fun handleInvalidTimeRangeException(e: InvalidTimeRangeException): ProblemDetail {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.message ?: "Something went wrong")
        problemDetail.title = "INVALID_TIME_RANGE"
        return problemDetail
    }

    @ExceptionHandler(OpeningNotFoundException::class)
    fun handleOpeningNotFoundException(e: OpeningNotFoundException): ProblemDetail {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message ?: "Something went wrong")
        problemDetail.title = "OPENING_NOT_FOUND"
        return problemDetail
    }

    @ExceptionHandler(IntervalNotFoundException::class)
    fun handleIntervalNotFoundException(e: IntervalNotFoundException): ProblemDetail {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message ?: "Something went wrong")
        problemDetail.title = "INTERVAL_NOT_FOUND"
        return problemDetail
    }

    @ExceptionHandler(TimeRangeConflictException::class)
    fun handleTimeRangeConflictException(e: TimeRangeConflictException): ProblemDetail {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message ?: "Something went wrong")
        problemDetail.title = "TIME_RANGE_CONFLICT"
        return problemDetail
    }

    @ExceptionHandler(OpeningPermissionException::class)
    fun handleOpeningPermissionException(e: OpeningPermissionException): ProblemDetail {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.message ?: "Something went wrong")
        problemDetail.title = "OPENING_PERMISSION_EXCEPTION"
        return problemDetail
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ProblemDetail {
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation error")
        problemDetail.title = "INVALID_ARGUMENT"
        val errors = e.bindingResult.allErrors.associate { error ->
            val fieldName = (error as? FieldError)?.field ?: error.objectName
            fieldName to (error.defaultMessage ?: "Invalid value")
        }
        problemDetail.setProperty("invalidFields", errors)
        return problemDetail
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(e: DataIntegrityViolationException): ProblemDetail {
        val problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "An item with these unique constraints already exists."
        )
        problemDetail.title = "DATA_CONFLICT"
        return problemDetail
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ProblemDetail {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.message ?: "Unknown error")
        problemDetail.title = "INTERVAL_SERVER_ERROR"
        return problemDetail
    }
}