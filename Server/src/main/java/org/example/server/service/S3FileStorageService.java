package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3FileStorageService {
    private final S3Client s3;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.region}")
    private String region;

    @Value("${app.s3.prefix}")
    private String prefix;

    public String uploadPublic(MultipartFile file) {
        try {
            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            LocalDate d = LocalDate.now();
            String key = String.format("%s/%d/%02d/%02d/%s_%s",
                    prefix, d.getYear(), d.getMonthValue(), d.getDayOfMonth(),
                    UUID.randomUUID(), safeName);

            String contentType = file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3.putObject(put, RequestBody.fromBytes(file.getBytes()));


            String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20");
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, encodedKey);
        } catch (Exception e) {
            throw new RuntimeException("Upload S3 failed: " + e.getMessage(), e);
        }
    }
}
