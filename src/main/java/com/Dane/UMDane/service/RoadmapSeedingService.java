package com.Dane.UMDane.service;

import com.Dane.UMDane.entity.Problem;
import com.Dane.UMDane.entity.RoadmapNode;
import com.Dane.UMDane.entity.TestCase;
import com.Dane.UMDane.repository.ProblemRepository;
import com.Dane.UMDane.repository.RoadmapNodeRepository;
import com.Dane.UMDane.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapSeedingService {

    private final RoadmapNodeRepository roadmapNodeRepository;
    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final GeminiAiService geminiAiService;
    private final DockerSandboxService sandboxService;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    private volatile boolean seedingActive = false;
    private volatile int seededCount = 0;
    private volatile String currentStatusMessage = "Idle";

    public boolean isSeedingActive() {
        return seedingActive;
    }

    public int getSeededCount() {
        return seededCount;
    }

    public String getCurrentStatusMessage() {
        return currentStatusMessage;
    }

    public synchronized void startSeeding() {
        if (seedingActive) {
            throw new IllegalStateException("Hệ thống đang tiến hành sinh đề bài lộ trình rồi!");
        }

        seedingActive = true;
        seededCount = 0;
        currentStatusMessage = "Đang bắt đầu...";

        executorService.submit(() -> {
            try {
                List<RoadmapNode> nodes = roadmapNodeRepository.findAllByOrderByNodeIdAsc();
                List<String> existingTitles = new ArrayList<>(problemRepository.findAll().stream()
                        .map(Problem::getTitle)
                        .toList());

                for (RoadmapNode node : nodes) {
                    // Check if node is already seeded
                    if (node.getProblemId() != null && problemRepository.existsById(node.getProblemId())) {
                        log.info("Node {} đã có đề bài ID = {}, bỏ qua sinh lại.", node.getNodeId(), node.getProblemId());
                        seededCount++;
                        continue;
                    }

                    currentStatusMessage = "Đang sinh đề bài cho Node " + node.getNodeId() + ": " + node.getTitle();
                    log.info("Bắt đầu sinh đề bài cho Roadmap Node {}: topic={}, keyword={}", 
                            node.getNodeId(), node.getTopic(), node.getKeyword());

                    // Get previous node details if N > 1
                    String prevDescription = "";
                    String prevSolution = "";
                    if (node.getNodeId() > 1) {
                        RoadmapNode prevNode = roadmapNodeRepository.findById(node.getNodeId() - 1).orElse(null);
                        if (prevNode != null && prevNode.getProblemId() != null) {
                            Problem prevProb = problemRepository.findById(prevNode.getProblemId()).orElse(null);
                            if (prevProb != null) {
                                prevDescription = prevProb.getDescription();
                                prevSolution = prevProb.getReferenceSolution();
                            }
                        }
                    }

                    // Call AI to generate roadmap problem
                    GeminiAiService.GeneratedProblem aiProb = geminiAiService.generateRoadmapProblem(
                            node.getTopic(), 
                            node.getKeyword(), 
                            node.getDifficulty(), 
                            prevDescription, 
                            prevSolution, 
                            existingTitles
                    );

                    // Compile & validate using sandbox
                    List<TestCase> testCases = new ArrayList<>();
                    for (GeminiAiService.GeneratedTestCase aiTc : aiProb.getTestCases()) {
                        testCases.add(TestCase.builder()
                                .inputData(aiTc.getInputData())
                                .isHidden(aiTc.isHidden())
                                .build());
                    }

                    List<String> inputs = testCases.stream().map(TestCase::getInputData).toList();
                    List<com.Dane.UMDane.dto.SandboxResult> sandboxResults = sandboxService.executeBatch(
                            aiProb.getReferenceSolution(),
                            aiProb.getDriverCode(),
                            inputs,
                            aiProb.getTimeLimit() != null ? aiProb.getTimeLimit() : 2000,
                            aiProb.getMemoryLimit() != null ? aiProb.getMemoryLimit() : 128
                    );

                    if (sandboxResults.isEmpty()) {
                        throw new RuntimeException("Không thể nhận kết quả từ Docker sandbox khi sinh bài cho Node " + node.getNodeId());
                    }

                    if (sandboxResults.get(0).getStatus() == com.Dane.UMDane.entity.SubmissionStatus.COMPILE_ERROR) {
                        throw new RuntimeException("Mã giải chuẩn bị lỗi biên dịch cho Node " + node.getNodeId() + ": " + sandboxResults.get(0).getErrorOutput());
                    }

                    // Set outputs from execution
                    for (int i = 0; i < testCases.size(); i++) {
                        TestCase tc = testCases.get(i);
                        tc.setExpectedOutput(sandboxResults.get(i).getOutput() != null ? sandboxResults.get(i).getOutput().trim() : "");
                    }

                    // Save problem and test cases
                    Problem problem = Problem.builder()
                            .topic(node.getTopic())
                            .keyword(node.getKeyword())
                            .title(aiProb.getTitle())
                            .description(aiProb.getDescription())
                            .hint(aiProb.getHint())
                            .constraints(aiProb.getConstraints())
                            .timeLimit(aiProb.getTimeLimit())
                            .memoryLimit(aiProb.getMemoryLimit())
                            .userTemplate(aiProb.getUserTemplate())
                            .referenceSolution(aiProb.getReferenceSolution())
                            .driverCode(aiProb.getDriverCode())
                            .build();

                    problem = problemRepository.save(problem);
                    existingTitles.add(problem.getTitle());

                    for (TestCase tc : testCases) {
                        tc.setProblemId(problem.getId());
                        testCaseRepository.save(tc);
                    }

                    // Link to node
                    node.setProblemId(problem.getId());
                    roadmapNodeRepository.save(node);

                    log.info("Node {} đã được sinh đề bài và lưu DB thành công! Problem ID = {}", 
                            node.getNodeId(), problem.getId());
                    
                    seededCount++;

                    // Cool-down sleep to respect rate limits
                    Thread.sleep(4000);
                }

                currentStatusMessage = "Tất cả 75 bài tập đã được tạo xong!";
                seedingActive = false;

            } catch (Exception e) {
                log.error("Lỗi trong quá trình sinh đề bài lộ trình", e);
                currentStatusMessage = "Lỗi: " + e.getMessage();
                seedingActive = false;
            }
        });
    }
}
