package com.example.bank.dto;

import com.example.bank.enums.AccountStatus;

import jakarta.validation.constraints.NotNull;

/**
 * 修改帳戶狀態的請求。
 */
public record UpdateAccountStatusRequest(

        @NotNull(message = "帳戶狀態不可為空")
        AccountStatus status
) {
}