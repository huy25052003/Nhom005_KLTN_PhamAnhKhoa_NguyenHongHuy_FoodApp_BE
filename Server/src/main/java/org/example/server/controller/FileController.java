package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.service.S3FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/uploads")
@RequiredArgsConstructor
public class FileController {

    private final S3FileStorageService storage;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        String url = storage.uploadPublic(file);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
