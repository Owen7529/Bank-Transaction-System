/**
 * TransactionHistoryPageResponse
 * 交易明細分頁 DTO，包含列表及下一頁等分頁資訊。
 */

package com.example.bank.dto;

import java.util.List;

/**
 * 功能說明：交易紀錄分頁回應 DTO。
 *
 * 除了本頁的交易明細，
 * 也將分頁資訊一起傳給前端。
 */
public record TransactionHistoryPageResponse(

        // 本頁交易紀錄
        List<TransactionHistoryItemResponse> items,

        // 目前頁碼（從0開始）
        int page,

        // 每頁最大筆數
        int size,

        // 符合條件的總筆數
        long totalElements,

        // 總頁數
        int totalPages,

        // 是否還有下一頁
        boolean hasNext

) {
}
