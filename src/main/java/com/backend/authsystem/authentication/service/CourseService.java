package com.backend.authsystem.authentication.service;


import com.backend.authsystem.authentication.dto.CourseCreateRequestDto;
import com.backend.authsystem.authentication.dto.CourseResponseDto;
import com.backend.authsystem.authentication.dto.CourseUpdateRequestDto;
import com.backend.authsystem.authentication.entity.AccountEntity;
import com.backend.authsystem.authentication.entity.CourseEntity;
import com.backend.authsystem.authentication.enums.CourseState;
import com.backend.authsystem.authentication.exception.CourseNotFoundException;
import com.backend.authsystem.authentication.exception.InvalidCourseStateException;
import com.backend.authsystem.authentication.mapper.CourseMapper;

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
public class CourseService {

    private final CourseRepository courseRepository;
    private final AuthenticatedUserService authenticatedUserService;


    //helper
    private CourseEntity getCourseEntity(UUID courseId) {
        log.debug("Fetching course with courseId={}", courseId);
        return courseRepository.findByCourseId(courseId)
                .orElseThrow(() -> {
                    log.warn("Course with courseId={} not found", courseId);
                    return new CourseNotFoundException("Course not found");
                });

    }


    public CourseResponseDto createCourse(CourseCreateRequestDto request) {
        AccountEntity user = authenticatedUserService.getCurrentUserAccount();

        CourseEntity course = CourseMapper.toEntity(request, user);
        log.debug("Mapped CourseCreateRequestDto to Course entity: {}", course);

      courseRepository.save(course);
        log.info("Course {} created successfully by user {}", course.getCourseId(), user.getEmail());

        return CourseMapper.toResponse(course);
    }


    public CourseResponseDto  updateCourse(UUID courseId, CourseUpdateRequestDto request) {
        log.info("User requested update for course {}", courseId);
         authenticatedUserService.getCurrentUserAccount();

        CourseEntity course = getCourseEntity(courseId);
        log.debug("Fetched course entity for update: {}", course);

        if (course.getState() != CourseState.CREATED && course.getState() != CourseState.PUBLISHED) {
            log.warn("Blocked course update. courseId={}, state={}", courseId, course.getState());
            throw new InvalidCourseStateException("Course cannot be updated in its current state");
        }

        CourseMapper.updateEntity(course, request);
        log.debug("Course entity after update mapping: {}", course);
        return CourseMapper.toResponse(course);
    }


    public CourseResponseDto publishCourse(UUID courseId) {
        log.info("User requested publish for course {}", courseId);
          authenticatedUserService.getCurrentUserAccount();

        CourseEntity course = getCourseEntity(courseId);
        log.debug("Fetched course entity for publishing: {}", course);


        if (course.getState() != CourseState.CREATED) {
            log.warn("Blocked publish attempt. courseId={}, state={}", courseId, course.getState());
            throw new InvalidCourseStateException("Only courses  CREATED courses can be published");
        }

        course.setState(CourseState.PUBLISHED);
        log.info("Course {} published successfully", courseId);
        log.debug("Course entity after publishing: {}", course);

        return CourseMapper.toResponse(course);
    }



    public CourseResponseDto openEnrollment(UUID courseId) {
        log.info("User requested enrollment for course {}", courseId);

        authenticatedUserService.getCurrentUserAccount();

        CourseEntity course = getCourseEntity(courseId);
        log.debug("Fetched course entity for enrollment: {}", course);

        if (course.getState() != CourseState.PUBLISHED) {
            log.warn("Blocked enrollment attempt. courseId={}, state={}", courseId, course.getState());
            throw new InvalidCourseStateException("Enrollment can only be opened for PUBLISHED courses");
        }

        // Optional: check maxCapacity if needed

        course.setState(CourseState.ENROLLMENT_OPEN);
        log.info("Course {} enrolled successfully", courseId);
        log.debug("Course entity after enrollment: {}", course);

        return CourseMapper.toResponse(course);
    }

    
    public CourseResponseDto closeEnrollment(UUID courseId) {
        log.info("User requested close enrollment for course {}", courseId);
        authenticatedUserService.getCurrentUserAccount();

        CourseEntity course = getCourseEntity(courseId);
        log.debug("Fetched course entity for close enrollment: {}", course);

        if (course.getState() != CourseState.ENROLLMENT_OPEN) {
            log.warn("Blocked close enrollment attempt. courseId={}, state={}", courseId, course.getState());
            throw new InvalidCourseStateException("Enrollment can only be closed for courses with open enrollment");
        }

        course.setState(CourseState.ENROLLMENT_CLOSED);
        log.info("Course {} close enrollment successfully", courseId);
        log.debug("Course entity after close enrollment: {}", course);

        return CourseMapper.toResponse(course);
    }

    public CourseResponseDto startCourse(UUID courseId) {
        log.info("User requested start course {}", courseId);
        authenticatedUserService.getCurrentUserAccount();

        CourseEntity course = getCourseEntity(courseId);
        log.debug("Fetched course entity for start course: {}", course);

        if (course.getState() != CourseState.ENROLLMENT_CLOSED) {
            log.warn("Blocked start course attempt. courseId={}, state={}", courseId, course.getState());
            throw new InvalidCourseStateException("Course can only be started after enrollment is closed");
        }

        course.setState(CourseState.ACTIVE);
        log.info("Course {} started successfully", courseId);
        log.debug("Course entity after start course: {}", course);

        return CourseMapper.toResponse(course);
    }

    public CourseResponseDto completeCourse(UUID courseId) {
        log.info("User requested course completion {}", courseId);
        authenticatedUserService.getCurrentUserAccount();

        CourseEntity course = getCourseEntity(courseId);
        log.debug("Fetched course entity for course completion: {}", course);

        if (course.getState() != CourseState.ACTIVE) {
            log.warn("Blocked course completion attempt. courseId={}, state={}", courseId, course.getState());
            throw new InvalidCourseStateException("Only ACTIVE courses can be completed");
        }

        course.setState(CourseState.COMPLETED);
        log.info("Course {} completed successfully", courseId);
        log.debug("Course entity after course completion: {}", course);

        return CourseMapper.toResponse(course);
    }

    public CourseResponseDto archiveCourse(UUID courseId) {
        log.info("User requested course archive {}", courseId);
        authenticatedUserService.getCurrentUserAccount();

        CourseEntity course = getCourseEntity(courseId);
        log.debug("Fetched course entity for course archive: {}", course);

        if (course.getState() != CourseState.COMPLETED) {
            log.warn("Blocked course archive attempt. courseId={}, state={}", courseId, course.getState());
            throw new InvalidCourseStateException("Only COMPLETED courses can be archived");
        }

        course.setState(CourseState.ARCHIVED);
        log.info("Course {} archived successfully", courseId);
        log.debug("Course entity after course archive: {}", course);

        return CourseMapper.toResponse(course);
    }

    public CourseResponseDto getCourseById(UUID courseId) {
        log.info("User requested course details {}", courseId);
        return CourseMapper.toResponse(getCourseEntity(courseId));
    }

    public List<CourseResponseDto> getAllCourses() {
        log.info("Fetching all courses");

        List<CourseEntity> courses = courseRepository.findAll();
        log.debug("Fetched {} course entities from database", courses.size());

        List<CourseResponseDto> responses = courses.stream()
                .map(CourseMapper::toResponse)
                .collect(Collectors.toList());

        log.info("Returning {} courses to client", responses.size());

        return responses;
    }






































}
