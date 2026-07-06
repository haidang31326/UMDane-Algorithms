package com.Dane.UMDane.service;

import com.Dane.UMDane.dto.UserRestreakDTO;
import com.Dane.UMDane.entity.UserRestreak;
import com.Dane.UMDane.entity.UserRestreakEvent;
import com.Dane.UMDane.repository.SubmissionRepository;
import com.Dane.UMDane.repository.UserRestreakEventRepository;
import com.Dane.UMDane.repository.UserRestreakRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRestreakService {

    private final UserRestreakRepository userRestreakRepository;
    private final UserRestreakEventRepository userRestreakEventRepository;
    private final SubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public UserRestreakDTO getUserRestreakInfo(Long userId) {
        UserRestreak restreak = userRestreakRepository.findById(userId)
                .orElseGet(() -> UserRestreak.builder().userId(userId).restreaksAvailable(0).build());

        List<UserRestreakEvent> events = userRestreakEventRepository.findByUserId(userId);

        List<String> earnedDates = events.stream()
                .filter(e -> "EARNED".equals(e.getEventType()))
                .map(e -> e.getEventDate().toString())
                .collect(Collectors.toList());

        List<String> usedDates = events.stream()
                .filter(e -> "USED".equals(e.getEventType()))
                .map(e -> e.getEventDate().toString())
                .collect(Collectors.toList());

        return UserRestreakDTO.builder()
                .restreaksAvailable(restreak.getRestreaksAvailable())
                .earnedDates(earnedDates)
                .usedDates(usedDates)
                .build();
    }

    @Transactional
    public void handleCheckAndEarnRestreak(Long userId, LocalDate date) {
        // 1. Check if user already earned a restreak card today
        boolean alreadyEarned = userRestreakEventRepository.existsByUserIdAndEventTypeAndEventDate(userId, "EARNED", date);
        if (alreadyEarned) {
            log.info("User {} đã nhận restreak trong ngày {} rồi, bỏ qua.", userId, date);
            return;
        }

        // 2. Count distinct successfully solved problems today
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        long solvedCount = submissionRepository.countSolvedProblemsBetween(userId, startOfDay, endOfDay);

        log.info("User {} đã giải {} bài trong ngày {}", userId, solvedCount, date);

        if (solvedCount >= 3) {
            // Earn 1 restreak card!
            UserRestreak restreak = userRestreakRepository.findById(userId)
                    .orElseGet(() -> UserRestreak.builder().userId(userId).restreaksAvailable(0).build());
            
            restreak.setRestreaksAvailable(restreak.getRestreaksAvailable() + 1);
            userRestreakRepository.save(restreak);

            // Log event
            UserRestreakEvent event = UserRestreakEvent.builder()
                    .userId(userId)
                    .eventType("EARNED")
                    .eventDate(date)
                    .build();
            userRestreakEventRepository.save(event);

            log.info("User {} được cộng 1 thẻ Restreak cho ngày {}. Tổng thẻ hiện có: {}", 
                    userId, date, restreak.getRestreaksAvailable());
        }
    }

    @Transactional
    public void useRestreak(Long userId, LocalDate date) {
        // 1. Verify card availability
        UserRestreak restreak = userRestreakRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa có thẻ Restreak nào!"));
        if (restreak.getRestreaksAvailable() <= 0) {
            throw new RuntimeException("Bạn đã dùng hết thẻ Restreak!");
        }

        // 2. Check if a restreak was already applied to this date
        boolean alreadyUsed = userRestreakEventRepository.existsByUserIdAndEventTypeAndEventDate(userId, "USED", date);
        if (alreadyUsed) {
            throw new RuntimeException("Bạn đã dùng thẻ Restreak cho ngày " + date + " rồi!");
        }

        // 3. Check if user actually has no ACCEPTED submissions on this date
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        long solvedCount = submissionRepository.countSolvedProblemsBetween(userId, startOfDay, endOfDay);
        if (solvedCount > 0) {
            throw new RuntimeException("Bạn đã giải bài tập thành công trong ngày " + date + ", không cần dùng thẻ Restreak!");
        }

        // 4. Consume card and save event
        restreak.setRestreaksAvailable(restreak.getRestreaksAvailable() - 1);
        userRestreakRepository.save(restreak);

        UserRestreakEvent event = UserRestreakEvent.builder()
                .userId(userId)
                .eventType("USED")
                .eventDate(date)
                .build();
        userRestreakEventRepository.save(event);

        log.info("User {} đã dùng 1 thẻ Restreak khôi phục cho ngày {}. Thẻ còn lại: {}", 
                userId, date, restreak.getRestreaksAvailable());
    }
}
