package com.backend.authsystem.authentication.mapper;

import com.backend.authsystem.authentication.dto.EnrollmentResponseDto;
import com.backend.authsystem.authentication.entity.CourseEnrollmentEntity;

public class EnrollmentMapper {

    public static EnrollmentResponseDto toResponse(CourseEnrollmentEntity entity) {
        return new EnrollmentResponseDto(
                entity.getEnrollmentId(),
                entity.getCourse().getCourseId(),
                entity.getStudent().getUserId(),
                entity.getEnrolledAt()
        );
    }

}
