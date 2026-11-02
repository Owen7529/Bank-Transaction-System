package com.example.bank.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供登入工作階段的 CSRF Token 查詢 API。
 *
 * Spring Security 負責建立及驗證 Token；此 Controller 僅將目前請求
 * 可用的 Token 資訊回傳給同源前端，供資料異動請求帶入 HTTP 標頭。
 */
@RestController
public class CsrfController {

    /**
     * 取得目前請求所使用的 CSRF Token。
     *
     * @param csrfToken Spring Security 提供的 Token 資訊
     * @return 包含 Token、HTTP 標頭名稱及表單參數名稱的資料
     */
    @GetMapping("/api/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }
}
