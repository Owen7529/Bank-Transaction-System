/**
 * AccountRepository
 * 以 Spring Data JPA 查詢、鎖定及儲存銀行帳戶的資料存取介面。
 */

package com.example.bank.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bank.entity.Account;

/**
 * 銀行帳戶資料存取介面。
 *
 * 包含帳戶列表查詢，以及以悲觀寫入鎖鎖定帳戶的查詢方法。
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
    
    /**
     * 依照帳戶 ID 查詢，並加上悲觀寫入鎖。
     *
     * 必須在有效的資料庫交易內呼叫，
     * 鎖定才有機會持續到交易結束。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(
        @Param("id") Long id
    );
}
