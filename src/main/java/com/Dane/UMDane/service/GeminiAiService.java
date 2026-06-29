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
        return apiKey != null && !cleanApiKey().isEmpty();
    }

    private String cleanApiKey() {
        if (apiKey == null) {
            return "";
        }
        String clean = apiKey.trim();
        // Defensive check: strip leading/trailing double quotes
        if (clean.startsWith("\"") && clean.endsWith("\"")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        // Defensive check: strip leading/trailing single quotes
        if (clean.startsWith("'") && clean.endsWith("'")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        return clean.trim();
    }

    public GeneratedProblem generateProblemFromAi(String topic, String keyword, String difficulty) {
        return generateProblemFromAi(topic, keyword, difficulty, List.of());
    }

    public GeneratedProblem generateProblemFromAi(String topic, String keyword, String difficulty, List<String> existingTitles) {
        if (!isApiKeyConfigured()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        String cleanedKey = cleanApiKey();
        // Switch back to gemini-2.5-flash due to 503 high demand on 3.5-flash
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + cleanedKey;

        StringBuilder promptBuilder = new StringBuilder(String.format(
                "Hãy biên soạn một bài tập lập trình competitive programming với độ khó '%s' bằng tiếng Việt cho chủ đề: '%s' và bối cảnh/từ khóa: '%s'.\n" +
                "Yêu cầu đề bài phải có tiêu đề, mô tả chi tiết, hướng dẫn đọc dữ liệu đầu vào (Standard Input) và in kết quả ra màn hình (Standard Output).\n" +
                "Độ khó '%s' yêu cầu:\n" +
                "- EASY: Thuật toán rất cơ bản, các bài tính toán đơn giản, nhập xuất đơn giản.\n" +
                "- MEDIUM: Thuật toán trung bình (Greedy, Quy hoạch động cơ bản, Sắp xếp, Tìm kiếm nhị phân, Cấu trúc dữ liệu mảng/list).\n" +
                "- HARD: Thuật toán nâng cao (Đồ thị, Quy hoạch động phức tạp, Cấu trúc dữ liệu nâng cao như Tree/Segment Tree, thời gian chạy tối ưu).\n" +
                "Yêu cầu bổ sung:\n" +
                "- Xác định rõ ràng các ràng buộc dữ liệu đầu vào (constraints) bằng tiếng Việt (ví dụ: '1 <= N <= 10^5', '1 <= A_i <= 10^9').\n" +
                "- Đặt giới hạn thời gian (timeLimit tính bằng mili giây, thường là 2000) và giới hạn bộ nhớ (memoryLimit tính bằng MB, thường là 128 hoặc 256) phù hợp cho bài toán.\n" +
                "- Thiết kế bài tập theo dạng hàm (LeetCode-style) để người dùng chỉ cần hoàn thành một hàm/class Solution mà không cần viết hàm main hay tự đọc xuất dữ liệu. Cụ thể:\n" +
                "  1. Sinh ra 'userTemplate': Chỉ chứa khai báo class Solution, tên phương thức mẫu, các tham số đầu vào, một giá trị trả về mặc định tượng trưng (ví dụ: return 0; hoặc return null; hoặc return new int[0];) và ghi chú // Code của bạn tại đây. TUYỆT ĐỐI KHÔNG được viết sẵn bất kỳ logic giải thuật hay mã nguồn giải bài tập nào vào trong 'userTemplate'. Hãy đảm bảo mã nguồn trong 'userTemplate' và 'driverCode' phải được định dạng đẹp mắt, thụt lề chuẩn và bắt buộc xuống dòng (sử dụng \\n) đầy đủ cho từng phần khai báo, dấu ngoặc nhọn, và chú thích; tuyệt đối không viết dồn trên một dòng hoặc dùng khoảng trắng thay cho dấu xuống dòng. Bắt buộc khai báo thư viện chuẩn (sử dụng import java.util.*;) ở đầu cả 'userTemplate' và 'driverCode' để người dùng sử dụng được các cấu trúc dữ liệu như HashSet, HashMap, Queue, Stack, v.v. \n" +
                "  2. Sinh ra 'driverCode': Là một chương trình Java hoàn chỉnh (public class Main) chứa hàm main. Hàm main này sẽ nhận dữ liệu từ Standard Input thông qua Scanner, phân tách/chuyển đổi dữ liệu (ví dụ đọc mảng, đọc số), khởi tạo đối tượng Solution và gọi phương thức của Solution, sau đó in kết quả ra Standard Output. Hãy đảm bảo driverCode có thể tự động chạy khớp hoàn toàn với định dạng testCases đầu vào.\n" +
                "Sinh ra từ 3 đến 5 test cases hợp lệ phục vụ cho việc chấm bài. Trong đó, hãy thiết kế ít nhất 1-2 test cases đặc biệt đại diện cho dữ liệu biên hoặc trường hợp góc (Edge Cases như đầu vào bằng rỗng, giá trị biên cực đại/cực tiểu, hoặc trùng lặp...) và đặt 'isHidden': true để ẩn chúng đi, còn các test cases bình thường khác thì để 'isHidden': false.",
                difficulty, topic, keyword, difficulty
        ));

        if (existingTitles != null && !existingTitles.isEmpty()) {
            promptBuilder.append("\n\nLƯU Ý QUAN TRỌNG: Để tránh trùng lặp nội dung, TUYỆT ĐỐI KHÔNG sinh ra bài tập có tiêu đề hoặc nội dung trùng lặp/tương tự với danh sách các bài tập đã tồn tại sau đây:\n");
            for (String title : existingTitles) {
                promptBuilder.append("- ").append(title).append("\n");
            }
            promptBuilder.append("Hãy tạo ra một bài tập có bài toán và câu chuyện hoàn toàn mới lạ khác biệt.");
        }

        String prompt = promptBuilder.toString();

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
                            "constraints", Map.of("type", "STRING"),
                            "timeLimit", Map.of("type", "INTEGER"),
                            "memoryLimit", Map.of("type", "INTEGER"),
                            "userTemplate", Map.of("type", "STRING"),
                            "driverCode", Map.of("type", "STRING"),
                            "testCases", Map.of(
                                    "type", "ARRAY",
                                    "items", testCaseItemSchema
                                )
                    ),
                    "required", List.of("title", "description", "hint", "constraints", "timeLimit", "memoryLimit", "userTemplate", "driverCode", "testCases")
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
            String constraints = aiData.path("constraints").asText();
            Integer timeLimit = aiData.path("timeLimit").asInt(2000);
            Integer memoryLimit = aiData.path("memoryLimit").asInt(128);
            String userTemplate = aiData.path("userTemplate").asText();
            String driverCode = aiData.path("driverCode").asText();

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

            return new GeneratedProblem(title, description, hint, constraints, timeLimit, memoryLimit, userTemplate, driverCode, testCases);

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
        private String constraints;
        private Integer timeLimit;
        private Integer memoryLimit;
        private String userTemplate;
        private String driverCode;
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
