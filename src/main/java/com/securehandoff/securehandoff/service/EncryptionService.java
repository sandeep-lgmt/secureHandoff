package com.securehandoff.securehandoff.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.securehandoff.securehandoff.exception.ApiException;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    /** packet-secret must be the Base64 encoding of exactly 32 random bytes (AES-256). */
    public EncryptionService(@Value("${app.encryption.packet-secret}") String base64Key) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.encryption.packet-secret is not valid Base64", e);
        }
        if (key.length != 32) {
            throw new IllegalStateException("app.encryption.packet-secret must decode to exactly 32 bytes");
        }
        this.secretKey = new SecretKeySpec(key, "AES");
    }

    public EncryptedPayload encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return new EncryptedPayload(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(iv));
        } catch (Exception e) {
            throw new ApiException("Failed to encrypt packet content", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String decrypt(String base64Ciphertext, String base64Iv) {
        try {
            byte[] ciphertext = Base64.getDecoder().decode(base64Ciphertext);
            byte[] iv = Base64.getDecoder().decode(base64Iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ApiException("Failed to decrypt packet content", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public record EncryptedPayload(String ciphertextBase64, String ivBase64) {
    }
}
