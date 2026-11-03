package dev.vuemind.api.transfers;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

/**
 * Identificador end-to-end no formato do arranjo PIX do Banco Central: {@code E} + ISPB do participante (8 dígitos)
 * + data e hora UTC {@code yyyyMMddHHmm} + 11 caracteres alfanuméricos, 32 no total.
 *
 * <p>O PIX desta API é simulado: nenhum valor sai da conta e nada passa pelo SPI. Por isso o ISPB é fictício,
 * {@value #ISPB_DEMONSTRACAO}, que não pertence a nenhum participante da lista do Banco Central.
 */
public final class EndToEndIds {

    /** ISPB fictício da demonstração: não identifica nenhuma instituição real. */
    public static final String ISPB_DEMONSTRACAO = "99999999";

    /** Formato aceito pelo arranjo PIX, com mês, dia, hora e minuto válidos. */
    public static final Pattern FORMATO = Pattern.compile(
            "E\\d{8}\\d{4}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])([01]\\d|2[0-3])[0-5]\\d[a-zA-Z0-9]{11}");

    private static final String ALFANUMERICOS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TAMANHO_SEQUENCIAL = 11;
    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC);
    private static final RandomGenerator ALEATORIO = new SecureRandom();

    private EndToEndIds() {
    }

    public static String gerar(Instant momento) {
        return gerar(momento, ALEATORIO);
    }

    static String gerar(Instant momento, RandomGenerator aleatorio) {
        StringBuilder id = new StringBuilder(32).append('E').append(ISPB_DEMONSTRACAO).append(DATA_HORA.format(momento));
        for (int i = 0; i < TAMANHO_SEQUENCIAL; i++) {
            id.append(ALFANUMERICOS.charAt(aleatorio.nextInt(ALFANUMERICOS.length())));
        }
        return id.toString();
    }
}
