package ingprompt.patricia.location.infrastructure.crypto;

import ingprompt.patricia.location.domain.exception.LocationEncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmEncryptionAdapterTest {

    private AesGcmEncryptionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AesGcmEncryptionAdapter("test-secret-key-for-unit-tests");
        adapter.init();
    }

    @Test
    void encryptAndDecryptRoundTrip() {
        String plaintext = "4.6097";
        String ciphertext = adapter.encrypt(plaintext);
        String decrypted = adapter.decrypt(ciphertext);

        assertEquals(plaintext, decrypted);
        assertNotEquals(plaintext, ciphertext);
    }

    @Test
    void encryptProducesDifferentCiphertextsForSamePlaintext() {
        String plaintext = "Hello World";
        String c1 = adapter.encrypt(plaintext);
        String c2 = adapter.encrypt(plaintext);

        assertNotEquals(c1, c2);
        assertEquals(plaintext, adapter.decrypt(c1));
        assertEquals(plaintext, adapter.decrypt(c2));
    }

    @Test
    void decryptWithTamperedCiphertextThrows() {
        String ciphertext = adapter.encrypt("some data");
        String tampered = ciphertext.substring(0, ciphertext.length() - 2) + "AA";

        assertThrows(LocationEncryptionException.class, () -> adapter.decrypt(tampered));
    }

    @Test
    void decryptWithInvalidBase64Throws() {
        assertThrows(LocationEncryptionException.class, () -> adapter.decrypt("not-valid-base64!!!"));
    }

    @Test
    void encryptEmptyString() {
        String ciphertext = adapter.encrypt("");
        assertEquals("", adapter.decrypt(ciphertext));
    }

    @Test
    void encryptLongCoordinateString() {
        String plaintext = "-74.08174523456789";
        String ciphertext = adapter.encrypt(plaintext);
        assertEquals(plaintext, adapter.decrypt(ciphertext));
    }

    @Test
    void differentKeysProduceDifferentResults() {
        AesGcmEncryptionAdapter other = new AesGcmEncryptionAdapter("different-secret-key");
        other.init();

        String plaintext = "4.6097";
        String ciphertext = adapter.encrypt(plaintext);

        assertThrows(LocationEncryptionException.class, () -> other.decrypt(ciphertext));
    }
}
