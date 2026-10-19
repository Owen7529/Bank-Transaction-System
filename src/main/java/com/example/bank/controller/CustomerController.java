package com.example.bank.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bank.dto.CreateCustomerRequest;
import com.example.bank.dto.CustomerResponse;
import com.example.bank.dto.UpdateCustomerStatusRequest;
import com.example.bank.service.AccountService;
import com.example.bank.service.CustomerService;
import com.example.bank.dto.AccountResponse;

import jakarta.validation.Valid;

/**
 * 客戶管理 REST API。
 */
@RestController
@RequestMapping("/api/customers")
@Validated
public class CustomerController {

    private final CustomerService customerService;
    private final AccountService accountService;

    public CustomerController(
            CustomerService customerService,
            AccountService accountService) {

        this.customerService = customerService;
        this.accountService = accountService;
    }

    /**
     * POST /api/customers
     *
     * 新增客戶，成功回傳201。
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {

        CustomerResponse response =
                customerService.createCustomer(request);

        return ResponseEntity
                .created(URI.create(
                        "/api/customers/" + response.id()
                ))
                .body(response);
    }

    /**
     * GET /api/customers/{id}
     *
     * 查詢單一客戶。
     */
    @GetMapping("/{id}")
    public CustomerResponse getCustomer(
            @PathVariable Long id) {

        return customerService.getCustomer(id);
    }

    /**
     * GET /api/customers
     *
     * 查詢客戶清單。
     */
    @GetMapping
    public List<CustomerResponse> getAllCustomers() {

        return customerService.getAllCustomers();
    }

    /**
     * PATCH /api/customers/{id}/status
     *
     * 修改客戶狀態。
     */
    @PatchMapping("/{id}/status")
    public CustomerResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody
            UpdateCustomerStatusRequest request) {

        return customerService.updateStatus(
                id,
                request.status()
        );
    }

    /**
     * GET /api/customers/{id}/accounts
     *
     * 查詢指定客戶的所有帳戶。
     */
    @GetMapping("/{id}/accounts")
    public List<AccountResponse> getCustomerAccounts(
            @PathVariable Long id) {

        return accountService.getAccountsByCustomer(id);
    }
}