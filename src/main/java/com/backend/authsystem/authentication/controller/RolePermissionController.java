package com.backend.authsystem.authentication.controller;


import com.backend.authsystem.authentication.dto.AssignPermissionRequestDto;
import com.backend.authsystem.authentication.dto.AssignRoleRequestDto;
import com.backend.authsystem.authentication.dto.RolePermissionResponseDto;
import com.backend.authsystem.authentication.service.AuthenticatedUserService;
import com.backend.authsystem.authentication.service.RolePermissionService;
import com.backend.authsystem.authentication.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@Tag(name = "ROLE AND PERMISSION ASSIGNMENT", description = "Endpoints for assigning roles and permissions to users")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;
    private final AuthenticatedUserService authenticatedUserService;


    @Operation(
            summary = "Assign Role to User",
            description = "Assign Role for  currently authenticated user. Requires 'ROLE_ASSIGN' authority."
    )
    @PatchMapping("/role")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    public ResponseEntity <ApiResponse<RolePermissionResponseDto>> assignRoleToUser(
            @RequestBody AssignRoleRequestDto request) {
        String userEmail = authenticatedUserService.getCurrentUserEmail();
        log.info("User {} requested to assign role {}", userEmail, request.userId());

        RolePermissionResponseDto roleResponse = rolePermissionService.assignRoleToUser(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Role assigned successfully.", roleResponse));

    }


    @Operation(
            summary = "Assign Permission  to Role",
            description = "Assign Permission to Role for  currently authenticated user. Requires 'PERMISSION_ASSIGN' authority."
    )
    @PatchMapping("/permission")
    @PreAuthorize("hasAuthority('PERMISSION_ASSIGN')")
    public ResponseEntity<ApiResponse<RolePermissionResponseDto>> assignPermissionsToRole(
            @RequestBody @Valid AssignPermissionRequestDto request) {
        String userEmail = authenticatedUserService.getCurrentUserEmail();
        log.info("User {} requested to assign permission size {}", userEmail, request.permissionName().size());

        RolePermissionResponseDto response =
                rolePermissionService.assignPermissionsToRole(request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Permissions assigned successfully", response)
        );
    }











}
