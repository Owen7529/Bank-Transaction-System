-- 本機資料庫初始化操作；執行前應確認 MySQL 權限與目標 Schema。

-- ============================================
-- 銀行帳戶交易管理系統
-- 資料庫與使用者初始化
-- ============================================

-- 1. 建立資料庫
CREATE DATABASE IF NOT EXISTS bank_transaction_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- 2. 設定本機測試使用者（目前應用程式使用 root 帳號）
-- 執行前請將下列密碼範例替換為安全密碼；正式環境應建立獨立且權限受限的帳號。
CREATE USER IF NOT EXISTS 'root'@'localhost'
IDENTIFIED BY 'REPLACE_WITH_SECURE_PASSWORD';

-- 3. 授權應用程式使用資料庫
-- 應用程式的資料操作僅需查詢、新增、修改與刪除權限
GRANT SELECT, INSERT, UPDATE, DELETE
ON bank_transaction_db.*
TO 'root'@'localhost';

-- 4. 檢查資料庫是否存在
SHOW DATABASES LIKE 'bank_transaction_db';

-- 5. 查看使用者權限
SHOW GRANTS FOR 'root'@'localhost';
