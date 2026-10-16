package com.example.bank.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.example.bank.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * 銀行交易主紀錄 Entity。
 *
 * 一筆交易可對應多筆 AccountEntry。
 *
 * 本課只建立映射，不實作資金交易。
 */
@Entity
@Table(name = "bank_transactions")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(
        name = "transaction_number",
        nullable = false,
        unique = true,
        length = 40
    )
    private String transactionNumber;

    // 一個請求最多產生一筆交易
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "request_id",
        nullable = false,
        unique = true
    )
    private TransactionRequest request;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "type",
        nullable = false,
        length = 20
    )
    private TransactionType type;

    @Column(
        name = "amount",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal amount;

    // 第 2 課 Schema 僅允許 COMPLETED
    @Column(
        name = "status",
        nullable = false,
        length = 20
    )
    private String status = "COMPLETED";

    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    protected BankTransaction() {
    }

    public BankTransaction(
            String transactionNumber,
            TransactionRequest request,
            TransactionType type,
            BigDecimal amount,
            String description) {

        this.transactionNumber = transactionNumber;
        this.request = request;
        this.type = type;
        this.amount = amount;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getTransactionNumber() {
        return transactionNumber;
    }

    public TransactionRequest getRequest() {
        return request;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
