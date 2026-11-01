/**
 * Spring Boot 應用程式啟動入口。
 */
package com.example.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
銀行帳戶交易管理系統主程式。

此類別負責啟動 Spring Boot。

@SpringBootApplication：
1. 啟用 Spring Boot 自動設定。
2. 掃描 Spring 管理的元件。
3. 建立並啟動應用程式
*/

@SpringBootApplication
public class BankApplication {	
	/** 啟動 Spring Boot，建立應用程式的 IoC 容器與 Web 服務。 */
	public static void main(String[] args) {
		SpringApplication.run(BankApplication.class, args);
	}
}