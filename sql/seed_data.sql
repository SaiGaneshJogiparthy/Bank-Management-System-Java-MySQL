-- Restore sample customer data
USE bankdb;

DELETE FROM transactions WHERE account_id IN (SELECT account_id FROM accounts WHERE customer_id IN (SELECT customer_id FROM customers WHERE username = 'sai'));
DELETE FROM accounts WHERE customer_id IN (SELECT customer_id FROM customers WHERE username = 'sai');
DELETE FROM login_history WHERE username = 'sai';
DELETE FROM audit_logs WHERE username = 'sai';
DELETE FROM customers WHERE username = 'sai';

INSERT INTO customers (username, email, password_hash, transaction_pin, full_name, phone, address, failed_logins, is_locked)
VALUES (
    'sai',
    'sai@gmail.com',
    '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
    '0da728f5d368de45514b6d911e43a8af8c3d03e627707160cf6b70242e9dd5f2',
    'sai',
    '8888888888',
    'hh',
    0,
    FALSE
);

SET @sai_id = LAST_INSERT_ID();

INSERT INTO accounts (customer_id, account_number, account_type, balance, is_active)
VALUES (@sai_id, '1001234567', 'SAVINGS', 5000.00, TRUE);

SET @account_id = LAST_INSERT_ID();

INSERT INTO transactions (account_id, transaction_type, amount, balance_after, description) VALUES
(@account_id, 'DEPOSIT',  5000.00, 5000.00, 'Initial deposit'),
(@account_id, 'WITHDRAW',  500.00, 4500.00, 'ATM withdrawal'),
(@account_id, 'DEPOSIT',  1000.00, 5500.00, 'Cash deposit'),
(@account_id, 'WITHDRAW',  500.00, 5000.00, 'Payment');

SELECT 'Sample data restored!' AS Status;
