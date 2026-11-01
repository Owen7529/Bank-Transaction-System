/**
 * 安全性篩選規則與登入流程設定。
 */

package com.example.bank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 銀行系統的基本登入及安全設定。
 *
 * 提供本機展示系統的登入驗證與角色授權。
 * 登入帳號存放於記憶體，並以 Spring Security 保護 HTTP 資源。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 設定 HTTP 請求的存取規則。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            // 設定哪些使用者可以存取網站
            .authorizeHttpRequests(authorize -> authorize

                // 目前允許 ADMIN 和 TELLER 存取受保護資源
                // 目前採用角色層級的整體授權，尚未針對各個 API 分別限制角色
                .anyRequest().hasAnyRole("ADMIN", "TELLER")
            )

            // 使用 Spring Security 內建登入頁面
            .formLogin(form -> form

            	// 登入成功後固定前往銀行系統首頁
            	// true：不優先返回登入前記住的網址
            	.defaultSuccessUrl("/bank/index.html", true)

            	// 允許未登入者存取登入頁
            	.permitAll()
            )

            // 使用 Spring Security 內建登出機制
            .logout(Customizer.withDefaults());

        // 沒有關閉 CSRF，維持框架預設防護
        return http.build();
    }

    /**
     * 使用 BCrypt 進行密碼雜湊。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 建立記憶體中的示範使用者。
     *
     * 登入密碼透過 Spring 設定屬性注入，可由外部 local-secrets.properties 提供。
     * 此方式只用於本機展示與測試，不適合直接用於正式銀行環境。
     */
    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder encoder,
            @Value("${BANK_DEMO_ADMIN_PASSWORD}")
            String adminPassword,
            @Value("${BANK_DEMO_TELLER_PASSWORD}")
            String tellerPassword) {

        // 避免使用空白密碼啟動
        if (adminPassword.isBlank()
                || tellerPassword.isBlank()) {
            throw new IllegalStateException(
                    "示範帳號密碼不可為空白");
        }

        // 建立管理員帳號
        UserDetails admin = User.withUsername("admin")
                .password(encoder.encode(adminPassword))
                .roles("ADMIN")
                .build();

        // 建立一般行員帳號
        UserDetails teller = User.withUsername("teller")
                .password(encoder.encode(tellerPassword))
                .roles("TELLER")
                .build();

        // 回傳記憶體帳號管理物件
        return new InMemoryUserDetailsManager(
                admin, teller);
    }
}
