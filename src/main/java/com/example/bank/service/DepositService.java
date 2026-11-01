/**
 * DepositService
 * 處理存款輸入驗證、請求重送判定、餘額調整及交易與明細記錄。
 */

package com.example.bank.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank.dto.DepositRequest;
import com.example.bank.dto.DepositResponse;
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

@Service
public class DepositService {

    // DECIMAL(19,2) 可儲存的最大正數
    private static final BigDecimal MAX_BALANCE =
        new BigDecimal("99999999999999999.99");

    private final AccountRepository accountRepository;

    private final TransactionRequestRepository requestRepository;

    private final BankTransactionRepository transactionRepository;

    private final AccountEntryRepository entryRepository;

    // 使用建構子注入 Repository
    public DepositService(
            AccountRepository accountRepository,
            TransactionRequestRepository requestRepository,
            BankTransactionRepository transactionRepository,
            AccountEntryRepository entryRepository) {

        this.accountRepository = accountRepository;
        this.requestRepository = requestRepository;
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
    }

    // 執行存款
    // @Transactional：所有資料庫異動使用同一交易
    // READ_COMMITTED：重新查詢時可看到其他交易已提交的資料
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DepositResponse deposit(DepositRequest request) {

        // 1. 基本資料檢查
        if (request == null ||
            request.accountId() == null ||
            request.accountId() <= 0 ||
            request.requestKey() == null ||
            request.requestKey().isBlank() ||
            request.requestKey().length() > 100 ||
            request.amount() == null ||
            request.amount().signum() <= 0 ||
            request.amount().scale() > 2 ||
            request.amount().compareTo(MAX_BALANCE) > 0 ||
            (request.description() != null &&
             request.description().length() > 255)) {

            throw new BusinessException("存款請求資料不合法");
        }

        // 2. 將金額統一為小數點後兩位
        BigDecimal amount = request.amount()
                .setScale(2, RoundingMode.UNNECESSARY);

        String requestKey = request.requestKey().trim();

        String description =
                normalizeDescription(request.description());

        // 3. 計算請求內容的 SHA-256
        String requestHash = calculateHash(
                request.accountId(),
                amount,
                description
        );

        // 4. 如果以前已完成相同請求，直接回傳結果
        var existing = requestRepository.findByRequestKey(requestKey);

        if (existing.isPresent()) {
            return replay(existing.get(), requestHash);
        }

        // 5. 取得帳戶，並鎖定帳戶資料列
        Account account = accountRepository
                .findByIdForUpdate(request.accountId())
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到指定帳戶"
                    )
                );

        // 6. 取得鎖定後再次查詢
        // 避免同一帳戶的並發請求造成重複執行
        existing = requestRepository.findByRequestKey(requestKey);

        if (existing.isPresent()) {
            return replay(existing.get(), requestHash);
        }

        // 7. 確認帳戶狀態
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("帳戶目前不是啟用狀態");
        }

        // 8. 確認客戶狀態
        if (account.getCustomer().getStatus()
                != CustomerStatus.ACTIVE) {

            throw new BusinessException("客戶目前不是啟用狀態");
        }

        // 9. 本專案目前只處理台幣
        if (!"TWD".equals(account.getCurrency())) {
            throw new BusinessException("目前只支援台幣存款");
        }

        // 10. 計算存款後餘額
        BigDecimal newBalance =
                account.getBalance().add(amount);

        if (newBalance.compareTo(MAX_BALANCE) > 0) {
            throw new BusinessException("存款後餘額超過上限");
        }

        // 11. 儲存請求識別碼
        // 資料庫的 UNIQUE 約束可避免相同 key 重複建立
        TransactionRequest transactionRequest =
                new TransactionRequest(
                    requestKey,
                    requestHash
                );

        transactionRequest =
                requestRepository.saveAndFlush(transactionRequest);

        // 12. 建立不重複的交易編號
        String transactionNumber =
                "TX" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .toUpperCase();

        // 13. 建立銀行交易主紀錄
        BankTransaction bankTransaction =
                new BankTransaction(
                    transactionNumber,
                    transactionRequest,
                    TransactionType.DEPOSIT,
                    amount,
                    description
                );

        bankTransaction =
                transactionRepository.save(bankTransaction);

        // 14. 更新帳戶餘額
        // 使用 Account.updateBalance() 更新帳戶餘額
        account.updateBalance(newBalance);

        // 15. 建立帳戶異動明細
        // 存款使用正數表示增加
        AccountEntry accountEntry =
                new AccountEntry(
                    bankTransaction,
                    account,
                    amount,
                    newBalance
                );

        entryRepository.save(accountEntry);

        // 16. 將請求狀態改成已完成
        transactionRequest.setStatus(RequestStatus.COMPLETED);

        // 17. 將待寫入的異動同步到資料庫
        accountRepository.saveAndFlush(account);
        entryRepository.flush();

        // 18. 準備成功回應
        // 方法正常結束後由 Spring 進行 Commit
        return new DepositResponse(
                bankTransaction.getId(),
                bankTransaction.getTransactionNumber(),
                account.getId(),
                account.getAccountNumber(),
                amount,
                newBalance,
                bankTransaction.getStatus(),
                bankTransaction.getCreatedAt(),
                false
        );
    }

    // 重複請求處理
    private DepositResponse replay(
            TransactionRequest previous,
            String currentHash) {

        // 相同 requestKey 不允許搭配不同內容
        if (!previous.getRequestHash().equals(currentHash)) {
            throw new BusinessException(
                "請求識別碼已被不同交易內容使用"
            );
        }

        if (previous.getStatus() != RequestStatus.COMPLETED) {
            throw new BusinessException(
                "此請求尚未完成，請稍後重新查詢"
            );
        }

        // 取得原本已完成的交易
        BankTransaction transaction = transactionRepository
                .findByRequest_Id(previous.getId())
                .orElseThrow(() ->
                    new IllegalStateException(
                        "交易資料不一致：找不到交易紀錄"
                    )
                );

        return buildResponse(transaction, true);
    }

    // 查詢指定的存款交易
    @Transactional(readOnly = true)
    public DepositResponse getDeposit(Long transactionId) {

        BankTransaction transaction = transactionRepository
                .findById(transactionId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "找不到交易紀錄"
                    )
                );

        return buildResponse(transaction, false);
    }

    // 統一建立查詢或重送的回應
    private DepositResponse buildResponse(
            BankTransaction transaction,
            boolean replayed) {

        if (transaction.getType() != TransactionType.DEPOSIT) {
            throw new BusinessException("指定交易不是存款交易");
        }

        List<AccountEntry> entries = entryRepository
                .findByTransaction_Id(transaction.getId());

        // 存款應該剛好有一筆帳戶異動
        if (entries.size() != 1) {
            throw new IllegalStateException(
                "交易資料不一致：帳戶異動筆數異常"
            );
        }

        AccountEntry entry = entries.get(0);

        return new DepositResponse(
                transaction.getId(),
                transaction.getTransactionNumber(),
                entry.getAccount().getId(),
                entry.getAccount().getAccountNumber(),
                transaction.getAmount(),
                entry.getBalanceAfter(),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                replayed
        );
    }

    // 備註標準化
    private String normalizeDescription(String description) {
        return description == null ? "" : description.strip();
    }

    // 產生交易內容的 SHA-256 雜湊
    private String calculateHash(
            Long accountId,
            BigDecimal amount,
            String description) {

        try {
            // 使用固定分隔符，避免不同欄位值碰巧組合相同
            String content =
                    "DEPOSIT\u0000" +
                    accountId + "\u0000" +
                    amount.toPlainString() + "\u0000" +
                    description;

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                content.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 演算法無法使用",
                exception
            );
        }
    }
}
