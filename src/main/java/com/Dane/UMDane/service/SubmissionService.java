package com.Dane.UMDane.service;

import com.Dane.UMDane.dto.CodeSubmitDTO;
import com.Dane.UMDane.entity.Submission;
import com.Dane.UMDane.entity.SubmissionStatus;
import com.Dane.UMDane.repository.ProblemRepository;
import com.Dane.UMDane.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final SubmissionRepository submissionRepository;
    private final ProblemRepository  problemRepository;

    public Submission submitCode (CodeSubmitDTO requestDTO) {
        problemRepository.findById(requestDTO.getProblemId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề bài!"));

        Submission submission = Submission.builder()
                .problemId(requestDTO.getProblemId())
                .code(requestDTO.getCode())
                .language(requestDTO.getLanguage())
                .status(SubmissionStatus.PENDING)
                .build();

        submission = submissionRepository.save(submission);

        return mockJudgeExecution(submission);
    }
    private Submission mockJudgeExecution(Submission submission) {
        try {
            Thread.sleep(1500);

            int randomResult = new Random().nextInt(10);
            if (randomResult < 7) {
                submission.setStatus(SubmissionStatus.ACCEPTED);
                submission.setRuntimeMs(new Random().nextInt(50) + 10); // Random chạy mất 10-60ms
            } else {
                submission.setStatus(SubmissionStatus.WRONG_ANSWER);
                submission.setRuntimeMs(0);
            }

            return submissionRepository.save(submission);

        } catch (InterruptedException e) {
            submission.setStatus(SubmissionStatus.COMPILE_ERROR);
            return submissionRepository.save(submission);
        }
    }
}
