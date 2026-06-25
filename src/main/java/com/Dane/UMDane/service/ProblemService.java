package com.Dane.UMDane.service;

import com.Dane.UMDane.dto.ProblemResponseDTO;
import com.Dane.UMDane.entity.Problem;
import com.Dane.UMDane.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ProblemService {
    private final ProblemRepository problemRepository;
    private final Random random = new Random();

    public List<ProblemResponseDTO> getAllProblems() {
        return problemRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    public ProblemResponseDTO getRandomProblemByVibe(String topic, String keyword) {
        List<Problem> problems = problemRepository.findByTopicAndKeyword(topic, keyword);

        if (problems.isEmpty()) {
            throw new RuntimeException("chưa có cha ơi");
        }
        Problem problem = problems.get(random.nextInt(problems.size()));
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
                .build();
    }
}
