package com.backend.authsystem.authentication.entity;

import com.backend.authsystem.authentication.enums.PermissionEnum;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "permission")
public class PermissionEntity {

    @Id
    private UUID permissionId;

    @Enumerated(EnumType.STRING)
    private PermissionEnum permissionName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PermissionEntity)) return false;
        PermissionEntity that = (PermissionEntity) o;
        return permissionId != null && permissionId.equals(that.permissionId);
    }

    @Override
    public int hashCode() {
        return permissionId != null ? permissionId.hashCode() : 0;
    }

}
