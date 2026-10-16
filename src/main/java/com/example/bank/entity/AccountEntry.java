package com.example.bank.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 帳戶餘額異動明細 Entity。
 *
 * amountChange：
 * 正數表示入帳。
 * 負數表示出帳。
 *
 * 一筆轉帳會有兩筆 AccountEntry。
 */
@Entity
@Table(
    name = "account_entries",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_entries_transaction_account",
            columnNames = {
                "transaction_id",
                "account_id"
            }
        )
    }
)
public class AccountEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 此異動所屬的銀行交易
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "transaction_id",
        nullable = false
    )
    private BankTransaction transaction;

    // 發生餘額異動的帳戶
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "account_id",
        nullable = false
    )
    private Account account;

    // 帶正負號的金額變化
    @Column(
        name = "amount_change",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal amountChange;

    // 異動完成後的帳戶餘額
    @Column(
        name = "balance_after",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal balanceAfter;

    @CreationTimestamp
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    protected AccountEntry() {
    }

    public AccountEntry(
            BankTransaction transaction,
            Account account,
            BigDecimal amountChange,
            BigDecimal balanceAfter) {

        this.transaction = transaction;
        this.account = account;
        this.amountChange = amountChange;
        this.balanceAfter = balanceAfter;
    }

    public Long getId() {
        return id;
    }

    public BankTransaction getTransaction() {
        return transaction;
    }

    public Account getAccount() {
        return account;
    }

    public BigDecimal getAmountChange() {
        return amountChange;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
