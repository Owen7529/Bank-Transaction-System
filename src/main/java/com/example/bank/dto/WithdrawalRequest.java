/**
 * WithdrawalRequest
 * 提款請求 DTO，包含請求識別碼、帳戶、金額及用途說明。
 */

package com.example.bank.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 功能說明：提款請求 DTO
 * 用來接收前端傳入的提款資訊。
 */
public record WithdrawalRequest(

        // 每次提款的唯一請求識別碼
        @NotBlank(message = "請求識別碼不可為空")
        @Size(max = 100, message = "請求識別碼不可超過100字")
        String requestKey,

        // 要提款的帳戶ID
        @NotNull(message = "帳戶ID不可為空")
        @Positive(message = "帳戶ID必須大於0")
        Long accountId,

        // 提款金額
        @NotNull(message = "提款金額不可為空")
        @DecimalMin(value = "0.01",
                    message = "提款金額必須大於0")
        @Digits(integer = 17, fraction = 2,
                message = "金額最多17位整數及2位小數")
        BigDecimal amount,

        // 提款備註，可以不填
        @Size(max = 255,
              message = "備註不可超過255字")
        String description

) {
}
