package com.backend.authsystem.authentication.service;


import com.backend.authsystem.authentication.dto.EnrollmentResponseDto;
import com.backend.authsystem.authentication.entity.AccountEntity;
import com.backend.authsystem.authentication.entity.CourseEnrollmentEntity;
import com.backend.authsystem.authentication.entity.CourseEntity;
import com.backend.authsystem.authentication.enums.CourseState;
import com.backend.authsystem.authentication.exception.CourseNotFoundException;
import com.backend.authsystem.authentication.exception.CourseStateException;
import com.backend.authsystem.authentication.exception.EnrollmentConflictException;
import com.backend.authsystem.authentication.exception.EnrollmentNotFoundException;
import com.backend.authsystem.authentication.mapper.EnrollmentMapper;
import com.backend.authsystem.authentication.repository.CourseEnrollmentRepository;
import com.backend.authsystem.authentication.repository.CourseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EnrollmentService {
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public EnrollmentResponseDto enrollStudent(UUID courseId) {
        log.info("Enrollment request:  for courseId={}", courseId);
        AccountEntity currentAuthUser = authenticatedUserService.getCurrentUserAccount();

        // 1️⃣ Check course exists
        CourseEntity course = courseRepository.findByCourseId(courseId)
                .orElseThrow(() -> {
                    log.warn("Course not found: courseId={}", courseId);
                    return new CourseNotFoundException("Course not found");
                });
        log.info("Found course: {} (state={})", course.getTitle(), course.getState());

        if (!(course.getState().equals(CourseState.ENROLLMENT_OPEN))) {
            log.warn("Course enrollment not open for courseId={}", courseId);
            throw new CourseStateException("Course enrollment is not open");
        }
        log.info("Course enrollment is open for courseId={}", courseId);

        if (enrollmentRepository.existsByCourse_CourseIdAndStudent_UserIdAndIsDeletedFalse(courseId, currentAuthUser.getUserId())) {
            log.warn("Student already enrolled: studentId={} courseId={}", currentAuthUser.getUserId(), courseId);
            throw new EnrollmentConflictException("Student already enrolled");
        }
        log.info("Student not enrolled yet: studentId={}", currentAuthUser.getUserId());

        // 4️⃣ Check capacity
        long enrolledCount = enrollmentRepository.countByCourse_CourseIdAndIsDeletedFalse(courseId);
        log.info("Current enrolled count={} for courseId={}", enrolledCount, courseId);
        if (enrolledCount >= course.getMaxCapacity()) {
            log.warn("Course capacity reached: courseId={} capacity={}", courseId, course.getMaxCapacity());
            throw new CourseStateException("Course capacity reached");
        }

        // 5️⃣ Get authenticated student
        log.info("Authenticated student: userId={}", currentAuthUser.getUserId());

        // 6️⃣ Create enrollment
        CourseEnrollmentEntity enrollment = CourseEnrollmentEntity.builder()
                .course(course)
                .student(currentAuthUser)
                .build();
        enrollment = enrollmentRepository.save(enrollment);
        log.info("Enrollment created: enrollmentId={}", enrollment.getEnrollmentId());

        // 7️⃣ Return response
        return EnrollmentMapper.toResponse(enrollment);
    }

    public long getEnrolledCount(UUID courseId) {
        long count = enrollmentRepository.countByCourse_CourseIdAndIsDeletedFalse(courseId);
        log.info("Enrolled count for courseId={} is {}", courseId, count);
        return count;
    }

    public List<EnrollmentResponseDto> getEnrolledStudents(UUID courseId) {
        log.info("Fetching enrolled students for courseId={}", courseId);

        List<CourseEnrollmentEntity> enrollments = enrollmentRepository.findAllByCourse_CourseIdAndIsDeletedFalse(courseId);

        if (enrollments.isEmpty()) {
            log.warn("No students enrolled for courseId={}", courseId);
            throw new EnrollmentNotFoundException("No students enrolled for this course");
        }

        log.info("Found {} students enrolled for courseId={}", enrollments.size(), courseId);

        return enrollments.stream()
                .map(EnrollmentMapper::toResponse)
                .peek(dto -> log.debug("Enrolled studentId={}", dto.studentId()))
                .collect(Collectors.toList());
    }













}
