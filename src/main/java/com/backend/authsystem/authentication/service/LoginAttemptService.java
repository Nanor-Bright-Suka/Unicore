package com.backend.authsystem.authentication.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptService {


        private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();
        private final Map<String, Long> firstAttemptTime = new ConcurrentHashMap<>();


        private final int MAX_ATTEMPTS = 5;
        private final long BLOCK_TIME_MS = Duration.ofMinutes(15).toMillis();

        public void loginFailed(String email) {
            attemptsCache.merge(email, 1, Integer::sum);
            firstAttemptTime.putIfAbsent(email, System.currentTimeMillis());
        }

        public void loginSucceeded(String email) {
            attemptsCache.remove(email);
            firstAttemptTime.remove(email);
        }

        public boolean isBlocked(String email) {
            Integer attempts = attemptsCache.get(email);
            Long firstTime = firstAttemptTime.get(email);

            if (attempts == null || firstTime == null) return false;

            long elapsed = System.currentTimeMillis() - firstTime;
            if (elapsed > BLOCK_TIME_MS) {
                attemptsCache.remove(email);
                firstAttemptTime.remove(email);
                return false;
            }

            return attempts >= MAX_ATTEMPTS;
        }

        public long getRemainingBlockTime(String email) {
            Long firstTime = firstAttemptTime.get(email);
            if (firstTime == null) return 0;

            long elapsed = System.currentTimeMillis() - firstTime;
            long remaining = BLOCK_TIME_MS - elapsed;
            return remaining > 0 ? remaining : 0;
        }

}
