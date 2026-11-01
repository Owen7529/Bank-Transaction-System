/**
 * AccountController
 * 處理帳戶建立、查詢、列表及狀態調整等 HTTP API。
 */

package com.example.bank.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bank.dto.AccountResponse;
import com.example.bank.dto.CreateAccountRequest;
import com.example.bank.dto.UpdateAccountStatusRequest;
import com.example.bank.service.AccountService;

import jakarta.validation.Valid;

/**
 * 銀行帳戶管理 REST API。
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    /** 透過建構子注入帳戶管理服務。 */
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * POST /api/accounts
     *
     * 開立零餘額銀行帳戶。
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {

        AccountResponse response =
                accountService.createAccount(request);

        return ResponseEntity
                .created(URI.create(
                        "/api/accounts/" + response.id()
                ))
                .body(response);
    }

    /**
     * GET /api/accounts/{id}
     *
     * 查詢單一帳戶。
     */
    @GetMapping("/{id}")
    public AccountResponse getAccount(
            @PathVariable Long id) {

        return accountService.getAccount(id);
    }

    /**
     * GET /api/accounts
     *
     * 查詢所有帳戶。
     */
    @GetMapping
    public List<AccountResponse> getAllAccounts() {

        return accountService.getAllAccounts();
    }

    /**
     * PATCH /api/accounts/{id}/status
     *
     * 修改帳戶狀態。
     */
    @PatchMapping("/{id}/status")
    public AccountResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody
            UpdateAccountStatusRequest request) {

        return accountService.updateStatus(
                id,
                request.status()
        );
    }
}