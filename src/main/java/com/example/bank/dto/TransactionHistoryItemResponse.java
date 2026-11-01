/**
 * TransactionHistoryItemResponse
 * 單筆帳戶交易明細 DTO，用於交易紀錄查詢的輸出。
 */

package com.example.bank.dto;

import java.math.BigDecimal;

/**
 * 功能說明：單筆交易紀錄回應 DTO。
 *
 * 不直接回傳 Entity，
 * 避免暴露不必要的資料庫欄位。
 */
public record TransactionHistoryItemResponse(

        // 帳戶異動紀錄 ID
        Long entryId,

        // 銀行交易主紀錄 ID
        Long transactionId,

        // 銀行交易編號
        String transactionNumber,

        // 交易類型，例如 DEPOSIT、WITHDRAWAL
        String transactionType,

        // 帳戶實際增減金額
        // 正數代表增加，負數代表減少
        BigDecimal amountChange,

        // 此筆交易完成後的帳戶餘額
        BigDecimal balanceAfter,

        // 交易備註
        String description,

        // 交易狀態
        String transactionStatus,

        // 交易建立時間
        String createdAt

) {
}
