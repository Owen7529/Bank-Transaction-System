package com.example.bank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.bank.entity.Account;
import com.example.bank.enums.AccountStatus;

/**
 * 銀行帳戶 API 回應格式。
 */
public record AccountResponse(
        Long id,
        Long customerId,
        String accountNumber,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        Long version,
        LocalDateTime createdAt
) {

    /**
     * 將 Account Entity 轉換為 DTO。
     *
     * 必須在有效的交易範圍內呼叫，
     * 以安全取得 customer 關聯。
     */
    public static AccountResponse from(Account account) {

        return new AccountResponse(
                account.getId(),
                account.getCustomer().getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getVersion(),
                account.getCreatedAt()
        );
    }
}