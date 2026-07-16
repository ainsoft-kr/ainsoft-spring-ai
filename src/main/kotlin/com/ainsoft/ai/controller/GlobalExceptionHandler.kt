package com.ainsoft.ai.controller

import com.ainsoft.ai.advisor.ModerationException
import com.ainsoft.ai.dto.ApiError
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(error: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val details = error.bindingResult.fieldErrors.map { fieldError ->
            "${fieldError.field}: ${fieldError.defaultMessage ?: "invalid value"}"
        }
        return ResponseEntity.badRequest().body(ApiError("Request validation failed", details))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(error: ConstraintViolationException): ResponseEntity<ApiError> {
        val details = error.constraintViolations.map { violation ->
            "${violation.propertyPath}: ${violation.message}"
        }
        return ResponseEntity.badRequest().body(ApiError("Request validation failed", details))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(error: IllegalArgumentException): ResponseEntity<ApiError> {
        return ResponseEntity.badRequest().body(ApiError(error.message ?: "Invalid request"))
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSize(error: MaxUploadSizeExceededException): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(ApiError(error.message ?: "Uploaded media is too large"))
    }

    @ExceptionHandler(ModerationException::class)
    fun handleModeration(error: ModerationException): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiError(error.message ?: "Content blocked by moderation policy"))
    }
}
