/**
 * TransferServiceTest
 * 測試轉帳業務流程、提款與存款服務的協作及例外處理。
 */

package com.example.bank.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.bank.dto.DepositRequest;
import com.example.bank.dto.TransferRequest;
import com.example.bank.dto.TransferResponse;
import com.example.bank.dto.WithdrawalRequest;

import com.example.bank.entity.Account;
import com.example.bank.exception.BusinessException;
import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.repository.AccountRepository;

/**
 * 功能說明：轉帳Service單元測試
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private WithdrawalService withdrawalService;

    @Mock
    private DepositService depositService;

    @InjectMocks
    private TransferService transferService;

    /**
     * 模擬帳戶1及帳戶2都存在
     */
    private void mockAccountsExist() {

        when(accountRepository.findByIdForUpdate(1L))
            .thenReturn(Optional.of(mock(Account.class)));

        when(accountRepository.findByIdForUpdate(2L))
            .thenReturn(Optional.of(mock(Account.class)));
    }

    /**
     * 測試一：正常轉帳
     */
    @Test
    void transferShouldSucceed() {

        mockAccountsExist();

        BigDecimal amount = new BigDecimal("3000.00");

        TransferRequest request = new TransferRequest(
            "transfer-test-001",
            1L,
            2L,
            amount
        );

        TransferResponse result =
                transferService.transfer(request);

        assertEquals("SUCCESS", result.status());

        // 取得實際傳入的提款DTO
        ArgumentCaptor<WithdrawalRequest> withdrawalCaptor =
            ArgumentCaptor.forClass(WithdrawalRequest.class);

        verify(withdrawalService)
            .withdraw(withdrawalCaptor.capture());

        WithdrawalRequest withdrawal =
                withdrawalCaptor.getValue();

        assertEquals(1L, withdrawal.accountId());
        assertEquals(0, amount.compareTo(withdrawal.amount()));

        assertEquals(
            "TR-OUT-transfer-test-001",
            withdrawal.requestKey()
        );

        // 取得實際傳入的存款DTO
        ArgumentCaptor<DepositRequest> depositCaptor =
            ArgumentCaptor.forClass(DepositRequest.class);

        verify(depositService)
            .deposit(depositCaptor.capture());

        DepositRequest deposit = depositCaptor.getValue();

        assertEquals(2L, deposit.accountId());
        assertEquals(0, amount.compareTo(deposit.amount()));

        assertEquals(
            "TR-IN-transfer-test-001",
            deposit.requestKey()
        );
    }

    /**
     * 測試二：禁止同帳戶轉帳
     */
    @Test
    void transferToSameAccountShouldFail() {

        TransferRequest request = new TransferRequest(
            "transfer-test-002",
            1L,
            1L,
            new BigDecimal("100.00")
        );

        assertThrows(
            BusinessException.class,
            () -> transferService.transfer(request)
        );

        verifyNoInteractions(
            withdrawalService,
            depositService
        );
    }

    /**
     * 測試三：帳戶不存在
     */
    @Test
    void missingAccountShouldFail() {

        when(accountRepository.findByIdForUpdate(1L))
            .thenReturn(Optional.empty());

        TransferRequest request = new TransferRequest(
            "transfer-test-003",
            1L,
            2L,
            new BigDecimal("500.00")
        );

        assertThrows(
            ResourceNotFoundException.class,
            () -> transferService.transfer(request)
        );

        verifyNoInteractions(
            withdrawalService,
            depositService
        );
    }

    /**
     * 測試四：存款執行失敗
     */
    @Test
    void depositFailureShouldPropagate() {

        mockAccountsExist();

        TransferRequest request = new TransferRequest(
            "transfer-test-004",
            1L,
            2L,
            new BigDecimal("500.00")
        );

        // 模擬存款發生例外
        doThrow(new IllegalStateException("模擬存款失敗"))
            .when(depositService)
            .deposit(any(DepositRequest.class));

        assertThrows(
            IllegalStateException.class,
            () -> transferService.transfer(request)
        );

        // 檢查執行順序
        var order = inOrder(
            withdrawalService,
            depositService
        );

        order.verify(withdrawalService)
            .withdraw(any(WithdrawalRequest.class));

        order.verify(depositService)
            .deposit(any(DepositRequest.class));
    }

    /**
     * 測試五：相同轉帳請求應使用相同識別碼
     */
    @Test
    void repeatedRequestShouldUseSameKeys() {

        mockAccountsExist();

        TransferRequest request = new TransferRequest(
            "transfer-test-005",
            1L,
            2L,
            new BigDecimal("100.00")
        );

        // 模擬同一請求執行兩次
        transferService.transfer(request);
        transferService.transfer(request);

        // 擷取兩次提款DTO
        ArgumentCaptor<WithdrawalRequest> withdrawalCaptor =
            ArgumentCaptor.forClass(WithdrawalRequest.class);

        verify(withdrawalService, times(2))
            .withdraw(withdrawalCaptor.capture());

        var withdrawals = withdrawalCaptor.getAllValues();

        assertEquals(
            withdrawals.get(0).requestKey(),
            withdrawals.get(1).requestKey()
        );

        // 擷取兩次存款DTO
        ArgumentCaptor<DepositRequest> depositCaptor =
            ArgumentCaptor.forClass(DepositRequest.class);

        verify(depositService, times(2))
            .deposit(depositCaptor.capture());

        var deposits = depositCaptor.getAllValues();

        assertEquals(
            deposits.get(0).requestKey(),
            deposits.get(1).requestKey()
        );
    }
}
