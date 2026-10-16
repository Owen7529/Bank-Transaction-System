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
 * 注意：
 * 正式交易不能直接透過 Controller
 * 修改 balance，必須由 TransactionService
 * 統一處理。
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

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 變更帳戶狀態。
     *
     * 第 4 課會由 AccountService
     * 驗證是否允許停用。
     */
    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    /**
     * 供未來 TransactionService 使用。
     *
     * 注意：此方法不會自行檢查餘額、
     * 金額格式或帳戶狀態。
     *
     * 不可直接提供給 Controller 呼叫。
     */
    public void updateBalance(BigDecimal newBalance) {
        this.balance = newBalance;
    }
}
