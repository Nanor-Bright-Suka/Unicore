package com.backend.authsystem.authentication.entity;


import com.backend.authsystem.authentication.enums.RoleEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "roles")
public class RoleEntity {

        @Id
        private UUID roleId;

        @Enumerated(EnumType.STRING)
        private RoleEnum roleName;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )

    @Builder.Default
    private Set<PermissionEntity> permissions = new HashSet<>();

        private Instant createdAt;

        private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleEntity)) return false;
        RoleEntity that = (RoleEntity) o;
        return roleId != null && roleId.equals(that.roleId);
    }

    @Override
    public int hashCode() {
        return roleId != null ? roleId.hashCode() : 0;
    }

    public void addPermission(PermissionEntity permission) {
        this.permissions.add(permission);
    }


    }
