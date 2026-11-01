/**
 * TransferService
 * 協調來源帳戶提款及目標帳戶存款，控制轉帳的交易原子性與鎖定順序。
 */

package com.example.bank.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank.dto.DepositRequest;
import com.example.bank.dto.WithdrawalRequest;
import com.example.bank.dto.TransferRequest;
import com.example.bank.dto.TransferResponse;

import com.example.bank.exception.BusinessException;
import com.example.bank.exception.ResourceNotFoundException;

import com.example.bank.repository.AccountRepository;

/**
 * 功能說明：銀行轉帳服務
 *
 * 轉帳透過存款與提款服務執行金額及帳戶相關業務規則。
 * 轉帳的兩個操作必須在同一筆資料庫交易內完成。
 */
@Service
public class TransferService {

    private final AccountRepository accountRepository;

    private final WithdrawalService withdrawalService;

    private final DepositService depositService;

    /**
     * 建構子注入
     */
    public TransferService(
            AccountRepository accountRepository,
            WithdrawalService withdrawalService,
            DepositService depositService) {

        this.accountRepository = accountRepository;
        this.withdrawalService = withdrawalService;
        this.depositService = depositService;
    }

    /**
     * 執行轉帳
     *
     * READ_COMMITTED：轉帳交易採用已提交讀取隔離等級。
     *
     * rollbackFor：發生 Exception 時要求回滾。
     */
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        rollbackFor = Exception.class
    )
    /** 驗證轉帳資料，依固定順序鎖定帳戶，再協調轉出與轉入操作。 */
    public TransferResponse transfer(TransferRequest request) {

        // ===== 第一步：基本資料檢查 =====

        if (request == null) {
            throw new BusinessException("轉帳請求不可為空");
        }

        String requestKey = request.requestKey();
        Long fromId = request.fromAccountId();
        Long toId = request.toAccountId();
        BigDecimal amount = request.amount();

        // 同時保護非HTTP途徑呼叫Service的情況
        if (requestKey == null ||
            !requestKey.matches("[A-Za-z0-9_-]{1,90}")) {

            throw new BusinessException("請求識別碼不合法");
        }

        if (fromId == null || fromId <= 0 ||
            toId == null || toId <= 0) {

            throw new BusinessException("轉帳帳戶ID不合法");
        }

        if (amount == null ||
            amount.signum() <= 0 ||
            amount.scale() > 2) {

            throw new BusinessException("轉帳金額不合法");
        }

        // ===== 第二步：防止自己轉給自己 =====

        if (fromId.equals(toId)) {
            throw new BusinessException(
                "來源帳戶與目標帳戶不可相同"
            );
        }

        // ===== 第三步：固定鎖定順序 =====

        // 降低兩個帳戶反向轉帳發生死結的機會
        Long firstId = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);

        // ===== 第四步：鎖定兩個帳戶 =====

        accountRepository.findByIdForUpdate(firstId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "找不到帳戶：" + firstId
                )
            );

        accountRepository.findByIdForUpdate(secondId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "找不到帳戶：" + secondId
                )
            );

        // ===== 第五步：建立固定的操作識別碼 =====

        // 相同轉帳重送時，會得到相同的兩個Key
        String withdrawalKey =
                "TR-OUT-" + requestKey;

        String depositKey =
                "TR-IN-" + requestKey;

        // ===== 第六步：建立提款 DTO =====

        WithdrawalRequest withdrawalRequest =
                new WithdrawalRequest(
                    withdrawalKey,
                    fromId,
                    amount,
                    "轉帳轉出至帳戶：" + toId
                );

        // ===== 第七步：執行提款 =====

        // 透過提款服務檢查餘額及單筆交易限額
        withdrawalService.withdraw(withdrawalRequest);

        // ===== 第八步：建立存款 DTO =====

        DepositRequest depositRequest =
                new DepositRequest(
                    depositKey,
                    toId,
                    amount,
                    "轉帳轉入自帳戶：" + fromId
                );

        // ===== 第九步：執行存款 =====

        depositService.deposit(depositRequest);

        // ===== 第十步：回傳結果 =====

        // 成功返回後由Spring交易管理器處理Commit
        return new TransferResponse(
            fromId,
            toId,
            amount,
            "SUCCESS"
        );
    }
}
