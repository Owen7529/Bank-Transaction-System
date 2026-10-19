package com.example.bank.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank.dto.CreateCustomerRequest;
import com.example.bank.dto.CustomerResponse;
import com.example.bank.entity.Customer;
import com.example.bank.enums.AccountStatus;
import com.example.bank.enums.CustomerStatus;
import com.example.bank.exception.BusinessException;
import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.CustomerRepository;

/**
 * 客戶管理業務邏輯。
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    /**
     * 建構子注入 Repository。
     */
    public CustomerService(
            CustomerRepository customerRepository,
            AccountRepository accountRepository) {

        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * 新增客戶。
     */
    @Transactional
    public CustomerResponse createCustomer(
            CreateCustomerRequest request) {

        String code = request.customerCode().trim();
        String name = request.name().trim();

        // 先檢查客戶代碼是否重複
        if (customerRepository.existsByCustomerCode(code)) {
            throw new BusinessException("客戶代碼已存在");
        }

        // 建立 Entity
        Customer customer = new Customer(
                code,
                name,
                request.email()
        );

        // 儲存並取得主鍵、建立時間
        Customer saved =
                customerRepository.saveAndFlush(customer);

        return CustomerResponse.from(saved);
    }

    /**
     * 查詢單一客戶。
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long id) {

        Customer customer = findCustomer(id);

        return CustomerResponse.from(customer);
    }

    /**
     * 查詢所有客戶。
     *
     * 本課先使用簡單查詢，
     * 未來可加入分頁。
     */
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {

        return customerRepository.findAll()
                .stream()
                .map(CustomerResponse::from)
                .toList();
    }

    /**
     * 修改客戶狀態。
     */
    @Transactional
    public CustomerResponse updateStatus(
            Long id,
            CustomerStatus newStatus) {

        Customer customer = findCustomer(id);

        // 停用客戶前，確認沒有 ACTIVE 帳戶
        if (newStatus == CustomerStatus.INACTIVE) {

            boolean hasActiveAccount =
                    accountRepository
                            .findByCustomer_Id(id)
                            .stream()
                            .anyMatch(account ->
                                    account.getStatus()
                                            == AccountStatus.ACTIVE
                            );

            if (hasActiveAccount) {
                throw new BusinessException(
                        "客戶仍有正常使用中的帳戶，無法停用"
                );
            }
        }

        customer.setStatus(newStatus);

        customerRepository.flush();

        return CustomerResponse.from(customer);
    }

    /**
     * 共用的客戶查詢方法。
     */
    private Customer findCustomer(Long id) {

        return customerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "找不到客戶，ID：" + id
                        )
                );
    }
}