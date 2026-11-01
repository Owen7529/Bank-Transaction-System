/**
 * BankTransaction
 * 對應 bank_transactions 資料表的 JPA Entity，記錄交易主檔與交易類別。
 */

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
 * 此類別負責交易主檔的資料庫映射，不直接執行存提款操作。
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

    // 目前的資料庫結構僅允許 COMPLETED 狀態
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

    /** 提供 JPA 建立實體所需的無參數建構子。 */
    protected BankTransaction() {
    }

    /** 建立交易資料實體並設定必要欄位。 */
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

    /** 取得資料表主鍵，供其他程式識別實體。 */
    public Long getId() {
        return id;
    }

    /** 取得交易的唯一識別編號。 */
    public String getTransactionNumber() {
        return transactionNumber;
    }

    /** 取得交易主檔對應的交易請求紀錄。 */
    public TransactionRequest getRequest() {
        return request;
    }

    /** 取得交易種類列舉。 */
    public TransactionType getType() {
        return type;
    }

    /** 取得交易金額。 */
    public BigDecimal getAmount() {
        return amount;
    }

    /** 取得目前業務狀態。 */
    public String getStatus() {
        return status;
    }

    /** 取得交易用途說明。 */
    public String getDescription() {
        return description;
    }

    /** 取得建立時間戳記。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
