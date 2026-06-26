package com.Dane.UMDane.repository;

import com.Dane.UMDane.entity.UserDeletedProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDeletedProblemRepository extends JpaRepository<UserDeletedProblem, Long> {

    @Query("SELECT udp.problemId FROM UserDeletedProblem udp WHERE udp.userId = :userId")
    List<Long> findProblemIdsByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndProblemId(Long userId, Long problemId);
    
    boolean existsByUserIdAndProblemId(Long userId, Long problemId);
}
