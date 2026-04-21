package com.backend.authsystem.authentication.service;


import com.backend.authsystem.authentication.dto.course_material.CreateAndUpdateRequestDto;
import com.backend.authsystem.authentication.dto.course_material.MaterialResponseDto;
import com.backend.authsystem.authentication.entity.AccountEntity;
import com.backend.authsystem.authentication.entity.CourseEntity;
import com.backend.authsystem.authentication.entity.CourseMaterialEntity;
import com.backend.authsystem.authentication.enums.CourseMaterialType;
import com.backend.authsystem.authentication.enums.CourseState;
import com.backend.authsystem.authentication.exception.*;
import com.backend.authsystem.authentication.mapper.CourseMaterialMapper;
import com.backend.authsystem.authentication.repository.CourseEnrollmentRepository;
import com.backend.authsystem.authentication.repository.CourseMaterialRepository;
import com.backend.authsystem.authentication.repository.CourseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseMaterialService {
    private final CourseRepository courseRepository;
    private final CourseMaterialRepository materialRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    @Value("${file.upload.directory}")
    private String uploadDirectory;

    @Value("${file.upload.max-size-course-material}")
    private DataSize maxFileSize;

    @CacheEvict(value = "courseMaterialsCache", allEntries = true)
    @Transactional
    public MaterialResponseDto createMaterial(UUID courseId,
                                              MultipartFile file,
                                              CreateAndUpdateRequestDto request) {
        log.info("Starting creation of course material for courseId={}, materialType={}",
                courseId, request.materialType());

        CourseEntity course = courseRepository.findByCourseId(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found"));
        log.info("Course found: id={}, title={}", course.getCourseId(), course.getTitle());

        AccountEntity currentAuthUser = authenticatedUserService.getCurrentUserAccount();
        log.info("Authenticated user: userId={}", currentAuthUser.getUserId());

        if (!course.getLecturer().getUserId().equals(currentAuthUser.getUserId())) {
            log.warn("User {} attempted to create material for course {} but is not the lecturer",
                    currentAuthUser.getUserId(), courseId);
            throw new CourseStateException("You are not the lecturer for this course");
        }

        if (!course.getState().equals(CourseState.ACTIVE)) {
            log.warn("User {} attempted to create material for unpublished course {}",
                    currentAuthUser.getUserId(), courseId);
            throw new CourseStateException("Course is not published");
        }

        String savedFilePath = null;
        validateMaterialType(request.materialType(), request.videoUrl());
        if(request.materialType() == CourseMaterialType.FILE && file != null && !file.isEmpty()) {
            log.info("Processing file upload for course material creation:  userId={}", currentAuthUser.getUserId());

            if (file.getSize() > maxFileSize.toBytes()) {
                log.warn("File size {} exceeds max allowed {} for course material creation {}", file.getSize(), maxFileSize, courseId);
                throw new FileTypeValidationException("File too large");
            }
            // 4️⃣ Validate file type
            if (file.getOriginalFilename() == null || !(file.getOriginalFilename().toLowerCase().endsWith(".pdf") || file.getOriginalFilename().toLowerCase().endsWith(".docx"))) {
                log.warn("Invalid file type for course material : {}", file.getOriginalFilename());
                throw new FileTypeValidationException("Only PDF or Docx files are allowed");
            }

            // 5️⃣ Upload to local storage

            String objectKey = "course-materials/" + courseId + "/" + currentAuthUser.getUserId() + "/" + UUID.randomUUID();
            String sanitizedFileName = sanitizeFileName(file.getOriginalFilename());
            Path directoryPath = Paths.get(uploadDirectory, objectKey);
            try {
                Files.createDirectories(directoryPath);
                Path filePath = directoryPath.resolve(sanitizedFileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                savedFilePath = filePath.toString();
                log.info("File saved locally at: {}", filePath);
            } catch (IOException e) {
                log.error("Failed to save file locally: studentId={}, assignmentId={}, error={}",
                        currentAuthUser.getUserId(), courseId, e.getMessage());
                throw new RuntimeException("Failed to save file", e);
            }
        }
        CourseMaterialEntity material = CourseMaterialEntity.builder()
                .title(request.title())
                .description(request.description())
                .materialType(request.materialType())
                .filePath(savedFilePath)
                .videoUrl(request.materialType() == CourseMaterialType.VIDEO_LINK ? request.videoUrl() : null)
                .course(course)
                .uploadedBy(currentAuthUser)
                .build();

        material = materialRepository.save(material);
        log.info("Course material saved successfully: materialId={}, courseId={}", material.getId(), courseId);

        return CourseMaterialMapper.toResponse(material);
    }

    @CacheEvict(value = "courseMaterialsCache", allEntries = true)
    @Transactional
    public MaterialResponseDto  updateMaterial(UUID courseId,
                                              UUID materialId,
                                              MultipartFile file,
                                              CreateAndUpdateRequestDto EditRequest) {

        log.info("Update on course material for courseId={}, materialType={}",
                courseId, EditRequest.materialType());

        CourseEntity course = courseRepository.findByCourseId(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found"));
        log.info("Course found for updates/edit: id={}, title={}", course.getCourseId(), course.getTitle());

        // 2️⃣ Fetch material
        CourseMaterialEntity material = materialRepository.findByIdAndIsDeletedFalse(materialId)
                .orElseThrow(() -> new CourseNotFoundException("Course material not found"));
        log.info("Course material found: materialId={}, title={}", material.getId(), material.getTitle());

        AccountEntity currentAuthUser = authenticatedUserService.getCurrentUserAccount();
        log.info("Authenticated user doing the update: userId={}", currentAuthUser.getUserId());

        // 3️⃣ Validate ownership and course state
        if (!course.getLecturer().getUserId().equals(currentAuthUser.getUserId())) {
            log.warn("User {} attempted to update material {} for course {} but is not the lecturer",
                    currentAuthUser.getUserId(), materialId, courseId);
            throw new CourseStateException("You are not the lecturer for this course");
        }

        if (!course.getState().equals(CourseState.ACTIVE)) {
            log.warn("User {} attempted to update material {} for unpublished {}",
                    currentAuthUser.getUserId(), materialId, courseId);
            throw new CourseStateException("Course is not published ");
        }
        String savedFilePath;
        validateMaterialType(EditRequest.materialType(),  EditRequest.videoUrl());
        if (EditRequest.materialType() == CourseMaterialType.FILE
                && (file == null || file.isEmpty())
                && material.getFilePath() == null) {

            log.warn("Update attempted with FILE type but no file provided for materialId={}", materialId);
            throw new FileTypeValidationException("File must be provided for FILE material");
        }

        if(EditRequest.materialType() == CourseMaterialType.FILE && file != null && !file.isEmpty()) {
            log.info("Processing file upload for course material update: materialId={}, userId={}", materialId, currentAuthUser.getUserId());

            if (file.getSize() > maxFileSize.toBytes()) {
                log.warn("File size {} exceeds max allowed {} for course material update {}", file.getSize(), maxFileSize, courseId);
                throw new FileTypeValidationException("File too large");
            }
            // 4️⃣ Validate file type
            if (file.getOriginalFilename() == null || !(file.getOriginalFilename().toLowerCase().endsWith(".pdf") || file.getOriginalFilename().toLowerCase().endsWith(".docx"))) {
                log.warn("Invalid file type for course material update: {}", file.getOriginalFilename());
                throw new FileTypeValidationException("Only PDF or Docx files are allowed");
            }


            String objectKey = "course-materials/" + courseId + "/" + currentAuthUser.getUserId() + "/" + UUID.randomUUID();
            String sanitizedFileName = sanitizeFileName(file.getOriginalFilename());
            Path directoryPath = Paths.get(uploadDirectory, objectKey);
            try {
                Files.createDirectories(directoryPath);
                Path filePath = directoryPath.resolve(sanitizedFileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                savedFilePath = filePath.toString();
                material.setFilePath(savedFilePath);
                material.setVideoUrl(null);
                log.info("File saved locally at during update: {}", filePath);

            } catch (IOException e) {
                log.error("Failed to save file locally during update: studentId={}, assignmentId={}, error={}",
                        currentAuthUser.getUserId(), courseId, e.getMessage());
                throw new RuntimeException("Failed to save file", e);
            }
        }

        if (EditRequest.materialType() == CourseMaterialType.VIDEO_LINK) {
            material.setVideoUrl(EditRequest.videoUrl());
            material.setFilePath(null); // clear file path if switching to VIDEO_LINK
        }

        // 7️⃣ Update other fields
        material.setTitle(EditRequest.title());
        material.setDescription(EditRequest.description());
        material.setMaterialType(EditRequest.materialType());

        // 8️⃣ Save material
        material = materialRepository.save(material);
        log.info("Course material updated successfully: materialId={}, courseId={}", material.getId(), courseId);

        return CourseMaterialMapper.toResponse(material);

    }

    @CacheEvict(value = "courseMaterialsCache", allEntries = true)
    @Transactional
    public void deleteMaterial(UUID materialId) {
        AccountEntity currentUser = authenticatedUserService.getCurrentUserAccount();
        UUID lecturerId = currentUser.getUserId();

        log.info("Lecturer {} requested deletion of material {}", lecturerId, materialId);

        CourseMaterialEntity material = materialRepository
                .findByIdAndIsDeletedFalse(materialId)
                .orElseThrow(() -> {
                    log.warn("Material {} not found or already deleted", materialId);
                    return new CourseMaterialNotFoundException("Material not found");
                });

        if (!material.getCourse().getLecturer().getUserId().equals(currentUser.getUserId())) {
            log.warn("Lecturer {} attempted to delete material {} without permission", lecturerId, materialId);
            throw new CourseStateException("You are not allowed to delete this material");
        }

        material.setDeleted(true);
        materialRepository.save(material);

        log.info("Material {} successfully marked as deleted by lecturer {}", materialId, lecturerId);
    }

    @Cacheable(
            value = "courseMaterialsCache",
            key = "#courseId + '_' + @authenticatedUserService.getCurrentUserId()"
    )
        public List<MaterialResponseDto> getMaterialsByCourse(UUID courseId) {
        log.info("Fetching materials for courseId={}", courseId);
        AccountEntity currentUser = authenticatedUserService.getCurrentUserAccount();
        log.info("Authenticated user fetching materials: userId={}", currentUser.getUserId());

        // 1️⃣ Check if user is allowed to access materials (student enrolled OR lecturer)
        boolean isLecturer = courseRepository.findByCourseId(courseId)
                .map(course -> course.getLecturer().getUserId().equals(currentUser.getUserId()))
                .orElse(false);

        boolean isStudentEnrolled = courseEnrollmentRepository
                .existsByCourse_CourseIdAndStudent_UserIdAndIsDeletedFalse(courseId, currentUser.getUserId());

        if (!isLecturer && !isStudentEnrolled) {
            log.warn("User {} attempted to access materials for course {} but is not enrolled or lecturer",
                    currentUser.getUserId(), courseId);
            throw new EnrollmentConflictException("You are not enrolled in this course");
        }
        log.info("User {} is authorized to access materials for course {}", currentUser.getUserId(), courseId);

        List<CourseMaterialEntity> materials = materialRepository.findAllByCourse_CourseIdAndIsDeletedFalse(courseId);
        log.info("{} material(s) found for course {}", materials.size(), courseId);

        List<MaterialResponseDto> responses = materials.stream()
                .map(CourseMaterialMapper::toResponse)
                .collect(Collectors.toList());

        log.info("Returning {} material(s) for course {}", responses.size(), courseId);
        return responses;

        }


    @Transactional
    public MaterialResponseDto getMaterial(UUID materialId) {

        log.info("Fetching course material: materialId={}", materialId);

        CourseMaterialEntity material = materialRepository
                .findByIdAndIsDeletedFalse(materialId)
                .orElseThrow(() -> {
                    log.warn("Course material not found: materialId={}", materialId);
                    return new CourseNotFoundException("Material not found");
                });

        AccountEntity currentUser = authenticatedUserService.getCurrentUserAccount();
        UUID courseId = material.getCourse().getCourseId();

        if (material.getCourse().getLecturer().getUserId().equals(currentUser.getUserId())) {
            log.info("Lecturer {} accessing material {}", currentUser.getUserId(), materialId);
            return CourseMaterialMapper.toResponse(material);
        }


        boolean enrolled = courseEnrollmentRepository
                .existsByCourse_CourseIdAndStudent_UserIdAndIsDeletedFalse(courseId, currentUser.getUserId());

        if (!enrolled) {
            log.warn("User {} attempted to access material {} but is not enrolled in course {}",
                    currentUser.getUserId(), materialId, courseId);
            throw new EnrollmentConflictException("You are not enrolled in this course");
        }

        log.info("Student {} accessing material {} for course {}",
                currentUser.getUserId(), materialId, courseId);

        return CourseMaterialMapper.toResponse(material);
    }


    @Transactional
    public Resource downloadMaterial(UUID materialId) {

        log.info("Download request for materialId={}", materialId);

        AccountEntity currentUser = authenticatedUserService.getCurrentUserAccount();

        CourseMaterialEntity material = materialRepository
                .findByIdAndIsDeletedFalse(materialId)
                .orElseThrow(() -> {
                    log.warn("Material not found {}", materialId);
                    return new CourseMaterialNotFoundException("Material not found");
                });

        CourseEntity course = material.getCourse();

        boolean isLecturer = course.getLecturer().getUserId()
                .equals(currentUser.getUserId());

        boolean isStudent = courseEnrollmentRepository
                .existsByCourse_CourseIdAndStudent_UserIdAndIsDeletedFalse(
                        course.getCourseId(),
                        currentUser.getUserId()
                );

        if (!isLecturer && !isStudent) {
            log.warn("Unauthorized access attempt by user={}", currentUser.getUserId());
            throw new CourseMaterialStateException("You are not allowed to access this material");
        }

        Path filePath = Paths.get(material.getFilePath());
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            log.error("File not found on disk: {}", filePath);
            throw new CourseMaterialNotFoundException("File not found");
        }

        log.info("Material {} downloaded by user {}", materialId, currentUser.getUserId());

        return resource;
    }


    private String  sanitizeFileName(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9\\-_.]", "_");
    }

    private void validateMaterialType(CourseMaterialType type, String videoUrl) {
        if (type == CourseMaterialType.VIDEO_LINK && (videoUrl == null || videoUrl.isBlank())) {
            throw new FileTypeValidationException("Video URL must be provided for VIDEO_LINK material");
        }
    }


}
