/**
 * TransactionHistoryServiceTest
 * 測試交易紀錄服務的查詢、分頁及錯誤處理。
 */

package com.example.bank.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.bank.dto.TransactionHistoryPageResponse;
import com.example.bank.entity.AccountEntry;
import com.example.bank.exception.BusinessException;
import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.repository.AccountEntryRepository;
import com.example.bank.repository.AccountRepository;

/**
 * 功能說明：交易紀錄服務單元測試。
 *
 * 使用Mockito模擬Repository，
 * 不依賴真實MySQL資料。
 */
@ExtendWith(MockitoExtension.class)
class TransactionHistoryServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountEntryRepository entryRepository;

    @InjectMocks
    private TransactionHistoryService service;

    // 測試1：負數頁碼應失敗
    @Test
    void negativePageShouldFail() {

        assertThrows(
                BusinessException.class,
                () -> service.getHistory(1L, -1, 10)
        );
    }

    // 測試2：超過每頁上限應失敗
    @Test
    void excessivePageSizeShouldFail() {

        assertThrows(
                BusinessException.class,
                () -> service.getHistory(1L, 0, 100)
        );
    }

    // 測試3：不存在的帳戶應失敗
    @Test
    void nonexistentAccountShouldFail() {

        when(accountRepository.existsById(99L))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getHistory(99L, 0, 10)
        );
    }

    // 測試4：沒有交易紀錄時回傳空清單
    @Test
    void emptyHistoryShouldReturnEmptyPage() {

        when(accountRepository.existsById(1L))
                .thenReturn(true);

        Page<AccountEntry> emptyPage =
                Page.empty(PageRequest.of(0, 10));

        when(entryRepository.findByAccount_Id(
                eq(1L),
                any(Pageable.class)
        )).thenReturn(emptyPage);

        TransactionHistoryPageResponse response =
                service.getHistory(1L, 0, 10);

        assertTrue(response.items().isEmpty());

        assertEquals(0, response.totalElements());

        assertEquals(0, response.totalPages());

        assertFalse(response.hasNext());
    }

    // 測試5：查詢必須依ID遞減排序
    @Test
    void historyShouldSortByIdDescending() {

        when(accountRepository.existsById(1L))
                .thenReturn(true);

        when(entryRepository.findByAccount_Id(
                eq(1L),
                any(Pageable.class)
        )).thenReturn(
                Page.empty(PageRequest.of(0, 10))
        );

        service.getHistory(1L, 0, 10);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(entryRepository).findByAccount_Id(
                eq(1L),
                captor.capture()
        );

        Sort.Order order = captor.getValue()
                .getSort()
                .getOrderFor("id");

        assertNotNull(order);

        assertEquals(
                Sort.Direction.DESC,
                order.getDirection()
        );
    }

    // 測試6：指定交易沒有帳戶明細時應失敗
    @Test
    void missingTransactionShouldFail() {

        when(accountRepository.existsById(1L))
                .thenReturn(true);

        when(entryRepository.findByTransaction_Id(999L))
                .thenReturn(List.of());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getTransaction(1L, 999L)
        );
    }
}
