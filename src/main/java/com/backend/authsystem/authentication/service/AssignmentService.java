package com.backend.authsystem.authentication.service;


import com.backend.authsystem.authentication.dto.assignment.AssignmentCreateRequestDto;
import com.backend.authsystem.authentication.dto.assignment.AssignmentResponseDto;
import com.backend.authsystem.authentication.dto.assignment.AssignmentUpdateRequestDto;
import com.backend.authsystem.authentication.entity.AccountEntity;
import com.backend.authsystem.authentication.entity.AssignmentEntity;
import com.backend.authsystem.authentication.entity.CourseEntity;
import com.backend.authsystem.authentication.enums.AssignmentState;
import com.backend.authsystem.authentication.enums.CourseState;
import com.backend.authsystem.authentication.exception.*;
import com.backend.authsystem.authentication.mapper.AssignmentMapper;
import com.backend.authsystem.authentication.repository.AssignmentRepository;
import com.backend.authsystem.authentication.repository.CourseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final AuthenticatedUserService authenticatedUserService;

    private CourseEntity getCourseEntity(UUID courseId) {
        log.debug("Fetching course with courseId={}", courseId);
        return courseRepository.findByCourseId(courseId)
                .orElseThrow(() -> {
                    log.warn("Course with courseId={} not found", courseId);
                    return new CourseNotFoundException("Course not found");
                });
    }

    private AssignmentEntity getAssignmentEntity(UUID id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() ->{
                        log.warn("Assignment {} not found", id);
                return new AssignmentNotFoundException("Assignment not found");
                });
    }


    public AssignmentResponseDto createAssignment(AssignmentCreateRequestDto request) {
        log.info("User requested to create assignment {}", request.courseId());
     AccountEntity currentUser = authenticatedUserService.getCurrentUserAccount();

        CourseEntity course = getCourseEntity(request.courseId());
        log.debug("Fetched course entity to create assignment: {}", course.getTitle());

        if (!(course.getState() == CourseState.ENROLLMENT_OPEN || course.getState() == CourseState.ACTIVE)) {
            log.error("Assignment creation blocked: course {} in state {}", course.getCourseId(), course.getState());
            throw new CourseStateException("Course not open for assignment");
        }

        if (!course.getLecturer().getUserId().equals(currentUser.getUserId())) {
            log.warn("Unauthorized assignment creation attempt by user {} for course {}", currentUser.getUserId(), course.getCourseId());
            throw new AssignmentPermissionException("Only the course lecturer can create assignments for this course");
        }

        AssignmentEntity assignment = AssignmentMapper.createDraftAssignment(request, course);

     AssignmentEntity  savedAssignment =   assignmentRepository.save(assignment);
        log.info("User {} created assignment '{}' for course {}", currentUser.getUserId(), request.title(), course.getCourseId());

        return AssignmentMapper.toResponseDto(savedAssignment);
    }


    public AssignmentResponseDto updateAssignment(UUID assignmentId, AssignmentUpdateRequestDto request) {

        log.info("User requested update for assignment {}", assignmentId);
        AccountEntity currentUser = authenticatedUserService.getCurrentUserAccount();

        AssignmentEntity assignment = getAssignmentEntity(assignmentId);
        log.debug("Fetched assignment entity to update assignment: {}", assignment.getAssignmentId());
        // --- State check ---
        if (assignment.getState() != AssignmentState.DRAFT && assignment.getState() != AssignmentState.PUBLISHED) {
            log.error("Assignment {} in state {} cannot be updated", assignment.getAssignmentId(), assignment.getState());
            throw new AssignmentStateException("Assignment cannot be updated in current state");
        }

        if (currentUser.getUserId() == null || !assignment.getLecturer().getUserId().equals(currentUser.getUserId())) {
            log.warn("Unauthorized assignment update attempt by user {} on assignment {}", currentUser.getUserId(), assignment.getAssignmentId());
            throw new AssignmentPermissionException("Only the assignment owner can update this assignment");
        }

        AssignmentMapper.updateEntity(request, assignment);
        log.info("User {} updated assignment {} for course {}", currentUser.getUserId(), assignment.getAssignmentId(), assignment.getCourse().getCourseId());

        return AssignmentMapper.toResponseDto(assignment);
    }




    public AssignmentResponseDto publishAssignment(UUID assignmentId) {
        log.info("User requested publish assignment {}", assignmentId);
        AccountEntity currentUser = authenticatedUserService.getCurrentUserAccount();

        AssignmentEntity assignment = getAssignmentEntity(assignmentId);
        log.debug("Fetched course entity to publish assignment: {}", assignment.getAssignmentId());
        CourseEntity course = getCourseEntity(assignment.getCourse().getCourseId());
        log.debug("Fetched course entity to publish assignment: {}", course.getCourseId());

        // --- State check ---
        if (assignment.getState() != AssignmentState.DRAFT) {
            log.error("Assignment {} in state {} cannot be published.Only DRAFT assignments can be published\"", assignment.getAssignmentId(), assignment.getState());
            throw new AssignmentStateException("Assignments cannot be published");
        }

        // --- Required fields check ---
        if (assignment.getDueDate() == null || assignment.getTotalMarks() <= 0) {
            log.error("Assignment {} missing due date or total marks. Assignment must have a due date and total marks before publishing", assignment.getAssignmentId());
            throw new AssignmentValidationException("Assignment Details Incomplete");
        }

        // --- Course state check ---
        if (course.getState() != CourseState.ENROLLMENT_OPEN && course.getState() != CourseState.ACTIVE) {
            log.error("Cannot publish assignment {}: course {} not open or active", assignment.getAssignmentId(), course.getCourseId());
            throw new CourseStateException("Assignment cannot be published");
        }

        // --- Owner check ---
        if (currentUser.getUserId() == null || !assignment.getLecturer().getUserId().equals(currentUser.getUserId())) {
            throw new AssignmentPermissionException("You do not have permission to publish this assignment");
        }

        // --- Publish ---
        assignment.setState(AssignmentState.PUBLISHED);
        log.info("User {} published assignment {} for course {}", currentUser.getUserId(), assignment.getAssignmentId(), course.getCourseId());

        return AssignmentMapper.toResponseDto(assignment);
    }


    public AssignmentResponseDto closeSubmissions(UUID assignmentId) {
        log.info("User requested close assignment submission {}", assignmentId);
        AccountEntity currentUser = authenticatedUserService.getCurrentUserAccount();

        AssignmentEntity assignment = getAssignmentEntity(assignmentId);
        log.debug("Fetched assignment entity to close assignment submission: {}", assignment.getAssignmentId());

        if (assignment.getState() != AssignmentState.PUBLISHED) {
            log.warn("Cannot close submissions for assignment {}: current state is {}", assignment.getAssignmentId(), assignment.getState());
            throw new AssignmentStateException("Cannot close submissions for assignments");
        }

        // Ownership check
        if (currentUser.getUserId() == null || !assignment.getLecturer().getUserId().equals(currentUser.getUserId())) {
            log.warn("Unauthorized attempt by user {} to close submissions for assignment {}", currentUser.getUserId(), assignment.getAssignmentId());
            throw new AssignmentPermissionException("You do not have the permission to  close submissions");
        }

        assignment.setState(AssignmentState.SUBMISSION_CLOSED);
        log.info("User {} closed submissions for assignment {} (course {})", currentUser.getUserId(), assignment.getAssignmentId(), assignment.getCourse().getCourseId());
        return AssignmentMapper.toResponseDto(assignment);
    }


    public AssignmentResponseDto startGrading(UUID assignmentId) {
        log.info("User requested to start grading assignment submission {}", assignmentId);
        AccountEntity currentUser = authenticatedUserService.getCurrentUserAccount();

        AssignmentEntity assignment = getAssignmentEntity(assignmentId);
        log.debug("Fetched assignment entity to start grading assignment: {}", assignment.getAssignmentId());

        if (assignment.getState() != AssignmentState.SUBMISSION_CLOSED) {
            log.warn("Cannot start grading assignment submissions for assignment {}: current state is {}", assignment.getAssignmentId(), assignment.getState());
            throw new AssignmentStateException("Cannot start grading assignment submissions");
        }

        if (currentUser.getUserId() == null || !assignment.getLecturer().getUserId().equals(currentUser.getUserId())) {
            log.warn("Unauthorized attempt by user {} to start grading assignment submissions for assignment {}", currentUser.getUserId(), assignment.getAssignmentId());
            throw new AssignmentPermissionException("You do not have the permission to start grading this assignment");
        }

        assignment.setState(AssignmentState.GRADING_IN_PROGRESS);
        log.info("User {} start grading assignment submissions for assignment {} (course {})", currentUser.getUserId(), assignment.getAssignmentId(), assignment.getCourse().getCourseId());
        return AssignmentMapper.toResponseDto(assignment);
    }

    public AssignmentResponseDto markAsGraded(UUID assignmentId) {
        log.info("User requested to mark  assignment as graded {}", assignmentId);
        AccountEntity currentUser = authenticatedUserService.getCurrentUserAccount();

        AssignmentEntity assignment = getAssignmentEntity(assignmentId);
        log.debug("Fetched assignment entity to mark assignment as graded: {}", assignment);

        if (assignment.getState() != AssignmentState.GRADING_IN_PROGRESS) {
            log.warn("Cannot mark assignment submissions as graded {}: current state is {}", assignment.getAssignmentId(), assignment.getState());
            throw new AssignmentStateException("Cannot mark assignment as graded");
        }

        if (currentUser.getUserId() == null || !assignment.getLecturer().getUserId().equals(currentUser.getUserId())) {
            log.warn("Unauthorized attempt by user {} to mark assignment submissions as graded {}", currentUser.getUserId(), assignment.getAssignmentId());
            throw new AssignmentPermissionException("You do not have the permission to grade assignment");
        }

        assignment.setState(AssignmentState.GRADED);
        log.info("User {} mark assignment submissions as graded {} (course {})", currentUser.getUserId(), assignment.getAssignmentId(), assignment.getCourse().getCourseId());
        return AssignmentMapper.toResponseDto(assignment);
    }

    public AssignmentResponseDto archiveAssignment(UUID assignmentId) {
        log.info("User requested to archive  assignment {}", assignmentId);
        AccountEntity currentUser = authenticatedUserService.getCurrentUserAccount();

        AssignmentEntity assignment = getAssignmentEntity(assignmentId);
        log.debug("Fetched assignment entity to mark as archive: {}", assignment);

        if (assignment.getState() != AssignmentState.GRADED) {
            log.warn("Cannot mark assignment as archive {}: current state is {}", assignment.getAssignmentId(), assignment.getState());
            throw new AssignmentStateException("Cannot archive assignment");
        }

        if (currentUser.getUserId() == null || !assignment.getLecturer().getUserId().equals(currentUser.getUserId())) {
            log.warn("Unauthorized attempt by user {} to archive assignment{}", currentUser.getUserId(), assignment.getAssignmentId());
            throw new AssignmentPermissionException("You do not have the permission to archive assignment");
        }

        assignment.setState(AssignmentState.ARCHIVED);
        log.info("User {} mark assignment as archive {} (course {})", currentUser.getUserId(), assignment.getAssignmentId(), assignment.getCourse().getCourseId());
        return AssignmentMapper.toResponseDto(assignment);
    }

    public AssignmentResponseDto viewAssignment(UUID assignmentId) {
        log.info("User requested to view assignment {}", assignmentId);
        AccountEntity currentUser = authenticatedUserService.getCurrentUserAccount();

        AssignmentEntity assignment = getAssignmentEntity(assignmentId);
        log.debug("Fetched assignment entity to view courses: {}", assignment.getAssignmentId());
        ///  Check whether student is enrolled in the course or lecturer is the owner of the assignment
        if (!assignment.getLecturer().getUserId().equals(currentUser.getUserId())) {
            log.warn("Unauthorized user with id{} to view assignment{}", currentUser.getUserId(), assignment.getAssignmentId());
            throw new AssignmentPermissionException("You do not have the permission to view assignment");
        }
        return AssignmentMapper.toResponseDto(assignment);
    }
















}
