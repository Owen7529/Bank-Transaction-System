package com.example.bank.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全域 API 例外處理器。
 *
 * 將常見錯誤轉換為統一的 JSON 格式。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 建立統一錯誤回應。
     */
    private ResponseEntity<ApiErrorResponse> buildError(
            HttpStatus status,
            String message,
            Map<String, String> fieldErrors) {

        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                fieldErrors
        );

        return ResponseEntity.status(status).body(body);
    }

    /**
     * 查無資源：404。
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex) {

        return buildError(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                Map.of()
        );
    }

    /**
     * 業務規則衝突：409。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(
            BusinessException ex) {

        return buildError(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                Map.of()
        );
    }

    /**
     * DTO 欄位驗證失敗：400。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.putIfAbsent(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return buildError(
                HttpStatus.BAD_REQUEST,
                "請求資料驗證失敗",
                errors
        );
    }

    /**
     * JSON 格式錯誤、Enum 值無效：400。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleJsonError(
            HttpMessageNotReadableException ex) {

        return buildError(
                HttpStatus.BAD_REQUEST,
                "JSON 格式錯誤或欄位值不正確",
                Map.of()
        );
    }

    /**
     * 資料庫唯一鍵或其他完整性約束衝突：409。
     *
     * 這是最後一道防線。
     * 不將資料庫內部錯誤直接回傳給使用者。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDatabaseConflict(
            DataIntegrityViolationException ex) {

        return buildError(
                HttpStatus.CONFLICT,
                "資料違反唯一性或完整性約束",
                Map.of()
        );
    }
}