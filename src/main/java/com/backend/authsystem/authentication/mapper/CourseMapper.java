package com.backend.authsystem.authentication.mapper;


import com.backend.authsystem.authentication.dto.CourseCreateRequestDto;
import com.backend.authsystem.authentication.dto.CourseResponseDto;
import com.backend.authsystem.authentication.dto.CourseUpdateRequestDto;
import com.backend.authsystem.authentication.entity.AccountEntity;
import com.backend.authsystem.authentication.entity.CourseEntity;
import com.backend.authsystem.authentication.enums.CourseState;
import org.springframework.stereotype.Component;



@Component
public class CourseMapper {
    // Map CreateRequest → Entity
    public static CourseEntity toEntity(CourseCreateRequestDto request, AccountEntity user) {
        return CourseEntity.builder()
                .code(request.code())
                .title(request.title())
                .description(request.description())
                .state(CourseState.CREATED)
                .semester(request.semester())
                .academicYear(request.academicYear())
              //  .maxCapacity(request.maxCapacity())
                .lecturer(user)
//                .createdAt(Instant.now())
//                .updatedAt(Instant.now())
                .build();

    }

    // Map UpdateRequest → Entity
    public static void updateEntity(CourseEntity course, CourseUpdateRequestDto request) {
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setSemester(request.semester());
        course.setAcademicYear(request.academicYear());
        //course.setMaxCapacity(request.maxCapacity());

    }

    public static CourseResponseDto toResponse(CourseEntity course) {
        return new CourseResponseDto(
                course.getCourseId(),
                course.getCode(),
                course.getTitle(),
                course.getDescription(),
                course.getState(),
                course.getSemester(),
                course.getAcademicYear(),
               // course.getMaxCapacity(),
                course.getLecturer().getUserId().toString(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }

}
