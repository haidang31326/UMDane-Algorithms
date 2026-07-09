package com.Dane.UMDane.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoadmapNodeResponseDTO {
    private Integer nodeId;
    private Integer phase;
    private String title;
    private String topic;
    private String keyword;
    private String difficulty;
    private Long problemId;
    private boolean solved;
    private boolean unlocked;
}
