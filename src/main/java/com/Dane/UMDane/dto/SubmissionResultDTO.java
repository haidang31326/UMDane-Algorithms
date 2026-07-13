package com.Dane.UMDane.dto;

import com.Dane.UMDane.entity.Submission;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResultDTO {
    private Submission submission;
    private double beatsPercentage;
    private Map<Integer, Integer> runtimeDistribution;
    private double memoryBeatsPercentage;
    private Map<Double, Integer> memoryDistribution;
}
