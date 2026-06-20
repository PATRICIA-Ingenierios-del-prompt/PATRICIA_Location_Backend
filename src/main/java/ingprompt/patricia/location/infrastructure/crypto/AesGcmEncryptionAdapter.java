package ingprompt.patricia.location.infrastructure.crypto;

import ingprompt.patricia.location.application.port.out.EncryptionPort;
import ingprompt.patricia.location.domain.exception.LocationEncryptionException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmEncryptionAdapter implements EncryptionPort {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final String secret;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKeySpec key;

    public AesGcmEncryptionAdapter(@Value("${location.encryption.secret}") String secret) {
        this.secret = secret;
    }

    @PostConstruct
    void init() {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new LocationEncryptionException("Failed to initialize AES key", e);
        }
    }

    @Override
    public String encrypt(String plaintext) {
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);

        byte[] ciphertext = doCipher(Cipher.ENCRYPT_MODE, iv, plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] out = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
        return Base64.getEncoder().encodeToString(out);
    }

    @Override
    public String decrypt(String ciphertext) {
        byte[] all = Base64.getDecoder().decode(ciphertext);
        byte[] iv = new byte[IV_LENGTH];
        byte[] body = new byte[all.length - IV_LENGTH];
        System.arraycopy(all, 0, iv, 0, IV_LENGTH);
        System.arraycopy(all, IV_LENGTH, body, 0, body.length);

        return new String(doCipher(Cipher.DECRYPT_MODE, iv, body), StandardCharsets.UTF_8);
    }

    private byte[] doCipher(int mode, byte[] iv, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(mode, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(input);
        } catch (Exception e) {
            String label = (mode == Cipher.ENCRYPT_MODE) ? "Encryption" : "Decryption";
            throw new LocationEncryptionException(label + " failed", e);
        }
    }
}
