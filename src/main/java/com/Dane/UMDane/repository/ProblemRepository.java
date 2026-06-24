package com.Dane.UMDane.repository;

import com.Dane.UMDane.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findByTopicAndKeyword(String topic, String keyword);
}
