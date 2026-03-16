package com.backend.authsystem.authentication.seeder;

import com.backend.authsystem.authentication.entity.RoleEntity;
import com.backend.authsystem.authentication.entity.AccountEntity;
import com.backend.authsystem.authentication.enums.RoleEnum;
import com.backend.authsystem.authentication.exception.RoleNotFoundException;
import com.backend.authsystem.authentication.repository.RoleRepository;
import com.backend.authsystem.authentication.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Profile("!test")
@Component
@RequiredArgsConstructor
@Order(value = 3)
public class AdminSeeder implements ApplicationRunner {

    private final AccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;


    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.firstname}")
    private String adminFirstName;

    @Value("${admin.lastname}")
    private String adminLastName;

    @Override
    public void run(ApplicationArguments args) {
        String admnEmail = adminEmail;
        if (userRepository.existsByEmail(admnEmail)) return;

        AccountEntity admin = AccountEntity.builder()
                .userId(UUID.randomUUID())
                .firstname(adminFirstName)
                .lastname(adminLastName)
                .password(passwordEncoder.encode(adminPassword))
                .email(admnEmail)
                .createdAt(Instant.now())
                .build();
        RoleEntity adminRole = roleRepository.findByRoleName(RoleEnum.ROLE_ADMIN)
                .orElseThrow(() -> new RoleNotFoundException("Admin role not found!"));

        admin.addRole(adminRole);
        userRepository.save(admin);
        System.out.println("Pre-seeded ADMIN created: " + adminEmail);
    }


}
