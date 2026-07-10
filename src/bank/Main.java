package bank;

import bank.ui.AdminMenu;
import bank.ui.CustomerMenu;
import bank.util.InputUtil;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            InputUtil.printHeader("BANK MANAGEMENT SYSTEM");
            System.out.println("1. Customer");
            System.out.println("2. Admin");
            System.out.println("3. Exit");
            int choice = InputUtil.readInt(scanner, "\nChoose Option : ");

            switch (choice) {
                case 1 -> new CustomerMenu(scanner).show();
                case 2 -> new AdminMenu(scanner).show();
                case 3 -> {
                    System.out.println("\nThank you for using Bank Management System. Goodbye!");
                    scanner.close();
                    return;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }
}
