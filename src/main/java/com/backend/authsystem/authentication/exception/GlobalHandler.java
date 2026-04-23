package com.backend.authsystem.authentication.exception;


import com.backend.authsystem.authentication.util.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalHandler {

    private  ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage()
        );
        return new ResponseEntity<>(error, status);
    }

    @ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleUserExists(UserAlreadyExistException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

  @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED);
    }

  @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoleNotFound(RoleNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MissingTokenException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(MissingTokenException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

  @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(InvalidTokenException ex) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED);
    }

@ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }


@ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCourseNotFoundException(CourseNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

@ExceptionHandler(InvalidCourseStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCourseStateException(InvalidCourseStateException ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }

@ExceptionHandler(PermissionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePermissionNotFoundException(PermissionNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

@ExceptionHandler(DuplicatePermissionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePermissionException(DuplicatePermissionException ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }

@ExceptionHandler(DuplicateRoleException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateRoleException(DuplicateRoleException ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }

@ExceptionHandler(CourseStateException.class)
    public ResponseEntity<ErrorResponse> handleCourseStateException(CourseStateException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

@ExceptionHandler(AssignmentPermissionException.class)
    public ResponseEntity<ErrorResponse> handleAssignmentPermissionException(AssignmentPermissionException ex) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED);
    }

@ExceptionHandler(AssignmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAssignmentNotFoundException(AssignmentNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

@ExceptionHandler(AssignmentValidationException.class)
    public ResponseEntity<ErrorResponse> handleAssignmentValidationException(AssignmentValidationException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

@ExceptionHandler(FileSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileSizeExceededException(FileSizeExceededException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

@ExceptionHandler(FileTypeValidationException.class)
    public ResponseEntity<ErrorResponse> handleFileTypeValidationException(FileTypeValidationException ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }


@ExceptionHandler(AssignmentSubmissionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAssignmentSubmissionNotFoundException(AssignmentSubmissionNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }


@ExceptionHandler(GradeSubmissionException.class)
    public ResponseEntity<ErrorResponse> handleGradeSubmissionException(GradeSubmissionException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AssignmentStateException.class)
    public ResponseEntity<ErrorResponse> handleAssignmentStateException(AssignmentStateException ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EnrollmentConflictException.class)
    public ResponseEntity<ErrorResponse> handleEnrollmentConflictException(EnrollmentConflictException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }


 @ExceptionHandler(EnrollmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEnrollmentNotFoundException(EnrollmentNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

 @ExceptionHandler(CourseMaterialNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCourseMaterialNotFoundException(CourseMaterialNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }


@ExceptionHandler(CourseMaterialStateException.class)
public ResponseEntity<ErrorResponse> handleCourseMaterialStateException(CourseMaterialStateException ex) {
    return buildErrorResponse(ex, HttpStatus.CONFLICT);
}


    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<?> handleExpired() {
        return ResponseEntity.status(401).body("Token expired");
    }

    @ExceptionHandler({MalformedJwtException.class, SignatureException.class, UnsupportedJwtException.class, IllegalArgumentException.class})
    public ResponseEntity<?> handleInvalid() {
        return ResponseEntity.status(401).body("Invalid token");
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<?> handleGenericJwt() {
        return ResponseEntity.status(401).body("Invalid token");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                false,
                "Validation failed",
                errors
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBadRequest(HttpMessageNotReadableException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("body", "Malformed JSON request");
        System.out.println(ex.getMessage());

        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                false,
                "Validation failed",
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<ApiResponse<Void>> handleTooManyAttempts(TooManyLoginAttemptsException ex) {
        ApiResponse<Void> response = new ApiResponse<>(false, ex.getMessage(), null);
        return ResponseEntity.status(429).body(response);
    }

    @ExceptionHandler(PasswordInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handlePasswordInvalidException(PasswordInvalidException  ex) {
        ApiResponse<Void> response = new ApiResponse<>(false, ex.getMessage(), null);
        return ResponseEntity.status(400).body(response);
          }



    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }




}
