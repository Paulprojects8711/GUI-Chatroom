package org.paul8711gamezz.helpers;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

public class Hash {
    // Hash a string and return "salt:hash" Base64-encoded
    public static String hash(String input) throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        KeySpec spec = new PBEKeySpec(input.toCharArray(), salt, 65536, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();

        return Base64.getEncoder().encodeToString(salt) + ":" +
                Base64.getEncoder().encodeToString(hash);
    }
    public static boolean verify(String input, String stored) throws Exception {
        String[] parts = stored.split(":");
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] storedHash = Base64.getDecoder().decode(parts[1]);

        KeySpec spec = new PBEKeySpec(input.toCharArray(), salt, 65536, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] newHash = factory.generateSecret(spec).getEncoded();

        if (newHash.length != storedHash.length) return false;

        // constant-time comparison
        int result = 0;
        for (int i = 0; i < newHash.length; i++) {
            result |= newHash[i] ^ storedHash[i];
        }
        return result == 0;
    }
}
