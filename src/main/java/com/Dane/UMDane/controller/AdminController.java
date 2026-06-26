package com.Dane.UMDane.controller;

import com.Dane.UMDane.dto.ApiResponse;
import com.Dane.UMDane.dto.ProblemRequestDTO;
import com.Dane.UMDane.dto.TestCaseDTO;
import com.Dane.UMDane.entity.Problem;
import com.Dane.UMDane.entity.TestCase;
import com.Dane.UMDane.repository.ProblemRepository;
import com.Dane.UMDane.repository.TestCaseRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;

    @PostMapping("/problems")
    public ResponseEntity<ApiResponse<Problem>> createProblem(@Valid @RequestBody ProblemRequestDTO request) {
        Problem problem = Problem.builder()
                .topic(request.getTopic())
                .keyword(request.getKeyword())
                .title(request.getTitle())
                .description(request.getDescription())
                .hint(request.getHint())
                .build();

        problem = problemRepository.save(problem);
        return ResponseEntity.ok(ApiResponse.success("Tạo bài toán thành công!", problem));
    }

    @PostMapping("/problems/{id}/testcases")
    public ResponseEntity<ApiResponse<TestCase>> addTestCase(
            @PathVariable Long id,
            @Valid @RequestBody TestCaseDTO request) {
        
        problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài toán!"));

        TestCase testCase = TestCase.builder()
                .problemId(id)
                .inputData(request.getInputData())
                .expectedOutput(request.getExpectedOutput())
                .isHidden(request.getIsHidden())
                .build();

        testCase = testCaseRepository.save(testCase);
        return ResponseEntity.ok(ApiResponse.success("Thêm Test Case thành công!", testCase));
    }
}
