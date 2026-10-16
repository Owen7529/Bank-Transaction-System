package com.example.bank.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bank.entity.Customer;

/**
 * 客戶資料存取介面。
 */
public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    // 依客戶代碼查詢
    Optional<Customer> findByCustomerCode(
            String customerCode
    );

    // 檢查客戶代碼是否已存在
    boolean existsByCustomerCode(
            String customerCode
    );
}
