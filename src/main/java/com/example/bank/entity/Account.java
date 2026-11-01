/**
 * Account
 * 對應 accounts 資料表的 JPA Entity，記錄客戶關聯、帳號、餘額及版本欄位。
 */

package com.example.bank.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.example.bank.enums.AccountStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * 銀行帳戶 Entity。
 *
 * 對應資料表：accounts
 *
 * 餘額更新由 DepositService 與 WithdrawalService 執行業務驗證，
 * TransferService 負責協調跨帳戶轉帳；Controller 不直接修改餘額。
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 一位客戶可以擁有多個帳戶
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "customer_id",
        nullable = false
    )
    private Customer customer;

    // 唯一銀行帳號
    @Column(
        name = "account_number",
        nullable = false,
        unique = true,
        length = 20
    )
    private String accountNumber;

    // 帳戶餘額，固定使用 BigDecimal
    @Column(
        name = "balance",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal balance = new BigDecimal("0.00");

    // 固定幣別 TWD，對應 MySQL CHAR(3)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "currency",
        nullable = false,
        length = 3,
        columnDefinition = "char(3)"
    )
    private String currency = "TWD";

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 20
    )
    private AccountStatus status = AccountStatus.ACTIVE;

    // JPA 樂觀鎖版本欄位
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @CreationTimestamp
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    /** 提供 JPA 建立實體所需的無參數建構子。 */
    protected Account() {
    }

    /**
     * 建立零餘額的新帳戶。
     */
    public Account(
            Customer customer,
            String accountNumber) {

        this.customer = customer;
        this.accountNumber = accountNumber;
    }

    /** 取得資料表主鍵，供其他程式識別實體。 */
    public Long getId() {
        return id;
    }

    /** 取得帳戶所屬客戶實體。 */
    public Customer getCustomer() {
        return customer;
    }

    /** 取得銀行帳號。 */
    public String getAccountNumber() {
        return accountNumber;
    }

    /** 取得目前帳戶餘額。 */
    public BigDecimal getBalance() {
        return balance;
    }

    /** 取得帳戶幣別。 */
    public String getCurrency() {
        return currency;
    }

    /** 取得目前業務狀態。 */
    public AccountStatus getStatus() {
        return status;
    }

    /** 取得 JPA 樂觀鎖版本欄位。 */
    public Long getVersion() {
        return version;
    }

    /** 取得建立時間戳記。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 變更帳戶狀態。
     *
     * 停用與重新啟用的業務條件由 AccountService 驗證。
     */
    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    /**
     * 設定交易完成後的帳戶餘額。
     * 此方法不會檢查餘額、金額格式或帳戶狀態，
     * 呼叫前應由存提款服務完成相關業務驗證。
     */
    public void updateBalance(BigDecimal newBalance) {
        this.balance = newBalance;
    }
}
