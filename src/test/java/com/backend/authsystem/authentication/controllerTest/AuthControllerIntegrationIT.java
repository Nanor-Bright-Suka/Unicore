package com.backend.authsystem.authentication.controllerTest;


import com.backend.authsystem.authentication.dto.UserRegisterDto;
import com.backend.authsystem.authentication.dto.UserloginDto;
import com.backend.authsystem.authentication.entity.AccountEntity;
import com.backend.authsystem.authentication.entity.RefreshTokenEntity;
import com.backend.authsystem.authentication.entity.RoleEntity;
import com.backend.authsystem.authentication.enums.RoleEnum;
import com.backend.authsystem.authentication.exception.InvalidTokenException;
import com.backend.authsystem.authentication.exception.MissingTokenException;
import com.backend.authsystem.authentication.repository.AccountRepository;
import com.backend.authsystem.authentication.repository.RefreshTokenRepository;
import com.backend.authsystem.authentication.repository.RoleRepository;
import com.backend.authsystem.authentication.service.JwtService;
import com.backend.authsystem.authentication.util.HashToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(TestRateLimitingConfig.class)
class AuthControllerIntegrationIT {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtService jwtService;
    


    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    private UserRegisterDto createTestUser() {
        return new UserRegisterDto(
                "Nanor",
                "Bright",
                "password123",
                "nanor@test.com"
        );
    }

    private AccountEntity insertTestUser(String email) {
        RoleEntity role = RoleEntity.builder()
                .roleId(UUID.randomUUID())
                .roleName(RoleEnum.ROLE_USER)
                .createdAt(Instant.now())
                .build();
        roleRepository.save(role);

        AccountEntity user = AccountEntity.builder()
                .userId(UUID.randomUUID())
                .firstname("Nanor")
                .lastname("Bright")
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .createdAt(Instant.now())
                .build();
        user.getRoles().add(role);
        return accountRepository.save(user);
    }


    private RefreshTokenEntity insertValidRefreshToken(AccountEntity user, String rawToken) {
        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .tokenId(UUID.randomUUID())
                .myUser(user)
                .tokenHash(HashToken.hashToken(rawToken))
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .revoked(false)
                .build();
        return refreshTokenRepository.save(token);
    }


    @BeforeEach
    void setup() {
        refreshTokenRepository.deleteAll();
        accountRepository.deleteAll();
        roleRepository.deleteAll();
    }

    // registration tests
    @Nested
    class RegistrationTests {

        @Test
        void shouldRegisterUserSuccessfully() throws Exception {
            // Prepare default role
            RoleEntity role = RoleEntity.builder()
                    .roleId(UUID.randomUUID())
                    .roleName(RoleEnum.ROLE_USER)
                    .createdAt(Instant.now())
                    .build();
            roleRepository.save(role);

            UserRegisterDto newUser = createTestUser();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Registration successfully, Please log in."));

            // Assert user persisted with default role
            AccountEntity savedUser = accountRepository.findByEmail(newUser.email()).orElseThrow();
            assertThat(savedUser.getRoles()).extracting(RoleEntity::getRoleName).contains(RoleEnum.ROLE_USER);
        }


        @Test
        void shouldFailRegistrationWhenEmailExists() throws Exception {
            RoleEntity role = RoleEntity.builder()
                    .roleId(UUID.randomUUID())
                    .roleName(RoleEnum.ROLE_USER)
                    .createdAt(Instant.now())
                    .build();
            roleRepository.save(role);

            // Save user first
            AccountEntity existingUser = AccountEntity.builder()
                    .userId(UUID.randomUUID())
                    .firstname("Test")
                    .lastname("User")
                    .email("nanor@test.com")
                    .password("password123")
                    .createdAt(Instant.now())
                    .build();
            accountRepository.save(existingUser);

            UserRegisterDto duplicateUser = createTestUser();

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicateUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("User with this email already exists"));


        }


        @Test
        void shouldFailRegistrationWhenDefaultRoleMissing() throws Exception {
            UserRegisterDto newUser = createTestUser();
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Default role not found"));
        }

    }

    @Nested
    class LoginTests {

        @Test
        void shouldLoginSuccessfully() throws Exception {
            insertTestUser("nanor@test.com");

            UserloginDto loginDto = new UserloginDto("password123", "nanor@test.com");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").isNotEmpty())
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")));
        }

        @Test
        void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {

            UserloginDto loginDto = new UserloginDto(
                    "password123",
                    "unknown@test.com"
            );

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("User not found"));
        }


        @Test
        void shouldReturnInvalidCredentialsWhenPasswordIsWrong() throws Exception {
            insertTestUser("nanor@test.com");

            UserloginDto loginDto = new UserloginDto("wrongpassword", "nanor@test.com");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDto)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Invalid credentials"));
        }


        @Test
        void shouldReturnBadRequestForInvalidEmailFormat() throws Exception {
            insertTestUser("nanor@test.com");

            UserloginDto loginDto = new UserloginDto("password123", "not-an-email");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnBadRequestWhenPasswordTooShort() throws Exception {

            UserloginDto loginDto = new UserloginDto(
                    "123",
                    "nanor@test.com"
            );

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class RefreshTests {

        @BeforeEach
        void setup() {
            refreshTokenRepository.deleteAll();
        }


        @Test
        void shouldRefreshTokensSuccessfully() throws Exception {
            AccountEntity user = insertTestUser("nanor@test.com");
            String rawRefreshToken = jwtService.generateRefreshToken(user);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new Cookie("refreshToken", rawRefreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
        }


        @Test
        void shouldFailWithInvalidRefreshToken() throws Exception {
            AccountEntity user = insertTestUser("nanor@test.com");
            insertValidRefreshToken(user, "valid-token"); // real token different

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new Cookie("refreshToken", "invalid-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(result -> assertInstanceOf(InvalidTokenException.class, result.getResolvedException()))
                    .andExpect(result -> assertEquals("Invalid refresh token", result.getResolvedException().getMessage()));
        }


        @Test
        void shouldFailIfRefreshTokenRevoked() throws Exception {
            AccountEntity user = insertTestUser("nanor@test.com");
            String rawToken = UUID.randomUUID().toString();
            RefreshTokenEntity token = insertValidRefreshToken(user, rawToken);

            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new Cookie("refreshToken", rawToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidTokenException))
                    .andExpect(result -> assertEquals("Refresh token revoked", result.getResolvedException().getMessage()));
        }

        @Test
        void shouldFailIfRefreshTokenExpired() throws Exception {
            AccountEntity user = insertTestUser("nanor@test.com");
            String rawToken = UUID.randomUUID().toString();

            RefreshTokenEntity token = RefreshTokenEntity.builder()
                    .tokenId(UUID.randomUUID())
                    .myUser(user)
                    .tokenHash(HashToken.hashToken(rawToken))
                    .expiresAt(Instant.now().minusSeconds(60)) // expired
                    .createdAt(Instant.now().minusSeconds(3600))
                    .revoked(false)
                    .build();
            refreshTokenRepository.save(token);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new Cookie("refreshToken", rawToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidTokenException))
                    .andExpect(result -> assertEquals("Refresh token expired", result.getResolvedException().getMessage()));
        }

        @Test
        void shouldRotateRefreshToken() throws Exception {
            AccountEntity user = insertTestUser("nanor@test.com");
            String oldToken = jwtService.generateRefreshToken(user);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new Cookie("refreshToken", oldToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken")));

            // old token should be revoked
            RefreshTokenEntity old = refreshTokenRepository.findByTokenHash(HashToken.hashToken(oldToken)).orElseThrow();
            assertTrue(old.getRevoked());
            assertNotNull(old.getRevokedAt());
        }

    }

    @Nested
    class LogoutTests {

        @BeforeEach
        void setup() {
            refreshTokenRepository.deleteAll(); // clean tokens before each test
            accountRepository.deleteAll();
            roleRepository.deleteAll();
        }

        @Test
        void shouldLogoutSuccessfully() throws Exception {
            AccountEntity user = insertTestUser("user@test.com");
            String token = jwtService.generateRefreshToken(user);

            mockMvc.perform(post("/api/v1/auth/logout")
                            .cookie(new Cookie("refreshToken", token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Logged out successfully"))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=;"))) // cookie cleared
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));

            // token should be revoked in DB
            RefreshTokenEntity saved = refreshTokenRepository.findByTokenHash(HashToken.hashToken(token))
                    .orElseThrow();
            assertTrue(saved.getRevoked());
            assertNotNull(saved.getRevokedAt());
        }


        @Test
        void shouldThrowForInvalidToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout")
                            .cookie(new Cookie("refreshToken", "invalid-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidTokenException))
                    .andExpect(result -> assertEquals("Invalid refresh token",
                            result.getResolvedException().getMessage()));
        }

        @Test
        void shouldThrowForBlankToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout")
                            .cookie(new Cookie("refreshToken", "")))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof MissingTokenException))
                    .andExpect(result -> assertEquals("Refresh token cannot be null or blank",
                            result.getResolvedException().getMessage()));
        }


        @Test
        void shouldHandleAlreadyRevokedTokenIdempotently() throws Exception {
            // Arrange
            AccountEntity user = insertTestUser("user@test.com");
            String token = jwtService.generateRefreshToken(user);

            RefreshTokenEntity saved = refreshTokenRepository
                    .findByTokenHash(HashToken.hashToken(token))
                    .orElseThrow();

            saved.setRevoked(true);
            saved.setRevokedAt(Instant.now());
            refreshTokenRepository.save(saved);

            // Act + Assert
            mockMvc.perform(post("/api/v1/auth/logout")
                            .cookie(new Cookie("refreshToken", token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));

            // Verify token is still revoked
            RefreshTokenEntity after = refreshTokenRepository
                    .findByTokenHash(HashToken.hashToken(token))
                    .orElseThrow();

            assertTrue(after.getRevoked());
            assertNotNull(after.getRevokedAt());
        }


    }


    @Nested
    class LogoutAllTests {

        private RoleEntity getOrCreateUserRole() {
            return roleRepository.findByRoleName(RoleEnum.ROLE_USER)
                    .orElseGet(() -> roleRepository.save(
                            RoleEntity.builder()
                                    .roleId(UUID.randomUUID())
                                    .roleName(RoleEnum.ROLE_USER)
                                    .createdAt(Instant.now())
                                    .build()
                    ));
        }

        AccountEntity insertTestUser(String email, String rawPassword) {
            RoleEntity role = getOrCreateUserRole();

            AccountEntity user = AccountEntity.builder()
                    .userId(UUID.randomUUID())
                    .firstname("Nanor")
                    .lastname("Bright")
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .createdAt(Instant.now())
                    .build();
            user.getRoles().add(role);
            return accountRepository.save(user);
        }


        @BeforeEach
        void clean() {
            refreshTokenRepository.deleteAll();
            accountRepository.deleteAll();
            roleRepository.deleteAll();
        }

        @Test
        @WithMockUser(username = "userA@test.com")
        void shouldRevokeAllTokensForUser() throws Exception {
            // Arrange
            AccountEntity user = insertTestUser("userA@test.com", "password123");

            String token1 = jwtService.generateRefreshToken(user);
            String token2 = jwtService.generateRefreshToken(user);

            // Act
            mockMvc.perform(post("/api/v1/auth/logout-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Logged out from all devices"))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=;")));

            // Assert
            List<RefreshTokenEntity> tokens =
                    refreshTokenRepository.findAllByMyUser(user);

            assertEquals(2, tokens.size());

            tokens.forEach(token -> {
                assertTrue(token.getRevoked());
                assertNotNull(token.getRevokedAt());
            });
        }

        @Test
        @WithMockUser(username = "userA@test.com")
        void shouldNotRevokeOtherUsersTokens() throws Exception {
            // Arrange
            AccountEntity userA = insertTestUser("userA@test.com", "password123");
            AccountEntity userB = insertTestUser("userB@test.com", "password123");

            jwtService.generateRefreshToken(userA);
            jwtService.generateRefreshToken(userA);

            jwtService.generateRefreshToken(userB);
            jwtService.generateRefreshToken(userB);

            // Act
            mockMvc.perform(post("/api/v1/auth/logout-all"))
                    .andExpect(status().isOk());

            // Assert
            List<RefreshTokenEntity> tokensA =
                    refreshTokenRepository.findAllByMyUser(userA);

            List<RefreshTokenEntity> tokensB =
                    refreshTokenRepository.findAllByMyUser(userB);

            // A should be revoked
            tokensA.forEach(token -> assertTrue(token.getRevoked()));

            // B should NOT be revoked
            tokensB.forEach(token -> assertFalse(token.getRevoked()));
        }

        @Test
        void shouldReturnUnauthorizedIfNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout-all"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "userNoTokens@test.com")
        void shouldHandleUserWithNoTokens() throws Exception {
            // Arrange
            insertTestUser("userNoTokens@test.com", "password123");

            // Act + Assert
            mockMvc.perform(post("/api/v1/auth/logout-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(username = "ghost@test.com")
        void shouldThrowIfUserNotFound() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout-all"))
                    .andExpect(status().isNotFound());

        }
    }

}














































