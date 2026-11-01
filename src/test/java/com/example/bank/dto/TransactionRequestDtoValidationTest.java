/**
 * TransactionRequestDtoValidationTest
 * 驗證交易 DTO 的 Bean Validation 約束與無效輸入處理。
 */

package com.example.bank.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

public class TransactionRequestDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        // 建立驗證器，讀取 DTO 上的驗證註解
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        // 測試結束時釋放驗證器資源
        factory.close();
    }

    /** 驗證合法的存款請求可通過 Bean Validation。 */
    @Test
    void validDepositRequestShouldPass() {

        // 建立符合驗證規則的存款請求
        DepositRequest request = new DepositRequest(
                "deposit-valid-001",
                1L,
                new BigDecimal("1000.00"),
                "正常存款測試"
        );

        // 正確資料不應出現驗證錯誤
        assertTrue(validator.validate(request).isEmpty());
    }

    /** 驗證零元存款請求會違反最小金額限制。 */
    @Test
    void zeroDepositShouldFail() {

        // 存款金額為零，應違反最低金額限制
        DepositRequest request = new DepositRequest(
                "deposit-invalid-001",
                1L,
                new BigDecimal("0.00"),
                "零元存款測試"
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    /** 驗證提款金額超過兩位小數時會產生驗證錯誤。 */
    @Test
    void invalidWithdrawalDecimalShouldFail() {

        // 提款金額有三位小數，應違反 Digits 限制
        WithdrawalRequest request = new WithdrawalRequest(
                "withdraw-invalid-001",
                1L,
                new BigDecimal("300.001"),
                "小數位數測試"
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    /** 驗證空白請求識別碼無法通過驗證。 */
    @Test
    void blankRequestKeyShouldFail() {

        // requestKey 不可只有空白
        WithdrawalRequest request = new WithdrawalRequest(
                "   ",
                1L,
                new BigDecimal("100.00"),
                "識別碼測試"
        );

        assertFalse(validator.validate(request).isEmpty());
    }
}
