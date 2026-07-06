package com.Dane.UMDane.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_restreaks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRestreak {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "restreaks_available", nullable = false)
    @Builder.Default
    private int restreaksAvailable = 0;
}
