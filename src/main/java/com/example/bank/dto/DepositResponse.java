/**
 * DepositResponse
 * 存款結果 DTO，包含交易結果及重送判定等回應資訊。
 */

package com.example.bank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 存款完成後，回傳給前端的資料
public record DepositResponse(

    Long transactionId,

    String transactionNumber,

    Long accountId,

    String accountNumber,

    BigDecimal amount,

    BigDecimal balanceAfter,

    String status,

    LocalDateTime createdAt,

    // true 表示這是已完成請求的重送結果
    boolean replayed

) {}
