package com.example.bank.dto;

import java.time.LocalDateTime;

import com.example.bank.entity.Customer;
import com.example.bank.enums.CustomerStatus;

/**
 * 客戶資料的 API 回應格式。
 */
public record CustomerResponse(
        Long id,
        String customerCode,
        String name,
        String email,
        CustomerStatus status,
        LocalDateTime createdAt
) {

    /**
     * 將 Customer Entity 轉換為 DTO。
     */
    public static CustomerResponse from(Customer customer) {

        return new CustomerResponse(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getName(),
                customer.getEmail(),
                customer.getStatus(),
                customer.getCreatedAt()
        );
    }
}