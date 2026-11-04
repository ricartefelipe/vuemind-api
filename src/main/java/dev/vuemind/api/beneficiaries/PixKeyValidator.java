package dev.vuemind.api.beneficiaries;

import dev.vuemind.api.common.ApiException;
import dev.vuemind.api.store.model.PixKeyType;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/** Mesmas regras de formato do SpringMind, para os dois backends da carteira aceitarem as mesmas chaves. */
public final class PixKeyValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern CPF_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d{10,13}$");
    private static final Pattern RANDOM_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$");

    private PixKeyValidator() {}

    public static boolean isValid(PixKeyType type, String key) {
        if (type == null || key == null) {
            return false;
        }
        return switch (type) {
            case EMAIL -> EMAIL_PATTERN.matcher(key).matches();
            case CPF -> CPF_PATTERN.matcher(key).matches();
            case PHONE -> PHONE_PATTERN.matcher(key).matches();
            case RANDOM -> RANDOM_PATTERN.matcher(key).matches();
        };
    }

    public static void assertValid(PixKeyType type, String key) {
        if (!isValid(type, key)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_PIX_KEY", "Chave PIX inválida para o tipo informado.");
        }
    }
}
