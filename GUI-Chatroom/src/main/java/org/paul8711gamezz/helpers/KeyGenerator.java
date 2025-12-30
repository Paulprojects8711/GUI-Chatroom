package org.paul8711gamezz.helpers;

import java.security.SecureRandom;
import java.time.LocalDate;

public class KeyGenerator {
    public static void main(String[] args) {
        int length = 32; // 32-char key
        SecureRandom random = new SecureRandom();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        StringBuilder keyBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            keyBuilder.append(characters.charAt(random.nextInt(characters.length())));
        }

        String key = "gui-chatroom-default-v" + LocalDate.now().getMonthValue() + "-" + keyBuilder.toString();
        int version = (args.length > 0) ? Integer.parseInt(args[0]) : 1; // version can be passed via args

        String json = String.format("{\"version\": %d, \"key\": \"%s\"}", version, key);
        System.out.println(json); // redirect to file in GitHub Actions
    }
}