package com.Dane.UMDane.controller;

import com.Dane.UMDane.dto.ApiResponse;
import com.Dane.UMDane.dto.ProblemResponseDTO;
import com.Dane.UMDane.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import com.Dane.UMDane.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;
    private final com.Dane.UMDane.repository.RoadmapNodeRepository roadmapNodeRepository;
    private final com.Dane.UMDane.repository.SubmissionRepository submissionRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProblemResponseDTO>>> getAllProblems() {
        Long userId = null;
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserPrincipal) {
                userId = ((UserPrincipal) principal).getId();
            }
        } catch (Exception e) {
            // Not authenticated
        }
        List<ProblemResponseDTO> problems = problemService.getAllProblems(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài toán thành công!", problems));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProblemById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        
        var nodeOpt = roadmapNodeRepository.findByProblemId(id);
        if (nodeOpt.isPresent()) {
            var node = nodeOpt.get();
            if (node.getNodeId() > 1) {
                if (userPrincipal == null) {
                    return ResponseEntity.status(401)
                            .body(new ApiResponse<>(401, "Bạn cần đăng nhập để làm bài tập thuộc lộ trình!", null));
                }
                
                var prevNodeOpt = roadmapNodeRepository.findById(node.getNodeId() - 1);
                if (prevNodeOpt.isPresent()) {
                    var prevNode = prevNodeOpt.get();
                    boolean prevSolved = false;
                    if (prevNode.getProblemId() != null) {
                        prevSolved = submissionRepository.existsByUserIdAndProblemIdAndStatus(
                                userPrincipal.getId(),
                                prevNode.getProblemId(),
                                com.Dane.UMDane.entity.SubmissionStatus.ACCEPTED
                        );
                    }
                    if (!prevSolved) {
                        return ResponseEntity.status(403)
                                .body(new ApiResponse<>(403, "Bài tập này đang bị khóa! Bạn cần giải quyết bài tập số " + prevNode.getNodeId() + " (" + prevNode.getTitle() + ") trước.", null));
                    }
                }
            }
        }

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

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ProblemResponseDTO>> generateProblem(
            @RequestParam String topic,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "MEDIUM") String difficulty) {
        ProblemResponseDTO problem = problemService.generateProblem(topic, keyword, difficulty);
        return ResponseEntity.ok(ApiResponse.success("Tạo bài toán ngẫu nhiên thành công!", problem));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProblem(@PathVariable Long id) {
        Long userId = null;
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserPrincipal) {
                userId = ((UserPrincipal) principal).getId();
            }
        } catch (Exception e) {
            // Ignore
        }

        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse<>(401, "Bạn cần đăng nhập để thực hiện chức năng này!", null));
        }

        problemService.hideProblemForUser(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa bài tập thành công!", null));
    }
}