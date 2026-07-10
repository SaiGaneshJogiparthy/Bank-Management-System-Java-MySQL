package bank.security;

import java.security.SecureRandom;

public final class OTPGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private OTPGenerator() {
    }

    public static String generate(int digits) {
        int max = (int) Math.pow(10, digits);
        int otp = RANDOM.nextInt(max);
        return String.format("%0" + digits + "d", otp);
    }

    public static void simulateSend(String phone, String otp) {
        System.out.println("\n[OTP SIMULATION] OTP sent to " + phone + ": " + otp);
        System.out.println("(In production, this would be sent via SMS)\n");
    }
}
