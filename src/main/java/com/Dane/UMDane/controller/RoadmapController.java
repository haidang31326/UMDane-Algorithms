package com.Dane.UMDane.controller;

import com.Dane.UMDane.dto.ApiResponse;
import com.Dane.UMDane.dto.RoadmapNodeResponseDTO;
import com.Dane.UMDane.security.UserPrincipal;
import com.Dane.UMDane.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;

    @GetMapping("/nodes")
    public ResponseEntity<ApiResponse<List<RoadmapNodeResponseDTO>>> getRoadmapNodes(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = (userPrincipal != null) ? userPrincipal.getId() : null;
        List<RoadmapNodeResponseDTO> nodes = roadmapService.getRoadmapNodes(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lộ trình thành công!", nodes));
    }

    @PostMapping("/nodes/{nodeId}/generate")
    public ResponseEntity<ApiResponse<Long>> generateNodeProblem(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer nodeId) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse<>(401, "Bạn cần đăng nhập để mở khóa bài tập!", null));
        }
        try {
            Long problemId = roadmapService.generateNodeProblem(nodeId);
            return ResponseEntity.ok(ApiResponse.success("Mở khóa bài tập thành công!", problemId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, e.getMessage(), null));
        }
    }
}
