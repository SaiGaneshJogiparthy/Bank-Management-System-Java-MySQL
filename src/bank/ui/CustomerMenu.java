package bank.ui;

import bank.exception.*;
import bank.model.Account;
import bank.model.Customer;
import bank.model.Transaction;
import bank.security.CaptchaGenerator;
import bank.security.OTPGenerator;
import bank.security.PasswordUtil;
import bank.service.AccountService;
import bank.service.AuthService;
import bank.service.CustomerService;
import bank.util.InputUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class CustomerMenu {

    private final Scanner scanner;
    private final AuthService authService = new AuthService();
    private final AccountService accountService = new AccountService();
    private final CustomerService customerService = new CustomerService();
    private Customer loggedInCustomer;

    public CustomerMenu(Scanner scanner) {
        this.scanner = scanner;
    }

    public void show() {
        while (true) {
            InputUtil.printHeader("CUSTOMER MENU");
            System.out.println("1. Login");
            System.out.println("2. Sign Up");
            System.out.println("3. Back");
            int choice = InputUtil.readInt(scanner, "\nChoose Option : ");

            switch (choice) {
                case 1 -> login();
                case 2 -> signUp();
                case 3 -> { return; }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void login() {
        InputUtil.printHeader("CUSTOMER LOGIN");
        String login = InputUtil.readLine(scanner, "Username / Email : ");
        String password = InputUtil.readLine(scanner, "Password         : ");

        String captcha = CaptchaGenerator.generate(5);
        System.out.println("\nCaptcha : " + captcha);
        String enteredCaptcha = InputUtil.readLine(scanner, "Enter Captcha    : ");

        if (!captcha.equalsIgnoreCase(enteredCaptcha)) {
            System.out.println("\nInvalid CAPTCHA. Login failed.");
            InputUtil.pause(scanner);
            return;
        }

        try {
            loggedInCustomer = authService.loginCustomer(login, password);
            System.out.println("\nLogin Successful...");
            InputUtil.pause(scanner);
            dashboard();
        } catch (AccountLockedException e) {
            System.out.println("\n" + e.getMessage());
            InputUtil.pause(scanner);
        } catch (InvalidLoginException e) {
            System.out.println("\n" + e.getMessage());
            InputUtil.pause(scanner);
        } catch (SQLException e) {
            System.out.println("\nDatabase error: " + e.getMessage());
            InputUtil.pause(scanner);
        }
    }

    private void signUp() {
        InputUtil.printHeader("CUSTOMER SIGN UP");
        try {
            String username = InputUtil.readLine(scanner, "Username         : ");
            if (username.isBlank()) {
                System.out.println("\nUsername is required.");
                InputUtil.pause(scanner);
                return;
            }
            if (customerService.isUsernameTaken(username)) {
                System.out.println("\nUsername already exists.");
                InputUtil.pause(scanner);
                return;
            }

            String email = InputUtil.readLine(scanner, "Email            : ");
            if (email.isBlank()) {
                System.out.println("\nEmail is required.");
                InputUtil.pause(scanner);
                return;
            }
            if (customerService.isEmailTaken(email)) {
                System.out.println("\nEmail already registered.");
                InputUtil.pause(scanner);
                return;
            }

            String phone = InputUtil.readLine(scanner, "Phone            : ");
            if (phone.isBlank()) {
                System.out.println("\nPhone number is required.");
                InputUtil.pause(scanner);
                return;
            }

            String password = InputUtil.readLine(scanner, "Password         : ");
            if (!PasswordUtil.isValidLength(password)) {
                System.out.println("\n" + PasswordUtil.lengthRequirementMessage());
                InputUtil.pause(scanner);
                return;
            }
            String confirmPassword = InputUtil.readLine(scanner, "Confirm Password : ");
            if (!password.equals(confirmPassword)) {
                System.out.println("\nPasswords do not match.");
                InputUtil.pause(scanner);
                return;
            }

            String otp = OTPGenerator.generate(6);
            OTPGenerator.simulateSend(phone, otp);
            String enteredOtp = InputUtil.readLine(scanner, "Enter OTP        : ");
            if (!otp.equals(enteredOtp)) {
                System.out.println("\nInvalid OTP. Sign up cancelled.");
                InputUtil.pause(scanner);
                return;
            }

            Customer customer = new Customer();
            customer.setUsername(username);
            customer.setEmail(email);
            customer.setPasswordHash(password);
            customer.setPhone(phone);

            authService.registerCustomer(customer);
            System.out.println("\nSign Up Successful! You can now login.");
            System.out.println("Set your Transaction PIN from dashboard (option 11) before banking.");
        } catch (InvalidLoginException e) {
            System.out.println("\n" + e.getMessage());
        } catch (SQLException e) {
            System.out.println("\nDatabase error: " + e.getMessage());
        }
        InputUtil.pause(scanner);
    }

    private void dashboard() {
        while (loggedInCustomer != null) {
            InputUtil.printHeader("CUSTOMER DASHBOARD");
            System.out.println("Welcome, " + displayName(loggedInCustomer) + "!");
            System.out.println();
            System.out.println("1.  Create Bank Account");
            System.out.println("2.  Deposit Money");
            System.out.println("3.  Withdraw Money");
            System.out.println("4.  Transfer Money");
            System.out.println("5.  Check Balance");
            System.out.println("6.  View My Accounts");
            System.out.println("7.  Mini Statement");
            System.out.println("8.  Transaction History");
            System.out.println("9.  Update Profile");
            System.out.println("10. Change Password");
            System.out.println("11. Change Transaction PIN");
            System.out.println("12. Logout");

            int choice = InputUtil.readInt(scanner, "\nChoose Option : ");

            try {
                switch (choice) {
                    case 1 -> createAccount();
                    case 2 -> deposit();
                    case 3 -> withdraw();
                    case 4 -> transfer();
                    case 5 -> checkBalance();
                    case 6 -> viewAccounts();
                    case 7 -> miniStatement();
                    case 8 -> transactionHistory();
                    case 9 -> updateProfile();
                    case 10 -> changePassword();
                    case 11 -> changePin();
                    case 12 -> {
                        System.out.println("\nLogged out successfully.");
                        loggedInCustomer = null;
                        return;
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (SQLException e) {
                System.out.println("\nDatabase error: " + e.getMessage());
            } catch (InvalidLoginException e) {
                System.out.println("\n" + e.getMessage());
            } catch (InsufficientBalanceException e) {
                System.out.println("\n" + e.getMessage());
            }
            InputUtil.pause(scanner);
        }
    }

    private void createAccount() throws SQLException {
        System.out.println("\nAccount Type: 1. SAVINGS  2. CURRENT");
        int typeChoice = InputUtil.readInt(scanner, "Choose : ");
        String type = typeChoice == 2 ? "CURRENT" : "SAVINGS";
        Account account = accountService.createAccount(loggedInCustomer.getId(), type);
        System.out.println("\nAccount created successfully!");
        System.out.println("Account Number: " + account.getAccountNumber());
        System.out.println("Account Type  : " + account.getAccountType());
    }

    private void deposit() throws SQLException, InvalidLoginException {
        int accountId = selectAccount();
        if (accountId == -1) return;
        double amount = InputUtil.readDouble(scanner, "Amount to deposit : ");
        String pin = InputUtil.readLine(scanner, "Transaction PIN   : ");
        accountService.deposit(accountId, BigDecimal.valueOf(amount), pin, loggedInCustomer.getId());
        System.out.println("\nDeposit successful!");
    }

    private void withdraw() throws SQLException, InvalidLoginException, InsufficientBalanceException {
        int accountId = selectAccount();
        if (accountId == -1) return;
        double amount = InputUtil.readDouble(scanner, "Amount to withdraw : ");
        String pin = InputUtil.readLine(scanner, "Transaction PIN    : ");
        accountService.withdraw(accountId, BigDecimal.valueOf(amount), pin, loggedInCustomer.getId());
        System.out.println("\nWithdrawal successful!");
    }

    private void transfer() throws SQLException, InvalidLoginException, InsufficientBalanceException {
        int accountId = selectAccount();
        if (accountId == -1) return;
        String toAccount = InputUtil.readLine(scanner, "Destination Account Number : ");
        double amount = InputUtil.readDouble(scanner, "Amount to transfer         : ");
        String pin = InputUtil.readLine(scanner, "Transaction PIN            : ");
        accountService.transfer(accountId, toAccount, BigDecimal.valueOf(amount), pin, loggedInCustomer.getId());
        System.out.println("\nTransfer successful!");
    }

    private void checkBalance() throws SQLException, InvalidLoginException {
        int accountId = selectAccount();
        if (accountId == -1) return;
        BigDecimal balance = accountService.checkBalance(accountId, loggedInCustomer.getId());
        System.out.println("\nCurrent Balance: Rs. " + balance);
    }

    private void viewAccounts() throws SQLException {
        List<Account> accounts = accountService.getMyAccounts(loggedInCustomer.getId());
        if (accounts.isEmpty()) {
            System.out.println("\nNo accounts found. Create one first.");
            return;
        }
        System.out.printf("%n%-6s %-15s %-10s %-12s%n", "ID", "Account No.", "Type", "Balance");
        System.out.println("-".repeat(50));
        for (Account a : accounts) {
            System.out.printf("%-6d %-15s %-10s Rs.%-10s%n",
                    a.getAccountId(), a.getAccountNumber(), a.getAccountType(), a.getBalance());
        }
    }

    private void miniStatement() throws SQLException, InvalidLoginException {
        int accountId = selectAccount();
        if (accountId == -1) return;
        List<Transaction> txs = accountService.getMiniStatement(accountId, loggedInCustomer.getId(), 5);
        printTransactions(txs, "MINI STATEMENT (Last 5)");
    }

    private void transactionHistory() throws SQLException, InvalidLoginException {
        int accountId = selectAccount();
        if (accountId == -1) return;
        List<Transaction> txs = accountService.getTransactionHistory(accountId, loggedInCustomer.getId());
        printTransactions(txs, "TRANSACTION HISTORY");
    }

    private void printTransactions(List<Transaction> txs, String title) {
        System.out.println("\n--- " + title + " ---");
        if (txs.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        System.out.printf("%-6s %-15s %-10s %-12s %-20s%n", "ID", "Type", "Amount", "Balance", "Date");
        System.out.println("-".repeat(70));
        for (Transaction t : txs) {
            System.out.printf("%-6d %-15s Rs.%-8s Rs.%-9s %-20s%n",
                    t.getTransactionId(), t.getTransactionType(), t.getAmount(),
                    t.getBalanceAfter(), t.getCreatedAt());
        }
    }

    private String displayName(Customer customer) {
        String name = customer.getFullName();
        if (name != null && !name.isBlank() && !name.equals(customer.getUsername())) {
            return name;
        }
        return customer.getUsername();
    }

    private void updateProfile() throws SQLException {
        String fullName = InputUtil.readLine(scanner, "Full Name (optional, Enter to skip) : ");
        if (!fullName.isBlank()) {
            loggedInCustomer.setFullName(fullName);
        }
        String phone = InputUtil.readLine(scanner, "Phone     : ");
        if (!phone.isBlank()) {
            loggedInCustomer.setPhone(phone);
        }
        String address = InputUtil.readLine(scanner, "Address (optional, Enter to skip) : ");
        if (!address.isBlank()) {
            loggedInCustomer.setAddress(address);
        }
        String email = InputUtil.readLine(scanner, "Email     : ");
        if (!email.isBlank()) {
            loggedInCustomer.setEmail(email);
        }
        if (customerService.updateProfile(loggedInCustomer)) {
            System.out.println("\nProfile updated successfully!");
        } else {
            System.out.println("\nProfile update failed.");
        }
    }

    private void changePassword() throws SQLException, InvalidLoginException {
        String oldPass = InputUtil.readLine(scanner, "Current Password : ");
        String newPass = InputUtil.readLine(scanner, "New Password     : ");
        if (!PasswordUtil.isValidLength(newPass)) {
            System.out.println("\n" + PasswordUtil.lengthRequirementMessage());
            return;
        }
        String confirm = InputUtil.readLine(scanner, "Confirm Password : ");
        if (!newPass.equals(confirm)) {
            System.out.println("\nPasswords do not match.");
            return;
        }
        if (authService.changePassword(loggedInCustomer.getId(), oldPass, newPass)) {
            System.out.println("\nPassword changed successfully!");
        }
    }

    private void changePin() throws SQLException, InvalidLoginException {
        String oldPin = InputUtil.readLine(scanner, "Current PIN : ");
        String newPin = InputUtil.readLine(scanner, "New PIN     : ");
        String confirm = InputUtil.readLine(scanner, "Confirm PIN : ");
        if (!newPin.equals(confirm)) {
            System.out.println("\nPINs do not match.");
            return;
        }
        if (authService.changeTransactionPin(loggedInCustomer.getId(), oldPin, newPin)) {
            System.out.println("\nTransaction PIN changed successfully!");
        }
    }

    private int selectAccount() throws SQLException {
        List<Account> accounts = accountService.getMyAccounts(loggedInCustomer.getId());
        if (accounts.isEmpty()) {
            System.out.println("\nNo accounts found. Create one first.");
            return -1;
        }
        viewAccounts();
        return InputUtil.readInt(scanner, "\nEnter Account ID : ");
    }
}
