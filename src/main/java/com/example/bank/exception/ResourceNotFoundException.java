package com.example.bank.exception;

/**
 * 查詢不到指定資源時使用。
 *
 * 對應 HTTP 404。
 */
public class ResourceNotFoundException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}