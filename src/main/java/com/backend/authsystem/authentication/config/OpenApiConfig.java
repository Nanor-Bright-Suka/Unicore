package com.backend.authsystem.authentication.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
    public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
            return new OpenAPI()
                    .info(new Info()
                            .title("UniCore")
                            .version("1.0")
                            .description("UniCore is a backend API implemented in Java Spring Boot for managing university operations. It provides RESTful endpoints for courses, assignments, assignment submissions, and course materials, enforcing academic workflows, student enrollments, and role-based access control (RBAC) for students, lecturers, and admins. Authentication and authorization are fully secured with JWTs, and test cases cover all authentication flows to ensure proper access control and system integrity.The project is containerized using Docker with multi-stage builds, and a CI/CD pipeline ensures automated testing and safe deployment to staging and production environments.")
                    );
        }
    }


