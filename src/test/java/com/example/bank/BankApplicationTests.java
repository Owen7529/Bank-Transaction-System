package com.example.bank;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/*
Spring Boot 應用程式測試。

用來確認 Spring 容器可以成功啟動。
*/
@SpringBootTest
class BankApplicationTests {

	/*
    驗證 Spring ApplicationContext
    是否能正常載入。
    */
	@Test
	void contextLoads() {
		// 如果 Spring 容器無法建立，
        // 測試就會失敗。
	}

}
