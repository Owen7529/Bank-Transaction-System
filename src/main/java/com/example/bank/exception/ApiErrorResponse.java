/**
 * ApiErrorResponse
 * 統一錯誤回應 DTO，供例外處理器回傳標準化 JSON。
 */

package com.example.bank.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 統一的 API 錯誤回應格式。
 */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
}