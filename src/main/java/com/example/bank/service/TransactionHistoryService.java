/**
 * TransactionHistoryService
 * 封裝帳戶交易明細的查詢、分頁與 DTO 轉換。
 */

package com.example.bank.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank.dto.TransactionHistoryItemResponse;
import com.example.bank.dto.TransactionHistoryPageResponse;

import com.example.bank.entity.AccountEntry;
import com.example.bank.entity.BankTransaction;

import com.example.bank.exception.BusinessException;
import com.example.bank.exception.ResourceNotFoundException;

import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.AccountEntryRepository;

/**
 * 功能說明：交易紀錄查詢服務。
 *
 * 提供：
 * 1. 帳戶交易歷史分頁查詢
 * 2. 指定帳戶的單筆交易查詢
 * 3. Entity 轉換為 DTO
 */
@Service
@Transactional(readOnly = true)
public class TransactionHistoryService {

    // 每頁最多50筆，避免一次查詢過多資料
    private static final int MAX_PAGE_SIZE = 50;

    private final AccountRepository accountRepository;

    private final AccountEntryRepository entryRepository;

    // 建構子注入
    public TransactionHistoryService(
            AccountRepository accountRepository,
            AccountEntryRepository entryRepository) {

        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
    }

    /**
     * 查詢指定帳戶的交易紀錄。
     *
     * @param accountId 帳戶ID
     * @param page 頁碼，從0開始
     * @param size 每頁筆數
     */
    public TransactionHistoryPageResponse getHistory(
            Long accountId,
            int page,
            int size) {

        // 第一步：驗證帳戶ID
        validateAccountId(accountId);

        // 第二步：驗證分頁參數
        if (page < 0) {
            throw new BusinessException(
                    "頁碼不可小於0");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(
                    "每頁筆數必須介於1到50");
        }

        // 第三步：確認帳戶存在
        checkAccountExists(accountId);

        // 第四步：建立分頁條件
        // 依 AccountEntry 的 ID 由大到小排序
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        // 第五步：執行資料庫查詢
        Page<AccountEntry> result =
                entryRepository.findByAccount_Id(
                        accountId,
                        pageable
                );

        // 第六步：將 Entity 轉換成 DTO
        List<TransactionHistoryItemResponse> items =
                result.getContent()
                      .stream()
                      .map(this::toResponse)
                      .toList();

        // 第七步：回傳分頁結果
        return new TransactionHistoryPageResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    /**
     * 查詢指定帳戶的某一筆交易。
     *
     * 必須同時符合 accountId 和 transactionId，
     * 避免直接用交易ID取得其他帳戶的明細。
     */
    public TransactionHistoryItemResponse getTransaction(
            Long accountId,
            Long transactionId) {

        // 第一步：驗證參數
        validateAccountId(accountId);

        if (transactionId == null || transactionId <= 0) {
            throw new BusinessException(
                    "交易ID必須大於0");
        }

        // 第二步：確認帳戶存在
        checkAccountExists(accountId);

        // 第三步：查詢交易對應的帳戶異動
        List<AccountEntry> entries =
                entryRepository.findByTransaction_Id(
                        transactionId
                );

        // 第四步：只保留指定帳戶的異動紀錄
        AccountEntry entry = entries.stream()
                .filter(item ->
                        accountId.equals(
                                item.getAccount().getId()
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "找不到此帳戶的指定交易"
                        )
                );

        // 第五步：轉換為DTO
        return toResponse(entry);
    }

    /**
     * 檢查帳戶ID是否合法。
     */
    private void validateAccountId(Long accountId) {

        if (accountId == null || accountId <= 0) {
            throw new BusinessException(
                    "帳戶ID必須大於0");
        }
    }

    /**
     * 確認帳戶是否存在。
     */
    private void checkAccountExists(Long accountId) {

        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException(
                    "找不到指定帳戶"
            );
        }
    }

    /**
     * 將 AccountEntry 轉換成回應 DTO。
     *
     * 與存提款不同，
     * 這裡不進行任何餘額修改。
     */
    private TransactionHistoryItemResponse toResponse(
            AccountEntry entry) {

        BankTransaction transaction =
                entry.getTransaction();

        return new TransactionHistoryItemResponse(

                entry.getId(),

                transaction.getId(),

                transaction.getTransactionNumber(),

                String.valueOf(transaction.getType()),

                entry.getAmountChange(),

                entry.getBalanceAfter(),

                transaction.getDescription(),

                String.valueOf(transaction.getStatus()),

                transaction.getCreatedAt() == null
                        ? null
                        : transaction.getCreatedAt().toString()
        );
    }
}
