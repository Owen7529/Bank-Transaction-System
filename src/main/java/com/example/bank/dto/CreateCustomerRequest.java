/**
 * CreateCustomerRequest
 * 建立客戶時的請求 DTO，定義輸入欄位與驗證條件。
 */

package com.example.bank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增客戶的請求資料。
 *
 * 使用 record 定義 API 輸入格式。
 */
public record CreateCustomerRequest(

        @NotBlank(message = "客戶代碼不可空白")
        @Size(max = 30, message = "客戶代碼最多30個字元")
        String customerCode,

        @NotBlank(message = "客戶名稱不可空白")
        @Size(max = 100, message = "客戶名稱最多100個字元")
        String name,

        @Email(message = "電子郵件格式不正確")
        @Size(max = 150, message = "電子郵件最多150個字元")
        String email
) {
}