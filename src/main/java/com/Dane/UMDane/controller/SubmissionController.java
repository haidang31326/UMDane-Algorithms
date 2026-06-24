package com.Dane.UMDane.controller;

import com.Dane.UMDane.dto.ApiResponse;
import com.Dane.UMDane.dto.CodeSubmitDTO;
import com.Dane.UMDane.entity.Submission;
import com.Dane.UMDane.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService  submissionService;
    @PostMapping
    public ResponseEntity<ApiResponse<Submission>> submitCode(@Valid @RequestBody CodeSubmitDTO request) {
        Submission result = submissionService.submitCode(request);
        return ResponseEntity.ok(ApiResponse.success("Chấm bài hoàn tất!", result));
    }
}
