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
@lombok.extern.slf4j.Slf4j
public class ProblemController {

    private final ProblemService problemService;
    private final com.Dane.UMDane.repository.RoadmapNodeRepository roadmapNodeRepository;
    private final com.Dane.UMDane.repository.SubmissionRepository submissionRepository;
    private final com.Dane.UMDane.repository.ProblemRepository problemRepository;
    private final com.Dane.UMDane.service.GeminiAiService geminiAiService;

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

    @GetMapping("/yesterday-review")
    public ResponseEntity<ApiResponse<List<java.util.Map<String, Object>>>> getYesterdayReview(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(value = "test", defaultValue = "false") boolean testMode) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).body(new ApiResponse<>(401, "Bạn cần đăng nhập để xem ôn tập!", null));
        }

        // 1. First, look for problems solved yesterday (traditional spaced repetition)
        java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
        java.time.LocalDateTime start = yesterday.atStartOfDay();
        java.time.LocalDateTime end = yesterday.atTime(java.time.LocalTime.MAX);
        List<Long> solvedIds = submissionRepository.findSolvedProblemIdsByUserIdAndDateBetween(
                userPrincipal.getId(), start, end
        );

        // 2. Fallbacks based on testMode:
        if (solvedIds.isEmpty()) {
            if (testMode) {
                // If testMode is true, look for problems solved today to allow instant verification!
                java.time.LocalDate today = java.time.LocalDate.now();
                solvedIds = submissionRepository.findSolvedProblemIdsByUserIdAndDateBetween(
                        userPrincipal.getId(), today.atStartOfDay(), today.atTime(java.time.LocalTime.MAX)
                );
            } else {
                // In production, look back 7 days but EXCLUDE today (yesterday end is the limit)
                java.time.LocalDate sevenDaysAgo = java.time.LocalDate.now().minusDays(7);
                solvedIds = submissionRepository.findSolvedProblemIdsByUserIdAndDateBetween(
                        userPrincipal.getId(), sevenDaysAgo.atStartOfDay(), end
                );
            }
        }

        if (solvedIds.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Hôm qua bạn chưa giải bài nào.", List.of()));
        }

        // 3. Fetch problem entities
        List<com.Dane.UMDane.entity.Problem> problems = problemRepository.findAllById(solvedIds);

        // 4. Sort problems by difficulty (HARD > MEDIUM > EASY) and limit to max 3
        java.util.Comparator<String> difficultyComparator = (d1, d2) -> {
            int w1 = getDiffWeight(d1);
            int w2 = getDiffWeight(d2);
            return Integer.compare(w2, w1); // descending
        };

        problems = problems.stream()
                .sorted((p1, p2) -> difficultyComparator.compare(p1.getDifficulty(), p2.getDifficulty()))
                .limit(3)
                .toList();

        // 5. Build review cards list
        List<java.util.Map<String, Object>> reviewCards = new java.util.ArrayList<>();
        for (com.Dane.UMDane.entity.Problem p : problems) {
            java.util.Optional<com.Dane.UMDane.entity.Submission> subOpt = submissionRepository.findFirstByUserIdAndProblemIdAndStatusOrderByCreatedAtDesc(
                    userPrincipal.getId(),
                    p.getId(),
                    com.Dane.UMDane.entity.SubmissionStatus.ACCEPTED
            );

            if (subOpt.isEmpty()) {
                continue;
            }

            com.Dane.UMDane.entity.Submission sub = subOpt.get();
            String digestJson = sub.getReviewDigest();

            if (digestJson == null || digestJson.trim().isEmpty() || !digestJson.contains("maskedCode")) {
                try {
                    // Lazy AI generation and caching based on User's accepted code
                    digestJson = geminiAiService.generateReviewDigestForProblem(
                            p.getTitle(),
                            p.getDescription(),
                            sub.getCode(),
                            p.getReferenceSolution()
                    );
                    sub.setReviewDigest(digestJson);
                    submissionRepository.save(sub);
                } catch (Exception e) {
                    log.error("Lỗi khi sinh ôn tập điền khuyết code bằng AI cho bài ID = {}, submission ID = {}", p.getId(), sub.getId(), e);
                    continue; // Skip this card if generation fails
                }
            }

            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> digestMap = mapper.readValue(digestJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
                
                java.util.Map<String, Object> card = new java.util.HashMap<>();
                card.put("problemId", p.getId());
                card.put("title", p.getTitle());
                card.put("topic", p.getTopic());
                card.put("difficulty", p.getDifficulty());
                card.put("referenceSolution", p.getReferenceSolution());
                card.put("userSolution", sub.getCode());
                card.putAll(digestMap);
                
                reviewCards.add(card);
            } catch (Exception e) {
                log.error("Lỗi khi giải mã review_digest của submission ID = {}", sub.getId(), e);
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ôn tập tư duy thành công!", reviewCards));
    }

    private int getDiffWeight(String d) {
        if (d == null) return 2;
        switch (d.toUpperCase()) {
            case "EASY": return 1;
            case "MEDIUM": return 2;
            case "HARD": return 3;
            default: return 2;
        }
    }
}