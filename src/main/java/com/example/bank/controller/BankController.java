package com.example.bank.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BankController {
	@GetMapping("/hello")
	public Map<String, String> hello(){
		// 建立 Map 儲存回傳資料
		Map<String, String> result = new LinkedHashMap<>();
		
		// 系統英文名稱
		result.put("system", "Bank Transaction System");
		
		// 系統中文訊息
		result.put("message", "銀行系統啟動成功");
		
		// 系統狀態
		result.put("status", "OK");
		
		// Spring MVC 會將資料轉成 JSON
		return result;
	}
}
