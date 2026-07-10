package bank.service;

import bank.dao.AccountDAO;
import bank.dao.AuditLogDAO;
import bank.dao.CustomerDAO;
import bank.dao.TransactionDAO;
import bank.exception.InsufficientBalanceException;
import bank.exception.InvalidLoginException;
import bank.model.Account;
import bank.model.Transaction;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

public class AccountService {

    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final AuthService authService = new AuthService();
    private final Random random = new Random();

    public Account createAccount(int customerId, String accountType) throws SQLException {
        Account account = new Account();
        account.setCustomerId(customerId);
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType(accountType.toUpperCase());
        account.setBalance(BigDecimal.ZERO);
        account.setActive(true);
        accountDAO.create(account);

        String username = customerDAO.findById(customerId).map(c -> c.getUsername()).orElse("unknown");
        auditLogDAO.log("CUSTOMER", customerId, username, "CREATE_ACCOUNT",
                "Account " + account.getAccountNumber() + " created");
        return account;
    }

    public void deposit(int accountId, BigDecimal amount, String pin, int customerId) throws SQLException,
            InvalidLoginException {
        validateAmount(amount);
        authService.verifyTransactionPin(customerId, pin);
        Account account = getOwnedAccount(accountId, customerId);

        BigDecimal newBalance = account.getBalance().add(amount);
        accountDAO.updateBalance(accountId, newBalance);
        recordTransaction(accountId, "DEPOSIT", amount, newBalance, "Cash deposit", null);

        String username = customerDAO.findById(customerId).map(c -> c.getUsername()).orElse("unknown");
        auditLogDAO.log("CUSTOMER", customerId, username, "DEPOSIT",
                "Deposited " + amount + " to " + account.getAccountNumber());
    }

    public void withdraw(int accountId, BigDecimal amount, String pin, int customerId) throws SQLException,
            InvalidLoginException, InsufficientBalanceException {
        validateAmount(amount);
        authService.verifyTransactionPin(customerId, pin);
        Account account = getOwnedAccount(accountId, customerId);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance. Available: " + account.getBalance());
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        accountDAO.updateBalance(accountId, newBalance);
        recordTransaction(accountId, "WITHDRAW", amount, newBalance, "Cash withdrawal", null);

        String username = customerDAO.findById(customerId).map(c -> c.getUsername()).orElse("unknown");
        auditLogDAO.log("CUSTOMER", customerId, username, "WITHDRAW",
                "Withdrew " + amount + " from " + account.getAccountNumber());
    }

    public void transfer(int fromAccountId, String toAccountNumber, BigDecimal amount, String pin, int customerId)
            throws SQLException, InvalidLoginException, InsufficientBalanceException {
        validateAmount(amount);
        authService.verifyTransactionPin(customerId, pin);

        Account fromAccount = getOwnedAccount(fromAccountId, customerId);
        Account toAccount = accountDAO.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new InvalidLoginException("Destination account not found."));

        if (!toAccount.isActive()) {
            throw new InvalidLoginException("Destination account is inactive.");
        }
        if (fromAccount.getAccountNumber().equals(toAccountNumber)) {
            throw new InvalidLoginException("Cannot transfer to the same account.");
        }
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance. Available: " + fromAccount.getBalance());
        }

        BigDecimal fromNewBalance = fromAccount.getBalance().subtract(amount);
        BigDecimal toNewBalance = toAccount.getBalance().add(amount);

        accountDAO.updateBalance(fromAccount.getAccountId(), fromNewBalance);
        accountDAO.updateBalance(toAccount.getAccountId(), toNewBalance);

        recordTransaction(fromAccount.getAccountId(), "TRANSFER_OUT", amount, fromNewBalance,
                "Transfer to " + toAccountNumber, toAccountNumber);
        recordTransaction(toAccount.getAccountId(), "TRANSFER_IN", amount, toNewBalance,
                "Transfer from " + fromAccount.getAccountNumber(), fromAccount.getAccountNumber());

        String username = customerDAO.findById(customerId).map(c -> c.getUsername()).orElse("unknown");
        auditLogDAO.log("CUSTOMER", customerId, username, "TRANSFER",
                "Transferred " + amount + " from " + fromAccount.getAccountNumber() + " to " + toAccountNumber);
    }

    public BigDecimal checkBalance(int accountId, int customerId) throws SQLException, InvalidLoginException {
        return getOwnedAccount(accountId, customerId).getBalance();
    }

    public List<Account> getMyAccounts(int customerId) throws SQLException {
        return accountDAO.findByCustomerId(customerId);
    }

    public List<Transaction> getTransactionHistory(int accountId, int customerId) throws SQLException,
            InvalidLoginException {
        getOwnedAccount(accountId, customerId);
        return transactionDAO.findByAccountId(accountId);
    }

    public List<Transaction> getMiniStatement(int accountId, int customerId, int limit) throws SQLException,
            InvalidLoginException {
        getOwnedAccount(accountId, customerId);
        return transactionDAO.findByAccountIdLimit(accountId, limit);
    }

    private Account getOwnedAccount(int accountId, int customerId) throws SQLException, InvalidLoginException {
        Account account = accountDAO.findById(accountId)
                .orElseThrow(() -> new InvalidLoginException("Account not found."));
        if (account.getCustomerId() != customerId || !account.isActive()) {
            throw new InvalidLoginException("Account not found or access denied.");
        }
        return account;
    }

    private void validateAmount(BigDecimal amount) throws InvalidLoginException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidLoginException("Amount must be greater than zero.");
        }
    }

    private void recordTransaction(int accountId, String type, BigDecimal amount, BigDecimal balanceAfter,
                                   String description, String relatedAccount) throws SQLException {
        Transaction tx = new Transaction();
        tx.setAccountId(accountId);
        tx.setTransactionType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setDescription(description);
        tx.setRelatedAccount(relatedAccount);
        transactionDAO.create(tx);
    }

    private String generateAccountNumber() throws SQLException {
        String number;
        do {
            number = String.format("%010d", 1000000000L + random.nextInt(900000000));
        } while (accountDAO.findByAccountNumber(number).isPresent());
        return number;
    }
}
