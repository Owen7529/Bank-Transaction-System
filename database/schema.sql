-- 銀行資料庫五張核心資料表：客戶、帳戶、請求、交易主檔與帳戶異動明細。

-- ============================================
-- Bank Account Transaction System
-- MySQL 8.x
-- Database: bank_transaction_db
-- ============================================

USE bank_transaction_db;

-- ============================================
-- 1. 客戶資料表
-- ============================================

CREATE TABLE IF NOT EXISTS customers (

    id BIGINT NOT NULL AUTO_INCREMENT,

    customer_code VARCHAR(30) NOT NULL,

    name VARCHAR(100) NOT NULL,

    email VARCHAR(150) NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_customers
        PRIMARY KEY (id),

    CONSTRAINT uk_customers_code
        UNIQUE (customer_code),

    CONSTRAINT chk_customers_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))

) ENGINE=InnoDB;


-- ============================================
-- 2. 銀行帳戶資料表
-- ============================================

CREATE TABLE IF NOT EXISTS accounts (

    id BIGINT NOT NULL AUTO_INCREMENT,

    customer_id BIGINT NOT NULL,

    account_number VARCHAR(20) NOT NULL,

    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,

    currency CHAR(3) NOT NULL DEFAULT 'TWD',

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    version BIGINT NOT NULL DEFAULT 0,

    created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_accounts
        PRIMARY KEY (id),

    CONSTRAINT uk_accounts_number
        UNIQUE (account_number),

    CONSTRAINT fk_accounts_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT chk_accounts_balance
        CHECK (balance >= 0),

    CONSTRAINT chk_accounts_currency
        CHECK (currency = 'TWD'),

    CONSTRAINT chk_accounts_status
        CHECK (status IN ('ACTIVE', 'INACTIVE')),

    CONSTRAINT chk_accounts_version
        CHECK (version >= 0),

    INDEX idx_accounts_customer (customer_id)

) ENGINE=InnoDB;


-- ============================================
-- 3. 交易請求紀錄表
-- ============================================

CREATE TABLE IF NOT EXISTS transaction_requests (

    id BIGINT NOT NULL AUTO_INCREMENT,

    request_key VARCHAR(100) NOT NULL,

    request_hash CHAR(64) NOT NULL,

    status VARCHAR(20) NOT NULL,

    created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_transaction_requests
        PRIMARY KEY (id),

    CONSTRAINT uk_transaction_requests_key
        UNIQUE (request_key),

    CONSTRAINT chk_transaction_requests_status
        CHECK (
            status IN (
                'PROCESSING',
                'COMPLETED',
                'FAILED'
            )
        )

) ENGINE=InnoDB;


-- ============================================
-- 4. 銀行交易主紀錄表
-- ============================================

CREATE TABLE IF NOT EXISTS bank_transactions (

    id BIGINT NOT NULL AUTO_INCREMENT,

    transaction_number VARCHAR(40) NOT NULL,

    request_id BIGINT NOT NULL,

    type VARCHAR(20) NOT NULL,

    amount DECIMAL(19,2) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',

    description VARCHAR(255) NULL,

    created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_bank_transactions
        PRIMARY KEY (id),

    CONSTRAINT uk_bank_transactions_number
        UNIQUE (transaction_number),

    CONSTRAINT uk_bank_transactions_request
        UNIQUE (request_id),

    CONSTRAINT fk_bank_transactions_request
        FOREIGN KEY (request_id)
        REFERENCES transaction_requests(id),

    CONSTRAINT chk_bank_transactions_type
        CHECK (
            type IN (
                'DEPOSIT',
                'WITHDRAWAL',
                'TRANSFER'
            )
        ),

    CONSTRAINT chk_bank_transactions_amount
        CHECK (amount > 0),

    CONSTRAINT chk_bank_transactions_status
        CHECK (status = 'COMPLETED'),

    INDEX idx_bank_transactions_created
        (created_at)

) ENGINE=InnoDB;


-- ============================================
-- 5. 帳戶異動明細表
-- ============================================

CREATE TABLE IF NOT EXISTS account_entries (

    id BIGINT NOT NULL AUTO_INCREMENT,

    transaction_id BIGINT NOT NULL,

    account_id BIGINT NOT NULL,

    amount_change DECIMAL(19,2) NOT NULL,

    balance_after DECIMAL(19,2) NOT NULL,

    created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_account_entries
        PRIMARY KEY (id),

    CONSTRAINT fk_entries_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES bank_transactions(id),

    CONSTRAINT fk_entries_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id),

    CONSTRAINT chk_entries_amount_change
        CHECK (amount_change <> 0),

    CONSTRAINT chk_entries_balance_after
        CHECK (balance_after >= 0),

    -- 同一筆交易對同一帳戶最多一筆異動
    CONSTRAINT uk_entries_transaction_account
        UNIQUE (transaction_id, account_id),

    INDEX idx_entries_account_created
        (account_id, created_at)

) ENGINE=InnoDB;


-- ============================================
-- 6. 檢查資料表
-- ============================================

SHOW TABLES;
