/**
 * AccountEntryRepository
 * 以 Spring Data JPA 查詢帳戶異動明細的資料存取介面。
 */

package com.example.bank.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
    
	// 根據帳戶 ID 查詢所有異動明細
	// Pageable 用來指定頁數、每頁筆數與排序
	@EntityGraph(attributePaths = {"transaction"})
	Page<AccountEntry> findByAccount_Id(
	        Long accountId,
	        Pageable pageable
		);
}
