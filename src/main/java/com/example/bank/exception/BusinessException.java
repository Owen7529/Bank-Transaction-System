/**
 * BusinessException
 * 業務條件不符合時拋出的例外類型。
 */

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

    /** 使用錯誤訊息建立業務例外，供全域例外處理器轉換回應。 */
    public BusinessException(String message) {
        super(message);
    }
}