package ingprompt.patricia.location.infrastructure.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AesGcmEncryptionAdapterTest {

    private AesGcmEncryptionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AesGcmEncryptionAdapter("test-secret-key");
        adapter.init();
    }

    @Test
    void encryptThenDecrypt_roundTrips() {
        String plaintext = "{\"lat\":4.65,\"lng\":-74.05}";

        String cipher = adapter.encrypt(plaintext);

        assertThat(cipher).isNotEqualTo(plaintext);
        assertThat(adapter.decrypt(cipher)).isEqualTo(plaintext);
    }

    @Test
    void encrypt_usesRandomIv_soCiphertextDiffersEachTime() {
        String plaintext = "same-input";

        String first = adapter.encrypt(plaintext);
        String second = adapter.encrypt(plaintext);

        assertThat(first).isNotEqualTo(second);
        assertThat(adapter.decrypt(first)).isEqualTo(plaintext);
        assertThat(adapter.decrypt(second)).isEqualTo(plaintext);
    }
}
