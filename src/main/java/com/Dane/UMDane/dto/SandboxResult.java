package com.Dane.UMDane.dto;

import com.Dane.UMDane.entity.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxResult {
    private SubmissionStatus status;
    private String output;
    private String errorOutput;
    private Integer runtimeMs;
}
