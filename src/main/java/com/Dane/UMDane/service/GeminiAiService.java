package com.Dane.UMDane.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAiService {

    @Value("${umdane.gemini.api-key:}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public GeneratedProblem generateProblemFromAi(String topic, String keyword) {
        if (!isApiKeyConfigured()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        // responseSchema/structured outputs are only supported in v1beta
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey.trim();

        String prompt = String.format(
                "Hãy biên soạn một bài tập lập trình competitive programming bằng tiếng Việt cho chủ đề: '%s' và bối cảnh/từ khóa: '%s'.\n" +
                "Yêu cầu đề bài phải có tiêu đề, mô tả chi tiết, hướng dẫn đọc dữ liệu đầu vào (Standard Input) và in kết quả ra màn hình (Standard Output).\n" +
                "Sinh ra ít nhất 3 test cases hợp lệ phục vụ cho việc chấm bài của hệ thống Online Judge. Dữ liệu đầu vào của test case phải khớp với cách mô tả trong đề bài, và đáp án expectedOutput phải chính xác hoàn toàn.",
                topic, keyword
        );

        try {
            // Build Gemini payload with structured JSON output schema
            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> parts = Map.of("parts", List.of(textPart));
            Map<String, Object> contentItem = Map.of("contents", List.of(parts));

            // Define schema for structured output
            Map<String, Object> testCaseItemSchema = Map.of(
                    "type", "OBJECT",
                    "properties", Map.of(
                            "inputData", Map.of("type", "STRING"),
                            "expectedOutput", Map.of("type", "STRING"),
                            "isHidden", Map.of("type", "BOOLEAN")
                    ),
                    "required", List.of("inputData", "expectedOutput", "isHidden")
            );

            Map<String, Object> responseSchema = Map.of(
                    "type", "OBJECT",
                    "properties", Map.of(
                            "title", Map.of("type", "STRING"),
                            "description", Map.of("type", "STRING"),
                            "hint", Map.of("type", "STRING"),
                            "testCases", Map.of(
                                    "type", "ARRAY",
                                    "items", testCaseItemSchema
                                )
                    ),
                    "required", List.of("title", "description", "hint", "testCases")
            );

            Map<String, Object> generationConfig = Map.of(
                    "responseMimeType", "application/json",
                    "responseSchema", responseSchema
            );

            Map<String, Object> payload = Map.of(
                    "contents", List.of(parts),
                    "generationConfig", generationConfig
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            String responseStr = restTemplate.postForObject(url, request, String.class);

            // Parse Gemini response
            JsonNode rootNode = objectMapper.readTree(responseStr);
            String aiJsonText = rootNode.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            JsonNode aiData = objectMapper.readTree(aiJsonText);

            String title = aiData.path("title").asText();
            String description = aiData.path("description").asText();
            String hint = aiData.path("hint").asText();

            List<GeneratedTestCase> testCases = new ArrayList<>();
            JsonNode testCasesNode = aiData.path("testCases");
            if (testCasesNode.isArray()) {
                for (JsonNode tcNode : testCasesNode) {
                    testCases.add(new GeneratedTestCase(
                            tcNode.path("inputData").asText(),
                            tcNode.path("expectedOutput").asText(),
                            tcNode.path("isHidden").asBoolean()
                    ));
                }
            }

            return new GeneratedProblem(title, description, hint, testCases);

        } catch (Exception e) {
            log.error("Lỗi khi kết nối đến Gemini API", e);
            throw new RuntimeException("Không thể sinh đề bài bằng AI: " + e.getMessage(), e);
        }
    }

    // Inner classes for structured data mapping
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class GeneratedProblem {
        private String title;
        private String description;
        private String hint;
        private List<GeneratedTestCase> testCases;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class GeneratedTestCase {
        private String inputData;
        private String expectedOutput;
        private boolean isHidden;
    }
}
