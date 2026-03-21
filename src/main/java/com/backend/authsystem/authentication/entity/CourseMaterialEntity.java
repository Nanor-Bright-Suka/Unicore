package com.backend.authsystem.authentication.entity;


import com.backend.authsystem.authentication.enums.CourseMaterialType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "course_materials")
public class CourseMaterialEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseMaterialType materialType;

    private String filePath;

    private String videoUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private CourseEntity course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by")
    private AccountEntity uploadedBy;

    private Instant uploadedAt;

    private Instant updatedAt;

    private boolean isDeleted;

    @PrePersist
    public void onCreate() {
        this.id = UUID.randomUUID();
        this.isDeleted = false;
        this.uploadedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }








}
