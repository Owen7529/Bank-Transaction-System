/**
 * DatabaseConnectionTest
 * 檢查 MySQL 資料庫連線所需條件的整合測試。
 */

package com.example.bank.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * MySQL 資料庫整合測試。
 *
 * 需要：
 * 1. MySQL Server 正在執行。
 * 2. bank_transaction_db 已建立。
 * 3. BANK_DB_PASSWORD 已設定。
 * 4. 五張核心資料表已建立。
 */
@SpringBootTest
class DatabaseConnectionTest {

    // 由 Spring 注入 JdbcTemplate
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 驗證 MySQL 可以正常執行 SQL。
     */
    @Test
    void databaseConnectionShouldWork() {

        Integer result = jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class
        );

        assertEquals(1, result);
    }

    /**
     * 驗證目前連接的是正確的資料庫。
     */
    @Test
    void shouldConnectToCorrectDatabase() {

        String databaseName = jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class
        );

        assertEquals(
                "bank_transaction_db",
                databaseName
        );
    }

    /**
     * 驗證五張核心資料表都存在。
     */
    @Test
    void shouldHaveFiveCoreTables() {

        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME IN (
                      'customers',
                      'accounts',
                      'transaction_requests',
                      'bank_transactions',
                      'account_entries'
                  )
                  AND TABLE_TYPE = 'BASE TABLE'
                """,
                Integer.class
        );

        assertTrue(
                tableCount != null && tableCount == 5,
                "銀行系統應存在五張核心資料表"
        );
    }
}
