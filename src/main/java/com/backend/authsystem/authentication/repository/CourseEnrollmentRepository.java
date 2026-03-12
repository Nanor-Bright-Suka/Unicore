package com.backend.authsystem.authentication.repository;

import com.backend.authsystem.authentication.entity.CourseEnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollmentEntity, UUID> {
    long countByCourse_CourseIdAndIsDeletedFalse(UUID courseId);

    boolean existsByCourse_CourseIdAndStudent_UserIdAndIsDeletedFalse(UUID courseId, UUID studentId);

    List<CourseEnrollmentEntity> findAllByCourse_CourseIdAndIsDeletedFalse(UUID courseId);

}
