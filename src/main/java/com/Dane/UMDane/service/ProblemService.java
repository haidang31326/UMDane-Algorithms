package com.Dane.UMDane.service;

import com.Dane.UMDane.dto.ProblemResponseDTO;
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
    private final Random random = new Random();

    public List<ProblemResponseDTO> getAllProblems(Long userId) {
        List<Problem> problems = problemRepository.findAll();
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
        List<TestCase> testCases = new ArrayList<>();
        String normalizedDifficulty = (difficulty == null || difficulty.trim().isEmpty()) ? "MEDIUM" : difficulty.toUpperCase().trim();

        if (geminiAiService.isApiKeyConfigured()) {
            try {
                log.info("Đang gọi Gemini AI để sinh đề bài: topic={}, keyword={}, difficulty={}", topic, keyword, normalizedDifficulty);
                GeminiAiService.GeneratedProblem aiProb = geminiAiService.generateProblemFromAi(topic, keyword, normalizedDifficulty);
                
                title = aiProb.getTitle();
                description = aiProb.getDescription();
                hint = aiProb.getHint();
                
                for (GeminiAiService.GeneratedTestCase aiTc : aiProb.getTestCases()) {
                    testCases.add(TestCase.builder()
                            .inputData(aiTc.getInputData())
                            .expectedOutput(aiTc.getExpectedOutput())
                            .isHidden(aiTc.isHidden())
                            .build());
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
        return ProblemResponseDTO.builder()
                .id(problem.getId())
                .topic(problem.getTopic())
                .keyword(problem.getKeyword())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .difficulty(problem.getDifficulty())
                .hint(problem.getHint())
                .build();
    }
}
