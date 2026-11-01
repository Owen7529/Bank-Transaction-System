/**
 * RepositoryIntegrationTest
 * 驗證 Repository 與資料表映射的資料庫整合測試。
 */

package com.example.bank.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank.entity.Account;
import com.example.bank.entity.Customer;
import com.example.bank.enums.AccountStatus;
import com.example.bank.enums.CustomerStatus;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.CustomerRepository;

/**
 * Entity 與 Repository 整合測試。
 *
 * 使用真正的 MySQL。
 *
 * @Transactional：
 * 測試方法結束後，Spring Test
 * 預設會回滾本測試交易。
 *
 * 測試期間請勿同時進行正式資料操作。
 */
@SpringBootTest
@Transactional
class RepositoryIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * 產生測試用唯一代碼。
     *
     * 避免與既有資料重複。
     */
    private String uniqueCode(String prefix) {

        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        return prefix + suffix;
    }

    /**
     * 測試 1：
     * 新增客戶並透過客戶代碼查詢。
     */
    @Test
    void shouldSaveAndFindCustomer() {

        String code = uniqueCode("C");

        // 建立客戶 Entity
        Customer customer = new Customer(
                code,
                "測試客戶",
                "customer@example.com"
        );

        // 寫入 MySQL
        Customer saved =
                customerRepository.saveAndFlush(customer);

        // 主鍵應由 MySQL 自動產生
        assertNotNull(saved.getId());

        // 建立時間應存在
        assertNotNull(saved.getCreatedAt());

        // 預設狀態為 ACTIVE
        assertEquals(
                CustomerStatus.ACTIVE,
                saved.getStatus()
        );

        // 使用 Repository 自訂方法查詢
        Customer found = customerRepository
                .findByCustomerCode(code)
                .orElseThrow();

        assertEquals(
                "測試客戶",
                found.getName()
        );

        assertTrue(
                customerRepository.existsByCustomerCode(code)
        );
    }

    /**
     * 測試 2：
     * 新增客戶及帳戶，確認外鍵關聯。
     */
    @Test
    void shouldSaveAccountWithCustomer() {

        // 先建立客戶
        Customer customer = new Customer(
                uniqueCode("C"),
                "帳戶測試客戶",
                "account@example.com"
        );

        customer =
                customerRepository.saveAndFlush(customer);

        // 建立帳戶
        Account account = new Account(
                customer,
                uniqueCode("A")
        );

        Account saved =
                accountRepository.saveAndFlush(account);

        // 驗證帳戶主鍵
        assertNotNull(saved.getId());

        // 驗證帳戶關聯的客戶
        assertEquals(
                customer.getId(),
                saved.getCustomer().getId()
        );

        // 驗證初始餘額
        assertEquals(
                0,
                new BigDecimal("0.00")
                        .compareTo(saved.getBalance())
        );

        // 驗證幣別
        assertEquals(
                "TWD",
                saved.getCurrency()
        );

        // 驗證帳戶狀態
        assertEquals(
                AccountStatus.ACTIVE,
                saved.getStatus()
        );

        // 驗證版本欄位
        assertNotNull(saved.getVersion());

        // 查詢指定客戶的帳戶
        List<Account> accounts =
                accountRepository.findByCustomer_Id(
                        customer.getId()
                );

        assertEquals(1, accounts.size());
    }

    /**
     * 測試 3：
     * 使用銀行帳號查詢帳戶。
     */
    @Test
    void shouldFindAccountByAccountNumber() {

        Customer customer = new Customer(
                uniqueCode("C"),
                "查詢測試客戶",
                null
        );

        customer =
                customerRepository.saveAndFlush(customer);

        String accountNumber = uniqueCode("A");

        Account account = new Account(
                customer,
                accountNumber
        );

        accountRepository.saveAndFlush(account);

        // 依帳號查詢
        Account found = accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow();

        assertEquals(
                accountNumber,
                found.getAccountNumber()
        );

        assertTrue(
                accountRepository.existsByAccountNumber(
                        accountNumber
                )
        );
    }
}
