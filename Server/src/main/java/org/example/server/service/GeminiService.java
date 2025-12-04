package org.example.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> analyzeNutrition(String text) {
        // Dùng model 2.0 Flash (ổn định nhất với key của bạn)
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

        String prompt = "Hãy đóng vai chuyên gia dinh dưỡng. Phân tích thành phần dinh dưỡng cho món: \"" + text + "\". " +
                "Hãy đưa ra con số trung bình, chính xác và phổ biến nhất. " +
                "Trả về kết quả CHỈ LÀ MỘT JSON duy nhất (không markdown) với định dạng: " +
                "{ \"calories\": int, \"protein\": float, \"carbs\": float, \"fat\": float }. " +
                "Ví dụ: { \"calories\": 500, \"protein\": 30.5, \"carbs\": 40.0, \"fat\": 10.0 }";

        // --- CẤU HÌNH THAM SỐ (FIX LỖI NHẢY SỐ) ---
        var requestBody = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[] {
                                Map.of("text", prompt)
                        })
                },
                // Thêm cấu hình này để AI trả lời ổn định (nhiệt độ = 0)
                "generationConfig", Map.of(
                        "temperature", 0.0,
                        "topP", 1.0,
                        "topK", 1
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            var response = restTemplate.postForEntity(url, new HttpEntity<>(requestBody, headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String resultText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            resultText = resultText.replaceAll("```json", "").replaceAll("```", "").trim();

            return objectMapper.readValue(resultText, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("calories", 0, "protein", 0, "carbs", 0, "fat", 0);
        }
    }
}