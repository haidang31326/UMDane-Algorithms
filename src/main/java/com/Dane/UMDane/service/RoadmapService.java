package com.Dane.UMDane.service;

import com.Dane.UMDane.dto.RoadmapNodeResponseDTO;
import com.Dane.UMDane.entity.RoadmapNode;
import com.Dane.UMDane.entity.SubmissionStatus;
import com.Dane.UMDane.repository.RoadmapNodeRepository;
import com.Dane.UMDane.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapService {

    private final RoadmapNodeRepository roadmapNodeRepository;
    private final SubmissionRepository submissionRepository;
    private final ProblemService problemService;

    @Transactional(readOnly = true)
    public List<RoadmapNodeResponseDTO> getRoadmapNodes(Long userId) {
        List<RoadmapNode> nodes = roadmapNodeRepository.findAllByOrderByNodeIdAsc();
        return nodes.stream().map(node -> {
            boolean solved = false;
            if (userId != null && node.getProblemId() != null) {
                solved = submissionRepository.existsByUserIdAndProblemIdAndStatus(
                        userId, 
                        node.getProblemId(), 
                        SubmissionStatus.ACCEPTED
                );
            }
            return RoadmapNodeResponseDTO.builder()
                    .nodeId(node.getNodeId())
                    .phase(node.getPhase())
                    .title(node.getTitle())
                    .topic(node.getTopic())
                    .keyword(node.getKeyword())
                    .difficulty(node.getDifficulty())
                    .problemId(node.getProblemId())
                    .solved(solved)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public Long generateNodeProblem(Integer nodeId) {
        RoadmapNode node = roadmapNodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Node tương ứng trong lộ trình!"));

        if (node.getProblemId() != null) {
            return node.getProblemId();
        }

        // Generate the problem via ProblemService
        log.info("Bắt đầu sinh đề bài tự động cho Roadmap Node {}: topic={}, keyword={}, difficulty={}",
                nodeId, node.getTopic(), node.getKeyword(), node.getDifficulty());
        
        var problemDTO = problemService.generateProblem(node.getTopic(), node.getKeyword(), node.getDifficulty());
        
        // Link the generated problem to the node
        node.setProblemId(problemDTO.getId());
        roadmapNodeRepository.save(node);

        log.info("Sinh đề bài thành công cho Node {}! Problem ID = {}", nodeId, problemDTO.getId());
        return problemDTO.getId();
    }
}
