package com.backend.authsystem.authentication.seeder;

import com.backend.authsystem.authentication.entity.PermissionEntity;
import com.backend.authsystem.authentication.entity.RoleEntity;
import com.backend.authsystem.authentication.enums.PermissionEnum;
import com.backend.authsystem.authentication.enums.RoleEnum;
import com.backend.authsystem.authentication.repository.PermissionRepository;
import com.backend.authsystem.authentication.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;


@Component
@RequiredArgsConstructor
@Order(value = 2)
public class RoleSeeder implements ApplicationRunner {


    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public void run(ApplicationArguments args) {

        // USER
        RoleEntity normalUser = roleRepository.findByRoleName(RoleEnum.ROLE_USER)
                .orElseGet(() -> roleRepository.save(
                RoleEntity.builder()
                        .roleId(UUID.randomUUID())
                        .roleName(RoleEnum.ROLE_USER)
                        .createdAt(Instant.now())
                        .build()
        ));

        add(normalUser,
                PermissionEnum.PROFILE_VIEW,
               PermissionEnum.PROFILE_UPDATE,
                PermissionEnum.ACCOUNT_VIEW,
                PermissionEnum.PASSWORD_CHANGE

        );
                // STUDENT
        RoleEntity student = roleRepository.findByRoleName(RoleEnum.ROLE_STUDENT)
                .orElseGet(() -> roleRepository.save(
                RoleEntity.builder()
                        .roleId(UUID.randomUUID())
                        .roleName(RoleEnum.ROLE_STUDENT)
                        .createdAt(Instant.now())
                        .build()
        ));

        add(student,
                PermissionEnum.COURSE_VIEW,
                PermissionEnum.COURSE_ARCHIVE,
                PermissionEnum.COURSE_VIEW_ALL,
               PermissionEnum.ASSIGNMENT_SUBMIT,
                PermissionEnum.ASSIGNMENT_VIEW,
                PermissionEnum.ASSIGNMENT_SUBMISSION_VIEW,
                PermissionEnum.COURSE_ENROLLMENT,
                PermissionEnum.COURSE_MATERIAL_VIEW,
                PermissionEnum.CLASS_FEEDBACK_SUBMIT,
                PermissionEnum.GENERAL_ANNOUNCEMENT_VIEW
        );


        // COURSE REP
        RoleEntity courseRep = roleRepository.findByRoleName(RoleEnum.ROLE_COURSE_REP)
                .orElseGet(() -> roleRepository.save(
                        RoleEntity.builder()
                                .roleId(UUID.randomUUID())
                                .roleName(RoleEnum.ROLE_COURSE_REP)
                                .createdAt(Instant.now())
                                .build()
                ));

        add(courseRep,
              PermissionEnum.CLASS_ANNOUNCEMENT_VIEW,
                PermissionEnum.CLASS_FEEDBACK_COLLECT,
                PermissionEnum.CLASS_ANNOUNCEMENT_RELAY,
                PermissionEnum.CLASS_FEEDBACK_FORWARD
        );

                // LECTURER
        RoleEntity lecturer = roleRepository.findByRoleName(RoleEnum.ROLE_LECTURER)
                .orElseGet(() -> roleRepository.save(
                        RoleEntity.builder()
                                .roleId(UUID.randomUUID())
                                .roleName(RoleEnum.ROLE_LECTURER)
                                .createdAt(Instant.now())
                                .build()
                ));

        add(lecturer,
                PermissionEnum.ASSIGNMENT_MARK_GRADED,
                PermissionEnum.ASSIGNMENT_CREATE,
                PermissionEnum.ASSIGNMENT_VIEW,
                PermissionEnum.ASSIGNMENT_SUBMISSION_CLOSE,
                PermissionEnum.ASSIGNMENT_PUBLISH,
                PermissionEnum.ASSIGNMENT_UPDATE,
                PermissionEnum.ASSIGNMENT_SUBMISSION_CLOSE,
                PermissionEnum.ASSIGNMENT_START_GRADING,
                PermissionEnum.ASSIGNMENT_ARCHIVE,
                PermissionEnum.ASSIGNMENT_SUBMISSION_VIEW,
                PermissionEnum.ASSIGNMENT_SUBMISSION_VIEW_ALL,
                PermissionEnum.ASSIGNMENT_GRADE,
                PermissionEnum.COURSE_CREATE,
                PermissionEnum.COURSE_UPDATE,
                PermissionEnum.COURSE_PUBLISH,
                PermissionEnum.COURSE_OPEN_ENROLLMENT,
                PermissionEnum.COURSE_CLOSE_ENROLLMENT,
                PermissionEnum.COURSE_START,
                PermissionEnum.COURSE_COMPLETE,
                PermissionEnum.COURSE_ARCHIVE,
                PermissionEnum.COURSE_CREATE,
                PermissionEnum.COURSE_VIEW,
                PermissionEnum.COURSE_ENROLLMENT_COUNT,
                PermissionEnum.COURSE_ENROLLMENT_LIST,
                PermissionEnum.COURSE_MATERIAL_CREATE,
                PermissionEnum.COURSE_MATERIAL_UPDATE,
                PermissionEnum.COURSE_MATERIAL_DELETE,
                PermissionEnum.CLASS_FEEDBACK_VIEW,
                PermissionEnum.CLASS_ANNOUNCEMENT_CREATE

                );


                // ADMIN (separate)
        RoleEntity admin = roleRepository.findByRoleName(RoleEnum.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(
                        RoleEntity.builder()
                                .roleId(UUID.randomUUID())
                                .roleName(RoleEnum.ROLE_ADMIN)
                                .createdAt(Instant.now())
                                .build()
                ));

        add(admin,
                PermissionEnum.ROLE_ASSIGN,
                PermissionEnum.PERMISSION_ASSIGN,
                PermissionEnum.USER_MANAGE
        );
    }

    private void add(RoleEntity role, PermissionEnum... perms) {
        for (PermissionEnum p : perms) {
            PermissionEntity perm = permissionRepository.findByPermissionName(p).orElseThrow(() ->
                    new IllegalStateException("Permission not found"));
            role.addPermission(perm);
        }
        roleRepository.save(role);
    }

}