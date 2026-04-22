package com.backend.authsystem.authentication.controller;

import com.backend.authsystem.authentication.dto.EnrollmentResponseDto;
import com.backend.authsystem.authentication.service.EnrollmentService;
import com.backend.authsystem.authentication.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "COURSE ENROLLMENT", description = "Endpoint for managing enrolled students in courses")
@RestController
@RequestMapping("/api/v1/course-enrollment")
@RequiredArgsConstructor
public class CourseEnrollmentController {

    private final EnrollmentService enrollmentService;

    @Operation(
            summary = "Enroll in a course",
            description = "Allows a student to enroll in a course. Requires 'COURSE_ENROLLMENT' authority."
    )
    @PostMapping("/{courseId}/enroll")
    @PreAuthorize("hasAuthority('COURSE_ENROLLMENT')")
    public ResponseEntity<ApiResponse<EnrollmentResponseDto>> enrollStudent(@PathVariable UUID courseId) {
        EnrollmentResponseDto enrolledStudent =  enrollmentService.enrollStudent(courseId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Student enrolled successfully", enrolledStudent));
    }


    @Operation(
            summary = "Get enrolled students count",
            description = "Returns the total number of students currently enrolled in the specified course. Requires 'COURSE_ENROL_COUNT' authority."
    )
    @GetMapping("/{courseId}/enrolled-count")
    @PreAuthorize("hasAuthority('COURSE_ENROLLMENT_COUNT')")
    public ResponseEntity<ApiResponse<Long>> getEnrolledCount(@PathVariable UUID courseId) {
        long count = enrollmentService.getEnrolledCount(courseId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Enrolled Students retrieved successfully", count));

    }


    @Operation(
            summary = "Get enrolled students list",
            description = "Return a list of students currently enrolled in the specified course. Requires 'COURSE_ENROL_LIST' authority. Each student is represented by an EnrollmentResponseDto containing their userId, name, and email."
    )
    @GetMapping("/{courseId}")
    @PreAuthorize("hasAuthority('COURSE_ENROLLMENT_LIST')")
    public ResponseEntity<ApiResponse<List<EnrollmentResponseDto>>> getEnrolledStudents(@PathVariable UUID courseId) {
        List<EnrollmentResponseDto> students = enrollmentService.getEnrolledStudents(courseId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Retrieved enrolled student list successfully", students));

    }








}
