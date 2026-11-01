/**
 * ResourceNotFoundException
 * 查無指定客戶、帳戶或交易資料時使用的例外。
 */

package com.example.bank.exception;

/**
 * 查詢不到指定資源時使用。
 *
 * 對應 HTTP 404。
 */
public class ResourceNotFoundException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 使用錯誤訊息建立找不到資源的例外。 */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}