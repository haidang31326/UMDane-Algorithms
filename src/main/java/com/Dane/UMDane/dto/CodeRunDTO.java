package com.Dane.UMDane.dto;

import lombok.Data;

@Data
public class CodeRunDTO {
    private Long problemId;
    private String code;
    private String inputData;
    private String language;
}
