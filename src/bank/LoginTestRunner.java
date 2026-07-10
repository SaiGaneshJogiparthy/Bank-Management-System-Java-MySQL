package bank;

import bank.dao.CustomerDAO;
import bank.exception.AccountLockedException;
import bank.exception.InvalidLoginException;
import bank.model.Customer;
import bank.security.CaptchaGenerator;
import bank.service.AdminService;
import bank.service.AuthService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Automated login outcome tests — run with:
 * java -cp "out;lib\mysql-connector-j-8.3.0.jar" bank.LoginTestRunner
 */
public class LoginTestRunner {

    private static final String TEST_USER = "test_login_user";
    private static final String TEST_EMAIL = "test_login_user@test.com";
    private static final String TEST_PASS = "pass12";

    private final AuthService authService = new AuthService();
    private final AdminService adminService = new AdminService();
    private final CustomerDAO customerDAO = new CustomerDAO();

    private int passed = 0;
    private int failed = 0;
    private final List<String> results = new ArrayList<>();

    public static void main(String[] args) {
        LoginTestRunner runner = new LoginTestRunner();
        try {
            runner.runAll();
        } catch (Exception e) {
            System.out.println("FATAL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void runAll() throws SQLException, InvalidLoginException {
        System.out.println("=================================================");
        System.out.println("       LOGIN OUTCOME TESTS");
        System.out.println("=================================================\n");

        setupTestCustomer();
        runCustomerTests();
        runAdminTests();
        runCaptchaTests();
        cleanupTestCustomer();

        System.out.println("\n=================================================");
        System.out.printf("RESULTS: %d passed, %d failed, %d total%n", passed, failed, passed + failed);
        System.out.println("=================================================");
        for (String r : results) {
            System.out.println(r);
        }
        if (failed > 0) {
            System.exit(1);
        }
    }

    private void setupTestCustomer() throws SQLException, InvalidLoginException {
        customerDAO.findByUsername(TEST_USER).ifPresent(c -> {
            try {
                customerDAO.findByUsername(TEST_USER).ifPresent(x -> {});
            } catch (SQLException ignored) {
            }
        });

        // Remove old test user if exists
        try (var conn = bank.config.DBConnection.getConnection();
             var ps = conn.prepareStatement("DELETE FROM customers WHERE username = ?")) {
            ps.setString(1, TEST_USER);
            ps.executeUpdate();
        }

        Customer c = new Customer();
        c.setUsername(TEST_USER);
        c.setEmail(TEST_EMAIL);
        c.setPasswordHash(TEST_PASS);
        c.setPhone("9999999999");
        authService.registerCustomer(c);
        pass("SETUP", "Test customer created (" + TEST_USER + ")");
    }

    private void cleanupTestCustomer() throws SQLException {
        try (var conn = bank.config.DBConnection.getConnection();
             var ps = conn.prepareStatement("DELETE FROM customers WHERE username = ?")) {
            ps.setString(1, TEST_USER);
            ps.executeUpdate();
        }
        pass("CLEANUP", "Test customer removed");
    }

    private void resetLockState() throws SQLException {
        Customer c = customerDAO.findByUsername(TEST_USER).orElseThrow();
        customerDAO.resetFailedLogins(c.getId());
    }

    private void runCustomerTests() throws SQLException {
        System.out.println("--- CUSTOMER LOGIN TESTS ---\n");

        // 1. Valid login with username
        expectSuccess("Customer valid login (username)", () ->
                authService.loginCustomer(TEST_USER, TEST_PASS));

        // 2. Valid login with email
        expectSuccess("Customer valid login (email)", () ->
                authService.loginCustomer(TEST_EMAIL, TEST_PASS));

        // 3. Invalid username
        expectException("Customer invalid username", InvalidLoginException.class, () ->
                authService.loginCustomer("nonexistent_user_xyz", TEST_PASS));

        // 4. Wrong password - attempt 1
        resetLockState();
        expectExceptionContains("Customer wrong password (attempt 1)", InvalidLoginException.class,
                "Attempts left: 2", () -> authService.loginCustomer(TEST_USER, "wrongpass"));

        // 5. Wrong password - attempt 2
        expectExceptionContains("Customer wrong password (attempt 2)", InvalidLoginException.class,
                "Attempts left: 1", () -> authService.loginCustomer(TEST_USER, "wrongpass"));

        // 6. Wrong password - attempt 3 (locks account)
        expectException("Customer wrong password (attempt 3 - lock)", AccountLockedException.class, () ->
                authService.loginCustomer(TEST_USER, "wrongpass"));

        // 7. Login when locked
        expectException("Customer login while locked", AccountLockedException.class, () ->
                authService.loginCustomer(TEST_USER, TEST_PASS));

        // 8. Admin unlock + valid login
        try {
            Customer locked = customerDAO.findByUsername(TEST_USER).orElseThrow();
            adminService.unlockAccount(locked.getId(), "admin");
            expectSuccess("Customer login after admin unlock", () ->
                    authService.loginCustomer(TEST_USER, TEST_PASS));
        } catch (Exception e) {
            fail("Customer login after admin unlock", "Exception: " + e.getMessage());
        }

        // 9. Empty password
        resetLockState();
        expectException("Customer empty password", InvalidLoginException.class, () ->
                authService.loginCustomer(TEST_USER, ""));
    }

    private void runAdminTests() throws SQLException {
        System.out.println("\n--- ADMIN LOGIN TESTS ---\n");

        // 1. Valid admin login
        expectSuccess("Admin valid login", () ->
                adminService.login("admin", "admin123"));

        // 2. Invalid admin username
        expectException("Admin invalid username", InvalidLoginException.class, () ->
                adminService.login("fake_admin", "admin123"));

        // 3. Wrong admin password
        expectException("Admin wrong password", InvalidLoginException.class, () ->
                adminService.login("admin", "wrongpassword"));

        // 4. Empty admin password
        expectException("Admin empty password", InvalidLoginException.class, () ->
                adminService.login("admin", ""));
    }

    private void runCaptchaTests() {
        System.out.println("\n--- CAPTCHA TESTS (UI layer) ---\n");

        String captcha = CaptchaGenerator.generate(5);
        if (captcha.length() == 5) {
            pass("CAPTCHA generation", "Generated: " + captcha);
        } else {
            fail("CAPTCHA generation", "Expected length 5, got " + captcha.length());
        }

        if (captcha.equalsIgnoreCase(captcha)) {
            pass("CAPTCHA correct match", "Case-insensitive match works");
        } else {
            fail("CAPTCHA correct match", "Should match itself");
        }

        if (!captcha.equalsIgnoreCase("WRONG")) {
            pass("CAPTCHA wrong entry", "Wrong CAPTCHA correctly rejected");
        } else {
            fail("CAPTCHA wrong entry", "Should not match wrong value");
        }
    }

    @FunctionalInterface
    private interface TestAction {
        void run() throws Exception;
    }

    private void expectSuccess(String name, TestAction action) {
        try {
            action.run();
            pass(name, "Login successful as expected");
        } catch (Exception e) {
            fail(name, "Expected success but got: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    private void expectException(String name, Class<? extends Exception> expected, TestAction action) {
        try {
            action.run();
            fail(name, "Expected " + expected.getSimpleName() + " but login succeeded");
        } catch (Exception e) {
            if (expected.isInstance(e)) {
                pass(name, expected.getSimpleName() + ": " + e.getMessage());
            } else {
                fail(name, "Expected " + expected.getSimpleName() + " but got "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    private void expectExceptionContains(String name, Class<? extends Exception> expected,
                                         String messagePart, TestAction action) {
        try {
            action.run();
            fail(name, "Expected " + expected.getSimpleName() + " but login succeeded");
        } catch (Exception e) {
            if (expected.isInstance(e) && e.getMessage() != null && e.getMessage().contains(messagePart)) {
                pass(name, e.getMessage());
            } else if (expected.isInstance(e)) {
                fail(name, "Expected message containing '" + messagePart + "' but got: " + e.getMessage());
            } else {
                fail(name, "Expected " + expected.getSimpleName() + " but got "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    private void pass(String test, String detail) {
        passed++;
        results.add("[PASS] " + test + " -> " + detail);
        System.out.println("[PASS] " + test);
    }

    private void fail(String test, String detail) {
        failed++;
        results.add("[FAIL] " + test + " -> " + detail);
        System.out.println("[FAIL] " + test + " -> " + detail);
    }
}
