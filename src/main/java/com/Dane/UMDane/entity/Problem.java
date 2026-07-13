package com.Dane.UMDane.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "problems")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String topic;

    @Column(nullable = false, length = 50)
    private String keyword;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String hint;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String difficulty = "MEDIUM";

    @Column(columnDefinition = "TEXT")
    private String constraints;

    @Column(name = "time_limit", nullable = false)
    @Builder.Default
    private Integer timeLimit = 2000;

    @Column(name = "memory_limit", nullable = false)
    @Builder.Default
    private Integer memoryLimit = 128;

    @Column(name = "user_template", columnDefinition = "TEXT")
    private String userTemplate;

    @Column(name = "reference_solution", columnDefinition = "TEXT")
    private String referenceSolution;

    @Column(name = "driver_code", columnDefinition = "TEXT")
    private String driverCode;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "review_digest", columnDefinition = "TEXT")
    private String reviewDigest;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
