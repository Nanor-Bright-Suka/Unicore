package com.backend.authsystem.authentication.controller;


import com.backend.authsystem.authentication.dto.AccountResponseDto;
import com.backend.authsystem.authentication.dto.ChangePasswordRequestDto;
import com.backend.authsystem.authentication.service.AccountService;
import com.backend.authsystem.authentication.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@Tag(name = "ACCOUNT", description = "Endpoints for managing user accounts, such as viewing account details and changing passwords.")
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;



    @Operation(
            summary = "Get My Account",
            description = "Retrieves the account details of the currently authenticated user. Requires 'ACCOUNT_VIEW' authority. Returns an AccountResponseDto containing the user's account information."
    )
    @GetMapping
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW')")
    public ResponseEntity<ApiResponse<AccountResponseDto>> getMyAccount() {
        AccountResponseDto response = accountService.getMyAccount();
        return ResponseEntity.ok(new ApiResponse<>(true, "Account retrieved", response));
    }


    @Operation(
            summary = "Change Password",
            description = "Allows the currently authenticated user to change their password. Requires 'PASSWORD_CHANGE' authority. Accepts a ChangePasswordRequestDto in the request body containing the old and new passwords, and returns a success message upon successful password change."
    )
    @PostMapping("/password")
    @PreAuthorize("hasAuthority('PASSWORD_CHANGE')")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody ChangePasswordRequestDto dto) {
        accountService.changePassword(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password changed successfully", null));
    }



}
