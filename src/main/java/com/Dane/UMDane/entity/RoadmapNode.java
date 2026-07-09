package com.Dane.UMDane.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roadmap_nodes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapNode {

    @Id
    @Column(name = "node_id")
    private Integer nodeId;

    @Column(nullable = false)
    private Integer phase;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 50)
    private String topic;

    @Column(nullable = false, length = 50)
    private String keyword;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(name = "problem_id")
    private Long problemId;
}
