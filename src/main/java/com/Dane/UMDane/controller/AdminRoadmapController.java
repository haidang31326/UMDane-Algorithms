package com.Dane.UMDane.controller;

import com.Dane.UMDane.dto.ApiResponse;
import com.Dane.UMDane.service.RoadmapSeedingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/roadmap")
@RequiredArgsConstructor
public class AdminRoadmapController {

    private final RoadmapSeedingService roadmapSeedingService;

    @PostMapping("/seed-start")
    public ResponseEntity<ApiResponse<String>> startSeeding() {
        try {
            roadmapSeedingService.startSeeding();
            return ResponseEntity.ok(ApiResponse.success("Bắt đầu tiến trình sinh đề bài lộ trình chạy ngầm!", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, e.getMessage(), null));
        }
    }

    @GetMapping("/seed-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSeedingStatus() {
        Map<String, Object> status = Map.of(
                "active", roadmapSeedingService.isSeedingActive(),
                "seededCount", roadmapSeedingService.getSeededCount(),
                "statusMessage", roadmapSeedingService.getCurrentStatusMessage()
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái thành công!", status));
    }
}
