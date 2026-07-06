package com.Dane.UMDane.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "user_restreak_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRestreakEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType; // "EARNED" or "USED"

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;
}
