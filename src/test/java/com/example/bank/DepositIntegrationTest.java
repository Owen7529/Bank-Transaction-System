/**
 * DepositIntegrationTest
 * 檢查存款、交易資料與帳戶餘額一致性的整合測試。
 */

package com.example.bank;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.bank.dto.DepositRequest;
import com.example.bank.dto.DepositResponse;
import com.example.bank.entity.Account;
import com.example.bank.entity.Customer;
import com.example.bank.exception.BusinessException;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.BankTransactionRepository;
import com.example.bank.repository.CustomerRepository;
import com.example.bank.repository.AccountEntryRepository;
import com.example.bank.repository.TransactionRequestRepository;
import com.example.bank.service.DepositService;

@SpringBootTest
class DepositIntegrationTest {

    @Autowired
    private DepositService depositService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRequestRepository requestRepository;

    @Autowired
    private BankTransactionRepository transactionRepository;

    @Autowired
    private AccountEntryRepository entryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long customerId;
    private Long accountId;
    private String keyPrefix;

    // 每次測試前建立獨立的客戶及帳戶
    @BeforeEach
    void setUp() {

        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        keyPrefix = "L5-" + suffix + "-";

        Customer customer = new Customer(
                "C" + suffix,
                "JUnit測試客戶",
                "test" + suffix + "@example.com"
        );

        customer = customerRepository.saveAndFlush(customer);
        customerId = customer.getId();

        Account account = new Account(
                customer,
                "9" + suffix
        );

        account = accountRepository.saveAndFlush(account);
        accountId = account.getId();
    }

    // 每次測試結束，清除這次測試建立的資料
    @AfterEach
    void cleanUp() {

        if (accountId == null || customerId == null) {
            return;
        }

        // 依照外鍵關聯的順序刪除
        jdbcTemplate.update(
            "DELETE FROM account_entries WHERE account_id = ?",
            accountId
        );

        jdbcTemplate.update(
            """
            DELETE FROM bank_transactions
            WHERE request_id IN (
                SELECT id FROM transaction_requests
                WHERE request_key LIKE ?
            )
            """,
            keyPrefix + "%"
        );

        jdbcTemplate.update(
            "DELETE FROM transaction_requests WHERE request_key LIKE ?",
            keyPrefix + "%"
        );

        jdbcTemplate.update(
            "DELETE FROM accounts WHERE id = ?",
            accountId
        );

        jdbcTemplate.update(
            "DELETE FROM customers WHERE id = ?",
            customerId
        );
    }

    // 建立測試用存款請求
    private DepositRequest newRequest(
            String key,
            String amount) {

        return new DepositRequest(
                keyPrefix + key,
                accountId,
                new BigDecimal(amount),
                "JUnit存款測試"
        );
    }

    // 比較資料庫中的實際餘額
    private void assertBalance(String expected) {

        Account account = accountRepository
                .findById(accountId)
                .orElseThrow();

        assertEquals(
                0,
                account.getBalance().compareTo(
                    new BigDecimal(expected)
                )
        );
    }

    /** 驗證存款會更新餘額，並建立請求、交易主檔與帳戶異動明細。 */
    @Test
    void depositShouldUpdateBalanceAndCreateRecords() {

        // Arrange：準備資料
        DepositRequest request = newRequest(
                "NORMAL",
                "2000.00"
        );

        // Act：執行存款
        DepositResponse response =
                depositService.deposit(request);

        // Assert：驗證回應
        assertNotNull(response.transactionId());
        assertEquals("COMPLETED", response.status());
        assertFalse(response.replayed());

        assertBalance("2000.00");

        // 確認請求紀錄存在
        var savedRequest = requestRepository
                .findByRequestKey(request.requestKey())
                .orElseThrow();

        // 確認交易主紀錄存在
        var transaction = transactionRepository
                .findByRequest_Id(savedRequest.getId())
                .orElseThrow();

        // 確認恰好產生一筆帳戶異動
        var entries = entryRepository
                .findByTransaction_Id(transaction.getId());

        assertEquals(1, entries.size());

        assertEquals(
                0,
                entries.get(0).getBalanceAfter()
                    .compareTo(new BigDecimal("2000.00"))
        );
    }

    /** 驗證相同請求重送時，不會再次增加帳戶餘額。 */
    @Test
    void sameRequestShouldNotDepositTwice() {

        DepositRequest request = newRequest(
                "SAME",
                "1500.00"
        );

        DepositResponse first =
                depositService.deposit(request);

        DepositResponse second =
                depositService.deposit(request);

        // 應回傳同一筆交易
        assertEquals(
                first.transactionId(),
                second.transactionId()
        );

        assertTrue(second.replayed());

        // 餘額只增加一次
        assertBalance("1500.00");
    }

    /** 驗證相同請求識別碼搭配不同金額時會被拒絕。 */
    @Test
    void sameKeyWithDifferentAmountShouldFail() {

        DepositRequest first = newRequest(
                "CONFLICT",
                "1000.00"
        );

        DepositRequest second = newRequest(
                "CONFLICT",
                "9000.00"
        );

        depositService.deposit(first);

        // 相同 key 卻有不同交易內容
        assertThrows(
                BusinessException.class,
                () -> depositService.deposit(second)
        );

        // 原存款仍然正確
        assertBalance("1000.00");
    }

    /** 驗證交易中途發生例外時，帳戶餘額與交易資料均回滾。 */
    @Test
    void failedTransactionShouldRollBack() {

        String key = keyPrefix + "ROLLBACK";

        DepositRequest request = newRequest(
                "ROLLBACK",
                "3000.00"
        );

        TransactionTemplate template =
                new TransactionTemplate(transactionManager);

        // 在同一資料庫交易中執行存款
        // 接著主動丟出例外，測試整筆 Rollback
        assertThrows(
                IllegalStateException.class,
                () -> template.executeWithoutResult(status -> {

                    depositService.deposit(request);

                    throw new IllegalStateException(
                        "故意觸發回滾測試"
                    );
                })
        );

        // 驗證餘額沒有改變
        assertBalance("0.00");

        // 驗證請求紀錄沒有被提交
        assertTrue(
            requestRepository.findByRequestKey(key).isEmpty()
        );
    }
}
