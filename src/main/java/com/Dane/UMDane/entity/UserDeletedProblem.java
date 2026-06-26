package com.Dane.UMDane.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_deleted_problems")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeletedProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;
}
