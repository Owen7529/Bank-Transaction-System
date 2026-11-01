/**
 * TransactionController
 * 接收存款、提款及其交易查詢的 HTTP 請求，委派給對應 Service。
 */

package com.example.bank.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bank.dto.DepositRequest;
import com.example.bank.dto.DepositResponse;
import com.example.bank.dto.WithdrawalRequest;
import com.example.bank.dto.WithdrawalResponse;

import com.example.bank.service.DepositService;
import com.example.bank.service.WithdrawalService;

import jakarta.validation.Valid;

/**
 * 存款、提款及其交易結果查詢 API。
 *
 * 功能：
 * 1. 存款
 * 2. 查詢存款
 * 3. 提款
 * 4. 查詢提款
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final DepositService depositService;

    private final WithdrawalService withdrawalService;

    /**
     * 使用建構子注入兩個Service。
     */
    public TransactionController(
            DepositService depositService,
            WithdrawalService withdrawalService) {

        this.depositService = depositService;
        this.withdrawalService = withdrawalService;
    }

    /**
     * 存款 API：建立存款交易並回傳結果
     */
    @PostMapping("/deposits")
    public ResponseEntity<DepositResponse> deposit(
            @Valid @RequestBody DepositRequest request) {

        DepositResponse response =
                depositService.deposit(request);

        HttpStatus status = response.replayed()
                ? HttpStatus.OK
                : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(response);
    }

    /**
     * 查詢存款交易：依交易 ID 取得存款明細
     */
    @GetMapping("/deposits/{transactionId}")
    public ResponseEntity<DepositResponse> getDeposit(
            @PathVariable Long transactionId) {

        DepositResponse response =
                depositService.getDeposit(transactionId);

        return ResponseEntity.ok(response);
    }

    /**
     * 提款 API：執行提款並回傳結果
     */
    @PostMapping("/withdrawals")
    public ResponseEntity<WithdrawalResponse> withdraw(
            @Valid @RequestBody WithdrawalRequest request) {

        WithdrawalResponse response =
                withdrawalService.withdraw(request);

        // 新交易201，重複請求200
        HttpStatus status = response.replayed()
                ? HttpStatus.OK
                : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(response);
    }

    /**
     * 查詢提款交易：依交易 ID 取得提款明細
     */
    @GetMapping("/withdrawals/{transactionId}")
    public ResponseEntity<WithdrawalResponse> getWithdrawal(
            @PathVariable Long transactionId) {

        WithdrawalResponse response =
                withdrawalService.getWithdrawal(transactionId);

        return ResponseEntity.ok(response);
    }
}
