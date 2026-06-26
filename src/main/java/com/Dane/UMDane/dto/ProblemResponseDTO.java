package com.Dane.UMDane.dto;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;

@Data
@Builder
public class ProblemResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String topic;
    private String keyword;
    private String title;
    private String description;
    private String difficulty;
    private String hint;
}