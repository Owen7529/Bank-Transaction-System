/**
 * TransactionHistoryController
 * 提供帳戶交易明細的分頁列表與單筆查詢 API。
 */

package com.example.bank.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bank.dto.TransactionHistoryItemResponse;
import com.example.bank.dto.TransactionHistoryPageResponse;
import com.example.bank.service.TransactionHistoryService;

/**
 * 功能說明：交易紀錄查詢 API。
 */
@RestController
@RequestMapping("/api/accounts/{accountId}/transactions")
public class TransactionHistoryController {

    private final TransactionHistoryService historyService;

    // 建構子注入
    public TransactionHistoryController(
            TransactionHistoryService historyService) {

        this.historyService = historyService;
    }

    /**
     * 查詢帳戶交易紀錄。
     *
     * 範例：
     * GET /api/accounts/1/transactions
     * GET /api/accounts/1/transactions?page=0&size=10
     */
    @GetMapping
    public TransactionHistoryPageResponse getHistory(

            @PathVariable Long accountId,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size) {

        return historyService.getHistory(
                accountId,
                page,
                size
        );
    }

    /**
     * 查詢指定帳戶的單筆交易。
     *
     * 範例：
     * GET /api/accounts/1/transactions/5
     */
    @GetMapping("/{transactionId}")
    public TransactionHistoryItemResponse getTransaction(

            @PathVariable Long accountId,

            @PathVariable Long transactionId) {

        return historyService.getTransaction(
                accountId,
                transactionId
        );
    }
}
