package com.Dane.UMDane.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestCaseDTO {

    @NotBlank(message = "Input data không được để trống!")
    private String inputData;

    @NotBlank(message = "Expected output không được để trống!")
    private String expectedOutput;

    private Boolean isHidden = false;
}
