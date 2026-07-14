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
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + cleanedKey;

        StringBuilder promptBuilder = new StringBuilder(String.format(
                "Hãy biên soạn một bài tập lập trình competitive programming với độ khó '%s' bằng tiếng Việt cho chủ đề: '%s' và bối cảnh/từ khóa: '%s'.\n" +
                "Yêu cầu đề bài phải có tiêu đề, mô tả chi tiết, hướng dẫn đọc dữ liệu đầu vào (Standard Input) và in kết quả ra màn hình (Standard Output).\n" +
                "Độ khó '%s' yêu cầu:\n" +
                "- EASY: Thuật toán rất cơ bản, các bài tính toán đơn giản, nhập xuất đơn giản, xử lý mảng/chuỗi cơ bản.\n" +
                "- MEDIUM: Cấu trúc dữ liệu trung cấp như Stack (ngăn xếp), Queue (hàng đợi), PriorityQueue (hàng đợi ưu tiên), HashSet, HashMap (tối ưu O(1)), hoặc thuật toán tầm trung như Two Pointers, Sliding Window, Tìm kiếm nhị phân, Quy hoạch động cơ bản. Hạn chế lặp lại thuật toán tham lam (Greedy) trừ khi người dùng yêu cầu trực tiếp chủ đề đó.\n" +
                "- HARD: Thuật toán nâng cao như Đồ thị (Dijkstra, Kruskal, BFS, DFS), các cấu trúc dữ liệu nâng cao (Tree, Segment Tree, Trie, Fenwick Tree), hoặc Quy hoạch động phức tạp tối ưu thời gian chạy.\n" +
                "Yêu cầu bổ sung:\n" +
                "- Đa dạng cấu trúc dữ liệu: Bắt buộc chọn cấu trúc dữ liệu tối ưu nhất cho bài toán và hiện thực nó trong mã giải mẫu (ví dụ: dùng HashSet để tìm phần tử duy nhất, HashMap để đếm tần số, Stack để xử lý ngoặc/biểu thức, Queue để xử lý hàng đợi phần tử). Tránh việc giải mọi bài toán bằng mảng thường (array) hoặc Greedy.\n" +
                "- Xác định rõ ràng các ràng buộc dữ liệu đầu vào (constraints) bằng tiếng Việt (ví dụ: '1 <= N <= 10^5', '1 <= A[i] <= 10^9'). Để hiển thị đẹp mắt trên giao diện web, các công thức toán học và biểu thức chỉ số phải được định dạng bằng ký tự văn bản thường trực quan (ví dụ: dùng H[i], H[i+1], |H[i+1] - H[i]|, abs(x) hoặc x^2), TUYỆT ĐỐI KHÔNG sử dụng định dạng LaTeX phức tạp chứa các ký tự như \\{, \\}, _, ^ (ví dụ như H_{i+1} - H_i) vì giao diện chỉ hiển thị văn bản thường.\n" +
                "- Đặt giới hạn thời gian (timeLimit tính bằng mili giây, thường là 2000) và giới hạn bộ nhớ (memoryLimit tính bằng MB, thường là 128 hoặc 256) phù hợp cho bài toán.\n" +
                "- Thiết kế bài tập theo dạng hàm (LeetCode-style) để người dùng chỉ cần hoàn thành một hàm/class Solution mà không cần viết hàm main hay tự đọc xuất dữ liệu. Cụ thể:\n" +
                "  1. Sinh ra 'userTemplate': Chỉ chứa khai báo class Solution, tên phương thức mẫu, các tham số đầu vào, một giá trị trả về mặc định tượng trưng (ví dụ: return 0; hoặc return null; hoặc return new int[0];) và ghi chú // Code của bạn tại đây. TUYỆT ĐỐI KHÔNG được viết sẵn bất kỳ logic giải thuật hay mã nguồn giải bài tập nào vào trong 'userTemplate'. Hãy đảm bảo mã nguồn trong 'userTemplate' và 'driverCode' phải được định dạng đẹp mắt, thụt lề chuẩn và bắt buộc xuống dòng (sử dụng \\n) đầy đủ cho từng phần khai báo, dấu ngoặc nhọn, và chú thích; tuyệt đối không viết dồn trên một dòng hoặc dùng khoảng trắng thay cho dấu xuống dòng. Bắt buộc khai báo thư viện chuẩn (sử dụng import java.util.*;) ở đầu cả 'userTemplate' và 'driverCode' để người dùng sử dụng được các cấu trúc dữ liệu như HashSet, HashMap, Queue, Stack, v.v. \n" +
                "  2. Sinh ra 'driverCode': Là một chương trình Java hoàn chỉnh (public class Main) chứa hàm main. Hàm main này sẽ nhận dữ liệu từ Standard Input thông qua Scanner, phân tách/chuyển đổi dữ liệu (ví dụ đọc mảng, đọc số), khởi tạo đối tượng Solution và gọi phương thức của Solution, sau đó in kết quả ra Standard Output. Hãy đảm bảo driverCode có thể tự động chạy khớp hoàn toàn với định dạng testCases đầu vào.\n" +
                "  3. Sinh ra 'referenceSolution': Là một đoạn mã nguồn giải bài tập hoàn chỉnh, viết bằng ngôn ngữ Java (chứa class Solution được hiện thực đầy đủ, logic giải chuẩn xác tối ưu, có thể có thêm helper class tĩnh lồng bên trong class Solution nếu cần). Lời giải chuẩn này sẽ được hệ thống biên dịch và chạy thử cùng với 'driverCode' trong sandbox để tự động xác thực và thu thập đáp án chuẩn cho các test cases.\n" +
                "Sinh ra từ 3 đến 5 test cases hợp lệ phục vụ cho việc chấm bài. Trong đó, hãy thiết kế ít nhất 1-2 test cases đặc biệt đại diện cho dữ liệu biên hoặc trường hợp góc (Edge Cases như đầu vào bằng rỗng, giá trị biên cực đại/cực tiểu, hoặc trùng lặp...) và đặt 'isHidden': true để ẩn chúng đi, còn các test cases bình thường khác thì để 'isHidden': false. Lưu ý: Chỉ cần sinh ra 'inputData' cho test cases, không cần sinh ra expectedOutput.",
                difficulty, topic, keyword, difficulty
        ));

        if (existingTitles != null && !existingTitles.isEmpty()) {
            promptBuilder.append("\n\nLƯU Ý QUAN TRỌNG: Để tránh trùng lặp nội dung, TUYỆT ĐỐI KHÔNG sinh ra bài tập có tiêu đề hoặc nội dung trùng lặp/tương tự với danh sách các bài tập đã tồn tại sau đây:\n");
            for (String title : existingTitles) {
                promptBuilder.append("- ").append(title).append("\n");
            }
            promptBuilder.append("Hãy tạo ra một bài tập có bài toán và câu chuyện hoàn toàn mới lạ khác biệt.");
        }

        return executeGeminiCall(url, promptBuilder.toString());
    }

    public GeneratedProblem generateRoadmapProblem(
            String topic, 
            String keyword, 
            String difficulty, 
            String prevDescription, 
            String prevSolution, 
            List<String> existingTitles) {
        if (!isApiKeyConfigured()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        String cleanedKey = cleanApiKey();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + cleanedKey;

        StringBuilder promptBuilder = new StringBuilder(String.format(
                "Hãy biên soạn một bài tập lập trình competitive programming với độ khó '%s' bằng tiếng Việt cho chủ đề: '%s' và bối cảnh/từ khóa: '%s'.\n" +
                "Yêu cầu đề bài phải có tiêu đề, mô tả chi tiết, hướng dẫn đọc dữ liệu đầu vào (Standard Input) và in kết quả ra màn hình (Standard Output).\n" +
                "Độ khó '%s' yêu cầu:\n" +
                "- EASY: Thuật toán rất cơ bản, các bài tính toán đơn giản, nhập xuất đơn giản, xử lý mảng/chuỗi cơ bản.\n" +
                "- MEDIUM: Cấu trúc dữ liệu trung cấp như Stack (ngăn xếp), Queue (hàng đợi), PriorityQueue (hàng đợi ưu tiên), HashSet, HashMap (tối ưu O(1)), hoặc thuật toán tầm trung như Two Pointers, Sliding Window, Tìm kiếm nhị phân, Quy hoạch động cơ bản. Hạn chế lặp lại thuật toán tham lam (Greedy) trừ khi người dùng yêu cầu trực tiếp chủ đề đó.\n" +
                "- HARD: Thuật toán nâng cao như Đồ thị (Dijkstra, Kruskal, BFS, DFS), các cấu trúc dữ liệu nâng cao (Tree, Segment Tree, Trie, Fenwick Tree), hoặc Quy hoạch động phức tạp tối ưu thời gian chạy.\n" +
                "Yêu cầu bổ sung:\n" +
                "- Đa dạng cấu trúc dữ liệu: Bắt buộc chọn cấu trúc dữ liệu tối ưu nhất cho bài toán và hiện thực nó trong mã giải mẫu (ví dụ: dùng HashSet để tìm phần tử duy nhất, HashMap để đếm tần số, Stack để xử lý ngoặc/biểu thức, Queue để xử lý hàng đợi phần tử). Tránh việc giải mọi bài toán bằng mảng thường (array) hoặc Greedy.\n" +
                "- Xác định rõ ràng các ràng buộc dữ liệu đầu vào (constraints) bằng tiếng Việt (ví dụ: '1 <= N <= 10^5', '1 <= A[i] <= 10^9'). Để hiển thị đẹp mắt trên giao diện web, các công thức toán học và biểu thức chỉ số phải được định dạng bằng ký tự văn bản thường trực quan (ví dụ: dùng H[i], H[i+1], |H[i+1] - H[i]|, abs(x) hoặc x^2), TUYỆT ĐỐI KHÔNG sử dụng định dạng LaTeX phức tạp chứa các ký tự như \\{, \\}, _, ^ (ví dụ như H_{i+1} - H_i) vì giao diện chỉ hiển thị văn bản thường.\n" +
                "- Đặt giới hạn thời gian (timeLimit tính bằng mili giây, thường là 2000) và giới hạn bộ nhớ (memoryLimit tính bằng MB, thường là 128 hoặc 256) phù hợp cho bài toán.\n" +
                "- Thiết kế bài tập theo dạng hàm (LeetCode-style) để người dùng chỉ cần hoàn thành một hàm/class Solution mà không cần viết hàm main hay tự đọc xuất dữ liệu. Cụ thể:\n" +
                "  1. Sinh ra 'userTemplate': Chỉ chứa khai báo class Solution, tên phương thức mẫu, các tham số đầu vào, một giá trị trả về mặc định tượng trưng (ví dụ: return 0; hoặc return null; hoặc return new int[0];) và ghi chú // Code của bạn tại đây. TUYỆT ĐỐI KHÔNG được viết sẵn bất kỳ logic giải thuật hay mã nguồn giải bài tập nào vào trong 'userTemplate'. Hãy đảm bảo mã nguồn trong 'userTemplate' và 'driverCode' phải được định dạng đẹp mắt, thụt lề chuẩn và bắt buộc xuống dòng (sử dụng \\n) đầy đủ cho từng phần khai báo, dấu ngoặc nhọn, và chú thích; tuyệt đối không viết dồn trên một dòng hoặc dùng khoảng trắng thay cho dấu xuống dòng. Bắt buộc khai báo thư viện chuẩn (sử dụng import java.util.*;) ở đầu cả 'userTemplate' và 'driverCode' để người dùng sử dụng được các cấu trúc dữ liệu như HashSet, HashMap, Queue, Stack, v.v. \n" +
                "  2. Sinh ra 'driverCode': Là một chương trình Java hoàn chỉnh (public class Main) chứa hàm main. Hàm main này sẽ nhận dữ liệu từ Standard Input thông qua Scanner, phân tách/chuyển đổi dữ liệu (ví dụ đọc mảng, đọc số), khởi tạo đối tượng Solution và gọi phương thức của Solution, sau đó in kết quả ra Standard Output. Hãy đảm bảo driverCode có thể tự động chạy khớp hoàn toàn với định dạng testCases đầu vào.\n" +
                "  3. Sinh ra 'referenceSolution': Là một đoạn mã nguồn giải bài tập hoàn chỉnh, viết bằng ngôn ngữ Java (chứa class Solution được hiện thực đầy đủ, logic giải chuẩn xác tối ưu, có thể có thêm helper class tĩnh lồng bên trong class Solution nếu cần). Lời giải chuẩn này sẽ được hệ thống biên dịch và chạy thử cùng với 'driverCode' trong sandbox để tự động xác thực và thu thập đáp án chuẩn cho các test cases.\n" +
                "Sinh ra từ 3 đến 5 test cases hợp lệ phục vụ cho việc chấm bài. Trong đó, hãy thiết kế ít nhất 1-2 test cases đặc biệt đại diện cho dữ liệu biên hoặc trường hợp góc (Edge Cases như đầu vào bằng rỗng, giá trị biên cực đại/cực tiểu, hoặc trùng lặp...) và đặt 'isHidden': true để ẩn chúng đi, còn các test cases bình thường khác thì để 'isHidden': false. Lưu ý: Chỉ cần sinh ra 'inputData' cho test cases, không cần sinh ra expectedOutput.",
                difficulty, topic, keyword, difficulty
        ));

        if (prevDescription != null && !prevDescription.trim().isEmpty()) {
            promptBuilder.append("\n\nYÊU CẦU ĐẶC BIỆT (GỐI ĐẦU NỘI DUNG / SCAFFOLDING):\n")
                    .append("Bài tập này là một phần trong lộ trình học tập tuần tự. Bài tập này BẮT BUỘC phải kế thừa, phát triển hoặc có liên quan mật thiết về giải thuật/nội dung từ bài tập của Node phía trước để người học có thể vận dụng tư duy và nền tảng của bài trước để làm bài này.\n")
                    .append("- Đề bài của Node phía trước:\n\"\"\"\n").append(prevDescription).append("\n\"\"\"\n")
                    .append("- Mã giải mẫu (Reference Solution) của Node phía trước:\n\"\"\"\n").append(prevSolution).append("\n\"\"\"\n")
                    .append("Hãy thiết kế bài này như một sự phát triển/nâng cấp tự nhiên từ bài trước đó (ví dụ: mở rộng số lượng chiều, thay đổi điều kiện tối ưu, thêm ràng buộc thời gian/không gian hoặc áp dụng cấu trúc dữ liệu tương ứng).");
        }

        if (existingTitles != null && !existingTitles.isEmpty()) {
            promptBuilder.append("\n\nLƯU Ý QUAN TRỌNG: Để tránh trùng lặp tiêu đề, TUYỆT ĐỐI KHÔNG đặt tiêu đề trùng với các bài sau đây:\n");
            for (String title : existingTitles) {
                promptBuilder.append("- ").append(title).append("\n");
            }
        }

        return executeGeminiCall(url, promptBuilder.toString());
    }

    private GeneratedProblem executeGeminiCall(String url, String prompt) {
        try {
            // Build Gemini payload with structured JSON output schema
            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> parts = Map.of("parts", List.of(textPart));

            // Define schema for structured output (omit expectedOutput from Gemini generation)
            Map<String, Object> testCaseItemSchema = Map.of(
                    "type", "OBJECT",
                    "properties", Map.of(
                            "inputData", Map.of("type", "STRING"),
                            "isHidden", Map.of("type", "BOOLEAN")
                    ),
                    "required", List.of("inputData", "isHidden")
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
                            "referenceSolution", Map.of("type", "STRING"),
                            "driverCode", Map.of("type", "STRING"),
                            "testCases", Map.of(
                                    "type", "ARRAY",
                                    "items", testCaseItemSchema
                                )
                    ),
                    "required", List.of("title", "description", "hint", "constraints", "timeLimit", "memoryLimit", "userTemplate", "referenceSolution", "driverCode", "testCases")
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
            String referenceSolution = aiData.path("referenceSolution").asText();
            String driverCode = aiData.path("driverCode").asText();

            List<GeneratedTestCase> testCases = new ArrayList<>();
            JsonNode testCasesNode = aiData.path("testCases");
            if (testCasesNode.isArray()) {
                for (JsonNode tcNode : testCasesNode) {
                    testCases.add(new GeneratedTestCase(
                            tcNode.path("inputData").asText(),
                            tcNode.path("isHidden").asBoolean()
                    ));
                }
            }

            return new GeneratedProblem(title, description, hint, constraints, timeLimit, memoryLimit, userTemplate, referenceSolution, driverCode, testCases);

        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("Lỗi HTTP phản hồi từ Gemini API (Status: {})", e.getStatusCode(), e);
            if (e.getStatusCode().value() == 429) {
                throw new RuntimeException("Hệ thống AI đang quá tải hoặc bạn đã vượt quá giới hạn yêu cầu tạo đề (Rate Limit: tối đa 15 lần/phút). Vui lòng đợi vài giây rồi thử lại!");
            }
            throw new RuntimeException("Lỗi từ hệ thống AI (HTTP " + e.getStatusCode().value() + "). Vui lòng thử lại!");
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
        private String referenceSolution;
        private String driverCode;
        private List<GeneratedTestCase> testCases;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class GeneratedTestCase {
        private String inputData;
        private boolean isHidden;
    }

    public String generateReviewDigestForProblem(String title, String description, String referenceSolution) {
        if (!isApiKeyConfigured()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        String cleanedKey = cleanApiKey();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + cleanedKey;

        String prompt = String.format(
                "Hãy phân tích bài tập lập trình '%s' với mô tả:\n" +
                "\"\"\"\n%s\n\"\"\"\n" +
                "Và mã nguồn giải mẫu hoàn chỉnh viết bằng Java:\n" +
                "\"\"\"\n%s\n\"\"\"\n\n" +
                "Nhiệm vụ của bạn là biên soạn một thử thách ôn tập điền khuyết code (Code Completion Challenge) theo các yêu cầu sau:\n" +
                "1. Trích xuất một ý tưởng mấu chốt ngắn gọn để giải bài toán tối ưu nhất (keyInsight).\n" +
                "2. Cắt bỏ 1 đến 3 dòng code logic mấu chốt nhất trong mã nguồn giải mẫu (ví dụ: điều kiện so sánh quyết định, công thức toán học/DP quy đổi, cập nhật trạng thái Map/Set, hoán vị phần tử...) và thay thế chính xác dòng code đó bằng chuỗi: `// TODO: Điền code còn thiếu tại đây`.\n" +
                "   Mã nguồn sau khi được thay thế này sẽ gọi là 'maskedCode'.\n" +
                "   LƯU Ý CỰC KỲ QUAN TRỌNG: Mã nguồn trong 'maskedCode' bắt buộc phải giữ nguyên toàn bộ các ký tự xuống dòng '\\n' và khoảng trắng thụt lề đầu dòng y hệt như code Java chuẩn gốc. Tuyệt đối không được nén code thành một hàng ngang duy nhất.\n" +
                "3. Trích xuất đoạn code chính xác bị cắt ra làm 'correctSnippet'.\n" +
                "4. Tạo thêm 2 phương án code gây nhiễu ('wrongSnippet1' và 'wrongSnippet2') có cấu trúc tương tự phương án đúng nhưng chứa lỗi logic nhỏ (ví dụ: sai toán tử so sánh, sai chỉ số, nhầm lẫn biến hoặc điều kiện biên lệch).\n" +
                "5. Viết lời giải thích chi tiết ngắn gọn (explanation) tại sao đoạn code đúng mới giúp thuật toán hoạt động chính xác.\n" +
                "Bắt buộc trả về kết quả bằng tiếng Việt theo định dạng JSON khớp hoàn toàn với schema quy định.",
                title, description, referenceSolution
        );

        try {
            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> parts = Map.of("parts", List.of(textPart));

            Map<String, Object> responseSchema = Map.of(
                    "type", "OBJECT",
                    "properties", Map.of(
                            "keyInsight", Map.of("type", "STRING"),
                            "maskedCode", Map.of("type", "STRING"),
                            "correctSnippet", Map.of("type", "STRING"),
                            "wrongSnippet1", Map.of("type", "STRING"),
                            "wrongSnippet2", Map.of("type", "STRING"),
                            "explanation", Map.of("type", "STRING")
                    ),
                    "required", List.of("keyInsight", "maskedCode", "correctSnippet", "wrongSnippet1", "wrongSnippet2", "explanation")
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

            JsonNode rootNode = objectMapper.readTree(responseStr);
            String aiJsonText = rootNode.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
            
            return aiJsonText;

        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("Lỗi HTTP từ Gemini API trong generateReviewDigest (Status: {})", e.getStatusCode(), e);
            if (e.getStatusCode().value() == 429) {
                throw new RuntimeException("Hệ thống AI đang quá tải. Vui lòng thử lại sau!");
            }
            throw new RuntimeException("Lỗi từ hệ thống AI (HTTP " + e.getStatusCode().value() + ").");
        } catch (Exception e) {
            log.error("Lỗi khi kết nối Gemini API để sinh review", e);
            throw new RuntimeException("Không thể sinh ôn tập tư duy bằng AI: " + e.getMessage(), e);
        }
    }
}
