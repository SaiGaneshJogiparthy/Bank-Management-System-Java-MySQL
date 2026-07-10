package bank.ui;

import bank.exception.InvalidLoginException;
import bank.model.Account;
import bank.model.Admin;
import bank.model.Customer;
import bank.model.Transaction;
import bank.security.CaptchaGenerator;
import bank.service.AdminService;
import bank.util.InputUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final Scanner scanner;
    private final AdminService adminService = new AdminService();
    private Admin loggedInAdmin;

    public AdminMenu(Scanner scanner) {
        this.scanner = scanner;
    }

    public void show() {
        login();
    }

    private void login() {
        InputUtil.printHeader("ADMIN LOGIN");
        String username = InputUtil.readLine(scanner, "Username : ");
        String password = InputUtil.readLine(scanner, "Password : ");

        String captcha = CaptchaGenerator.generate(5);
        System.out.println("\nCaptcha : " + captcha);
        String enteredCaptcha = InputUtil.readLine(scanner, "Enter Captcha : ");

        if (!captcha.equalsIgnoreCase(enteredCaptcha)) {
            System.out.println("\nInvalid CAPTCHA. Login failed.");
            InputUtil.pause(scanner);
            return;
        }

        try {
            loggedInAdmin = adminService.login(username, password);
            System.out.println("\nLogin Successful...");
            InputUtil.pause(scanner);
            dashboard();
        } catch (InvalidLoginException e) {
            System.out.println("\n" + e.getMessage());
            InputUtil.pause(scanner);
        } catch (SQLException e) {
            System.out.println("\nDatabase error: " + e.getMessage());
            InputUtil.pause(scanner);
        }
    }

    private void dashboard() {
        while (loggedInAdmin != null) {
            InputUtil.printHeader("ADMIN DASHBOARD");
            System.out.println("Welcome, " + loggedInAdmin.getFullName() + "!");
            System.out.println();
            System.out.println("1.  View All Customers");
            System.out.println("2.  Search Customer");
            System.out.println("3.  View All Accounts");
            System.out.println("4.  Search Account");
            System.out.println("5.  View Transactions");
            System.out.println("6.  Unlock Account");
            System.out.println("7.  Delete Account");
            System.out.println("8.  View Login History");
            System.out.println("9.  View Audit Logs");
            System.out.println("10. Logout");

            int choice = InputUtil.readInt(scanner, "\nChoose Option : ");

            try {
                switch (choice) {
                    case 1 -> viewAllCustomers();
                    case 2 -> searchCustomer();
                    case 3 -> viewAllAccounts();
                    case 4 -> searchAccount();
                    case 5 -> viewTransactions();
                    case 6 -> unlockAccount();
                    case 7 -> deleteAccount();
                    case 8 -> adminService.showLoginHistory();
                    case 9 -> adminService.showAuditLogs();
                    case 10 -> {
                        System.out.println("\nLogged out successfully.");
                        loggedInAdmin = null;
                        return;
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (SQLException e) {
                System.out.println("\nDatabase error: " + e.getMessage());
            } catch (InvalidLoginException e) {
                System.out.println("\n" + e.getMessage());
            }
            InputUtil.pause(scanner);
        }
    }

    private void viewAllCustomers() throws SQLException {
        printCustomers(adminService.getAllCustomers());
    }

    private void searchCustomer() throws SQLException {
        String keyword = InputUtil.readLine(scanner, "Search (name/username/email) : ");
        printCustomers(adminService.searchCustomer(keyword));
    }

    private void printCustomers(List<Customer> customers) {
        if (customers.isEmpty()) {
            System.out.println("\nNo customers found.");
            return;
        }
        System.out.printf("%n%-6s %-15s %-25s %-15s %-8s%n", "ID", "Username", "Email", "Name", "Locked");
        System.out.println("-".repeat(75));
        for (Customer c : customers) {
            System.out.printf("%-6d %-15s %-25s %-15s %-8s%n",
                    c.getId(), c.getUsername(), c.getEmail(), c.getFullName(),
                    c.isLocked() ? "YES" : "NO");
        }
    }

    private void viewAllAccounts() throws SQLException {
        printAccounts(adminService.getAllAccounts());
    }

    private void searchAccount() throws SQLException {
        String keyword = InputUtil.readLine(scanner, "Search by account number : ");
        printAccounts(adminService.searchAccount(keyword));
    }

    private void printAccounts(List<Account> accounts) {
        if (accounts.isEmpty()) {
            System.out.println("\nNo accounts found.");
            return;
        }
        System.out.printf("%n%-6s %-15s %-12s %-10s %-8s%n", "ID", "Account No.", "CustomerID", "Type", "Active");
        System.out.println("-".repeat(60));
        for (Account a : accounts) {
            System.out.printf("%-6d %-15s %-12d %-10s %-8s%n",
                    a.getAccountId(), a.getAccountNumber(), a.getCustomerId(),
                    a.getAccountType(), a.isActive() ? "YES" : "NO");
        }
    }

    private void viewTransactions() throws SQLException {
        List<Transaction> txs = adminService.getAllTransactions();
        if (txs.isEmpty()) {
            System.out.println("\nNo transactions found.");
            return;
        }
        System.out.printf("%n%-6s %-10s %-15s %-10s %-12s %-20s%n",
                "ID", "AccountID", "Type", "Amount", "Balance", "Date");
        System.out.println("-".repeat(80));
        for (Transaction t : txs) {
            System.out.printf("%-6d %-10d %-15s Rs.%-8s Rs.%-9s %-20s%n",
                    t.getTransactionId(), t.getAccountId(), t.getTransactionType(),
                    t.getAmount(), t.getBalanceAfter(), t.getCreatedAt());
        }
    }

    private void unlockAccount() throws SQLException {
        int customerId = InputUtil.readInt(scanner, "Enter Customer ID to unlock : ");
        adminService.unlockAccount(customerId, loggedInAdmin.getUsername());
        System.out.println("\nAccount unlocked successfully!");
    }

    private void deleteAccount() throws SQLException, InvalidLoginException {
        int accountId = InputUtil.readInt(scanner, "Enter Account ID to delete : ");
        String confirm = InputUtil.readLine(scanner, "Are you sure? (yes/no) : ");
        if ("yes".equalsIgnoreCase(confirm)) {
            adminService.deleteAccount(accountId, loggedInAdmin.getUsername());
            System.out.println("\nAccount deleted successfully!");
        } else {
            System.out.println("\nDeletion cancelled.");
        }
    }
}
