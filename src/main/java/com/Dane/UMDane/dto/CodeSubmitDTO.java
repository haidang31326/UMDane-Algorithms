package com.Dane.UMDane.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CodeSubmitDTO {

    @NotNull(message = "Phải cung cấp ID của bài toán!")
    private Long problemId;

    @NotBlank(message = "Làm ơn gõ code vào rồi hẵng nộp, tính hack server à?")
    private String code;

    @NotBlank(message = "Ngôn ngữ lập trình không được để trống!")
    private String language;
}