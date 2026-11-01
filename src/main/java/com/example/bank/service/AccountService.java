/**
 * AccountService
 * 封裝帳戶開立、查詢及狀態更新的業務規則。
 */

package com.example.bank.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank.dto.AccountResponse;
import com.example.bank.dto.CreateAccountRequest;
import com.example.bank.entity.Account;
import com.example.bank.entity.Customer;
import com.example.bank.enums.AccountStatus;
import com.example.bank.enums.CustomerStatus;
import com.example.bank.exception.BusinessException;
import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.CustomerRepository;

/**
 * 銀行帳戶管理業務邏輯。
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    /** 透過建構子注入帳戶與客戶 Repository。 */
    public AccountService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository) {

        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * 開立新帳戶。
     *
     * 帳戶餘額一律從0元開始。
     */
    @Transactional
    public AccountResponse createAccount(
            CreateAccountRequest request) {

        // 確認客戶存在
        Customer customer = customerRepository
                .findById(request.customerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "找不到客戶，ID：" + request.customerId()
                        )
                );

        // 只有正常客戶可以開戶
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessException(
                    "客戶已停用，無法開立新帳戶"
            );
        }

        String accountNumber =
                request.accountNumber().trim();

        // 檢查銀行帳號是否重複
        if (accountRepository.existsByAccountNumber(
                accountNumber)) {

            throw new BusinessException(
                    "銀行帳號已存在"
            );
        }

        // 使用 Account 建構子建立初始餘額為零的帳戶
        Account account = new Account(
                customer,
                accountNumber
        );

        Account saved =
                accountRepository.saveAndFlush(account);

        return AccountResponse.from(saved);
    }

    /**
     * 查詢單一帳戶。
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {

        Account account = findAccount(id);

        return AccountResponse.from(account);
    }

    /**
     * 查詢所有帳戶。
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {

        return accountRepository.findAll()
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    /**
     * 查詢指定客戶的所有帳戶。
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomer(
            Long customerId) {

        // 先確認客戶存在，避免不存在的客戶
        // 直接回傳空清單而造成誤解
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException(
                    "找不到客戶，ID：" + customerId
            );
        }

        return accountRepository
                .findByCustomer_Id(customerId)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    /**
     * 修改帳戶狀態。
     */
    @Transactional
    public AccountResponse updateStatus(
            Long id,
            AccountStatus newStatus) {

        Account account = findAccount(id);

        // 停用帳戶前，確認餘額為0
        if (newStatus == AccountStatus.INACTIVE) {

            if (account.getBalance()
                    .compareTo(BigDecimal.ZERO) != 0) {

                throw new BusinessException(
                        "帳戶餘額不為零，無法停用"
                );
            }
        }

        // 已停用客戶的帳戶不可重新啟用
        if (newStatus == AccountStatus.ACTIVE
                && account.getCustomer().getStatus()
                        != CustomerStatus.ACTIVE) {

            throw new BusinessException(
                    "客戶已停用，無法啟用帳戶"
            );
        }

        account.setStatus(newStatus);

        accountRepository.flush();

        return AccountResponse.from(account);
    }

    /**
     * 共用的帳戶查詢方法。
     */
    private Account findAccount(Long id) {

        return accountRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "找不到帳戶，ID：" + id
                        )
                );
    }
}