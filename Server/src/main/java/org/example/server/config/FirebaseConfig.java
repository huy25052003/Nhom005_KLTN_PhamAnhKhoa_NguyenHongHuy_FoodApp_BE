package org.example.server.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // File này phải nằm trong thư mục src/main/resources/
        ClassPathResource resource = new ClassPathResource("firebase-service-account.json");

        if (!resource.exists()) {
            // Ném lỗi ngay lập tức để bạn biết đường sửa
            throw new RuntimeException("❌ LỖI: Không tìm thấy file 'firebase-service-account.json' trong thư mục resources! Vui lòng tải từ Firebase Console.");
        }

        try (InputStream is = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(is))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            }
            return FirebaseApp.getInstance();
        }
    }
}