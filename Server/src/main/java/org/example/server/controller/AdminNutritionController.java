package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/nutrition")
@RequiredArgsConstructor
public class AdminNutritionController {

    private final GeminiService geminiService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body("Vui lòng nhập mô tả món ăn");
        }
        return ResponseEntity.ok(geminiService.analyzeNutrition(text));
    }
}