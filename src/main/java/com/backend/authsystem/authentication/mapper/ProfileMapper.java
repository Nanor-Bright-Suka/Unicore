package com.backend.authsystem.authentication.mapper;

import com.backend.authsystem.authentication.dto.profile.ProfileResponseDto;
import com.backend.authsystem.authentication.entity.ProfileEntity;
import com.backend.authsystem.authentication.entity.AccountEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ProfileMapper {

    public ProfileEntity createDefaultViewProfile(AccountEntity user) {
        return ProfileEntity.builder()
                .profileId(UUID.randomUUID())
                .user(user)
                .firstName(user.getFirstname())
                .lastName(user.getLastname())
                .bio("")
                .imageUrl(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }


//
//    public void updateDefaultProfile(ProfileEntity entity, ProfileUpdateDto dto) {
//
//        entity.setFirstName(dto.firstName());
//        entity.setLastName(dto.lastName());
//        entity.setBio(dto.bio());
//        entity.setImageUrl(dto.imageUrl());
//        entity.setUpdatedAt(Instant.now());
//    }


    public ProfileResponseDto toResponse(ProfileEntity profile) {
        return new ProfileResponseDto(
                profile.getFirstName(),
                profile.getLastName(),
                profile.getBio(),
                profile.getImageUrl()
        );
    }




}
