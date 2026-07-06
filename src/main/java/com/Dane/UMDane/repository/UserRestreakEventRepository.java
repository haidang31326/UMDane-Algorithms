package com.Dane.UMDane.repository;

import com.Dane.UMDane.entity.UserRestreakEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserRestreakEventRepository extends JpaRepository<UserRestreakEvent, Long> {
    List<UserRestreakEvent> findByUserId(Long userId);
    List<UserRestreakEvent> findByUserIdAndEventType(Long userId, String eventType);
    boolean existsByUserIdAndEventTypeAndEventDate(Long userId, String eventType, LocalDate eventDate);
}
