package com.backend.authsystem.authentication.controller;

import com.backend.authsystem.authentication.dto.profile.ProfileResponseDto;
import com.backend.authsystem.authentication.dto.profile.ProfileUpdateDto;
import com.backend.authsystem.authentication.service.AuthenticatedUserService;
import com.backend.authsystem.authentication.service.ProfileService;
import com.backend.authsystem.authentication.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@Tag(name = "PROFILE", description = "Endpoints for managing user profiles")
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final AuthenticatedUserService authenticatedUserService;

    @Operation(
            summary = "Get My Profile",
            description = "Retrieves the profile information of the currently authenticated user. Requires 'PROFILE_VIEW' authority."
    )
    @GetMapping
    @PreAuthorize("hasAuthority('PROFILE_VIEW')")
    public ResponseEntity<ApiResponse<ProfileResponseDto>> getMyProfile() {
        ProfileResponseDto profileResponseDto = profileService.getMyProfileService();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Profile retrieved successfully.", profileResponseDto));
    }


    @Operation(
            summary = "Update My Profile",
            description = "Updates the profile information of the currently authenticated user. Requires 'PROFILE_UPDATE' authority. Accepts a ProfileUpdateDto in the request body and returns a success message upon successful update."
    )

    @PutMapping
    @PreAuthorize("hasAuthority('PROFILE_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> updateMyProfile(
            @RequestBody ProfileUpdateDto dto) {
        profileService.updateMyProfileService(dto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Profile updated successfully.", null));
    }





}
