package com.example.bank.dto;

import com.example.bank.enums.CustomerStatus;

import jakarta.validation.constraints.NotNull;

/**
 * 修改客戶狀態的請求。
 */
public record UpdateCustomerStatusRequest(

        @NotNull(message = "客戶狀態不可為空")
        CustomerStatus status
) {
}