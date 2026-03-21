package com.backend.authsystem.authentication.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "my_refresh_tokens")
public class RefreshTokenEntity {
    @Id
    private UUID tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AccountEntity myUser;

    private String tokenHash;

    private Instant expiresAt;

    private Instant createdAt;
    @Builder.Default
    private Boolean revoked = false;

    private Instant revokedAt;

}
