package com.backend.authsystem.authentication.service;

import com.backend.authsystem.authentication.dto.profile.ProfileResponseDto;
import com.backend.authsystem.authentication.dto.profile.ProfileUpdateDto;
import com.backend.authsystem.authentication.entity.ProfileEntity;
import com.backend.authsystem.authentication.entity.AccountEntity;
import com.backend.authsystem.authentication.exception.FileTypeValidationException;
import com.backend.authsystem.authentication.exception.ResourceNotFoundException;
import com.backend.authsystem.authentication.exception.UserNotFoundException;
import com.backend.authsystem.authentication.repository.ProfileRepository;
import com.backend.authsystem.authentication.repository.AccountRepository;
import com.backend.authsystem.authentication.mapper.ProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {
    private final AccountRepository userRepository;
    private final ProfileRepository profileRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final ProfileMapper profileMapper;


    @Value("${file.upload.directory}")
    private String uploadDirectory;

    @Value("${file.upload.max-size-profile-picture}")
    private DataSize maxFileSize;

    private String  sanitizeFileName(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9\\-_.]", "_");
    }



    @Cacheable(value = "profileCache", key = "@authenticatedUserService.getCurrentUserEmail()")
    public ProfileResponseDto getMyProfileService() {
        String email = authenticatedUserService.getCurrentUserEmail();
        AccountEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));


        ProfileEntity profile = profileRepository.findByUser(user)
                .orElseGet(() -> {
                    ProfileEntity created = profileMapper.createDefaultViewProfile(user);
                    return profileRepository.save(created);
                });

       return profileMapper.toResponse(profile);
    }




   @CacheEvict(value = "profileCache", key = "@authenticatedUserService.getCurrentUserEmail()")
    public void updateMyProfileService(ProfileUpdateDto dto, MultipartFile imageFile) {
       String email = authenticatedUserService.getCurrentUserEmail();
        AccountEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

       ProfileEntity profile = profileRepository.findByUser(user)
               .orElseThrow(() -> {
                   log.info("Profile not found for user with email={}", email);
                   return new ResourceNotFoundException("Profile not found for user");
               });

       if (imageFile == null || imageFile.isEmpty()) {
           throw new FileTypeValidationException("Image file is required");
       }

       if (imageFile.getOriginalFilename() == null ||
               (!imageFile.getOriginalFilename().toLowerCase().endsWith(".png") &&
               !imageFile.getOriginalFilename().toLowerCase().endsWith(".jpg") &&
               !imageFile.getOriginalFilename().toLowerCase().endsWith(".jpeg"))) {
           log.warn("Invalid file type for profile {}", dto.imageUrl());
           throw new FileTypeValidationException("Only png, jpeg,jpg files are allowed");
       }

       if (imageFile.getSize() > maxFileSize.toBytes()) {
           log.warn("File size for profile picture {} exceeds max allowed {}", imageFile.getSize(), maxFileSize);
           throw new FileTypeValidationException("File too large");
       }

       String contentType = imageFile.getContentType();
       if (contentType == null ||
               (!contentType.equals("image/png") && !contentType.equals("image/jpeg"))) {
           throw new FileTypeValidationException("Only PNG and JPEG are allowed");
       }

       String objectKey = "profile-picture/" + user.getUserId() + "/" + UUID.randomUUID();
       String sanitizedFileName = sanitizeFileName(imageFile.getOriginalFilename());
       Path directoryPath = Paths.get(uploadDirectory, objectKey);

       try {
           Files.createDirectories(directoryPath);
           Path filePath = directoryPath.resolve(sanitizedFileName);
           Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
           profile.setImageUrl(filePath.toString());
           log.info("File saved locally at during update: {}", filePath);

       } catch (IOException e) {
           log.error("Failed to save file locally during update: error={}", e.getMessage());
           throw new RuntimeException("Failed to save file", e);
       }
       profile.setBio(dto.bio());
       profile.setFirstName(dto.firstName());
       profile.setLastName(dto.lastName());

        profileRepository.save(profile);
        log.info("Profile updated successfully for user with email={}", email);
    }







}
