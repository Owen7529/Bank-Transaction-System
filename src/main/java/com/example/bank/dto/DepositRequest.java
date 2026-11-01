/**
 * DepositRequest
 * 存款請求 DTO，包含請求識別碼、帳戶、金額及用途說明。
 */

package com.example.bank.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// 存款 API 的請求資料
public record DepositRequest(

    // 每一次業務請求的唯一識別碼
    @NotBlank(message = "請求識別碼不可空白")
    @Size(max = 100, message = "請求識別碼最多100字")
    String requestKey,

    // 要存款的帳戶 ID
    @NotNull(message = "帳戶ID不可為空")
    @Positive(message = "帳戶ID必須大於0")
    Long accountId,

    // 存款金額必須至少0.01元
    @NotNull(message = "存款金額不可為空")
    @DecimalMin(
        value = "0.01",
        message = "存款金額必須至少0.01元"
    )
    @Digits(
        integer = 17,
        fraction = 2,
        message = "金額最多17位整數、2位小數"
    )
    BigDecimal amount,

    // 存款備註，可不填
    @Size(max = 255, message = "備註最多255字")
    String description

) {}
