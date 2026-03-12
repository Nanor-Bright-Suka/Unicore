package com.backend.authsystem.authentication.dto;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponseDto(
        UUID enrollmentId,
        UUID courseId,
        UUID studentId,
        Instant enrolledAt
) {
}
