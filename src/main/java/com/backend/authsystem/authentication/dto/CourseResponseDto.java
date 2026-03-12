package com.backend.authsystem.authentication.dto;

import com.backend.authsystem.authentication.enums.CourseState;
import com.backend.authsystem.authentication.enums.Semester;

import java.time.Instant;
import java.util.UUID;


public record CourseResponseDto(
        UUID id,
        String code,
        String title,
        String description,
        CourseState state,
        Semester semester,
        String academicYear,
        int maxCapacity,
        String lecturerId,
        Instant createdAt,
        Instant updatedAt
) {

}
