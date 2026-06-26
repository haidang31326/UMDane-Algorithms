package com.Dane.UMDane.repository;

import com.Dane.UMDane.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    void deleteByProblemId(Long problemId);
}
