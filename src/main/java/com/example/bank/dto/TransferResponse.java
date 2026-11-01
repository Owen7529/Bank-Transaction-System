/**
 * TransferResponse
 * 轉帳完成後的回應 DTO，向前端提供轉帳結果。
 */

package com.example.bank.dto;

import java.math.BigDecimal;

/**
 * 轉帳結果 DTO
 *
 * 僅回傳必要的結果資訊，
 * 避免直接暴露 Entity。
 */
public record TransferResponse(

    Long fromAccountId,

    Long toAccountId,

    BigDecimal amount,

    String status

) {}
