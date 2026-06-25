package com.Dane.UMDane.controller;

import com.Dane.UMDane.dto.ApiResponse;
import com.Dane.UMDane.dto.ProblemResponseDTO;
import com.Dane.UMDane.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProblemResponseDTO>>> getAllProblems() {
        List<ProblemResponseDTO> problems = problemService.getAllProblems();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài toán thành công!", problems));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemResponseDTO> getProblemById(@PathVariable Long id) {
        ProblemResponseDTO problem = problemService.getProblemById(id);
        return ResponseEntity.ok(problem);
    }

    @GetMapping("/vibe")
    public ResponseEntity<ProblemResponseDTO> getProblemByVibe(
            @RequestParam String topic,
            @RequestParam String keyword) {
        ProblemResponseDTO problem = problemService.getRandomProblemByVibe(topic, keyword);
        return ResponseEntity.ok(problem);
    }
}