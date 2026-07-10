package bank.service;

import bank.dao.AuditLogDAO;
import bank.dao.CustomerDAO;
import bank.dao.LoginHistoryDAO;
import bank.exception.AccountLockedException;
import bank.exception.InvalidLoginException;
import bank.model.Customer;
import bank.security.PasswordUtil;

import java.sql.SQLException;

public class AuthService {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final LoginHistoryDAO loginHistoryDAO = new LoginHistoryDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private static final int MAX_FAILED_LOGINS = 3;
    private static final String DEFAULT_TRANSACTION_PIN = "000000";

    public Customer loginCustomer(String login, String password) throws SQLException,
            InvalidLoginException, AccountLockedException {
        Customer customer = customerDAO.findByUsernameOrEmail(login)
                .orElseThrow(() -> new InvalidLoginException("Invalid username/email or password."));

        if (customer.isLocked()) {
            loginHistoryDAO.log("CUSTOMER", customer.getId(), customer.getUsername(), "FAILED");
            throw new AccountLockedException("Account is locked. Contact admin to unlock.");
        }

        if (!PasswordUtil.verify(password, customer.getPasswordHash())) {
            int failed = customer.getFailedLogins() + 1;
            boolean locked = failed >= MAX_FAILED_LOGINS;
            customerDAO.updateFailedLogins(customer.getId(), failed, locked);
            loginHistoryDAO.log("CUSTOMER", customer.getId(), customer.getUsername(), "FAILED");
            if (locked) {
                auditLogDAO.log("CUSTOMER", customer.getId(), customer.getUsername(),
                        "ACCOUNT_LOCKED", "Locked after " + MAX_FAILED_LOGINS + " failed logins");
                throw new AccountLockedException("Account locked after 3 failed attempts. Contact admin.");
            }
            throw new InvalidLoginException("Invalid username/email or password. Attempts left: "
                    + (MAX_FAILED_LOGINS - failed));
        }

        customerDAO.resetFailedLogins(customer.getId());
        loginHistoryDAO.log("CUSTOMER", customer.getId(), customer.getUsername(), "SUCCESS");
        return customer;
    }

    public void registerCustomer(Customer customer) throws SQLException, InvalidLoginException {
        if (!PasswordUtil.isValidLength(customer.getPasswordHash())) {
            throw new InvalidLoginException(PasswordUtil.lengthRequirementMessage());
        }
        customer.setPasswordHash(PasswordUtil.hash(customer.getPasswordHash()));
        customer.setTransactionPin(PasswordUtil.hash(DEFAULT_TRANSACTION_PIN));
        if (customer.getFullName() == null || customer.getFullName().isBlank()) {
            customer.setFullName(customer.getUsername());
        }
        if (customer.getAddress() == null) {
            customer.setAddress("");
        }
        customerDAO.register(customer);
        auditLogDAO.log("CUSTOMER", null, customer.getUsername(), "SIGNUP", "New customer registered");
    }

    public boolean changePassword(int customerId, String oldPassword, String newPassword) throws SQLException,
            InvalidLoginException {
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new InvalidLoginException("Customer not found."));
        if (!PasswordUtil.verify(oldPassword, customer.getPasswordHash())) {
            throw new InvalidLoginException("Current password is incorrect.");
        }
        if (!PasswordUtil.isValidLength(newPassword)) {
            throw new InvalidLoginException(PasswordUtil.lengthRequirementMessage());
        }
        boolean updated = customerDAO.updatePassword(customerId, PasswordUtil.hash(newPassword));
        if (updated) {
            auditLogDAO.log("CUSTOMER", customerId, customer.getUsername(), "CHANGE_PASSWORD", "Password updated");
        }
        return updated;
    }

    public boolean changeTransactionPin(int customerId, String oldPin, String newPin) throws SQLException,
            InvalidLoginException {
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new InvalidLoginException("Customer not found."));
        if (!PasswordUtil.verify(oldPin, customer.getTransactionPin())) {
            throw new InvalidLoginException("Current transaction PIN is incorrect.");
        }
        boolean updated = customerDAO.updateTransactionPin(customerId, PasswordUtil.hash(newPin));
        if (updated) {
            auditLogDAO.log("CUSTOMER", customerId, customer.getUsername(), "CHANGE_PIN", "Transaction PIN updated");
        }
        return updated;
    }

    public void verifyTransactionPin(int customerId, String pin) throws SQLException, InvalidLoginException {
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new InvalidLoginException("Customer not found."));
        if (!PasswordUtil.verify(pin, customer.getTransactionPin())) {
            throw new InvalidLoginException("Invalid transaction PIN.");
        }
    }
}
