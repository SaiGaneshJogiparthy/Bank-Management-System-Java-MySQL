-- Bank Management System - Full Setup Script
-- Run this entire script in MySQL Workbench

CREATE DATABASE IF NOT EXISTS bankdb;
USE bankdb;

DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS login_history;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS admins;

CREATE TABLE admins (
    admin_id        INT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE customers (
    customer_id     INT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    transaction_pin VARCHAR(255) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(15),
    address         VARCHAR(255),
    failed_logins   INT DEFAULT 0,
    is_locked       BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
    account_id      INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT NOT NULL,
    account_number  VARCHAR(20) NOT NULL UNIQUE,
    account_type    ENUM('SAVINGS', 'CURRENT') DEFAULT 'SAVINGS',
    balance         DECIMAL(15, 2) DEFAULT 0.00,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

CREATE TABLE transactions (
    transaction_id   INT AUTO_INCREMENT PRIMARY KEY,
    account_id       INT NOT NULL,
    transaction_type ENUM('DEPOSIT', 'WITHDRAW', 'TRANSFER_IN', 'TRANSFER_OUT') NOT NULL,
    amount           DECIMAL(15, 2) NOT NULL,
    balance_after    DECIMAL(15, 2) NOT NULL,
    description      VARCHAR(255),
    related_account  VARCHAR(20),
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE
);

CREATE TABLE login_history (
    history_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_type    ENUM('CUSTOMER', 'ADMIN') NOT NULL,
    user_id      INT NOT NULL,
    username     VARCHAR(50) NOT NULL,
    login_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status       ENUM('SUCCESS', 'FAILED') NOT NULL,
    ip_address   VARCHAR(45) DEFAULT '127.0.0.1'
);

CREATE TABLE audit_logs (
    log_id       INT AUTO_INCREMENT PRIMARY KEY,
    user_type    ENUM('CUSTOMER', 'ADMIN') NOT NULL,
    user_id      INT,
    username     VARCHAR(50),
    action       VARCHAR(100) NOT NULL,
    details      TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Default admin: username=admin, password=admin123
INSERT INTO admins (username, password_hash, full_name) VALUES
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'System Administrator');

SELECT 'Database setup complete!' AS Status;
