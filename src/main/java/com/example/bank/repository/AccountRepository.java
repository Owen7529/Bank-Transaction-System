package com.example.bank.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bank.entity.Account;

/**
 * 銀行帳戶資料存取介面。
 *
 * 第 6～8 課會增加交易專用的
 * 資料列鎖定查詢方法。
 */
public interface AccountRepository
        extends JpaRepository<Account, Long> {

    // 依銀行帳號查詢
    Optional<Account> findByAccountNumber(
            String accountNumber
    );

    // 檢查銀行帳號是否已存在
    boolean existsByAccountNumber(
            String accountNumber
    );

    // 查詢指定客戶的所有帳戶
    List<Account> findByCustomer_Id(Long customerId);
}
