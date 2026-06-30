package com.Dane.UMDane.controller;

import com.Dane.UMDane.dto.ApiResponse;
import com.Dane.UMDane.dto.CodeRunDTO;
import com.Dane.UMDane.dto.CodeSubmitDTO;
import com.Dane.UMDane.dto.SandboxResult;
import com.Dane.UMDane.entity.Submission;
import com.Dane.UMDane.repository.SubmissionRepository;
import com.Dane.UMDane.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;
    private final SubmissionRepository submissionRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Submission>> submitCode(@Valid @RequestBody CodeSubmitDTO request) {
        Submission result = submissionService.submitCode(request);
        return ResponseEntity.ok(ApiResponse.success("Chấm bài hoàn tất!", result));
    }

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<List<SandboxResult>>> runCode(@Valid @RequestBody CodeRunDTO request) {
        List<SandboxResult> results = submissionService.runCode(request);
        return ResponseEntity.ok(ApiResponse.success("Chạy thử hoàn tất!", results));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Submission>>> getRecentSubmissions() {
        List<Submission> submissions = submissionRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài nộp thành công!", submissions));
    }
}
