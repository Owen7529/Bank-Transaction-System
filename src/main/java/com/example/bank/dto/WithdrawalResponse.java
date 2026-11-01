/**
 * WithdrawalResponse
 * 提款結果 DTO，包含交易結果及重送判定等回應資訊。
 */

package com.example.bank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 功能說明：提款交易回應DTO
 */
public record WithdrawalResponse(

        Long transactionId,

        String transactionNumber,

        Long accountId,

        String accountNumber,

        BigDecimal amount,

        BigDecimal balanceAfter,

        String status,

        LocalDateTime createdAt,

        boolean replayed

) {
}
