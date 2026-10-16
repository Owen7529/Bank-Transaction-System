package com.example.bank.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.bank.enums.RequestStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 交易請求紀錄 Entity。
 *
 * 用來保存冪等鍵、請求雜湊及處理狀態。
 */
@Entity
@Table(name = "transaction_requests")
public class TransactionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 客戶端提交的唯一請求識別碼
    @Column(
        name = "request_key",
        nullable = false,
        unique = true,
        length = 100
    )
    private String requestKey;

    // 請求內容的 SHA-256 雜湊
    @Column(
        name = "request_hash",
        nullable = false,
        length = 64
    )
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 20
    )
    private RequestStatus status = RequestStatus.PROCESSING;

    @CreationTimestamp
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TransactionRequest() {
    }

    public TransactionRequest(
            String requestKey,
            String requestHash) {

        this.requestKey = requestKey;
        this.requestHash = requestHash;
    }

    public Long getId() {
        return id;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }
}
