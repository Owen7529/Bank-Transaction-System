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
	
	public static void main(String[] args) {
		SpringApplication.run(BankApplication.class, args);
	}

}
