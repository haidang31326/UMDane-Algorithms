package com.Dane.UMDane.service;

import com.Dane.UMDane.dto.CodeSubmitDTO;
import com.Dane.UMDane.dto.SandboxResult;
import com.Dane.UMDane.entity.Submission;
import com.Dane.UMDane.entity.SubmissionStatus;
import com.Dane.UMDane.entity.TestCase;
import com.Dane.UMDane.repository.ProblemRepository;
import com.Dane.UMDane.repository.SubmissionRepository;
import com.Dane.UMDane.repository.TestCaseRepository;
import com.Dane.UMDane.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {
    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final DockerSandboxService sandboxService;
    private final SimpMessagingTemplate messagingTemplate;

    public Submission submitCode(CodeSubmitDTO requestDTO) {
        problemRepository.findById(requestDTO.getProblemId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề bài!"));

        // Retrieve logged-in user id
        Long userId = null;
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserPrincipal) {
                userId = ((UserPrincipal) principal).getId();
            }
        } catch (Exception e) {
            log.warn("Không lấy được thông tin user từ Security Context", e);
        }

        // 1. Save pending submission
        Submission submission = Submission.builder()
                .problemId(requestDTO.getProblemId())
                .userId(userId)
                .code(requestDTO.getCode())
                .language(requestDTO.getLanguage())
                .status(SubmissionStatus.PENDING)
                .runtimeMs(0)
                .build();

        submission = submissionRepository.save(submission);

        // Broadcast pending status
        broadcastUpdate(submission);

        // 2. Fetch test cases
        List<TestCase> testCases = testCaseRepository.findByProblemId(requestDTO.getProblemId());
        if (testCases.isEmpty()) {
            submission.setStatus(SubmissionStatus.COMPILE_ERROR);
            submission.setErrorMessage("Hệ thống chưa cấu hình Test Case cho bài toán này!");
            submission = submissionRepository.save(submission);
            broadcastUpdate(submission);
            return submission;
        }

        // 3. Run execution
        int maxRuntimeMs = 0;
        SubmissionStatus finalStatus = SubmissionStatus.ACCEPTED;
        String errorMessage = null;

        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
            
            // Timeout limit: 2000ms, Memory limit: 128MB
            SandboxResult result = sandboxService.execute(requestDTO.getCode(), tc.getInputData(), 2000, 128);

            if (result.getStatus() == SubmissionStatus.COMPILE_ERROR) {
                finalStatus = SubmissionStatus.COMPILE_ERROR;
                errorMessage = result.getErrorOutput();
                break;
            }

            if (result.getStatus() == SubmissionStatus.TIME_LIMIT_EXCEEDED) {
                finalStatus = SubmissionStatus.TIME_LIMIT_EXCEEDED;
                errorMessage = "Time Limit Exceeded ở test case #" + (i + 1);
                maxRuntimeMs = Math.max(maxRuntimeMs, result.getRuntimeMs());
                break;
            }

            if (result.getStatus() == SubmissionStatus.RUNTIME_ERROR) {
                finalStatus = SubmissionStatus.RUNTIME_ERROR;
                errorMessage = "Runtime Error ở test case #" + (i + 1) + ":\n" + result.getErrorOutput();
                maxRuntimeMs = Math.max(maxRuntimeMs, result.getRuntimeMs());
                break;
            }

            // Compare outputs
            String cleanExpected = cleanOutput(tc.getExpectedOutput());
            String cleanActual = cleanOutput(result.getOutput());
            
            maxRuntimeMs = Math.max(maxRuntimeMs, result.getRuntimeMs());

            if (!cleanExpected.equals(cleanActual)) {
                finalStatus = SubmissionStatus.WRONG_ANSWER;
                errorMessage = "Wrong Answer ở test case #" + (i + 1) + ".\nExpected:\n" + tc.getExpectedOutput() + "\nActual:\n" + result.getOutput();
                break;
            }
        }

        // Update submission status
        submission.setStatus(finalStatus);
        submission.setRuntimeMs(maxRuntimeMs);
        submission.setErrorMessage(errorMessage);
        submission = submissionRepository.save(submission);

        // Broadcast final status
        broadcastUpdate(submission);

        return submission;
    }

    private void broadcastUpdate(Submission submission) {
        try {
            messagingTemplate.convertAndSend("/topic/submissions", submission);
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo WebSocket", e);
        }
    }

    private String cleanOutput(String output) {
        if (output == null) {
            return "";
        }
        // Normalize line endings and trim spaces
        return output.replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }
}
