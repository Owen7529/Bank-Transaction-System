package com.example.bank.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bank.entity.BankTransaction;

/**
 * 銀行交易主紀錄資料存取介面。
 */
public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    // 依交易編號查詢
    Optional<BankTransaction> findByTransactionNumber(
            String transactionNumber
    );

    // 依交易請求 ID 查詢
    Optional<BankTransaction> findByRequest_Id(
            Long requestId
    );
}
