package com.example.bank.exception;

/**
 * 業務規則不允許執行操作時使用。
 *
 * 例如：
 * 客戶代碼重複、帳戶仍有餘額、
 * 已停用客戶要求開戶等。
 *
 * 對應 HTTP 409。
 */
public class BusinessException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }
}