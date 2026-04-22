package com.backend.authsystem.authentication.controller;


import com.backend.authsystem.authentication.dto.course.CourseCreateRequestDto;
import com.backend.authsystem.authentication.dto.course.CourseResponseDto;
import com.backend.authsystem.authentication.dto.course.CourseUpdateRequestDto;
import com.backend.authsystem.authentication.service.AuthenticatedUserService;
import com.backend.authsystem.authentication.service.CourseService;
import com.backend.authsystem.authentication.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@Tag(name = "COURSE", description = "Endpoints for managing courses")
@RestController
@RequestMapping("/api/v1/course")
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final CourseService courseService;
    private final AuthenticatedUserService authenticatedUserService;

    @Operation(
            summary = "Create Course",
            description = "Create course of the  currently authenticated user. Requires 'COURSE_CREATE' authority."
    )
    @PostMapping
    @PreAuthorize("hasAuthority('COURSE_CREATE')")
    public ResponseEntity<ApiResponse<CourseResponseDto>> createCourse(@Valid @RequestBody CourseCreateRequestDto request) {

        String userEmail = authenticatedUserService.getCurrentUserEmail();
        log.info("User {} requested to create course {}", userEmail, request.title());

        CourseResponseDto createdCourse =  courseService.createCourse(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Course created successfully.", createdCourse));

    }


    @Operation(
            summary = "Update Course",
            description = "Update course of the  currently authenticated user. Requires 'COURSE_UPDATE' authority."
    )
    @PutMapping("/{courseId}")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public ResponseEntity <ApiResponse<CourseResponseDto>> updateCourse(@PathVariable UUID courseId,
                                       @Valid @RequestBody CourseUpdateRequestDto request) {

        String userEmail = authenticatedUserService.getCurrentUserEmail();
        log.info("User {} requested to update course {}", userEmail, courseId);

        CourseResponseDto updated = courseService.updateCourse(courseId, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Course updated successfully.", updated));

    }


    @Operation(
            summary = "Publish Course",
            description = "Publish course of the  currently authenticated user. Requires 'COURSE_PUBLISH' authority."
    )
    @PatchMapping("/{courseId}/publish")
    @PreAuthorize("hasAuthority('COURSE_PUBLISH')")
    public ResponseEntity <ApiResponse<CourseResponseDto>> publishCourse(@PathVariable UUID courseId) {
        String userEmail = authenticatedUserService.getCurrentUserEmail();
        log.info("User {} requested to publish course {}", userEmail, courseId);

        CourseResponseDto publishCourse = courseService.publishCourse(courseId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Course published successfully.", publishCourse));

    }



    @Operation(
            summary = "Open Course Enrollment",
            description = "Open course enrollment of the  currently authenticated user. Requires 'COURSE_OPEN_ENROLLMENT' authority."
    )
    @PatchMapping("/{courseId}/open-enrollment")
    @PreAuthorize("hasAuthority('COURSE_OPEN_ENROLLMENT')")
    public ResponseEntity <ApiResponse<CourseResponseDto>> openEnrollment(@PathVariable UUID courseId) {
        String userEmail = authenticatedUserService.getCurrentUserEmail();
        log.info("User {} requested to open course enrollment {}", userEmail, courseId);

        CourseResponseDto courseOpen =  courseService.openEnrollment(courseId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Course Enrollment Opened successfully", courseOpen));
    }


    @Operation(
            summary = "Close Course Enrollment",
            description = "Close course enrollment of the  currently authenticated user. Requires 'COURSE_CLOSE_ENROLLMENT' authority."
    )
    @PatchMapping("/{courseId}/close-enrollment")
    @PreAuthorize("hasAuthority('COURSE_CLOSE_ENROLLMENT')")
    public ResponseEntity <ApiResponse<CourseResponseDto>> closeEnrollment(@PathVariable UUID courseId) {
        String userEmail = authenticatedUserService.getCurrentUserEmail();
        log.info("User {} requested to close course enrollment {}", userEmail, courseId);

        CourseResponseDto courseClose =  courseService.closeEnrollment(courseId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Course Enrollment closed successfully", courseClose));
    }



    @Operation(
            summary = "Start Course",
            description = "Start course for the  currently authenticated user. Requires 'COURSE_START' authority."
    )
    @PatchMapping("/{courseId}/start")
    @PreAuthorize("hasAuthority('COURSE_START')")
    public ResponseEntity <ApiResponse<CourseResponseDto>> startCourse(@PathVariable UUID courseId) {
        String userEmail = authenticatedUserService.getCurrentUserEmail();
        log.info("User {} requested to start course {}", userEmail, courseId);

        CourseResponseDto courseStated =  courseService.startCourse(courseId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Course started successfully", courseStated));
    }


    @Operation(
            summary = "Complete Course",
            description = "Complete course for the  currently authenticated user. Requires 'COURSE_COMPLETE' authority."
    )
    @PatchMapping("/{courseId}/complete")
    @PreAuthorize("hasAuthority('COURSE_COMPLETE')")
    public ResponseEntity <ApiResponse<CourseResponseDto>> completeCourse(@PathVariable UUID courseId) {
        String userEmail = authenticatedUserService.getCurrentUserEmail();
        log.info("User {} requested to complete course {}", userEmail, courseId);

        CourseResponseDto courseStarted =  courseService.completeCourse(courseId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Course completed successfully", courseStarted));
    }


    @Operation(
            summary = "Archive Course",
            description = "Archive course for the  currently authenticated user. Requires 'COURSE_ARCHIVE' authority."
    )
    @PatchMapping("/{courseId}/archive")
    @PreAuthorize("hasAuthority('COURSE_ARCHIVE')")
    public ResponseEntity <ApiResponse<CourseResponseDto>> archiveCourse(@PathVariable UUID courseId) {
        String userEmail = authenticatedUserService.getCurrentUserEmail();
        log.info("User {} requested to archive course {}", userEmail, courseId);

        CourseResponseDto courseArchived =  courseService.archiveCourse(courseId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Course archived successfully", courseArchived));
    }



    @Operation(
            summary = "View Course",
            description = "View course for the  currently authenticated user. Requires 'COURSE_VIEW' authority."
    )
    @GetMapping("/{courseId}")
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    public ResponseEntity <ApiResponse<CourseResponseDto>> getCourseById(@PathVariable UUID courseId) {
        String userEmail = authenticatedUserService.getCurrentUserEmail();
        log.info("User {} requested to view course {}", userEmail, courseId);

        CourseResponseDto theCourse =  courseService.getCourseById(courseId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Course details retrieved successfully", theCourse));

    }




    @Operation(
            summary = "View All Courses",
            description = "View all courses for the  currently authenticated user. Requires 'COURSE_VIEW_ALL' authority."
    )
    @GetMapping("/courses")
    @PreAuthorize("hasAuthority('COURSE_VIEW_ALL')")
    public ResponseEntity <ApiResponse<List<CourseResponseDto>>> getAllCourses() {
        String userEmail = authenticatedUserService.getCurrentUserEmail();
        log.info("User {} requested to view all courses", userEmail);

     List<CourseResponseDto>  allCourses =  courseService.getAllCourses();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "All courses retrieved successfully", allCourses));
    }


























}
