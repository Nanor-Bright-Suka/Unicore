package com.backend.authsystem.authentication.controller;

import com.backend.authsystem.authentication.config.SecurityEnvironments;
import com.backend.authsystem.authentication.dto.*;
import com.backend.authsystem.authentication.service.AccountService;
import com.backend.authsystem.authentication.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;



@Tag(name = "AUTHENTICATION", description = "Endpoints for user authentication")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AccountService accountService;
    private final SecurityEnvironments securityEnvironments;

    @Operation(
            summary = "Health Check",
            description = "Endpoint to check if the authentication system is running. Returns a simple greeting message."
    )

    @GetMapping("/health")
    public String health() {
        return "Hello from Auth System!";
    }


    @Operation(
            summary = "Register user",
            description = "Registers a new user with the provided details. Returns a success message upon successful registration."
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerUser(@RequestBody @Valid UserRegisterDto newUser) {
       accountService.createUserService(newUser);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Registration successfully, Please log in.", null));
  }

    @Operation(
            summary = "Login user",
            description = "Authenticates user and returns access token in response body and refresh token in HttpOnly cookie"
    )
 @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> loginUser(
            @RequestBody @Valid UserloginDto loginUser,
            HttpServletResponse response) {
       LoginResponseDto result =  accountService.LoginService(loginUser, response);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse<>(true, "Login successfully.", result));
  }


    @Operation(
            summary = "Refresh token",
            description = "Refreshes the access token using the refresh token stored in HttpOnly cookie. Returns new access token in response body and new refresh token in HttpOnly cookie"
    )
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response) {

        RefreshAndAccessToken result =
                accountService.refreshService(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", result.refreshToken())
                .httpOnly(true)
                .secure(false) // true in prod, false in dev
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(
                        securityEnvironments.getRefreshTokenExpirationInDays()))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(
                Map.of("accessToken", result.accessToken())
        );
    }


    @Operation(
            summary = "Logout user",
            description = "Logs out the user by revoking the refresh token provided in the HttpOnly cookie and deleting the cookie from the client. Returns a success message upon successful logout."
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@CookieValue("refreshToken") String refreshToken,
                                                    HttpServletResponse response) {
        accountService.logout(refreshToken);

        // Delete the cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false) // true in prod, false in dev
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new ApiResponse<>(true, "Logged out successfully", null));
    }




    @Operation(
            summary = "Logout from all devices",
            description = "Logs out the user from all devices by revoking all refresh tokens associated with the user's email and deleting the cookie from the client. Returns a success message upon successful logout from all devices."
    )
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(Authentication authentication,
                                                       HttpServletResponse response) {
        String userEmail = authentication.getName();

        accountService.logoutAll(userEmail);

        // Delete client cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new ApiResponse<>(true, "Logged out from all devices", null));
    }






}
