package com.backend.authsystem.authentication.mapper;

import com.backend.authsystem.authentication.dto.AssignmentCreateRequestDto;
import com.backend.authsystem.authentication.dto.AssignmentResponseDto;
import com.backend.authsystem.authentication.dto.AssignmentUpdateRequestDto;
import com.backend.authsystem.authentication.entity.AssignmentEntity;
import com.backend.authsystem.authentication.entity.CourseEntity;

public class AssignmentMapper {


 public static AssignmentResponseDto toResponseDto(AssignmentEntity assignment) {
     return AssignmentResponseDto.builder()
                .assignmentId(assignment.getAssignmentId())
                .courseId(assignment.getCourse().getCourseId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .state(assignment.getState())
                .dueDate(assignment.getDueDate())
                .totalMarks(assignment.getTotalMarks())
                .lecturerId(assignment.getLecturer().getUserId())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();

    }

    // inside Assignment.java
    public static AssignmentEntity createDraftAssignment(AssignmentCreateRequestDto request, CourseEntity course) {
      return AssignmentEntity.builder()
//                .assignmentId(UUID.randomUUID())
                .title(request.title())
                .description(request.description())
                .course(course)
                .dueDate(request.dueDate())
                .totalMarks(request.totalMarks())
                .lecturer(course.getLecturer())
                .build();
    }

public static void updateEntity(AssignmentUpdateRequestDto request, AssignmentEntity assignment) {
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());
        assignment.setTotalMarks(request.totalMarks());

    }







}
