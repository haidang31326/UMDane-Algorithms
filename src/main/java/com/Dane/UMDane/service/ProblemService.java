package com.Dane.UMDane.service;

import com.Dane.UMDane.dto.ProblemResponseDTO;
import com.Dane.UMDane.dto.TestCaseDTO;
import com.Dane.UMDane.entity.Problem;
import com.Dane.UMDane.entity.TestCase;
import com.Dane.UMDane.repository.ProblemRepository;
import com.Dane.UMDane.repository.TestCaseRepository;
import com.Dane.UMDane.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.Dane.UMDane.entity.UserDeletedProblem;
import com.Dane.UMDane.repository.UserDeletedProblemRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemService {
    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final UserDeletedProblemRepository userDeletedProblemRepository;
    private final GeminiAiService geminiAiService;
    private final DockerSandboxService sandboxService;
    private final com.Dane.UMDane.repository.RoadmapNodeRepository roadmapNodeRepository;
    private final Random random = new Random();

    public List<ProblemResponseDTO> getAllProblems(Long userId) {
        List<Problem> problems = problemRepository.findAll();
        
        List<Long> roadmapProblemIds = roadmapNodeRepository.findAll().stream()
                .map(com.Dane.UMDane.entity.RoadmapNode::getProblemId)
                .filter(java.util.Objects::nonNull)
                .toList();

        problems = problems.stream()
                .filter(p -> !roadmapProblemIds.contains(p.getId()))
                .toList();

        if (userId != null) {
            List<Long> deletedProblemIds = userDeletedProblemRepository.findProblemIdsByUserId(userId);
            problems = problems.stream()
                    .filter(p -> !deletedProblemIds.contains(p.getId()))
                    .toList();
        }
        return problems.stream()
                .map(this::mapToDTO)
                .toList();
    }

    public ProblemResponseDTO getRandomProblemByVibe(String topic, String keyword) {
        List<Problem> problems = problemRepository.findByTopicAndKeyword(topic, keyword);
        
        List<Long> roadmapProblemIds = roadmapNodeRepository.findAll().stream()
                .map(com.Dane.UMDane.entity.RoadmapNode::getProblemId)
                .filter(java.util.Objects::nonNull)
                .toList();

        problems = problems.stream()
                .filter(p -> !roadmapProblemIds.contains(p.getId()))
                .toList();

        if (problems.isEmpty()) {
            throw new RuntimeException("Chưa có bài tập nào tương ứng chủ đề và từ khóa này!");
        }
        Problem problem = problems.get(random.nextInt(problems.size()));
        return mapToDTO(problem);
    }

    @Transactional
    public ProblemResponseDTO generateProblem(String topic, String keyword, String difficulty) {
        String title;
        String description;
        String hint;
        String constraints = null;
        Integer timeLimit = 2000;
        Integer memoryLimit = 128;
        String userTemplate = null;
        String referenceSolution = null;
        String driverCode = null;
        List<TestCase> testCases = new ArrayList<>();
        String normalizedDifficulty = (difficulty == null || difficulty.trim().isEmpty()) ? "MEDIUM" : difficulty.toUpperCase().trim();

        if (geminiAiService.isApiKeyConfigured()) {
            try {
                log.info("Đang gọi Gemini AI để sinh đề bài: topic={}, keyword={}, difficulty={}", topic, keyword, normalizedDifficulty);
                List<String> existingTitles = problemRepository.findAll().stream()
                        .map(Problem::getTitle)
                        .toList();
                GeminiAiService.GeneratedProblem aiProb = geminiAiService.generateProblemFromAi(topic, keyword, normalizedDifficulty, existingTitles);
                
                title = aiProb.getTitle();
                description = aiProb.getDescription();
                hint = aiProb.getHint();
                constraints = aiProb.getConstraints();
                timeLimit = aiProb.getTimeLimit();
                memoryLimit = aiProb.getMemoryLimit();
                userTemplate = aiProb.getUserTemplate();
                referenceSolution = aiProb.getReferenceSolution();
                driverCode = aiProb.getDriverCode();
                
                for (GeminiAiService.GeneratedTestCase aiTc : aiProb.getTestCases()) {
                    testCases.add(TestCase.builder()
                            .inputData(aiTc.getInputData())
                            .isHidden(aiTc.isHidden())
                            .build());
                }

                // Batch execute reference solution in the sandbox to generate exact expected outputs
                int tLimit = timeLimit != null ? timeLimit : 2000;
                int mLimit = memoryLimit != null ? memoryLimit : 128;
                List<String> inputs = testCases.stream().map(TestCase::getInputData).toList();
                
                log.info("Đang tự động xác thực và chạy thử mã giải mẫu (Reference Solution) cho {} test cases...", testCases.size());
                List<com.Dane.UMDane.dto.SandboxResult> sandboxResults = sandboxService.executeBatch(
                        referenceSolution, 
                        driverCode, 
                        inputs, 
                        tLimit, 
                        mLimit
                );
                
                if (sandboxResults.isEmpty()) {
                    throw new RuntimeException("Không nhận được kết quả xác thực từ Docker sandbox!");
                }
                
                if (sandboxResults.get(0).getStatus() == com.Dane.UMDane.entity.SubmissionStatus.COMPILE_ERROR) {
                    log.error("Reference solution compilation failed during auto-validation: {}", sandboxResults.get(0).getErrorOutput());
                    throw new RuntimeException("Mã giải mẫu (Reference Solution) bị lỗi biên dịch: " + sandboxResults.get(0).getErrorOutput());
                }
                
                for (int i = 0; i < testCases.size(); i++) {
                    TestCase tc = testCases.get(i);
                    com.Dane.UMDane.dto.SandboxResult res = sandboxResults.get(i);
                    
                    if (res.getStatus() == com.Dane.UMDane.entity.SubmissionStatus.RUNTIME_ERROR) {
                        log.error("Reference solution runtime error at case #{}: {}", i + 1, res.getErrorOutput());
                        throw new RuntimeException("Mã giải mẫu gặp lỗi thời gian chạy ở test case #" + (i + 1) + ": " + res.getErrorOutput());
                    }
                    if (res.getStatus() == com.Dane.UMDane.entity.SubmissionStatus.TIME_LIMIT_EXCEEDED) {
                        log.error("Reference solution TLE at case #{}", i + 1);
                        throw new RuntimeException("Mã giải mẫu chạy quá giới hạn thời gian (TLE) ở test case #" + (i + 1));
                    }
                    
                    tc.setExpectedOutput(res.getOutput() != null ? res.getOutput().trim() : "");
                }
            } catch (Exception e) {
                log.error("AI generation failed", e);
                throw new RuntimeException("AI gặp lỗi khi thiết kế đề bài: " + e.getMessage());
            }
        } else {
            log.warn("Chưa cấu hình Gemini API Key.");
            throw new RuntimeException("Hệ thống chưa cấu hình Gemini API Key để sinh đề bài bằng AI!");
        }

        Problem problem = Problem.builder()
                .title(title)
                .topic(topic)
                .keyword(keyword)
                .description(description)
                .hint(hint)
                .difficulty(normalizedDifficulty)
                .constraints(constraints)
                .timeLimit(timeLimit)
                .memoryLimit(memoryLimit)
                .userTemplate(userTemplate)
                .referenceSolution(referenceSolution)
                .driverCode(driverCode)
                .build();
        problem = problemRepository.save(problem);

        for (TestCase tc : testCases) {
            tc.setProblemId(problem.getId());
            testCaseRepository.save(tc);
        }

        return mapToDTO(problem);
    }

    @Transactional
    public void deleteProblem(Long id) {
        log.info("Đang xóa bài tập ID={} (và cascade xóa test cases, submissions)...", id);
        testCaseRepository.deleteByProblemId(id);
        submissionRepository.deleteByProblemId(id);
        problemRepository.deleteById(id);
    }

    @Transactional
    public void hideProblemForUser(Long userId, Long problemId) {
        log.info("Ẩn bài tập ID={} đối với User ID={}...", problemId, userId);
        if (!userDeletedProblemRepository.existsByUserIdAndProblemId(userId, problemId)) {
            UserDeletedProblem udp = UserDeletedProblem.builder()
                    .userId(userId)
                    .problemId(problemId)
                    .build();
            userDeletedProblemRepository.save(udp);
        }
    }

    private ProblemResponseDTO generateMockProblem(String topic, String keyword, String difficulty) {
        String title;
        String description;
        String hint;
        List<TestCase> testCases = new ArrayList<>();

        if ("Greedy".equalsIgnoreCase(topic) && "tiktok".equalsIgnoreCase(keyword)) {
            title = "TikTok Video Scheduler (Greedy) [OFFLINE]";
            description = "Bạn là một nhà sáng tạo nội dung trên TikTok và đang có kế hoạch đăng một số video quảng cáo. Bạn có danh sách N video, mỗi video i bắt đầu vào thời điểm S_i và kết thúc vào thời điểm E_i. Bạn không thể đăng hai video trùng lặp hoặc chồng chéo thời gian lên nhau. Hãy thiết lập chiến thuật tham lam (Greedy) để chọn được số lượng video quảng cáo nhiều nhất có thể.\n\nĐầu vào (Standard Input):\n- Dòng đầu tiên chứa số nguyên N (số lượng video).\n- N dòng tiếp theo, mỗi dòng chứa 2 số nguyên S_i và E_i biểu diễn thời gian bắt đầu và kết thúc của video thứ i.\n\nĐầu ra (Standard Output):\n- Số lượng video tối đa bạn có thể đăng.";
            hint = "Sắp xếp các video theo thời gian kết thúc tăng dần (Earliest Deadline First) và tham lam chọn video tiếp theo không bị trùng lặp.";
            
            testCases.add(TestCase.builder().inputData("3\n1 3\n2 5\n3 6").expectedOutput("2").isHidden(false).build());
            testCases.add(TestCase.builder().inputData("4\n1 4\n2 6\n4 8\n5 7").expectedOutput("2").isHidden(false).build());
            testCases.add(TestCase.builder().inputData("1\n10 20").expectedOutput("1").isHidden(false).build());
        } else {
            title = "Bài toán tối ưu hóa " + keyword + " (" + topic + ") [OFFLINE]";
            description = "Đề bài tự động tạo cho từ khóa: " + keyword + " và chủ đề: " + topic + ".\nĐọc hai số nguyên A và B từ standard input, in ra A + B.";
            hint = "Sử dụng Scanner để đọc hai số nguyên và in ra tổng.";
            testCases.add(TestCase.builder().inputData("5 10").expectedOutput("15").isHidden(false).build());
            testCases.add(TestCase.builder().inputData("0 0").expectedOutput("0").isHidden(false).build());
        }

        Problem problem = Problem.builder()
                .title(title)
                .topic(topic)
                .keyword(keyword)
                .description(description)
                .hint(hint)
                .difficulty(difficulty)
                .constraints("1 <= N <= 10^5\nCác giá trị nhập vào đều hợp lệ.")
                .timeLimit(2000)
                .memoryLimit(128)
                .build();
        problem = problemRepository.save(problem);

        for (TestCase tc : testCases) {
            tc.setProblemId(problem.getId());
            testCaseRepository.save(tc);
        }

        return mapToDTO(problem);
    }

    @Cacheable(value = "problemCache", key = "#id")
    public ProblemResponseDTO getProblemById(Long id) {
        System.out.println(">>> [DB-CALL] Đang chọc xuống MySQL để lấy đề bài ID: " + id);

        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề bài!"));

        return mapToDTO(problem);
    }

    private ProblemResponseDTO mapToDTO(Problem problem) {
        List<TestCaseDTO> samples = testCaseRepository.findByProblemId(problem.getId()).stream()
                .filter(tc -> tc.getIsHidden() == null || !tc.getIsHidden())
                .map(tc -> {
                    TestCaseDTO dto = new TestCaseDTO();
                    dto.setInputData(tc.getInputData());
                    dto.setExpectedOutput(tc.getExpectedOutput());
                    dto.setIsHidden(false);
                    return dto;
                })
                .toList();

        return ProblemResponseDTO.builder()
                .id(problem.getId())
                .topic(problem.getTopic())
                .keyword(problem.getKeyword())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .difficulty(problem.getDifficulty())
                .hint(problem.getHint())
                .constraints(problem.getConstraints())
                .timeLimit(problem.getTimeLimit())
                .memoryLimit(problem.getMemoryLimit())
                .userTemplate(problem.getUserTemplate())
                .driverCode(problem.getDriverCode())
                .sampleTestCases(samples)
                .build();
    }
}
