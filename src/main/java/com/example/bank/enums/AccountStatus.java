/**
 * AccountStatus
 * 帳戶狀態列舉，限制帳戶可使用的狀態值。
 */

package com.example.bank.enums;

/**
 * 銀行帳戶狀態。
 * 對應 accounts.status。
 */
public enum AccountStatus {

    ACTIVE,     // 正常帳戶
    INACTIVE    // 停用帳戶
}
