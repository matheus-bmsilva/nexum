package nexum.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utilitário de segurança criptográfica para senhas de usuários.
 * Utiliza o algoritmo padrão PBKDF2 com HMAC-SHA256 e salt randômico de 128 bits,
 * em total conformidade com os padrões OWASP para armazenamento seguro de credenciais,
 * mantendo zero dependências externas (biblioteca padrão do Java SE).
 */
public class PasswordSecurity {

    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_BYTES = 16;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private PasswordSecurity() {
        // Construtor privado para classe utilitária
    }

    /**
     * Gera o hash criptográfico seguro da senha informada utilizando PBKDF2WithHmacSHA256.
     * 
     * @param password Senha em texto plano
     * @return String formatada contendo saltHex:hashHex
     */
    public static String hashPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("A senha não pode ser nula ou vazia.");
        }

        byte[] salt = new byte[SALT_BYTES];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);

        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

        HexFormat hex = HexFormat.of();
        return hex.formatHex(salt) + ":" + hex.formatHex(hash);
    }

    /**
     * Verifica se a senha informada corresponde ao hash armazenado, utilizando comparação
     * em tempo constante (MessageDigest.isEqual) contra ataques de temporização (timing attacks).
     * 
     * @param password Senha fornecida pelo usuário
     * @param storedHash Hash armazenado no formato saltHex:hashHex
     * @return true se a senha for válida, false caso contrário
     */
    public static boolean checkPassword(String password, String storedHash) {
        if (password == null || storedHash == null || !storedHash.contains(":")) {
            return false;
        }

        try {
            String[] parts = storedHash.split(":", 2);
            HexFormat hex = HexFormat.of();
            byte[] salt = hex.parseHex(parts[0]);
            byte[] expectedHash = hex.parseHex(parts[1]);

            byte[] actualHash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Falha no algoritmo criptográfico PBKDF2: " + e.getMessage(), e);
        }
    }
}
