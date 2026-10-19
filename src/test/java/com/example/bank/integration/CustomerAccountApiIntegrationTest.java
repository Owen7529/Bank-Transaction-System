package com.example.bank.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank.dto.AccountResponse;
import com.example.bank.dto.CreateAccountRequest;
import com.example.bank.dto.CreateCustomerRequest;
import com.example.bank.dto.CustomerResponse;
import com.example.bank.enums.AccountStatus;
import com.example.bank.enums.CustomerStatus;
import com.example.bank.exception.BusinessException;
import com.example.bank.service.AccountService;
import com.example.bank.service.CustomerService;

/**
 * 第4課客戶與帳戶管理整合測試。
 *
 * 使用真正的 MySQL。
 *
 * 每個測試預設會在結束後回滾。
 */
@SpringBootTest
@Transactional
class CustomerAccountApiIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AccountService accountService;

    /**
     * 產生唯一測試代碼，避免資料衝突。
     */
    private String uniqueCode(String prefix) {

        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        return prefix + suffix;
    }

    /**
     * 建立測試客戶。
     */
    private CustomerResponse createTestCustomer() {

        return customerService.createCustomer(
                new CreateCustomerRequest(
                        uniqueCode("C"),
                        "測試客戶",
                        "test@example.com"
                )
        );
    }

    /**
     * 測試1：新增客戶。
     */
    @Test
    void shouldCreateCustomer() {

        CustomerResponse customer = createTestCustomer();

        assertNotNull(customer.id());

        assertEquals(
                CustomerStatus.ACTIVE,
                customer.status()
        );

        CustomerResponse found =
                customerService.getCustomer(customer.id());

        assertEquals(
                customer.customerCode(),
                found.customerCode()
        );
    }

    /**
     * 測試2：重複客戶代碼應被拒絕。
     */
    @Test
    void shouldRejectDuplicateCustomerCode() {

        String code = uniqueCode("C");

        CreateCustomerRequest request =
                new CreateCustomerRequest(
                        code,
                        "第一位客戶",
                        null
                );

        customerService.createCustomer(request);

        assertThrows(
                BusinessException.class,
                () -> customerService.createCustomer(
                        new CreateCustomerRequest(
                                code,
                                "第二位客戶",
                                null
                        )
                )
        );
    }

    /**
     * 測試3：新帳戶必須為零餘額。
     */
    @Test
    void shouldCreateZeroBalanceAccount() {

        CustomerResponse customer = createTestCustomer();

        AccountResponse account =
                accountService.createAccount(
                        new CreateAccountRequest(
                                customer.id(),
                                uniqueCode("A")
                        )
                );

        assertNotNull(account.id());

        assertEquals(
                customer.id(),
                account.customerId()
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        account.balance()
                )
        );

        assertEquals("TWD", account.currency());

        assertEquals(
                AccountStatus.ACTIVE,
                account.status()
        );
    }

    /**
     * 測試4：停用客戶不能開立新帳戶。
     */
    @Test
    void shouldRejectAccountForInactiveCustomer() {

        CustomerResponse customer = createTestCustomer();

        customerService.updateStatus(
                customer.id(),
                CustomerStatus.INACTIVE
        );

        assertThrows(
                BusinessException.class,
                () -> accountService.createAccount(
                        new CreateAccountRequest(
                                customer.id(),
                                uniqueCode("A")
                        )
                )
        );
    }

    /**
     * 測試5：
     * 客戶仍有正常帳戶時不能停用。
     */
    @Test
    void shouldRejectDeactivationWithActiveAccount() {

        CustomerResponse customer = createTestCustomer();

        accountService.createAccount(
                new CreateAccountRequest(
                        customer.id(),
                        uniqueCode("A")
                )
        );

        assertThrows(
                BusinessException.class,
                () -> customerService.updateStatus(
                        customer.id(),
                        CustomerStatus.INACTIVE
                )
        );
    }
}