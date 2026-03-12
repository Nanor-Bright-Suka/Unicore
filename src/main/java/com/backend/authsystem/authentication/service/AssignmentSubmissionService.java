package com.backend.authsystem.authentication.service;


import com.backend.authsystem.authentication.dto.AssignmentSubmissionResponseDto;
import com.backend.authsystem.authentication.entity.AccountEntity;
import com.backend.authsystem.authentication.entity.AssignmentEntity;
import com.backend.authsystem.authentication.entity.AssignmentSubmissionEntity;
import com.backend.authsystem.authentication.entity.CourseEntity;
import com.backend.authsystem.authentication.enums.AssignmentState;
import com.backend.authsystem.authentication.enums.CourseState;
import com.backend.authsystem.authentication.exception.*;
import com.backend.authsystem.authentication.repository.AccountRepository;
import com.backend.authsystem.authentication.repository.AssignmentRepository;
import com.backend.authsystem.authentication.repository.AssignmentSubmissionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AssignmentSubmissionService {
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final AccountRepository accountRepository;
    private final AssignmentRepository assignmentRepository;
    private final AuthenticatedUserService authenticatedUserService;


    @Value("${file.upload.directory}")
    private String uploadDirectory;

    @Value("${file.upload.max-size-bytes}")
    private long maxFileSize;


    private String  sanitizeFileName(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9\\-_.]", "_");
    }

    public AssignmentSubmissionResponseDto submitAssignment(UUID assignmentId, UUID studentId, MultipartFile file) {
        log.info("Starting assignment submission: assignmentId={}, studentId={}", assignmentId, studentId);

        // 1️⃣ Validate assignment exists
        AssignmentEntity assignment = assignmentRepository.findByAssignmentId(assignmentId)
                .orElseThrow(() -> {
                    log.warn("Assignment not found: {}", assignmentId);
                    return new AssignmentNotFoundException("Assignment not found");
                });

        //Validate that course exist
        CourseEntity course = assignment.getCourse();
            if (course == null) {
                log.warn("Associated course not found for assignment: {}", assignmentId);
                throw new AssignmentNotFoundException("Associated course not found for the assignment");
            }

        // Course must be ENROLLMENT_OPEN or ACTIVE
        if (!(course.getState() == CourseState.ENROLLMENT_OPEN || course.getState() == CourseState.ACTIVE)) {
            log.warn("Course {} is not in ENROLLMENT_OPEN or ACTIVE state", course.getCourseId());
            throw new CourseStateException("Course is not open for submissions");
        }

        // Assignment must be PUBLISHED and accept submissions
        if (assignment.getState() != AssignmentState.PUBLISHED) {
            log.warn("Assignment {} is not PUBLISHED", assignmentId);
            throw new IllegalStateException("Assignment is not open for submissions");
        }

        // 2️⃣ Validate student exists
       AccountEntity student = accountRepository.findByUserId(studentId)
                .orElseThrow(() -> {
                    log.warn("Student not found: {}", studentId);
                    return new UserNotFoundException("Student not found");
                });

        // TODO: enforce student is enrolled in course

        // 3️⃣ Enforce one submission per student
        if (assignmentSubmissionRepository.existsByAssignment_AssignmentIdAndStudent_UserId(assignmentId, studentId)) {
            log.warn("Student {} has already submitted assignment {}", studentId, assignmentId);
            throw new AssignmentStateException("Student has already submitted this assignment");
        }

        if (file.getSize() > maxFileSize) {
                log.warn("File size {} exceeds max allowed {} for assignment {}", file.getSize(), maxFileSize, assignmentId);
            throw new IllegalArgumentException("File too large");
        }
        // 4️⃣ Validate file type
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            log.warn("Invalid file type for assignment {}: {}", assignmentId, file.getOriginalFilename());
            throw new AssignmentValidationException("Only PDF files are allowed");
        }

        // 5️⃣ Upload to local storage

        String objectKey = "assignments/" + assignmentId + "/" + studentId + "/" + UUID.randomUUID();
        String sanitizedFileName = sanitizeFileName(file.getOriginalFilename());
        String fullPath = uploadDirectory + "/" + objectKey;

        try {
            Path directoryPath = Paths.get(fullPath);
            Files.createDirectories(directoryPath);

            Path filePath = directoryPath.resolve(sanitizedFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File saved locally at: {}", filePath);

        } catch (IOException e) {
            log.error("Failed to save file locally: studentId={}, assignmentId={}, error={}",
                    studentId, assignmentId, e.getMessage());
            throw new RuntimeException("Failed to save file", e);
        }
        // 6️⃣ Save submission record
        AssignmentSubmissionEntity submission = new AssignmentSubmissionEntity();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setFilePath(sanitizeFileName(fullPath + "/" + sanitizedFileName)); // store S3 key
        submission.setOriginalFileName(file.getOriginalFilename());
        assignmentSubmissionRepository.save(submission);
        log.info("Assignment submission saved successfully: submissionId={}, studentId={}, assignmentId={}",
                submission.getSubmissionId(), studentId, assignmentId);
        // 7️⃣ Return response
        return new AssignmentSubmissionResponseDto(
                submission.getSubmissionId(),
                assignment.getAssignmentId(),
                student.getUserId(),
                submission.getFilePath(),
                submission.getOriginalFileName(),
                submission.getSubmittedAt(),
                submission.getMarks(),
                submission.getFeedback(),
                submission.getCreatedAt(),
                submission.getUpdatedAt()
        );


    }

    public AssignmentSubmissionResponseDto getSubmission(UUID submissionId) {
        log.info("Fetching submission: submissionId={}", submissionId);
        AssignmentSubmissionEntity submission = assignmentSubmissionRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> {
                    log.info("Fetching submission: submissionId={}", submissionId);
                    return new AssignmentNotFoundException("Submission not found");
                });

        log.info("Submission found: submissionId={}, assignmentId={}, studentId={}",
                submission.getSubmissionId(),
                submission.getAssignment().getAssignmentId(),
                submission.getStudent().getUserId());

        // Get current user
        AccountEntity currentAuthUser = authenticatedUserService.getCurrentUserAccount();
        if (currentAuthUser.getUserId() == null) {
            log.warn("Unauthenticated access attempt for submissionId={}", submissionId);
            throw new InvalidTokenException("Unauthenticated access");
        }

        AccountEntity currentUser = accountRepository.findByUserId(currentAuthUser.getUserId())
                .orElseThrow(() -> {
                    log.warn("Authenticated user not found in DB: userId={}", currentAuthUser.getUserId());
                    return new UserNotFoundException("User not found");
                });

        log.info("Current user: userId={}, role(s)={}", currentUser.getUserId(), currentUser.getRoles());

        // Check permissions
        boolean isStudentViewingOwn = submission.getStudent().getUserId().equals(currentUser.getUserId());
        boolean isLecturerViewingCourse = submission.getAssignment().getLecturer().getUserId().equals(currentUser.getUserId());

        if (!(isStudentViewingOwn || isLecturerViewingCourse)) {
            log.warn("User {} attempted unauthorized access to submission {}", currentUser.getUserId(), submissionId);
            throw new PermissionNotFoundException("You are not allowed to view this submission");
        }
        log.info("User {} authorized to view submission {}", currentUser.getUserId(), submissionId);
        return new AssignmentSubmissionResponseDto(
                submission.getSubmissionId(),
                submission.getAssignment().getAssignmentId(),
                submission.getStudent().getUserId(),
                submission.getFilePath(),
                submission.getOriginalFileName(),
                submission.getSubmittedAt(),
                submission.getMarks(),
                submission.getFeedback(),
                submission.getCreatedAt(),
                submission.getUpdatedAt()

        );
    }



    public List<AssignmentSubmissionResponseDto> getSubmissionsForAssignment(UUID assignmentId) {
        log.info("Fetching submissions for assignmentId={}", assignmentId);

        // 1️⃣ Validate assignment exists
        AssignmentEntity assignment = assignmentRepository.findByAssignmentId(assignmentId)
                .orElseThrow(() -> {
                    log.warn("Assignment not found in the getSubmissionsForAssigment method: {}", assignmentId);
                    return new AssignmentNotFoundException("Assignment not found");
                });

        log.info("Assignment found: assignmentId={}, title={}", assignment.getAssignmentId(), assignment.getTitle());

        // 2️⃣ Get current authenticated user
        AccountEntity currentAuthUser = authenticatedUserService.getCurrentUserAccount();
        if (currentAuthUser.getUserId() == null) {
            log.warn("Unauthenticated access attempt for assignmentId={}", assignmentId);
            throw new InvalidTokenException("Unauthenticated access");
        }

        AccountEntity currentUser = accountRepository.findByUserId(currentAuthUser.getUserId())
                .orElseThrow(() -> {
                    log.warn("Authenticated user not found in DB for this method: userId={}", currentAuthUser.getUserId());
                    return new UserNotFoundException("User not found");
                });

        log.info("Current user detail: userId={}, role(s)={}", currentUser.getUserId(), currentUser.getRoles());

        // 3️⃣ Fetch all submissions
        List<AssignmentSubmissionEntity> submissions = assignmentSubmissionRepository.findAllByAssignment_AssignmentId(assignmentId);
        log.info("Found {} submission(s) for assignmentId={}", submissions.size(), assignmentId);

        // 4️⃣ Filter submissions based on permissions
        List<AssignmentSubmissionEntity> authorizedSubmissions = submissions.stream()
                .filter(sub -> {
                    boolean isStudentViewingOwn = sub.getStudent().getUserId().equals(currentUser.getUserId());
                    boolean isLecturerOfCourse = sub.getAssignment().getLecturer().getUserId().equals(currentUser.getUserId());
                    return isStudentViewingOwn || isLecturerOfCourse;
                })
                .peek(sub -> log.debug("Authorized submissionId={} for studentId={}", sub.getSubmissionId(), sub.getStudent().getUserId()))
                .toList();

        log.info("Returning {} authorized submission(s) for userId={}", authorizedSubmissions.size(), currentUser.getUserId());

        // 5️⃣ Map to response DTOs
        return authorizedSubmissions.stream()
                .map(sub -> new AssignmentSubmissionResponseDto(
                        sub.getSubmissionId(),
                        sub.getAssignment().getAssignmentId(),
                        sub.getStudent().getUserId(),
                        sub.getFilePath(),
                        sub.getOriginalFileName(),
                        sub.getSubmittedAt(),
                        sub.getMarks(),
                        sub.getFeedback(),
                        sub.getCreatedAt(),
                        sub.getUpdatedAt()
                ))
                .toList();
    }


    public AssignmentSubmissionResponseDto gradeSubmission(UUID submissionId, int marks, String feedback) {
        log.info("Grading submission: submissionId={}, marks={}, feedbackLength={}", submissionId, marks, feedback == null ? 0 : feedback.length());

        // 1️⃣ Validate submission exists
        AssignmentSubmissionEntity submission = assignmentSubmissionRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> {
                    log.warn("Submission not found: {}", submissionId);
                    return new AssignmentSubmissionNotFoundException("Submission not found");
                });

        AssignmentEntity assignment = submission.getAssignment();
        if (assignment == null) {
            log.warn("Assignment not found for submission: {}", submissionId);
            throw new AssignmentNotFoundException("Assignment not found");
        }

        log.info("Assignment found in the grade submission method: assignmentId={}, title={}", assignment.getAssignmentId(), assignment.getTitle());

        // 2️⃣ Check assignment state
        if (assignment.getState() != AssignmentState.GRADING_IN_PROGRESS && assignment.getState() != AssignmentState.SUBMISSION_CLOSED) {
            log.warn("Cannot grade submission: assignment {} is not in GRADING_IN_PROGRESS or SUBMISSION_CLOSED", assignment.getAssignmentId());
            throw new IllegalStateException("Cannot grade submission unless assignment is in grading state or submission closed");
        }

        // 3️⃣ Validate marks
        if (marks < 0 || marks > assignment.getTotalMarks()) {
            log.warn("Invalid marks {} for submission {} (max marks={})", marks, submissionId, assignment.getTotalMarks());
            throw new GradeSubmissionException("Marks must be between 0 and " + assignment.getTotalMarks());
        }

        // 4️⃣ Ownership check
        AccountEntity currentAuthUser = authenticatedUserService.getCurrentUserAccount();
        if (currentAuthUser.getUserId() == null) {
            log.warn("Unauthenticated grading attempt for submissionId={}", submissionId);
            throw new InvalidTokenException("Unauthenticated access");
        }

        if (!assignment.getLecturer().getUserId().equals(currentAuthUser.getUserId())) {
            log.warn("Unauthorized grading attempt by user {} on submission {}", currentAuthUser.getUserId(), submissionId);
            throw new SecurityException("Only the lecturer  can grade submissions");
        }

        // 5️⃣ Apply grading
        submission.setMarks(marks);
        submission.setFeedback(feedback);

        log.info("Submission graded successfully: submissionId={}, gradedBy={}, marks={}", submissionId, currentAuthUser.getUserId(), marks);

        return new AssignmentSubmissionResponseDto(
                submission.getSubmissionId(),
                submission.getAssignment().getAssignmentId(),
                submission.getStudent().getUserId(),
                submission.getFilePath(),
                submission.getOriginalFileName(),
                submission.getSubmittedAt(),
                submission.getMarks(),
                submission.getFeedback(),
                submission.getCreatedAt(),
                submission.getUpdatedAt()
        );
    }

















}
