package com.Dane.UMDane.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProblemRequestDTO {

    @NotBlank(message = "Topic không được để trống!")
    private String topic;

    @NotBlank(message = "Keyword không được để trống!")
    private String keyword;

    @NotBlank(message = "Title không được để trống!")
    private String title;

    @NotBlank(message = "Description không được để trống!")
    private String description;

    private String hint;
}
