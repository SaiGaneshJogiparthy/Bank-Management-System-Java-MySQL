package bank.service;

import bank.dao.*;
import bank.exception.InvalidLoginException;
import bank.model.Account;
import bank.model.Admin;
import bank.model.Customer;
import bank.model.Transaction;
import bank.security.PasswordUtil;

import java.sql.SQLException;
import java.util.List;

public class AdminService {

    private final AdminDAO adminDAO = new AdminDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final LoginHistoryDAO loginHistoryDAO = new LoginHistoryDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public Admin login(String username, String password) throws SQLException, InvalidLoginException {
        Admin admin = adminDAO.findByUsername(username)
                .orElseThrow(() -> new InvalidLoginException("Invalid username or password."));

        if (!PasswordUtil.verify(password, admin.getPasswordHash())) {
            loginHistoryDAO.log("ADMIN", admin.getId(), admin.getUsername(), "FAILED");
            throw new InvalidLoginException("Invalid username or password.");
        }

        loginHistoryDAO.log("ADMIN", admin.getId(), admin.getUsername(), "SUCCESS");
        return admin;
    }

    public List<Customer> getAllCustomers() throws SQLException {
        return customerDAO.findAll();
    }

    public List<Customer> searchCustomer(String keyword) throws SQLException {
        return customerDAO.searchByName(keyword);
    }

    public List<Account> getAllAccounts() throws SQLException {
        return accountDAO.findAll();
    }

    public List<Account> searchAccount(String keyword) throws SQLException {
        return accountDAO.searchByAccountNumber(keyword);
    }

    public List<Transaction> getAllTransactions() throws SQLException {
        return transactionDAO.findAll();
    }

    public void unlockAccount(int customerId, String adminUsername) throws SQLException {
        customerDAO.unlockAccount(customerId);
        auditLogDAO.log("ADMIN", null, adminUsername, "UNLOCK_ACCOUNT",
                "Unlocked customer ID: " + customerId);
    }

    public void deleteAccount(int accountId, String adminUsername) throws SQLException, InvalidLoginException {
        Account account = accountDAO.findById(accountId)
                .orElseThrow(() -> new InvalidLoginException("Account not found."));
        accountDAO.deleteAccount(accountId);
        auditLogDAO.log("ADMIN", null, adminUsername, "DELETE_ACCOUNT",
                "Deleted account " + account.getAccountNumber());
    }

    public void showLoginHistory() throws SQLException {
        loginHistoryDAO.printAll();
    }

    public void showAuditLogs() throws SQLException {
        auditLogDAO.printAll();
    }
}
