
-- ============================================
-- 銀行帳戶交易管理系統
-- 資料庫與使用者初始化
-- ============================================

-- 1. 建立資料庫
CREATE DATABASE IF NOT EXISTS bank_transaction_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- 2. 建立專案專用的 MySQL 使用者
-- 請先將密碼替換為自己設定的強密碼
CREATE USER IF NOT EXISTS 'root'@'localhost'
IDENTIFIED BY 's123456S';

-- 3. 授權應用程式使用資料庫
-- 第一版執行期只需要資料讀寫權限
GRANT SELECT, INSERT, UPDATE, DELETE
ON bank_transaction_db.*
TO 'root'@'localhost';

-- 4. 檢查資料庫是否存在
SHOW DATABASES LIKE 'bank_transaction_db';

-- 5. 查看使用者權限
SHOW GRANTS FOR 'root'@'localhost';
