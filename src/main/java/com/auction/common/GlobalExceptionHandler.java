package com.auction.common;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.auction.auth.exceptions.JwtExpiredException;

import jakarta.persistence.EntityNotFoundException;

/**
 * Bộ xử lý ngoại lệ toàn cục (Global Exception Handler).
 * Tự động bắt các loại ngoại lệ xảy ra trong Controller và định dạng lại dữ liệu phản hồi trả về cho Client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý lỗi xác thực dữ liệu đầu vào (Validation Errors) từ @Valid.
     * Trả về mã lỗi HTTP 400 (Bad Request) cùng với danh sách các trường bị lỗi và thông điệp tương ứng.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Xử lý ngoại lệ nghiệp vụ tự định nghĩa BaseException.
     * Trả về mã lỗi HTTP 400 (Bad Request) kèm theo đối tượng BaseResponse chứa thông điệp lỗi.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse> handleBaseException(BaseException exception) {
        return ResponseEntity.badRequest().body(exception.getResponse());
    }

    /**
     * Xử lý lỗi khi không tìm thấy thực thể trong cơ sở dữ liệu (EntityNotFoundException).
     * Trả về mã lỗi HTTP 404 (Not Found).
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<BaseResponse> handleEntitiyNotFound(EntityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new BaseResponse(false, "What you find does not exist"));
    }

    /**
     * Xử lý lỗi khi mã token JWT hết hạn (JwtExpiredException).
     * Trả về mã HTTP 498 (Token expired/invalid).
     */
    @ExceptionHandler(JwtExpiredException.class)
    public ResponseEntity<BaseResponse> handleJwtExpiredException() {
        return ResponseEntity.status(498).body(new BaseResponse(false, "Your jwt token expired"));
    }
}
