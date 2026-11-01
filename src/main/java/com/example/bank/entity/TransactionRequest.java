/**
 * TransactionRequest
 * 對應 transaction_requests 資料表的 JPA Entity，記錄請求識別碼、雜湊與處理狀態。
 */

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

    /** 提供 JPA 建立實體所需的無參數建構子。 */
    protected TransactionRequest() {
    }

    /** 建立交易資料實體並設定必要欄位。 */
    public TransactionRequest(
            String requestKey,
            String requestHash) {

        this.requestKey = requestKey;
        this.requestHash = requestHash;
    }

    /** 取得資料表主鍵，供其他程式識別實體。 */
    public Long getId() {
        return id;
    }

    /** 取得前端傳入的請求識別碼。 */
    public String getRequestKey() {
        return requestKey;
    }

    /** 取得用來確認請求內容一致性的摘要值。 */
    public String getRequestHash() {
        return requestHash;
    }

    /** 取得目前業務狀態。 */
    public RequestStatus getStatus() {
        return status;
    }

    /** 取得建立時間戳記。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 取得最後更新時間戳記。 */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** 設定狀態欄位；呼叫端仍應先通過合法性及權限檢查。 */
    public void setStatus(RequestStatus status) {
        this.status = status;
    }
}
