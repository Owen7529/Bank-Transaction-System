/**
 * RequestStatus
 * 交易請求處理狀態列舉，協助追蹤重送與完成情況。
 */

package com.example.bank.enums;

/**
 * 交易請求處理狀態。
 * 對應 transaction_requests.status。
 */
public enum RequestStatus {

    PROCESSING,    // 處理中
    COMPLETED,     // 已完成
    FAILED         // 已失敗
}
