package com.example.bank.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bank.entity.AccountEntry;

/**
 * 帳戶異動明細資料存取介面。
 */
public interface AccountEntryRepository
        extends JpaRepository<AccountEntry, Long> {

    // 查詢指定帳戶的異動紀錄
    List<AccountEntry>
        findByAccount_IdOrderByCreatedAtDescIdDesc(
                Long accountId
        );

    // 查詢指定交易產生的所有異動
    List<AccountEntry> findByTransaction_Id(
            Long transactionId
    );
}
