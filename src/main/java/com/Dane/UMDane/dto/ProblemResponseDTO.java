package com.Dane.UMDane.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String topic;
    private String keyword;
    private String title;
    private String description;
    private String difficulty;
    private String hint;
    private String constraints;
    private Integer timeLimit;
    private Integer memoryLimit;
    private String userTemplate;
    private String driverCode;
    private List<TestCaseDTO> sampleTestCases;
}