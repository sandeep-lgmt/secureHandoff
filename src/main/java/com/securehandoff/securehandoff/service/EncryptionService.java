package com.securehandoff.securehandoff.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.kafka.common.errors.ApiException;
import org.springframework.http.HttpStatus;

public class EncryptionService {
      private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(@Value("${app.encryption.packet-secret}") String packetSecret) {
        byte[] keyBytes = normalizeTo32Bytes(packetSecret.getBytes(StandardCharsets.UTF_8));
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * @return an EncryptedPayload holding the base64 ciphertext and base64 IV.
     *         The IV MUST be stored alongside the ciphertext — it's not secret, but
     *         it must never be reused with the same key.
     */
    public EncryptedPayload encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return new EncryptedPayload(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(iv)
            );
        } catch (Exception e) {
            throw new ApiException("Failed to encrypt packet content", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String decrypt(String base64Ciphertext, String base64Iv) {
        try {
            byte[] ciphertext = Base64.getDecoder().decode(base64Ciphertext);
            byte[] iv = Base64.getDecoder().decode(base64Iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ApiException("Failed to decrypt packet content", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Pads or trims the configured secret to exactly 32 bytes (AES-256). */
    private byte[] normalizeTo32Bytes(byte[] input) {
        byte[] key = new byte[32];
        System.arraycopy(input, 0, key, 0, Math.min(input.length, 32));
        return key;
    }

    public record EncryptedPayload(String ciphertextBase64, String ivBase64) {}

}
