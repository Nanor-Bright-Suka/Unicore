package com.backend.authsystem.authentication.controller;


import com.backend.authsystem.authentication.dto.assignment.AssignmentCreateRequestDto;
import com.backend.authsystem.authentication.dto.assignment.AssignmentResponseDto;
import com.backend.authsystem.authentication.dto.assignment.AssignmentUpdateRequestDto;
import com.backend.authsystem.authentication.service.AssignmentService;
import com.backend.authsystem.authentication.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "ASSIGNMENT", description = "Endpoints for managing assignments")
@RestController
@RequestMapping("/api/v1/assignment")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @Operation(
            summary = "Create Assignment",
            description = "Allows a lecturer to create a new assignment in DRAFT state. Requires 'COURSE_CREATE' or 'ASSIGNMENT_CREATE' authority. Returns an AssignmentResponse containing assignment details."
    )
    @PostMapping
    @PreAuthorize("hasAuthority('ASSIGNMENT_CREATE')")
    public ResponseEntity<ApiResponse<AssignmentResponseDto>> createAssignment(
            @RequestBody AssignmentCreateRequestDto request) {

        AssignmentResponseDto response = assignmentService.createAssignment(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Assignment created successfully", response));
    }


    @Operation(
            summary = "Update Assignment",
            description = "Allows a lecturer to update an existing assignment in DRAFT or PUBLISHED state. Requires 'ASSIGNMENT_UPDATE' authority. Returns the updated AssignmentResponse."
    )
    @PutMapping("/{assignmentId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_UPDATE')")
    public ResponseEntity<ApiResponse<AssignmentResponseDto>> updateAssignment(
            @PathVariable UUID assignmentId,
            @RequestBody AssignmentUpdateRequestDto request) {

        AssignmentResponseDto response = assignmentService.updateAssignment(assignmentId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Assignment updated successfully", response));
    }


    @Operation(
            summary = "Publish Assignment",
            description = "Allows a lecturer to publish a DRAFT assignment, making it available for students to submit. Requires 'ASSIGNMENT_PUBLISH' authority. Returns the updated AssignmentResponse with state PUBLISHED."
    )
    @PatchMapping("/{assignmentId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_PUBLISH')")
    public ResponseEntity<ApiResponse<AssignmentResponseDto>> publishAssignment(
            @PathVariable UUID assignmentId) {

        AssignmentResponseDto response = assignmentService.publishAssignment(assignmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Assignment published successfully", response));
    }


    @Operation(
            summary = "Close Assignment Submissions",
            description = "Allows a lecturer to close submissions for a PUBLISHED assignment. Requires 'ASSIGNMENT_SUBMISSION_CLOSE' authority. Returns the updated AssignmentResponse with submissions closed."
    )
    @PatchMapping("/{assignmentId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_SUBMISSION_CLOSE')")
    public ResponseEntity<ApiResponse<AssignmentResponseDto>> closeSubmissions(
            @PathVariable UUID assignmentId) {

        AssignmentResponseDto response = assignmentService.closeSubmissions(assignmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Assignment submissions closed successfully", response));
    }


    @Operation(
            summary = "Start Grading Assignment",
            description = "Allows a lecturer to start grading an assignment after submissions are closed. Requires 'ASSIGNMENT_START_GRADING' authority. Returns the updated AssignmentResponse with state GRADING_IN_PROGRESS."
    )
    @PatchMapping("/{assignmentId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_START_GRADING')")
    public ResponseEntity<ApiResponse<AssignmentResponseDto>> startGrading(
            @PathVariable UUID assignmentId) {

        AssignmentResponseDto response = assignmentService.startGrading(assignmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Assignment grading started successfully", response));
    }


    @Operation(
            summary = "Mark Assignment Graded",
            description = "Allows a lecturer to mark an assignment as graded after completing grading. Requires 'ASSIGNMENT_MARK_GRADED' authority. Returns the updated AssignmentResponse with state GRADED."
    )
    @PatchMapping("/{assignmentId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_MARK_GRADED')")
    public ResponseEntity<ApiResponse<AssignmentResponseDto>> markGraded(
            @PathVariable UUID assignmentId) {

        AssignmentResponseDto response = assignmentService.markAsGraded(assignmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Assignment marked as graded successfully", response));
    }


    @Operation(
            summary = "Archive Assignment",
            description = "Allows a lecturer to archive a graded assignment. Requires 'ASSIGNMENT_ARCHIVE' authority. Returns the updated AssignmentResponse with state ARCHIVED."
    )
    @PatchMapping("/{assignmentId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_ARCHIVE')")
    public ResponseEntity<ApiResponse<AssignmentResponseDto>> archiveAssignment(
            @PathVariable UUID assignmentId) {

        AssignmentResponseDto response = assignmentService.archiveAssignment(assignmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Assignment archived successfully", response));
    }


    @Operation(
            summary = "View Assignment",
            description = "Retrieves details of a single assignment by ID. Requires 'ASSIGNMENT_VIEW' authority. Returns an AssignmentResponse."
    )
    @GetMapping("/{assignmentId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public ResponseEntity<ApiResponse<AssignmentResponseDto>> getAssignment(
            @PathVariable UUID assignmentId) {

        AssignmentResponseDto response = assignmentService.viewAssignment(assignmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Assignment retrieved successfully", response));
    }










}
