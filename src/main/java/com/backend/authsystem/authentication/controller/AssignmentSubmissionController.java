package com.backend.authsystem.authentication.controller;


import com.backend.authsystem.authentication.dto.AssignmentSubmissionResponseDto;
import com.backend.authsystem.authentication.service.AssignmentSubmissionService;
import com.backend.authsystem.authentication.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "ASSIGNMENT SUBMISSION", description = "Endpoints for submitting and grading assignments")
@RestController
@RequestMapping("/api/v1/assignment-submission")
@RequiredArgsConstructor
public class AssignmentSubmissionController {

    private final AssignmentSubmissionService submissionService;

    @Operation(
            summary = "Submit Assignment",
            description = "Allows a student to submit a PDF file for a published assignment. Requires 'ASSIGNMENT_SUBMIT' authority. Returns the submitted AssignmentSubmissionResponse."
    )
    @PostMapping("/{assignmentId}/submit")
    @PreAuthorize("hasAuthority('ASSIGNMENT_SUBMIT')")
    public ResponseEntity<ApiResponse<AssignmentSubmissionResponseDto>> submitAssignment(
            @PathVariable UUID assignmentId,
            @RequestParam UUID studentId,
            @RequestParam MultipartFile file) {

        AssignmentSubmissionResponseDto response = submissionService.submitAssignment(assignmentId, studentId, file);
        return ResponseEntity.ok(new ApiResponse<>(true, "Assignment submitted successfully", response));
    }


    @Operation(
            summary = "View Submission",
            description = "Retrieves a single assignment submission by ID. Requires 'ASSIGNMENT_SUBMISSION_VIEW' authority. Returns the AssignmentSubmissionResponse."
    )
    @GetMapping("/{submissionId}/view")
    @PreAuthorize("hasAuthority('ASSIGNMENT_SUBMISSION_VIEW')")
    public ResponseEntity<ApiResponse<AssignmentSubmissionResponseDto>> getSubmission(
            @PathVariable UUID submissionId) {

        AssignmentSubmissionResponseDto response = submissionService.getSubmission(submissionId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Submission retrieved successfully", response));
    }


    @Operation(
            summary = "View All Submissions for Assignment",
            description = "Retrieves all submissions for a specific assignment. Requires 'ASSIGNMENT_SUBMISSION_VIEW' authority. Returns a list of AssignmentSubmissionResponse."
    )
    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_SUBMISSION_VIEW_ALL')")
    public ResponseEntity<ApiResponse<List<AssignmentSubmissionResponseDto>>> getSubmissionsForAssignment(
            @PathVariable UUID assignmentId) {

        List<AssignmentSubmissionResponseDto> response = submissionService.getSubmissionsForAssignment(assignmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Submissions retrieved successfully", response));
    }

    @Operation(
            summary = "Grade Assignment Submission",
            description = "Allows a lecturer to grade a student's submission. Requires 'ASSIGNMENT_GRADE' authority. Returns the updated AssignmentSubmissionResponse."
    )
    @PatchMapping("/{submissionId}/grade")
    @PreAuthorize("hasAuthority('ASSIGNMENT_GRADE')")
    public ResponseEntity<ApiResponse<AssignmentSubmissionResponseDto>> gradeSubmission(
            @PathVariable UUID submissionId,
            @RequestParam int marks,
            @RequestParam(required = false) String feedback) {

        AssignmentSubmissionResponseDto response = submissionService.gradeSubmission(submissionId, marks, feedback);
        return ResponseEntity.ok(new ApiResponse<>(true, "Submission graded successfully", response));
    }
























}
