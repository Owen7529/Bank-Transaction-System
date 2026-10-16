package com.example.bank.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bank.entity.TransactionRequest;

/**
 * 交易請求資料存取介面。
 */
public interface TransactionRequestRepository
        extends JpaRepository<TransactionRequest, Long> {

    // 依冪等鍵查詢請求
    Optional<TransactionRequest> findByRequestKey(
            String requestKey
    );

    // 檢查冪等鍵是否存在
    boolean existsByRequestKey(
            String requestKey
    );
}
