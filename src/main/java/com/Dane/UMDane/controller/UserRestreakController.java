package com.Dane.UMDane.controller;

import com.Dane.UMDane.dto.ApiResponse;
import com.Dane.UMDane.dto.UserRestreakDTO;
import com.Dane.UMDane.security.UserPrincipal;
import com.Dane.UMDane.service.UserRestreakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/users/restreak")
@RequiredArgsConstructor
@Slf4j
public class UserRestreakController {

    private final UserRestreakService userRestreakService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserRestreakDTO>> getUserRestreakInfo(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).body(new ApiResponse<>(401, "Chưa đăng nhập!", null));
        }
        UserRestreakDTO info = userRestreakService.getUserRestreakInfo(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin Restreak thành công!", info));
    }

    @PostMapping("/use")
    public ResponseEntity<ApiResponse<String>> useRestreak(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, String> payload) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).body(new ApiResponse<>(401, "Chưa đăng nhập!", null));
        }

        String dateStr = payload.get("date");
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Thiếu thông tin ngày cần khôi phục!", null));
        }

        try {
            LocalDate date = LocalDate.parse(dateStr);
            userRestreakService.useRestreak(userPrincipal.getId(), date);
            return ResponseEntity.ok(ApiResponse.success("Khôi phục Streak thành công cho ngày " + dateStr + "!", null));
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Định dạng ngày không hợp lệ (hợp lệ: YYYY-MM-DD)!", null));
        } catch (Exception e) {
            log.error("Lỗi khi sử dụng restreak cho user {}", userPrincipal.getId(), e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, e.getMessage(), null));
        }
    }
}
