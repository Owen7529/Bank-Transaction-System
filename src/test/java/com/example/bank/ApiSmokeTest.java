/**
 * ApiSmokeTest
 * 使用模擬 HTTP 請求檢查基本 API 路由及回應格式的測試類別。
 */

package com.example.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 功能說明：測試已啟動的Spring Boot。
 *
 * 需要設定兩個環境變數：
 * BANK_TEST_BASE_URL
 * BANK_TEST_ACCOUNT_ID
 *
 * 若未設定，測試會略過。
 */
class ApiSmokeTest {

    // Spring Boot服務的網址
    private static final String BASE_URL =
            System.getenv()
                .getOrDefault("BANK_TEST_BASE_URL", "")
                .replaceAll("/+$", "");

    // MySQL中實際存在的帳戶ID
    private static final String ACCOUNT_ID =
            System.getenv()
                .getOrDefault("BANK_TEST_ACCOUNT_ID", "");

    // 建立HTTP用戶端
    private final HttpClient client =
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

    /**
     * 每項測試前檢查測試設定。
     */
    @BeforeEach
    void requireConfiguredRunningServer() {

        assumeTrue(
                !BASE_URL.isBlank()
                    && !ACCOUNT_ID.isBlank(),
                "未設定測試網址或帳戶ID，略過測試");

        assertTrue(
                ACCOUNT_ID.matches("[0-9]+"),
                "BANK_TEST_ACCOUNT_ID必須是數字");
    }

    /**
     * 共用的HTTP GET方法。
     */
    private HttpResponse<String> get(String path)
            throws IOException, InterruptedException {

        HttpRequest request =
                HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers
                    .ofString(StandardCharsets.UTF_8));
    }

    /**
     * 測試兩個網頁能否被存取。
     */
    @Test
    void bothWebPagesShouldBeAccessible()
            throws Exception {

        assertEquals(
                200,
                get("/bank/index.html").statusCode());

        assertEquals(
                200,
                get("/bank/dashboard.html").statusCode());
    }

    /**
     * 測試帳戶API與主要回傳欄位。
     */
    @Test
    void accountApiShouldReturnExpectedFields()
            throws Exception {

        HttpResponse<String> response = get(
                "/api/accounts/" + ACCOUNT_ID);

        assertEquals(
                200,
                response.statusCode(),
                response.body());

        assertTrue(
                response.body().contains(
                        "\"accountNumber\""),
                "缺少accountNumber欄位");

        assertTrue(
                response.body().contains(
                        "\"balance\""),
                "缺少balance欄位");
    }

    /**
     * 測試交易歷史分頁API。
     */
    @Test
    void historyApiShouldReturnPageStructure()
            throws Exception {

        String path =
                "/api/accounts/" + ACCOUNT_ID
                + "/transactions?page=0&size=20";

        HttpResponse<String> response = get(path);

        assertEquals(
                200,
                response.statusCode(),
                response.body());

        assertTrue(
                response.body().contains("\"items\""),
                "缺少items欄位");

        assertTrue(
                response.body().contains("\"hasNext\""),
                "缺少hasNext欄位");
    }
}
