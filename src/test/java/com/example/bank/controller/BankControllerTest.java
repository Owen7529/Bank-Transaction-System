/**
 * BankControllerTest
 * 測試基本銀行 API 回應的 Controller 測試類別。
 */

package com.example.bank.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

/*
BankController 單元測試。

直接呼叫 Java 方法，
驗證回傳的資料是否正確。

此測試不需要啟動 Web Server。
*/
public class BankControllerTest {
	/** 驗證健康檢查 API 回傳預期的系統識別與狀態。 */
	@Test
	void helloShouldReturnCorrectData() {
		
		// Arrange：準備測試物件
		BankController controller = new BankController();
		
		// Act：執行要測試的方法
        Map<String, String> result = controller.hello();

        // Assert：驗證回傳資料
        assertNotNull(result);

        assertEquals(
                "Bank Transaction System",
                result.get("system")
        );

        assertEquals(
                "銀行系統啟動成功",
                result.get("message")
        );

        assertEquals(
                "OK",
                result.get("status")
        );
	}
}
