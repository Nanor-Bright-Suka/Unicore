package com.backend.authsystem.authentication.seeder;

import com.backend.authsystem.authentication.entity.PermissionEntity;
import com.backend.authsystem.authentication.enums.PermissionEnum;
import com.backend.authsystem.authentication.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Profile("!test")
@Component
@RequiredArgsConstructor
@Order(value = 1)
public class PermissionSeeder implements ApplicationRunner {

    private final PermissionRepository permissionRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (PermissionEnum p : PermissionEnum.values()) {
            permissionRepository.findByPermissionName(p)
                    .orElseGet(() -> permissionRepository.save(
                            PermissionEntity.builder()
                                    .permissionId(UUID.randomUUID())
                                    .permissionName(p)
                                    .build()
                    ));

        }
    }


}
