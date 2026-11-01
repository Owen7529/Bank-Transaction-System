/**
 * WithdrawalService
 * 處理提款金額上限、帳戶餘額與狀態檢查、重送判定及資料庫交易。
 */

package com.example.bank.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank.dto.WithdrawalRequest;
import com.example.bank.dto.WithdrawalResponse;

import com.example.bank.entity.Account;
import com.example.bank.entity.AccountEntry;
import com.example.bank.entity.BankTransaction;
import com.example.bank.entity.TransactionRequest;

import com.example.bank.enums.AccountStatus;
import com.example.bank.enums.CustomerStatus;
import com.example.bank.enums.RequestStatus;
import com.example.bank.enums.TransactionType;

import com.example.bank.exception.BusinessException;
import com.example.bank.exception.ResourceNotFoundException;

import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.AccountEntryRepository;
import com.example.bank.repository.BankTransactionRepository;
import com.example.bank.repository.TransactionRequestRepository;

/**
 * 功能說明：提款服務
 *
 * 主要功能：
 * 1. 驗證提款請求
 * 2. 防止重複扣款
 * 3. 檢查帳戶狀態及餘額
 * 4. 執行提款交易
 * 5. 建立交易紀錄
 * 6. 發生例外時自動回滾
 */
@Service
public class WithdrawalService {

    // 模擬系統單筆提款上限：新台幣50,000元
    private static final BigDecimal MAX_WITHDRAWAL =
            new BigDecimal("50000.00");

    private final AccountRepository accountRepository;

    private final TransactionRequestRepository requestRepository;

    private final BankTransactionRepository transactionRepository;

    private final AccountEntryRepository entryRepository;

    /**
     * 建構子注入 Repository。
     */
    public WithdrawalService(
            AccountRepository accountRepository,
            TransactionRequestRepository requestRepository,
            BankTransactionRepository transactionRepository,
            AccountEntryRepository entryRepository) {

        this.accountRepository = accountRepository;
        this.requestRepository = requestRepository;
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
    }

    /**
     * 執行提款。
     *
     * @Transactional：
     * 將餘額修改及交易紀錄保存放在同一個資料庫交易。
     *
     * READ_COMMITTED：
     * 只能讀取其他交易已提交的資料。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WithdrawalResponse withdraw(
            WithdrawalRequest request) {

        // ===== 第一步：檢查請求內容 =====

        if (request == null
                || request.accountId() == null
                || request.accountId() <= 0) {
            throw new BusinessException("帳戶ID不正確");
        }

        if (request.requestKey() == null
                || request.requestKey().isBlank()) {
            throw new BusinessException("請求識別碼不可為空");
        }

        String requestKey = request.requestKey().trim();

        if (requestKey.length() > 100) {
            throw new BusinessException("請求識別碼過長");
        }

        if (request.amount() == null
                || request.amount().signum() <= 0) {
            throw new BusinessException("提款金額必須大於0");
        }

        // 統一金額格式，例如3000轉成3000.00
        BigDecimal amount;

        try {
            amount = request.amount().setScale(
                    2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new BusinessException(
                    "提款金額最多只能有兩位小數");
        }

        String description =
                normalizeDescription(request.description());

        // ===== 第二步：產生請求內容雜湊 =====

        String requestHash = calculateHash(
                request.accountId(),
                amount,
                description
        );

        // ===== 第三步：檢查是否已執行過 =====

        Optional<TransactionRequest> existing =
                requestRepository.findByRequestKey(requestKey);

        if (existing.isPresent()) {
            return replay(existing.get(), requestHash);
        }

        // ===== 第四步：鎖定帳戶資料 =====

        // 使用 AccountRepository 提供的悲觀鎖查詢，降低並行更新衝突
        Account account = accountRepository
                .findByIdForUpdate(request.accountId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "找不到指定帳戶"));

        // 鎖定後再次確認，處理同時送出的請求
        existing =
                requestRepository.findByRequestKey(requestKey);

        if (existing.isPresent()) {
            return replay(existing.get(), requestHash);
        }

        // ===== 第五步：驗證帳戶狀態 =====

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(
                    "帳戶不是啟用狀態，無法提款");
        }

        if (account.getCustomer().getStatus()
                != CustomerStatus.ACTIVE) {
            throw new BusinessException(
                    "客戶不是啟用狀態，無法提款");
        }

        if (!"TWD".equals(account.getCurrency())) {
            throw new BusinessException(
                    "目前只支援新台幣提款");
        }

        // ===== 第六步：驗證提款限額 =====

        if (amount.compareTo(MAX_WITHDRAWAL) > 0) {
            throw new BusinessException(
                    "單筆提款不可超過50000元");
        }

        // ===== 第七步：驗證餘額是否足夠 =====

        BigDecimal oldBalance = account.getBalance();

        if (oldBalance.compareTo(amount) < 0) {
            throw new BusinessException(
                    "帳戶餘額不足，無法提款");
        }

        // ===== 第八步：計算新餘額 =====

        BigDecimal newBalance =
                oldBalance.subtract(amount);

        // ===== 第九步：新增請求紀錄 =====

        TransactionRequest transactionRequest =
                new TransactionRequest(
                        requestKey,
                        requestHash
                );

        transactionRequest =
                requestRepository.saveAndFlush(
                        transactionRequest);

        // ===== 第十步：新增交易主紀錄 =====

        String transactionNumber =
                "TX" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .toUpperCase(Locale.ROOT);

        BankTransaction bankTransaction =
                new BankTransaction(
                        transactionNumber,
                        transactionRequest,
                        TransactionType.WITHDRAWAL,
                        amount,
                        description
                );

        bankTransaction =
                transactionRepository.saveAndFlush(
                        bankTransaction);

        // ===== 第十一步：實際扣除餘額 =====

        // 呼叫 Account 實體提供的餘額更新方法
        account.updateBalance(newBalance);

        accountRepository.save(account);

        // ===== 第十二步：建立帳戶異動明細 =====

        // 提款的異動金額必須是負數
        BigDecimal amountChange = amount.negate();

        AccountEntry entry =
                new AccountEntry(
                        bankTransaction,
                        account,
                        amountChange,
                        newBalance
                );

        entryRepository.save(entry);

        // ===== 第十三步：標記請求完成 =====

        transactionRequest.setStatus(
                RequestStatus.COMPLETED);

        requestRepository.save(transactionRequest);

        // 主動將待寫入的變更送往資料庫
        entryRepository.flush();
        accountRepository.flush();
        requestRepository.flush();

        // ===== 第十四步：回傳交易結果 =====

        // 不在此處手動commit；
        // 由Spring在@Transactional方法成功結束後提交。
        return new WithdrawalResponse(
                bankTransaction.getId(),
                bankTransaction.getTransactionNumber(),
                account.getId(),
                account.getAccountNumber(),
                amount,
                newBalance,
                String.valueOf(bankTransaction.getStatus()),
                bankTransaction.getCreatedAt(),
                false
        );
    }

    /**
     * 相同requestKey再次送出時，回傳原有交易。
     */
    private WithdrawalResponse replay(
            TransactionRequest previous,
            String requestHash) {

        // 相同key不得用於不同內容
        if (!previous.getRequestHash().equals(requestHash)) {
            throw new BusinessException(
                    "requestKey已用於不同交易內容");
        }

        if (previous.getStatus() != RequestStatus.COMPLETED) {
            throw new BusinessException(
                    "此請求尚未完成，請勿重複提款");
        }

        BankTransaction transaction =
                transactionRepository
                        .findByRequest_Id(previous.getId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "請求缺少交易主紀錄"));

        if (transaction.getType()
                != TransactionType.WITHDRAWAL) {
            throw new BusinessException(
                    "此請求識別碼不是提款交易");
        }

        return buildResponse(transaction, true);
    }

    /**
     * 使用交易ID查詢提款紀錄。
     */
    @Transactional(readOnly = true)
    public WithdrawalResponse getWithdrawal(
            Long transactionId) {

        BankTransaction transaction =
                transactionRepository.findById(transactionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "找不到指定交易"));

        if (transaction.getType()
                != TransactionType.WITHDRAWAL) {
            throw new ResourceNotFoundException(
                    "此交易不是提款交易");
        }

        return buildResponse(transaction, false);
    }

    /**
     * 從交易及帳戶異動紀錄建立回應。
     */
    private WithdrawalResponse buildResponse(
            BankTransaction transaction,
            boolean replayed) {

        List<AccountEntry> entries =
                entryRepository.findByTransaction_Id(
                        transaction.getId());

        // 提款只能有一筆帳戶異動
        if (entries.size() != 1) {
            throw new IllegalStateException(
                    "提款交易明細數量不正確");
        }

        AccountEntry entry = entries.get(0);

        // 提款的異動金額應為負數
        if (entry.getAmountChange().signum() >= 0) {
            throw new IllegalStateException(
                    "提款交易異動金額不正確");
        }

        return new WithdrawalResponse(
                transaction.getId(),
                transaction.getTransactionNumber(),
                entry.getAccount().getId(),
                entry.getAccount().getAccountNumber(),
                transaction.getAmount(),
                entry.getBalanceAfter(),
                String.valueOf(transaction.getStatus()),
                transaction.getCreatedAt(),
                replayed
        );
    }

    /**
     * 統一備註內容，避免雜湊不一致。
     */
    private String normalizeDescription(
            String description) {

        if (description == null) {
            return "";
        }

        String normalized = description.trim();

        if (normalized.length() > 255) {
            throw new BusinessException(
                    "交易備註不可超過255字");
        }

        return normalized;
    }

    /**
     * 計算請求內容的SHA-256。
     *
     * 加入WITHDRAWAL字樣，
     * 避免與存款交易使用相同識別內容。
     */
    private String calculateHash(
            Long accountId,
            BigDecimal amount,
            String description) {

        String source =
                "WITHDRAWAL\u0000"
                + accountId
                + "\u0000"
                + amount.toPlainString()
                + "\u0000"
                + description;

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    source.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "無法產生請求雜湊", ex);
        }
    }
}
