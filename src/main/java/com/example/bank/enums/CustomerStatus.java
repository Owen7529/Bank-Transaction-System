/**
 * CustomerStatus
 * 客戶狀態列舉，限制客戶可使用的狀態值。
 */

package com.example.bank.enums;

/**
 * 客戶狀態。
 * 對應 customers.status。
 */
public enum CustomerStatus {

    ACTIVE,     // 正常
    INACTIVE    // 停用
}
