package com.backend.authsystem.authentication.service;


import com.backend.authsystem.authentication.config.SecurityEnvironments;
import com.backend.authsystem.authentication.dto.*;
import com.backend.authsystem.authentication.entity.RefreshTokenEntity;
import com.backend.authsystem.authentication.entity.RoleEntity;
import com.backend.authsystem.authentication.entity.AccountEntity;
import com.backend.authsystem.authentication.enums.RoleEnum;
import com.backend.authsystem.authentication.exception.*;
import com.backend.authsystem.authentication.repository.RefreshTokenRepository;
import com.backend.authsystem.authentication.repository.RoleRepository;
import com.backend.authsystem.authentication.repository.AccountRepository;
import com.backend.authsystem.authentication.util.HashToken;
import com.backend.authsystem.authentication.mapper.AccountMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AccountService {


    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityEnvironments securityEnvironments;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final LoginAttemptService loginAttemptService;


    public void createUserService(UserRegisterDto newUser) {
     Optional<AccountEntity> registeredUsers = accountRepository.findByEmail(newUser.email().trim());
        if (registeredUsers.isPresent()) {
            throw new UserAlreadyExistException("User with this email already exists");
        }

        RoleEntity defaultRole = roleRepository.findByRoleName(RoleEnum.ROLE_USER)
                .orElseThrow(() -> new RoleNotFoundException("Default role not found"));

        AccountEntity user = AccountEntity.builder()
                .userId(UUID.randomUUID())
                .email(newUser.email().trim())
                .password(passwordEncoder.encode(newUser.password().trim()))
                .firstname(newUser.firstname().trim())
                .lastname(newUser.lastname().trim())
                .createdAt(Instant.now())
                .build();

        user.getRoles().add(defaultRole);
        accountRepository.save(user);
    }

    public LoginResponseDto LoginService(UserloginDto loginDto, HttpServletResponse response) {
        String email = loginDto.email().trim();
        if (loginAttemptService.isBlocked(email)) {
            long remainingMs = loginAttemptService.getRemainingBlockTime(loginDto.email().trim());
            long remainingMinutes = (remainingMs + 59999) / 60000;

            throw new TooManyLoginAttemptsException(
                    "Too many failed login attempts. Try again in " + remainingMinutes + " minutes."
            );
        }


        AccountEntity user = accountRepository.findByEmail(loginDto.email().trim())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(loginDto.password().trim(), user.getPassword())) {
            loginAttemptService.loginFailed(email);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        loginAttemptService.loginSucceeded(email);

        String refreshToken = jwtService.generateRefreshToken(user);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // disable for local dev
                .path("/")
                .maxAge(Duration.ofDays(securityEnvironments.getRefreshTokenExpirationInDays()))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        String accessToken = jwtService.generateAccessTokenFromUser(user);
        return new LoginResponseDto(accessToken);

    }

    @Transactional
    public RefreshAndAccessToken refreshService(String rawRefreshToken){
        // 1. Hash the incoming token
        String hashedToken = HashToken.hashToken(rawRefreshToken);

        // 2. Retrieve stored token
        RefreshTokenEntity storedToken = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // 3. Validate token
        if (storedToken.getRevoked()) {
            throw new InvalidTokenException("Refresh token revoked");
        }
        // Check expiration
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token expired");
        }

        // 4. Generate new tokens
        storedToken.setRevoked(true);
        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        String newRefreshTokenPlain = jwtService.generateRefreshToken(storedToken.getMyUser());
        String newAccessToken = jwtService.generateAccessTokenFromUser(storedToken.getMyUser());
        return new RefreshAndAccessToken(newRefreshTokenPlain, newAccessToken);

    }


    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new MissingTokenException("Refresh token cannot be null or blank");
        }
        String tokenHash = HashToken.hashToken(rawRefreshToken);

        RefreshTokenEntity token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        token.setRevoked(true);
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
    }


    @Transactional
    public void logoutAll(String userEmail) {
        AccountEntity user = accountRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<RefreshTokenEntity> tokens = refreshTokenRepository.findAllByMyUser(user);

        tokens.forEach(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
        });

        refreshTokenRepository.saveAll(tokens);
    }


    // ACCOUNT_VIEW
    public AccountResponseDto getMyAccount() {
        String email = authenticatedUserService.getCurrentUserEmail();
        AccountEntity account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Account not found"));

        return accountMapper.toResponse(account);
    }


    public void changePassword(ChangePasswordRequestDto dto) {
        String email = authenticatedUserService.getCurrentUserEmail();
        AccountEntity account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Account not found"));

        if (!passwordEncoder.matches(dto.oldPassword(), account.getPassword())) {
            throw new IllegalArgumentException("Old password does not match");
        }

        account.setPassword(passwordEncoder.encode(dto.newPassword()));
        accountRepository.save(account);
    }

























}
