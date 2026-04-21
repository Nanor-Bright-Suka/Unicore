package com.backend.authsystem.authentication.service;


import com.backend.authsystem.authentication.entity.AccountEntity;
import com.backend.authsystem.authentication.exception.UserNotFoundException;
import com.backend.authsystem.authentication.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticatedUserService {

    private final AccountRepository accountRepository;


    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        return auth.getName();
    }

    public AccountEntity getCurrentUserAccount() {
        String email = getCurrentUserEmail();
        log.info("Fetching authenticated user from security context: {}", email);

        return accountRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Authenticated user not found in DB: {}", email);
                    return new UserNotFoundException("User not found");
                });
    }

    public UUID getCurrentUserId() {
        String email = getCurrentUserEmail();
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found in DB: {}", email);
                    return new UserNotFoundException("User not found");
                })
                .getUserId();
    }



}
