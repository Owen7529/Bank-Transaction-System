/**
 * TransferController
 * 接收轉帳請求，委派 TransferService 執行同一資料庫交易內的轉出與轉入。
 */

package com.example.bank.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bank.dto.TransferRequest;
import com.example.bank.dto.TransferResponse;
import com.example.bank.service.TransferService;

import jakarta.validation.Valid;

/**
 * 銀行轉帳 API Controller
 */
@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    /**
     * 建構子注入 TransferService
     */
    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * 執行銀行轉帳
     *
     * POST /api/transfers
     */
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request) {

        // 交由 Service 處理轉帳邏輯
        TransferResponse response =
                transferService.transfer(request);

        // 回傳 HTTP 200
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
