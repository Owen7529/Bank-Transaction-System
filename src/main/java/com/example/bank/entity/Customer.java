/**
 * Customer
 * 對應 customers 資料表的 JPA Entity，保存客戶代碼、名稱及狀態。
 */

package com.example.bank.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.example.bank.enums.CustomerStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 客戶資料 Entity。
 *
 * 對應資料表：customers
 */
@Entity
@Table(name = "customers")
public class Customer {

    // 客戶主鍵，由 MySQL 自動產生
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 唯一客戶代碼
    @Column(
        name = "customer_code",
        nullable = false,
        unique = true,
        length = 30
    )
    private String customerCode;

    // 客戶名稱
    @Column(
        name = "name",
        nullable = false,
        length = 100
    )
    private String name;

    // 電子郵件，可為 NULL
    @Column(name = "email", length = 150)
    private String email;

    // 客戶狀態，以字串儲存
    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 20
    )
    private CustomerStatus status = CustomerStatus.ACTIVE;

    // 建立時間，由 Hibernate 在新增時產生
    @CreationTimestamp
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    // JPA 需要無參數建構子
    protected Customer() {
    }

    /**
     * 建立新客戶。
     */
    public Customer(
            String customerCode,
            String name,
            String email) {

        this.customerCode = customerCode;
        this.name = name;
        this.email = email;
    }

    /** 取得資料表主鍵，供其他程式識別實體。 */
    public Long getId() {
        return id;
    }

    /** 取得唯一的客戶代碼。 */
    public String getCustomerCode() {
        return customerCode;
    }

    /** 取得客戶名稱。 */
    public String getName() {
        return name;
    }

    /** 取得客戶電子郵件。 */
    public String getEmail() {
        return email;
    }

    /** 取得目前業務狀態。 */
    public CustomerStatus getStatus() {
        return status;
    }

    /** 取得建立時間戳記。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 變更客戶名稱。 */
    public void setName(String name) {
        this.name = name;
    }

    /** 變更客戶電子郵件。 */
    public void setEmail(String email) {
        this.email = email;
    }

    /** 設定狀態欄位；呼叫端仍應先通過合法性及權限檢查。 */
    public void setStatus(CustomerStatus status) {
        this.status = status;
    }
}
