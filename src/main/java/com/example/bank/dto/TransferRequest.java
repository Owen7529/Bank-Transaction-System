/**
 * TransferRequest
 * 轉帳請求 DTO，要求來源帳戶、目標帳戶、金額與請求識別碼。
 */

package com.example.bank.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * 功能說明：轉帳請求 DTO
 */
public record TransferRequest(

    // 同一筆轉帳重送時，必須使用相同識別碼
    @NotBlank(message = "請求識別碼不可為空")
    @Pattern(
        regexp = "[A-Za-z0-9_-]{1,90}",
        message = "請求識別碼只能包含英數字、底線及連字號，最多90字"
    )
    String requestKey,

    // 轉出帳戶
    @NotNull(message = "來源帳戶不可為空")
    @Positive(message = "來源帳戶ID必須大於0")
    Long fromAccountId,

    // 轉入帳戶
    @NotNull(message = "目標帳戶不可為空")
    @Positive(message = "目標帳戶ID必須大於0")
    Long toAccountId,

    // 轉帳金額
    @NotNull(message = "轉帳金額不可為空")
    @DecimalMin(
        value = "0.01",
        message = "轉帳金額必須至少0.01元"
    )
    @Digits(
        integer = 17,
        fraction = 2,
        message = "金額最多17位整數及2位小數"
    )
    BigDecimal amount

) {}
