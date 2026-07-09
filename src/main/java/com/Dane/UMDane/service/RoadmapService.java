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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapService {

    private final RoadmapNodeRepository roadmapNodeRepository;
    private final SubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public List<RoadmapNodeResponseDTO> getRoadmapNodes(Long userId) {
        List<RoadmapNode> nodes = roadmapNodeRepository.findAllByOrderByNodeIdAsc();
        List<RoadmapNodeResponseDTO> dtos = new ArrayList<>();
        
        boolean nextUnlocked = true; // Node 1 is always unlocked

        for (RoadmapNode node : nodes) {
            boolean solved = false;
            boolean isUnlocked = nextUnlocked;

            if (userId != null && node.getProblemId() != null) {
                solved = submissionRepository.existsByUserIdAndProblemIdAndStatus(
                        userId, 
                        node.getProblemId(), 
                        SubmissionStatus.ACCEPTED
                );
            }

            dtos.add(RoadmapNodeResponseDTO.builder()
                    .nodeId(node.getNodeId())
                    .phase(node.getPhase())
                    .title(node.getTitle())
                    .topic(node.getTopic())
                    .keyword(node.getKeyword())
                    .difficulty(node.getDifficulty())
                    .problemId(node.getProblemId())
                    .solved(solved)
                    .unlocked(isUnlocked)
                    .build());

            // Next node is unlocked if current node is solved
            nextUnlocked = solved;
        }

        return dtos;
    }
}
