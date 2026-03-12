package com.backend.authsystem.authentication.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "course_enrollments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"course_id", "student_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseEnrollmentEntity {
    @Id
    private UUID enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private CourseEntity course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private AccountEntity student;

    private Instant enrolledAt;

    private boolean isDeleted = false;

    @PrePersist
    public void onCreate() {
        this.enrollmentId = UUID.randomUUID();
        this.enrolledAt = Instant.now();
        this.isDeleted = false;
    }




}
