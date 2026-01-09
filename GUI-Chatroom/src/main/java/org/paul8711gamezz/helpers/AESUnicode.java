package org.paul8711gamezz.helpers;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

public class AESUnicode {
    // i am not explaining this in detail i have no idea how this works (i stole this code from the internet)

    // encryption for unicode
    public static String encrypt(String str, String SALT, String KEY) {
        try {
            byte[] iv = new byte[16];
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(KEY.toCharArray(), SALT.getBytes(StandardCharsets.UTF_8), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);

            byte[] encryptedBytes = cipher.doFinal(str.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // decryption also for unicode
    public static String decrypt(String str, String SALT, String KEY) {
        try {
            byte[] iv = new byte[16];
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(KEY.toCharArray(), SALT.getBytes(StandardCharsets.UTF_8), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);

            byte[] decodedBytes = Base64.getDecoder().decode(str);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ChaCha20-Poly1305 (i wrote this here because i thought it sounded funny
    private static final int NONCE_LENGTH = 12;

    // encryption BUT FOR BYTES
    public static byte[] encryptBytes(byte[] plaintext, String SALT, String KEY) {
        try {
            byte[] keyBytes = deriveKey(KEY, SALT);
            SecretKey secretKey = new SecretKeySpec(keyBytes, "ChaCha20");

            byte[] nonce = new byte[NONCE_LENGTH];
            new SecureRandom().nextBytes(nonce);
            IvParameterSpec iv = new IvParameterSpec(nonce);

            Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305/None/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            byte[] ciphertext = cipher.doFinal(plaintext);

            ByteBuffer buffer = ByteBuffer.allocate(NONCE_LENGTH + ciphertext.length);
            buffer.put(nonce);
            buffer.put(ciphertext);
            return buffer.array();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // and now: decryption BUT ALSO FOR BYTES
    public static byte[] decryptBytes(byte[] encrypted, String SALT, String KEY) {
        try {
            byte[] keyBytes = deriveKey(KEY, SALT);
            SecretKey secretKey = new SecretKeySpec(keyBytes, "ChaCha20");

            ByteBuffer buffer = ByteBuffer.wrap(encrypted);
            byte[] nonce = new byte[NONCE_LENGTH];
            buffer.get(nonce);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            IvParameterSpec iv = new IvParameterSpec(nonce);
            Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305/None/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] deriveKey(String KEY, String SALT) {
        byte[] keyBytes = new byte[32];
        byte[] input = (KEY + SALT).getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] = input[i % input.length];
        }
        return keyBytes;
    }
}