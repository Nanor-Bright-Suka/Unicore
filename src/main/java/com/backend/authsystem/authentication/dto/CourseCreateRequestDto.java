package com.backend.authsystem.authentication.dto;

import com.backend.authsystem.authentication.enums.Semester;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record CourseCreateRequestDto(
        @Schema(description = "Course code, e.g., CS401", example = "CS401")
        @NotBlank(message = "code must not be empty")
        @Pattern(
                regexp = "^[A-Z]{2,4}-?\\d{3}[A-Z]?$",
                message = "courseCode must be valid (e.g., CS101, MATH-202A)"
        )
        String code,

        @Schema(description = "Course title, e.g., Data Structures", example = "Data Structures")
        @NotBlank(message = "title must not be empty")
        @Size(min = 2, max = 100, message = "title must be between 2 and 100 characters")
        String title,

        @Schema(description = "Optional description of the course", example = "Introduction to basic data structures")
        @NotBlank(message = "description must not be empty")
        @Size(max = 2000, message = "description cannot exceed 2000 characters")
        String description,

        @Schema(description = "Semester in which the course is offered", example = "FALL")
        @NotNull(message = "semester must not be null")
        Semester semester,

        @Schema(description = "Academic year of the course, e.g., 2025/2026", example = "2025/2026")
        @NotBlank(message = "academicYear must not be empty")
        @Size(min = 7, max = 9, message = "academicYear must be in the format YYYY/YYYY")
        String academicYear,

        @Schema(description = "Maximum number of students allowed", example = "50")
        @Positive(message = "maxCapacity must be greater than 0")
        int maxCapacity



) {
}
