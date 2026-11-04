package dev.vuemind.api.beneficiaries;

import dev.vuemind.api.common.ApiException;
import dev.vuemind.api.store.model.PixKeyType;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/**
 * Formato de cada tipo de chave PIX, com as mesmas regras do SpringMind. CPF e CNPJ conferem os dígitos verificadores; o CNPJ aceita também o formato
 * alfanumérico (12 caracteres de A a Z ou 0 a 9 e 2 dígitos verificadores). A chave aleatória (EVP) é um UUID, com ou
 * sem hífens.
 */
public final class PixKeyValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern CPF_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern CNPJ_PATTERN = Pattern.compile("^[0-9A-Z]{12}\\d{2}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{10,13}$");
    private static final Pattern RANDOM_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}$");
    private static final int[] CNPJ_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private PixKeyValidator() {}

    public static boolean isValid(PixKeyType type, String key) {
        if (type == null || key == null) {
            return false;
        }
        return switch (type) {
            case EMAIL -> EMAIL_PATTERN.matcher(key).matches();
            case CPF -> isValidCpf(key);
            case CNPJ -> isValidCnpj(key);
            case PHONE -> PHONE_PATTERN.matcher(key).matches();
            case RANDOM -> isValidRandom(key);
        };
    }

    public static void assertValid(PixKeyType type, String key) {
        if (!isValid(type, key)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PIX_KEY",
                    "Chave PIX inválida para o tipo informado.");
        }
    }

    private static boolean isValidCpf(String key) {
        if (!CPF_PATTERN.matcher(key).matches() || key.chars().distinct().count() == 1) {
            return false;
        }
        return cpfDigit(key, 9) == key.charAt(9) - '0' && cpfDigit(key, 10) == key.charAt(10) - '0';
    }

    private static int cpfDigit(String key, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (key.charAt(i) - '0') * (length + 1 - i);
        }
        int rest = (sum * 10) % 11;
        return rest == 10 ? 0 : rest;
    }

    private static boolean isValidCnpj(String key) {
        if (!CNPJ_PATTERN.matcher(key).matches() || key.chars().distinct().count() == 1) {
            return false;
        }
        return cnpjDigit(key, 12) == key.charAt(12) - '0' && cnpjDigit(key, 13) == key.charAt(13) - '0';
    }

    /** Cada caractere vale o código ASCII menos 48: os números continuam valendo 0 a 9 e as letras, 17 a 42. */
    private static int cnpjDigit(String key, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (key.charAt(i) - '0') * CNPJ_WEIGHTS[CNPJ_WEIGHTS.length - length + i];
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }

    private static boolean isValidRandom(String key) {
        return RANDOM_PATTERN.matcher(key).matches() && (key.length() == 32 || key.length() == 36);
    }
}
