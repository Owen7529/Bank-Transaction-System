/**
 * CreateAccountRequest
 * 建立帳戶時的請求 DTO，定義欄位與 Bean Validation 規則。
 */

package com.example.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 開立銀行帳戶的請求資料。
 *
 * 不接受初始餘額，開戶一律從0元開始。
 */
public record CreateAccountRequest(

        @NotNull(message = "客戶ID不可為空")
        @Positive(message = "客戶ID必須為正整數")
        Long customerId,

        @NotBlank(message = "銀行帳號不可空白")
        @Size(max = 20, message = "銀行帳號最多20個字元")
        String accountNumber
) {
}