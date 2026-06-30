package com.Dane.UMDane.service;

import com.Dane.UMDane.dto.CodeRunDTO;
import com.Dane.UMDane.dto.CodeSubmitDTO;
import com.Dane.UMDane.dto.SandboxResult;
import com.Dane.UMDane.entity.Submission;
import com.Dane.UMDane.entity.SubmissionStatus;
import com.Dane.UMDane.entity.TestCase;
import com.Dane.UMDane.entity.Problem;
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
        Problem problem = problemRepository.findById(requestDTO.getProblemId())
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

        // 3. Run execution in batch
        int timeLimit = (problem.getTimeLimit() != null) ? problem.getTimeLimit() : 2000;
        int memoryLimit = (problem.getMemoryLimit() != null) ? problem.getMemoryLimit() : 128;
        
        List<String> inputs = testCases.stream().map(TestCase::getInputData).toList();
        List<SandboxResult> results = sandboxService.executeBatch(requestDTO.getCode(), problem.getDriverCode(), inputs, timeLimit, memoryLimit);

        int maxRuntimeMs = 0;
        SubmissionStatus finalStatus = SubmissionStatus.ACCEPTED;
        String errorMessage = null;

        if (!results.isEmpty() && results.get(0).getStatus() == SubmissionStatus.COMPILE_ERROR) {
            finalStatus = SubmissionStatus.COMPILE_ERROR;
            errorMessage = results.get(0).getErrorOutput();
        } else {
            for (int i = 0; i < results.size(); i++) {
                TestCase tc = testCases.get(i);
                SandboxResult result = results.get(i);

                if (result.getStatus() == SubmissionStatus.TIME_LIMIT_EXCEEDED) {
                    finalStatus = SubmissionStatus.TIME_LIMIT_EXCEEDED;
                    String caseType = (tc.getIsHidden() != null && tc.getIsHidden()) ? " (Edge Case Ẩn)" : "";
                    errorMessage = "Time Limit Exceeded ở test case #" + (i + 1) + caseType + ".\n" +
                            "Input:\n" + tc.getInputData() + "\n" +
                            "Limit: " + timeLimit + " ms, Actual: " + result.getRuntimeMs() + " ms";
                    maxRuntimeMs = Math.max(maxRuntimeMs, result.getRuntimeMs());
                    break;
                }

                if (result.getStatus() == SubmissionStatus.RUNTIME_ERROR) {
                    finalStatus = SubmissionStatus.RUNTIME_ERROR;
                    String caseType = (tc.getIsHidden() != null && tc.getIsHidden()) ? " (Edge Case Ẩn)" : "";
                    errorMessage = "Runtime Error ở test case #" + (i + 1) + caseType + ".\n" +
                            "Input:\n" + tc.getInputData() + "\n" +
                            "Details:\n" + result.getErrorOutput();
                    maxRuntimeMs = Math.max(maxRuntimeMs, result.getRuntimeMs());
                    break;
                }

                // Compare outputs
                String cleanExpected = cleanOutput(tc.getExpectedOutput());
                String cleanActual = cleanOutput(result.getOutput());
                
                maxRuntimeMs = Math.max(maxRuntimeMs, result.getRuntimeMs());

                if (!cleanExpected.equals(cleanActual)) {
                    finalStatus = SubmissionStatus.WRONG_ANSWER;
                    String caseType = (tc.getIsHidden() != null && tc.getIsHidden()) ? " (Edge Case Ẩn)" : "";
                    errorMessage = "Wrong Answer ở test case #" + (i + 1) + caseType + ".\n" +
                            "Input:\n" + tc.getInputData() + "\n" +
                            "Expected:\n" + tc.getExpectedOutput() + "\n" +
                            "Actual:\n" + result.getOutput();
                    break;
                }
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

    public List<SandboxResult> runCode(CodeRunDTO requestDTO) {
        log.info("Đang chạy thử code (không lưu database)...");
        String driverCode = null;
        int timeLimit = 2000;
        int memoryLimit = 128;
        if (requestDTO.getProblemId() != null) {
            try {
                Problem problem = problemRepository.findById(requestDTO.getProblemId()).orElse(null);
                if (problem != null) {
                    driverCode = problem.getDriverCode();
                    if (problem.getTimeLimit() != null) timeLimit = problem.getTimeLimit();
                    if (problem.getMemoryLimit() != null) memoryLimit = problem.getMemoryLimit();
                }
            } catch (Exception e) {
                log.warn("Lỗi khi lấy driver code cho chạy thử", e);
            }
        }
        
        List<String> inputs = new java.util.ArrayList<>();
        if (requestDTO.getInputs() != null && !requestDTO.getInputs().isEmpty()) {
            inputs.addAll(requestDTO.getInputs());
        } else {
            inputs.add(requestDTO.getInputData() != null ? requestDTO.getInputData() : "");
        }
        
        return sandboxService.executeBatch(requestDTO.getCode(), driverCode, inputs, timeLimit, memoryLimit);
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
