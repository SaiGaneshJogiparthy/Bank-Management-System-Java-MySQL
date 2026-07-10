package bank.security;

import java.security.SecureRandom;

public final class CaptchaGenerator {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CaptchaGenerator() {
    }

    public static String generate(int length) {
        StringBuilder captcha = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            captcha.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return captcha.toString();
    }
}
