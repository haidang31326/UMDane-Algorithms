package com.Dane.UMDane.dto;

import lombok.Data;
import java.util.List;

@Data
public class CodeRunDTO {
    private Long problemId;
    private String code;
    private String inputData;
    private List<String> inputs;
    private String language;
}
