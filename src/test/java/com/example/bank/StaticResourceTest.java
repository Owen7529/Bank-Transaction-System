/**
 * StaticResourceTest
 * 檢查銀行系統 HTML、CSS 與 JavaScript 靜態資源的可讀性。
 */

package com.example.bank;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * 功能說明：確認網頁資源已正確放進 Maven classpath。
 *
 * 注意：本測試不會啟動資料庫，也不代表 API 串接已通過；
 * API 的行為請依功能規格中的手動驗收案例測試。
 */
class StaticResourceTest {

    /** 從測試 classpath 開啟靜態資源，檢查檔案是否能被載入。 */
    private String readResource(String path) throws IOException {
        try (InputStream stream = getClass()
                .getClassLoader()
                .getResourceAsStream(path)) {
            assertNotNull(stream, "找不到網頁資源：" + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 確認銀行系統首頁可從 classpath 載入。 */
    @Test
    void indexPageShouldExist() throws IOException {
        String html = readResource("static/bank/index.html");
        assertTrue(html.contains("帳戶交易管理"));
    }

    /** 確認頁面所需的 CSS 與 JavaScript 資源存在。 */
    @Test
    void cssAndJavaScriptShouldExist() throws IOException {
        assertTrue(readResource("static/bank/style.css").contains(".panel"));
        assertTrue(readResource("static/bank/app.js").contains("const API"));
    }
}
