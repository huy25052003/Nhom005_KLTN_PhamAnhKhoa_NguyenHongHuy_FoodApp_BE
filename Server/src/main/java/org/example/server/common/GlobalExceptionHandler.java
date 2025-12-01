package org.example.server.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Tạo Logger để ghi lại lỗi thực sự ra màn hình console
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Xử lý Validate (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Dữ liệu không hợp lệ", details));
    }

    // 2. Xử lý Login thất bại (Code bạn đã thêm)
    @ExceptionHandler({
            org.springframework.security.authentication.BadCredentialsException.class,
            org.springframework.security.core.userdetails.UsernameNotFoundException.class,
            org.springframework.security.authentication.InternalAuthenticationServiceException.class
    })
    public ResponseEntity<ApiError> handleAuthError(Exception ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("AUTH_FAILED", "Tài khoản hoặc mật khẩu không chính xác"));
    }

    // 3. Xử lý 404 Not Found (Quan trọng để không bị nhầm thành 500)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("NOT_FOUND", "Đường dẫn không tồn tại: " + ex.getResourcePath()));
    }

    // 4. Xử lý logic nghiệp vụ (Ví dụ: throw new RuntimeException("Hết hàng"))
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegal(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntime(RuntimeException ex) {
        // Với lỗi logic thông thường, trả về 400 và message chi tiết
        return ResponseEntity.badRequest().body(ApiError.of("APP_ERROR", ex.getMessage()));
    }

    // 5. Xử lý lỗi không xác định (Lưới lọc cuối cùng)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        log.error("🚨 Lỗi hệ thống không mong muốn:", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "Lỗi hệ thống, vui lòng liên hệ Admin."));
    }
}