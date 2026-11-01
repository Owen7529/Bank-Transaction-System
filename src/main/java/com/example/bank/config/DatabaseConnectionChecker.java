/**
 * 啟動時檢查資料庫連線狀態。
 */

package com.example.bank.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 資料庫連線檢查器。
 *
 * 在 Spring Boot 啟動完成後，
 * 執行簡單的 SQL，確認 MySQL 可正常連線。
 *
 * 此類別只負責連線驗收，
 * 不負責銀行交易或帳戶資料操作。
 */
@Component
public class DatabaseConnectionChecker
        implements ApplicationRunner {

    // Spring 注入 JdbcTemplate
    private final JdbcTemplate jdbcTemplate;

    /**
     * 使用建構子注入依賴。
     *
     * @param jdbcTemplate Spring 提供的 JDBC 工具
     */
    public DatabaseConnectionChecker(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Spring Boot 啟動後執行。
     *
     * @param args 啟動參數
     */
    @Override
    public void run(ApplicationArguments args) {

        // 執行 SELECT 1，確認資料庫連線正常
        Integer result = jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class
        );

        // 若結果不是 1，代表驗收失敗
        if (!Integer.valueOf(1).equals(result)) {
            throw new IllegalStateException(
                    "MySQL 連線檢查失敗"
            );
        }

        // 顯示成功訊息
        System.out.println(
                "MySQL 資料庫連線檢查成功！"
        );
    }
}
