/**
 * TransactionType
 * 交易種類列舉，用於標記存款、提款等交易類型。
 */

package com.example.bank.enums;

/**
 * 銀行交易類型。
 * 對應 bank_transactions.type。
 */
public enum TransactionType {

    DEPOSIT,       // 存款
    WITHDRAWAL,    // 提款
    TRANSFER       // 轉帳
}
