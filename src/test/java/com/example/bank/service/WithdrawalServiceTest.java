/**
 * WithdrawalServiceTest
 * 測試提款限制、帳戶餘額與重送處理等業務規則。
 */

package com.example.bank.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.bank.dto.WithdrawalRequest;
import com.example.bank.dto.WithdrawalResponse;

import com.example.bank.entity.Account;
import com.example.bank.entity.AccountEntry;
import com.example.bank.entity.BankTransaction;
import com.example.bank.entity.Customer;
import com.example.bank.entity.TransactionRequest;

import com.example.bank.enums.AccountStatus;
import com.example.bank.enums.CustomerStatus;

import com.example.bank.exception.BusinessException;

import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.AccountEntryRepository;
import com.example.bank.repository.BankTransactionRepository;
import com.example.bank.repository.TransactionRequestRepository;

/**
 * 功能說明：提款服務單元測試
 *
 * 使用Mockito模擬Repository，
 * 測試時不會存取真實銀行資料庫。
 */
@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRequestRepository requestRepository;

    @Mock
    private BankTransactionRepository transactionRepository;

    @Mock
    private AccountEntryRepository entryRepository;

    @InjectMocks
    private WithdrawalService withdrawalService;

    /**
     * 測試工具：
     * 準備一個正常啟用的模擬帳戶。
     */
    private Account prepareAccount(BigDecimal balance) {

        Account account = mock(Account.class);
        Customer customer = mock(Customer.class);

        when(accountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        when(account.getStatus())
                .thenReturn(AccountStatus.ACTIVE);

        when(account.getCustomer())
                .thenReturn(customer);

        when(customer.getStatus())
                .thenReturn(CustomerStatus.ACTIVE);

        when(account.getCurrency())
                .thenReturn("TWD");

        // 只有測試需要餘額時，才設定模擬回傳值
        if (balance != null) {
            when(account.getBalance())
                    .thenReturn(balance);
        }

        return account;
    }

    /**
     * 測試1：正常提款。
     *
     * 10000 - 3000 = 7000
     */
    @Test
    void withdrawalShouldSucceed() {

        Account account = prepareAccount(
                new BigDecimal("10000.00"));

        when(account.getId()).thenReturn(1L);

        when(account.getAccountNumber())
                .thenReturn("TEST123");

        // 模擬Repository保存後回傳Entity
        when(requestRepository.saveAndFlush(
                any(TransactionRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(transactionRepository.saveAndFlush(
                any(BankTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        WithdrawalRequest request =
                new WithdrawalRequest(
                        "TEST-WD-001",
                        1L,
                        new BigDecimal("3000.00"),
                        "測試提款"
                );

        WithdrawalResponse result =
                withdrawalService.withdraw(request);

        // 檢查回傳的新餘額
        assertEquals(
                0,
                result.balanceAfter().compareTo(
                        new BigDecimal("7000.00")));

        assertFalse(result.replayed());

        // 確認有執行扣款
        verify(account).updateBalance(
                new BigDecimal("7000.00"));

        // 擷取寫入的AccountEntry
        ArgumentCaptor<AccountEntry> captor =
                ArgumentCaptor.forClass(
                        AccountEntry.class);

        verify(entryRepository).save(captor.capture());

        AccountEntry entry = captor.getValue();

        // 提款的amountChange必須是負數
        assertEquals(
                0,
                entry.getAmountChange().compareTo(
                        new BigDecimal("-3000.00")));
    }

    /**
     * 測試2：餘額不足應拒絕提款。
     */
    @Test
    void insufficientBalanceShouldFail() {

        Account account = prepareAccount(
                new BigDecimal("1000.00"));

        WithdrawalRequest request =
                new WithdrawalRequest(
                        "TEST-WD-002",
                        1L,
                        new BigDecimal("3000.00"),
                        "餘額不足測試"
                );

        assertThrows(
                BusinessException.class,
                () -> withdrawalService.withdraw(request)
        );

        // 不應修改餘額
        verify(account, never())
                .updateBalance(any());

        // 不應新增交易主紀錄
        verify(transactionRepository, never())
                .saveAndFlush(any());
    }

    /**
     * 測試3：超過單筆限額應被拒絕。
     */
    @Test
    void exceedLimitShouldFail() {

    	Account account = prepareAccount(null);

        WithdrawalRequest request =
                new WithdrawalRequest(
                        "TEST-WD-003",
                        1L,
                        new BigDecimal("50000.01"),
                        "超過限額"
                );

        assertThrows(
                BusinessException.class,
                () -> withdrawalService.withdraw(request)
        );

        verify(account, never())
                .updateBalance(any());
    }

    /**
     * 測試4：
     * 相同requestKey但不同交易內容，
     * 應拒絕提款。
     */
    @Test
    void sameKeyWithDifferentContentShouldFail() {

        TransactionRequest previous =
                mock(TransactionRequest.class);

        when(previous.getRequestHash())
                .thenReturn("different-hash");

        when(requestRepository.findByRequestKey(
                "TEST-WD-004"))
                .thenReturn(Optional.of(previous));

        WithdrawalRequest request =
                new WithdrawalRequest(
                        "TEST-WD-004",
                        1L,
                        new BigDecimal("3000.00"),
                        "不同的提款內容"
                );

        assertThrows(
                BusinessException.class,
                () -> withdrawalService.withdraw(request)
        );

        // 應在鎖定帳戶前就拒絕不一致的請求
        verify(accountRepository, never())
                .findByIdForUpdate(any());
    }
}
