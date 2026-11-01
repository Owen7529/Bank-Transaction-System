/**
 * TransactionValidationTest
 * 測試交易請求參數的驗證及拒絕條件。
 */

package com.example.bank.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.bank.dto.DepositRequest;
import com.example.bank.dto.WithdrawalRequest;
import com.example.bank.exception.BusinessException;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.AccountEntryRepository;
import com.example.bank.repository.BankTransactionRepository;
import com.example.bank.repository.TransactionRequestRepository;

/**
 * 交易輸入驗證的單元測試。
 *
 * 本測試不啟動 Spring Boot，也不連接 MySQL。
 */
@ExtendWith(MockitoExtension.class)
class TransactionValidationTest {

    // 使用假的 Repository；避免單元測試碰觸真正的資料庫。
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRequestRepository requestRepository;

    @Mock
    private BankTransactionRepository transactionRepository;

    @Mock
    private AccountEntryRepository entryRepository;

    private DepositService depositService;
    private WithdrawalService withdrawalService;

    /** 使用 Mockito Repository 模擬物件初始化存款與提款服務。 */
    @BeforeEach
    void setUp() {
        // 透過建構子注入 Repository 模擬物件，隔離資料庫操作。
        depositService = new DepositService(
                accountRepository,
                requestRepository,
                transactionRepository,
                entryRepository);

        withdrawalService = new WithdrawalService(
                accountRepository,
                requestRepository,
                transactionRepository,
                entryRepository);
    }

    /** 驗證零元存款在存取資料庫前就被拒絕。 */
    @Test
    void depositRejectsZeroAmount() {
        // 存款金額不得為零。
        DepositRequest request = new DepositRequest(
                "unit-deposit-zero", 1L, new BigDecimal("0.00"), "單元測試");

        assertThrows(BusinessException.class,
                () -> depositService.deposit(request));

        // 不合法的輸入應在查詢或寫入資料庫之前就被拒絕。
        verifyNoInteractions(accountRepository, requestRepository,
                transactionRepository, entryRepository);
    }

    /** 驗證存款金額超過兩位小數時會被拒絕。 */
    @Test
    void depositRejectsExcessiveDecimalPlaces() {
        // 台幣金額最多允許兩位小數。
        DepositRequest request = new DepositRequest(
                "unit-deposit-scale", 1L, new BigDecimal("10.001"), "單元測試");

        assertThrows(BusinessException.class,
                () -> depositService.deposit(request));

        verifyNoInteractions(accountRepository, requestRepository,
                transactionRepository, entryRepository);
    }

    /** 驗證存款請求識別碼不可為空白。 */
    @Test
    void depositRejectsMissingRequestKey() {
        // 冪等請求識別碼不可為空白。
        DepositRequest request = new DepositRequest(
                "  ", 1L, new BigDecimal("100.00"), "單元測試");

        assertThrows(BusinessException.class,
                () -> depositService.deposit(request));

        verifyNoInteractions(accountRepository, requestRepository,
                transactionRepository, entryRepository);
    }

    /** 驗證零元提款會被拒絕。 */
    @Test
    void withdrawalRejectsZeroAmount() {
        // 提款金額不得為零。
        WithdrawalRequest request = new WithdrawalRequest(
                "unit-withdraw-zero", 1L, new BigDecimal("0.00"), "單元測試");

        assertThrows(BusinessException.class,
                () -> withdrawalService.withdraw(request));

        verifyNoInteractions(accountRepository, requestRepository,
                transactionRepository, entryRepository);
    }

    /** 驗證提款金額超過兩位小數時會被拒絕。 */
    @Test
    void withdrawalRejectsExcessiveDecimalPlaces() {
        // 不合法的提款金額應在存取 Repository 前被拒絕。
        WithdrawalRequest request = new WithdrawalRequest(
                "unit-withdraw-scale", 1L, new BigDecimal("10.001"), "單元測試");

        assertThrows(BusinessException.class,
                () -> withdrawalService.withdraw(request));

        verifyNoInteractions(accountRepository, requestRepository,
                transactionRepository, entryRepository);
    }

    /** 驗證提款請求識別碼不可為空白。 */
    @Test
    void withdrawalRejectsMissingRequestKey() {
        // 空白 requestKey 不能進入交易流程。
        WithdrawalRequest request = new WithdrawalRequest(
                " ", 1L, new BigDecimal("100.00"), "單元測試");

        assertThrows(BusinessException.class,
                () -> withdrawalService.withdraw(request));

        verifyNoInteractions(accountRepository, requestRepository,
                transactionRepository, entryRepository);
    }
}
