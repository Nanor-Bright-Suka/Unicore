package com.backend.authsystem.authentication.controller;


import com.backend.authsystem.authentication.dto.course_material.CreateAndUpdateRequestDto;
import com.backend.authsystem.authentication.dto.course_material.MaterialResponseDto;
import com.backend.authsystem.authentication.service.CourseMaterialService;
import com.backend.authsystem.authentication.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "COURSE MATERIAL", description = "Endpoints for managing course materials")
@RestController
@RequestMapping("/api/v1/course-materials")
@RequiredArgsConstructor
public class CourseMaterialController {

    private final CourseMaterialService materialService;

    @Operation( summary = "Create Course Material", description = "Allows a lecturer to upload a course material (PDF or DOCX) or provide a video link for a published course. Requires 'COURSE_MATERIAL_CREATE' authority." )
    @PostMapping("/{courseId}")
    @PreAuthorize("hasAuthority('COURSE_MATERIAL_CREATE')")
    public ResponseEntity<ApiResponse<MaterialResponseDto>> createMaterial(
            @PathVariable UUID courseId,
            @RequestParam(required = false)
            MultipartFile file,
            @ModelAttribute CreateAndUpdateRequestDto request ) {
        MaterialResponseDto response = materialService.createMaterial(courseId, file, request);
        return ResponseEntity.ok( new ApiResponse<>(true, "Course material created successfully", response) );
    }



    @Operation(
            summary = "Update Course Material",
            description = "Allows a lecturer to update an existing course material. Supports updating the title, description, file (PDF/DOCX), or video link. Requires 'COURSE_MATERIAL_UPDATE' authority."
    )
    @PutMapping("/courses/{courseId}/materials/{materialId}")
    @PreAuthorize("hasAuthority('COURSE_MATERIAL_UPDATE')")
    public ResponseEntity<ApiResponse<MaterialResponseDto>> updateMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID materialId,
            @RequestParam(required = false) MultipartFile file,
            @ModelAttribute CreateAndUpdateRequestDto EditRequest
    ) {

        MaterialResponseDto response = materialService.updateMaterial(courseId, materialId, file, EditRequest);
        return ResponseEntity.ok(new ApiResponse<>(true, "Course material updated successfully", response));

    }


    @Operation(
            summary = "Delete Course Material",
            description = "Allows a lecturer to delete a course material. Requires 'COURSE_MATERIAL_DELETE' authority.")
    @DeleteMapping("/{materialId}")
    @PreAuthorize("hasAuthority('COURSE_MATERIAL_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(
            @PathVariable UUID materialId
    ) {
        materialService.deleteMaterial(materialId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Course material deleted successfully", null));


    }

    @Operation(
            summary = "Get Course Materials",
            description = "Returns all materials for a course. Only enrolled students or the course lecturer can access these materials."
    )
    @GetMapping("/course/{courseId}/materials")
    @PreAuthorize("hasAuthority('COURSE_MATERIAL_VIEW_ALL')")
    public ResponseEntity<ApiResponse<List<MaterialResponseDto>>> getMaterialsByCourse(
            @PathVariable UUID courseId
    ) {

        List<MaterialResponseDto> response = materialService.getMaterialsByCourse(courseId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Course materials retrieved successfully", response));

    }


    @Operation(
            summary = "Get Course Material",
            description = "Returns details of a specific course material. Only enrolled students or the course lecturer can access this material."
    )
    @GetMapping("/{materialId}")
    @PreAuthorize("hasAuthority('COURSE_MATERIAL_VIEW')")
    public ResponseEntity<ApiResponse<MaterialResponseDto>> getMaterial(
            @PathVariable UUID materialId
    ) {

        MaterialResponseDto response = materialService.getMaterial(materialId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Course material retrieved successfully", response)
        );

    }


    @Operation(
            summary = "Download Course Material",
            description = "Downloads the file for a specific course material. Only enrolled students or the course lecturer can access this material."
    )
    @GetMapping("/{materialId}/download")
    @PreAuthorize("hasAuthority('COURSE_MATERIAL_DOWNLOAD')")
    public ResponseEntity<Resource> downloadMaterial(
            @PathVariable UUID materialId
    ) {

        Resource file = materialService.downloadMaterial(materialId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }










}
