package com.backend.authsystem.authentication.config;

import com.backend.authsystem.authentication.util.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class RateLimitingFilter extends OncePerRequestFilter {


    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        String key = resolveKey(request, path);
        Bucket bucket = buckets.computeIfAbsent(key, this::newBucketForEndpoint);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            long waitForRefillSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            long waitForRefillMinutes = (waitForRefillSeconds + 59) / 60;

            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(waitForRefillMinutes * 60));

            ApiResponse<Void> apiResponse = new ApiResponse<>(
                    false,
                    "Too many requests. Try again in " + waitForRefillMinutes + " minutes.",
                    null
            );

            String json = new ObjectMapper().writeValueAsString(apiResponse);
            response.getWriter().write(json);
        }
    }




    private String resolveKey(HttpServletRequest request, String path){
        String ip = request.getRemoteAddr();

        if (path.equals("/api/v1/auth/login")) {
            return "LOGIN_" + ip;
        } else if (path.equals("/api/v1/auth/register")) {
            return "REGISTER_" + ip;
        }
        return "DEFAULT_" + ip;
    }




    private Bucket newBucketForEndpoint(String key) {
        if (key.startsWith("LOGIN_")) {
            return Bucket.builder()
                    .addLimit(Bandwidth.simple(15, Duration.ofMinutes(10)))
                    .build();
        } else if (key.startsWith("REGISTER_")) {
            return Bucket.builder()
                    .addLimit(Bandwidth.simple(7, Duration.ofHours(1)))
                    .build();
        }

        return Bucket.builder()
                .addLimit(Bandwidth.simple(20, Duration.ofMinutes(1)))
                .build();
    }










}
