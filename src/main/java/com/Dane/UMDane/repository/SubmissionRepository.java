package com.Dane.UMDane.repository;

import com.Dane.UMDane.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    void deleteByProblemId(Long problemId);

    @Query("SELECT COUNT(DISTINCT s.problemId) FROM Submission s WHERE s.userId = :userId AND s.status = com.Dane.UMDane.entity.SubmissionStatus.ACCEPTED AND s.createdAt >= :start AND s.createdAt <= :end")
    long countSolvedProblemsBetween(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    boolean existsByUserIdAndProblemIdAndStatus(Long userId, Long problemId, com.Dane.UMDane.entity.SubmissionStatus status);

    java.util.List<Submission> findByUserId(Long userId, org.springframework.data.domain.Sort sort);
}
