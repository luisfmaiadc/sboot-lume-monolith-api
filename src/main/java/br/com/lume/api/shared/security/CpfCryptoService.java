package br.com.lume.api.shared.security;

import br.com.lume.api.shared.config.CryptoProperties;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class CpfCryptoService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private final byte[] hmacKeyBytes;
    private final byte[] aesKeyBytes;
    private final SecureRandom secureRandom = new SecureRandom();

    public CpfCryptoService(CryptoProperties cryptoProperties) {
        this.hmacKeyBytes = cryptoProperties.cpfHmacKey().getBytes(StandardCharsets.UTF_8);
        byte[] rawAes = cryptoProperties.cpfAesKey().getBytes(StandardCharsets.UTF_8);
        if (rawAes.length != 32) {
            throw new IllegalArgumentException("A chave AES do CPF deve conter exatamente 32 bytes (256 bits).");
        }
        this.aesKeyBytes = rawAes;
    }

    public String gerarHash(String cpf) {
        if (cpf == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hmacKeyBytes, HMAC_ALGORITHM));
            byte[] hmacBytes = mac.doFinal(cpf.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Falha ao gerar hash HMAC do CPF.", ex);
        }
    }

    public String cifrar(String cpf) {
        if (cpf == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            SecretKeySpec keySpec = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

            byte[] cipherText = cipher.doFinal(cpf.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Falha ao cifrar CPF com AES-GCM.", ex);
        }
    }

    public String decifrar(String cpfCifrado) {
        if (cpfCifrado == null || cpfCifrado.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cpfCifrado);
            if (decoded.length < GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Payload cifrado do CPF inválido.");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            SecretKeySpec keySpec = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Falha ao decifrar CPF com AES-GCM.", ex);
        }
    }
}