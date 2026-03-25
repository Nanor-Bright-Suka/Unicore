package com.backend.authsystem.authentication.service;

import com.backend.authsystem.authentication.config.SecurityEnvironments;
import com.backend.authsystem.authentication.dto.LoginResponseDto;
import com.backend.authsystem.authentication.dto.RefreshAndAccessToken;
import com.backend.authsystem.authentication.dto.UserRegisterDto;
import com.backend.authsystem.authentication.dto.UserloginDto;
import com.backend.authsystem.authentication.entity.AccountEntity;
import com.backend.authsystem.authentication.entity.RefreshTokenEntity;
import com.backend.authsystem.authentication.entity.RoleEntity;
import com.backend.authsystem.authentication.enums.RoleEnum;
import com.backend.authsystem.authentication.exception.*;
import com.backend.authsystem.authentication.repository.AccountRepository;
import com.backend.authsystem.authentication.repository.RefreshTokenRepository;
import com.backend.authsystem.authentication.repository.RoleRepository;
import com.backend.authsystem.authentication.util.HashToken;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private SecurityEnvironments securityEnvironments;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AccountService accountService;



    private UserRegisterDto createTestUser() {
        return new UserRegisterDto("  John  ", "  Doe  ", "  password123  ", "  john@example.com  ");
    }

// CREATE USER SERVICE TEST
    @Test
    void shouldSaveUserWhenEmailIsNewAndRoleExists() {
        //Arrange
        UserRegisterDto newUser = createTestUser();
        when(accountRepository.findByEmail(newUser.email().trim())).thenReturn(Optional.empty());
        RoleEntity role = new RoleEntity();
        role.setRoleName(RoleEnum.ROLE_USER);
        when(roleRepository.findByRoleName(RoleEnum.ROLE_USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(newUser.password().trim())).thenReturn("encodedPassword");

        //Act
        accountService.createUserService(newUser);

        //Assert
        verify(accountRepository, times(1)).save(any(AccountEntity.class));
    }



    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        UserRegisterDto newUser = createTestUser();
        when(accountRepository.findByEmail(newUser.email().trim()))
                .thenReturn(Optional.of(new AccountEntity()));

        assertThrows(UserAlreadyExistException.class, () -> accountService.createUserService(newUser));

        verify(accountRepository, never()).save(any());
        verify(roleRepository, never()).findByRoleName(any());
    }



    @Test
    void shouldThrowExceptionWhenDefaultRoleNotFound() {
        UserRegisterDto newUser = createTestUser();
        when(accountRepository.findByEmail(newUser.email().trim())).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName(RoleEnum.ROLE_USER)).thenReturn(Optional.empty());

        RoleNotFoundException exception = assertThrows(
                RoleNotFoundException.class,
                () -> accountService.createUserService(newUser)
        );
        assertEquals("Default role not found", exception.getMessage());

        verify(accountRepository, never()).save(any());
    }


    @Test
    void shouldTrimInputFieldsBeforeSaving() {
        UserRegisterDto newUser = createTestUser();
        when(accountRepository.findByEmail(newUser.email().trim())).thenReturn(Optional.empty());
        RoleEntity role = new RoleEntity();
        role.setRoleName(RoleEnum.ROLE_USER);
        when(roleRepository.findByRoleName(RoleEnum.ROLE_USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(newUser.password().trim())).thenReturn("encodedPassword");

        accountService.createUserService(newUser);

        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(accountRepository).save(captor.capture());
        AccountEntity savedUser = captor.getValue();

        assertEquals("john@example.com", savedUser.getEmail());
        assertEquals("John", savedUser.getFirstname());
        assertEquals("Doe", savedUser.getLastname());
    }

    @Test
    void shouldEncodePasswordBeforeSaving() {
        UserRegisterDto newUser = createTestUser();
        when(accountRepository.findByEmail(newUser.email().trim())).thenReturn(Optional.empty());
        RoleEntity role = new RoleEntity();
        role.setRoleName(RoleEnum.ROLE_USER);
        when(roleRepository.findByRoleName(RoleEnum.ROLE_USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(newUser.password().trim())).thenReturn("encodedPassword");

        accountService.createUserService(newUser);

        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(accountRepository).save(captor.capture());
        AccountEntity savedUser = captor.getValue();

        assertEquals("encodedPassword", savedUser.getPassword());
    }

    @Test
    void shouldAssignDefaultRoleToUser() {
        UserRegisterDto newUser = createTestUser();
        when(accountRepository.findByEmail(newUser.email().trim())).thenReturn(Optional.empty());
        RoleEntity role = new RoleEntity();
        role.setRoleName(RoleEnum.ROLE_USER);
        when(roleRepository.findByRoleName(RoleEnum.ROLE_USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(newUser.password().trim())).thenReturn("encodedPassword");

        accountService.createUserService(newUser);

        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(accountRepository).save(captor.capture());
        AccountEntity savedUser = captor.getValue();

        assertTrue(savedUser.getRoles().contains(role));
    }


    // LOGIN SERVICE TEST

    private AccountEntity createLoginTestUser() {
        AccountEntity user = new AccountEntity();
        user.setEmail("john@example.com");
        user.setPassword("encodedPassword");
        return user;
    }

    private UserloginDto createLoginDto() {
        return new UserloginDto("john@example.com", "password123");
    }


    @Test
    void shouldLoginSuccessfullyWhenEmailExistsAndPasswordIsCorrect() {
        AccountEntity user = createLoginTestUser();
        UserloginDto loginDto = createLoginDto();

        when(accountRepository.findByEmail(loginDto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDto.password(), user.getPassword())).thenReturn(true);
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken123");
        when(jwtService.generateAccessTokenFromUser(user)).thenReturn("accessToken123");
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(securityEnvironments.getRefreshTokenExpirationInDays()).thenReturn(7);

        LoginResponseDto responseDto = accountService.LoginService(loginDto, response);

        assertEquals("accessToken123", responseDto.token());
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), contains("refreshToken123"));
    }

    @Test
    void shouldGenerateAccessTokenForAuthenticatedUser() {
        AccountEntity user = createLoginTestUser();
        UserloginDto loginDto = createLoginDto();

        when(accountRepository.findByEmail(loginDto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDto.password(), user.getPassword())).thenReturn(true);
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken123");
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(securityEnvironments.getRefreshTokenExpirationInDays()).thenReturn(7);

        accountService.LoginService(loginDto, response);

        verify(jwtService).generateAccessTokenFromUser(user);
    }


    @Test
    void shouldGenerateRefreshTokenForAuthenticatedUser() {
        AccountEntity user = createLoginTestUser();
        UserloginDto loginDto = createLoginDto();

        when(accountRepository.findByEmail(loginDto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDto.password(), user.getPassword())).thenReturn(true);
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(securityEnvironments.getRefreshTokenExpirationInDays()).thenReturn(7);

        accountService.LoginService(loginDto, response);

        verify(jwtService).generateRefreshToken(user);
    }


    @Test
    void shouldAddRefreshTokenAsHttpOnlyCookie() {
        AccountEntity user = createLoginTestUser();
        UserloginDto loginDto = createLoginDto();

        when(accountRepository.findByEmail(loginDto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDto.password(), user.getPassword())).thenReturn(true);
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken123");
        when(jwtService.generateAccessTokenFromUser(user)).thenReturn("accessToken123");
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(securityEnvironments.getRefreshTokenExpirationInDays()).thenReturn(7);

        accountService.LoginService(loginDto, response);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), headerCaptor.capture());
        String cookie = headerCaptor.getValue();
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("refreshToken=refreshToken123"));
    }


    @Test
    void shouldSetRefreshTokenCookieWithCorrectMaxAge() {
        AccountEntity user = createLoginTestUser();
        UserloginDto loginDto = createLoginDto();
        when(securityEnvironments.getRefreshTokenExpirationInDays()).thenReturn(7);

        when(accountRepository.findByEmail(loginDto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDto.password(), user.getPassword())).thenReturn(true);
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken123");

        accountService.LoginService(loginDto, response);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), headerCaptor.capture());
        String cookie = headerCaptor.getValue();
        assertTrue(cookie.contains("Max-Age=" + Duration.ofDays(7).getSeconds()));
    }

    @Test
    void shouldSetRefreshTokenCookieWithCorrectPathAndSameSite() {
        AccountEntity user = createLoginTestUser();
        UserloginDto loginDto = createLoginDto();
        when(securityEnvironments.getRefreshTokenExpirationInDays()).thenReturn(7);

        when(accountRepository.findByEmail(loginDto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDto.password(), user.getPassword())).thenReturn(true);
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken123");

        accountService.LoginService(loginDto, response);

        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), headerCaptor.capture());
        String cookie = headerCaptor.getValue();
        assertTrue(cookie.contains("Path=/"));
        assertTrue(cookie.contains("SameSite=Lax"));
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenEmailDoesNotExist() {

        UserloginDto loginDto = createLoginDto();
        when(accountRepository.findByEmail(loginDto.email())).thenReturn(Optional.empty());
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> accountService.LoginService(loginDto, response)
        );

        assertEquals("User not found", exception.getMessage());
    }


    @Test
    void shouldThrowInvalidCredentialsExceptionWhenPasswordIsIncorrect() {
        AccountEntity user = createLoginTestUser();
        UserloginDto loginDto = createLoginDto();

        when(accountRepository.findByEmail(loginDto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDto.password(), user.getPassword())).thenReturn(false);
        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> accountService.LoginService(loginDto, response)
        );

        assertEquals("Invalid credentials", exception.getMessage());
    }



// REFRESH TOKEN SERVICE TEST

    private RefreshTokenEntity aValidStoredToken() {
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setTokenHash("hashedToken");
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setMyUser(new AccountEntity());
        return token;
    }

    @Test
    void shouldReturnNewTokensWhenRefreshTokenIsValid() {
        RefreshTokenEntity storedToken = aValidStoredToken();

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(storedToken));
        when(jwtService.generateRefreshToken(storedToken.getMyUser()))
                .thenReturn("newRefreshToken");
        when(jwtService.generateAccessTokenFromUser(storedToken.getMyUser()))
                .thenReturn("newAccessToken");

        RefreshAndAccessToken result =
                accountService.refreshService("rawToken");

        assertEquals("newAccessToken", result.accessToken());
        assertEquals("newRefreshToken", result.refreshToken());
    }

    @Test
    void shouldHashIncomingRefreshTokenBeforeLookup() {
        RefreshTokenEntity storedToken = aValidStoredToken();

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(storedToken));
        when(jwtService.generateRefreshToken(any()))
                .thenReturn("newRefreshToken");
        when(jwtService.generateAccessTokenFromUser(any()))
                .thenReturn("newAccessToken");

        accountService.refreshService("rawToken");

        verify(refreshTokenRepository)
                .findByTokenHash(HashToken.hashToken("rawToken"));
    }

    @Test
    void shouldRevokeOldRefreshTokenAfterSuccessfulRefresh() {
        RefreshTokenEntity storedToken = aValidStoredToken();

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(storedToken));
        when(jwtService.generateRefreshToken(any()))
                .thenReturn("newRefreshToken");
        when(jwtService.generateAccessTokenFromUser(any()))
                .thenReturn("newAccessToken");

        accountService.refreshService("rawToken");

        assertTrue(storedToken.getRevoked());
    }

    @Test
    void shouldSetRevokedAtTimestampWhenRefreshingToken() {
        RefreshTokenEntity storedToken = aValidStoredToken();

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(storedToken));
        when(jwtService.generateRefreshToken(any()))
                .thenReturn("newRefreshToken");
        when(jwtService.generateAccessTokenFromUser(any()))
                .thenReturn("newAccessToken");

        accountService.refreshService("rawToken");

        assertNotNull(storedToken.getRevokedAt());
    }


    @Test
    void shouldSaveRevokedTokenBeforeGeneratingNewTokens() {
        RefreshTokenEntity storedToken = aValidStoredToken();

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(storedToken));
        when(jwtService.generateRefreshToken(any()))
                .thenReturn("newRefreshToken");
        when(jwtService.generateAccessTokenFromUser(any()))
                .thenReturn("newAccessToken");

        accountService.refreshService("rawToken");

        verify(refreshTokenRepository).save(storedToken);
    }



    @Test
    void shouldGenerateNewRefreshTokenForUser() {
        RefreshTokenEntity storedToken = aValidStoredToken();

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(storedToken));
        when(jwtService.generateRefreshToken(any()))
                .thenReturn("newRefreshToken");
        when(jwtService.generateAccessTokenFromUser(any()))
                .thenReturn("newAccessToken");

        accountService.refreshService("rawToken");

        verify(jwtService).generateRefreshToken(storedToken.getMyUser());
    }

    @Test
    void shouldGenerateNewAccessTokenForUser() {
        RefreshTokenEntity storedToken = aValidStoredToken();

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(storedToken));
        when(jwtService.generateRefreshToken(any()))
                .thenReturn("newRefreshToken");
        when(jwtService.generateAccessTokenFromUser(any()))
                .thenReturn("newAccessToken");

        accountService.refreshService("rawToken");

        verify(jwtService).generateAccessTokenFromUser(storedToken.getMyUser());
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenDoesNotExist() {
        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.empty());
        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> accountService.refreshService("rawToken")
        );

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenIsRevoked() {
        RefreshTokenEntity storedToken = aValidStoredToken();
        storedToken.setRevoked(true);

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(storedToken));

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> accountService.refreshService("rawToken")
        );

        assertEquals("Refresh token revoked", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenIsExpired() {
        RefreshTokenEntity storedToken = aValidStoredToken();
        storedToken.setExpiresAt(Instant.now().minusSeconds(10));

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(storedToken));

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> accountService.refreshService("rawToken")
        );

        assertEquals("Refresh token expired", exception.getMessage());
    }


    //LOGOUT TEST SERVICE
    private RefreshTokenEntity aValidLogoutToken() {
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setTokenHash("hashedToken");
        token.setRevoked(false);
        token.setRevokedAt(null);
        token.setMyUser(new AccountEntity());
        return token;
    }

    @Test
    void shouldRevokeValidRefreshToken() {
        RefreshTokenEntity token = aValidLogoutToken();

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(token));

        accountService.logout("rawToken");

        assertTrue(token.getRevoked());
    }

    @Test
    void shouldSetRevokedAtTimestamp() {
        RefreshTokenEntity token = aValidLogoutToken();

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(token));

        accountService.logout("rawToken");

        assertNotNull(token.getRevokedAt());
    }

    @Test
    void shouldSaveRevokedToken() {
        RefreshTokenEntity token = aValidLogoutToken();

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(token));

        accountService.logout("rawToken");

        verify(refreshTokenRepository).save(token);
    }

    @Test
    void shouldHashTokenBeforeLookup() {
        RefreshTokenEntity token = aValidLogoutToken();

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(token));

        accountService.logout("rawToken");

        verify(refreshTokenRepository)
                .findByTokenHash(HashToken.hashToken("rawToken"));
    }


    @Test
    void shouldBeIdempotentWhenTokenAlreadyRevoked() {
        RefreshTokenEntity token = aValidLogoutToken();
        token.setRevoked(true);
        token.setRevokedAt(Instant.now().minusSeconds(60));

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(token));

        accountService.logout("rawToken");

        assertTrue(token.getRevoked());
    }

    @Test
    void shouldOverrideRevokedAtWhenAlreadyRevoked() {
        RefreshTokenEntity token = aValidLogoutToken();
        token.setRevoked(true);
        Instant oldTime = Instant.now().minusSeconds(60);
        token.setRevokedAt(oldTime);

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(token));

        accountService.logout("rawToken");

        assertTrue(token.getRevokedAt().isAfter(oldTime));
    }

    @Test
    void shouldThrowExceptionWhenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.empty());

        InvalidTokenException ex = assertThrows(
                InvalidTokenException.class,
                () -> accountService.logout("rawToken")
        );

        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRawTokenIsNull() {
        MissingTokenException ex = assertThrows(
                MissingTokenException.class,
                () -> accountService.logout(null)
        );
        assertEquals("Refresh token cannot be null or blank", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRawTokenIsBlank() {
        MissingTokenException ex = assertThrows(
                MissingTokenException.class,
                () -> accountService.logout("   ")
        );
        assertEquals("Refresh token cannot be null or blank", ex.getMessage());
    }

    @Test
    void shouldNotSaveWhenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidTokenException.class,
                () -> accountService.logout("rawToken")
        );

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldNotBreakWhenLogoutCalledTwice() {
        RefreshTokenEntity token = aValidLogoutToken();

        when(refreshTokenRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(token));

        accountService.logout("rawToken");
        Instant firstRevokedAt = token.getRevokedAt();

        accountService.logout("rawToken");
        Instant secondRevokedAt = token.getRevokedAt();

        assertTrue(token.getRevoked());
        assertTrue(secondRevokedAt.isAfter(firstRevokedAt));
    }



    //LOGOUT ALL TEST SERVICE

    private AccountEntity createLogoutAllTestUser() {
        AccountEntity user = new AccountEntity();
        user.setEmail("user@example.com");
        return user;
    }


    private RefreshTokenEntity createLogoutAllToken() {
        return new RefreshTokenEntity();
    }

    @Test
    void shouldRevokeAllRefreshTokensForValidUser() {
        AccountEntity user = createLogoutAllTestUser();
        RefreshTokenEntity token1 = createLogoutAllToken();
        RefreshTokenEntity token2 = createLogoutAllToken();
        List<RefreshTokenEntity> tokens = List.of(token1, token2);

        when(accountRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(refreshTokenRepository.findAllByMyUser(user))
                .thenReturn(tokens);

        accountService.logoutAll(user.getEmail());

        assertTrue(token1.getRevoked() && token2.getRevoked());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        String email = "nonexistent@example.com";

        when(accountRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> accountService.logoutAll(email)
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void shouldRemainSafeWhenTokensAlreadyRevoked() {
        AccountEntity user = createLogoutAllTestUser();
        RefreshTokenEntity token = createLogoutAllToken();
        token.setRevoked(true);

        when(accountRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(refreshTokenRepository.findAllByMyUser(user))
                .thenReturn(List.of(token));

        accountService.logoutAll(user.getEmail());

        assertTrue(token.getRevoked());
    }


    @Test
    void shouldSaveAllRevokedTokens() {
        AccountEntity user = createLogoutAllTestUser();
        RefreshTokenEntity token1 = createLogoutAllToken();
        RefreshTokenEntity token2 = createLogoutAllToken();
        List<RefreshTokenEntity> tokens = List.of(token1, token2);

        when(accountRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(refreshTokenRepository.findAllByMyUser(user))
                .thenReturn(tokens);

        accountService.logoutAll(user.getEmail());

        verify(refreshTokenRepository).saveAll(tokens);
    }

    @Test
    void shouldSetRevokedAtTimestampForAllTokens() {
        AccountEntity user = createLogoutAllTestUser();
        RefreshTokenEntity token1 = createLogoutAllToken();
        RefreshTokenEntity token2 = createLogoutAllToken();
        List<RefreshTokenEntity> tokens = List.of(token1, token2);

        when(accountRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(refreshTokenRepository.findAllByMyUser(user))
                .thenReturn(tokens);

        accountService.logoutAll(user.getEmail());

        assertNotNull(token1.getRevokedAt());
        assertNotNull(token2.getRevokedAt());
    }


    @Test
    void shouldHandleUserWithZeroTokensGracefully() {
        AccountEntity user = createLogoutAllTestUser();

        when(accountRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(refreshTokenRepository.findAllByMyUser(user))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> accountService.logoutAll(user.getEmail()));
    }




    @Test
    void shouldRevokeAllMultipleTokens() {
        AccountEntity user = createLogoutAllTestUser();
        List<RefreshTokenEntity> tokens = List.of(createLogoutAllToken(), createLogoutAllToken(), createLogoutAllToken());

        when(accountRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(refreshTokenRepository.findAllByMyUser(user))
                .thenReturn(tokens);

        accountService.logoutAll(user.getEmail());

        assertTrue(tokens.stream().allMatch(RefreshTokenEntity::getRevoked));
    }

    @Test
    void shouldOverrideExistingRevokedAtTimestamps() {
        AccountEntity user = createLogoutAllTestUser();
        RefreshTokenEntity token = createLogoutAllToken();
        token.setRevokedAt(Instant.now().minusSeconds(3600));

        when(accountRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
        when(refreshTokenRepository.findAllByMyUser(user))
                .thenReturn(List.of(token));

        accountService.logoutAll(user.getEmail());

        assertTrue(token.getRevokedAt().isAfter(Instant.now().minusSeconds(10)));
    }






















}