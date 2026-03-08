package ar.gob.rdam.pagos.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Servicio de encriptación compatible con el módulo crypto.js de PlusPagos.
 * <p>
 * Algoritmo: AES-256-CBC
 * Clave:      SHA-256 del secreto (32 bytes)
 * IV:         16 bytes aleatorios
 * Salida:     Base64(IV_16_bytes | Ciphertext)
 */
@Service
public class PlusPagosEncryptionService {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    /**
     * Encripta un texto plano con la clave secreta de PlusPagos.
     *
     * @param plainText  texto a encriptar
     * @param secretKey  clave compartida con el mock
     * @return Base64(IV + Ciphertext)
     */
    public String encrypt(String plainText, String secretKey) {
        try {
            byte[] keyBytes  = sha256(secretKey);
            byte[] ivBytes   = generateIv();

            SecretKeySpec   key    = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] ciphertext = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Combinar IV + ciphertext → Base64
            byte[] combined = new byte[ivBytes.length + ciphertext.length];
            System.arraycopy(ivBytes,    0, combined, 0,            ivBytes.length);
            System.arraycopy(ciphertext, 0, combined, ivBytes.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            throw new IllegalStateException("Error encriptando campo para PlusPagos", e);
        }
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private byte[] sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
