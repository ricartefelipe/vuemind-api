package dev.vuemind.api.beneficiaries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vuemind.api.common.ApiException;
import dev.vuemind.api.store.model.PixKeyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PixKeyValidatorTest {

    @Test
    void cpf_confereOsDigitosVerificadores() {
        assertTrue(PixKeyValidator.isValid(PixKeyType.CPF, "52998224725"));
        assertFalse(PixKeyValidator.isValid(PixKeyType.CPF, "52998224724"));
        assertFalse(PixKeyValidator.isValid(PixKeyType.CPF, "12345678901"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "5299822472", "529.982.247-25", "5299822472a"})
    void cpf_recusaRepetidosEFormatoErrado(String key) {
        assertFalse(PixKeyValidator.isValid(PixKeyType.CPF, key));
    }

    @Test
    void cnpj_confereOsDigitosVerificadores() {
        assertTrue(PixKeyValidator.isValid(PixKeyType.CNPJ, "11222333000181"));
        assertFalse(PixKeyValidator.isValid(PixKeyType.CNPJ, "11222333000182"));
    }

    @Test
    void cnpj_aceitaOFormatoAlfanumerico() {
        // Exemplo publicado pela Receita Federal para o CNPJ alfanumérico.
        assertTrue(PixKeyValidator.isValid(PixKeyType.CNPJ, "12ABC34501DE35"));
        assertFalse(PixKeyValidator.isValid(PixKeyType.CNPJ, "12ABC34501DE36"));
        assertFalse(PixKeyValidator.isValid(PixKeyType.CNPJ, "12abc34501de35"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000000", "1122233300018", "11.222.333/0001-81", "112223330001AB"})
    void cnpj_recusaRepetidosEFormatoErrado(String key) {
        assertFalse(PixKeyValidator.isValid(PixKeyType.CNPJ, key));
    }

    @Test
    void cnpjNaoPassaComoCpfNemComoAleatoria() {
        assertFalse(PixKeyValidator.isValid(PixKeyType.CPF, "11222333000181"));
        assertFalse(PixKeyValidator.isValid(PixKeyType.RANDOM, "11222333000181"));
    }

    @Test
    void aleatoria_aceitaUuidComOuSemHifens() {
        assertTrue(PixKeyValidator.isValid(PixKeyType.RANDOM, "123e4567-e89b-12d3-a456-426614174000"));
        assertTrue(PixKeyValidator.isValid(PixKeyType.RANDOM, "123e4567e89b12d3a456426614174000"));
        assertFalse(PixKeyValidator.isValid(PixKeyType.RANDOM, "123e4567-e89b12d3a456426614174000"));
        assertFalse(PixKeyValidator.isValid(PixKeyType.RANDOM, "chave-aleatoria"));
    }

    @Test
    void emailETelefone() {
        assertTrue(PixKeyValidator.isValid(PixKeyType.EMAIL, "ana@email.com"));
        assertFalse(PixKeyValidator.isValid(PixKeyType.EMAIL, "ana@email"));
        assertTrue(PixKeyValidator.isValid(PixKeyType.PHONE, "+5511999998888"));
        assertFalse(PixKeyValidator.isValid(PixKeyType.PHONE, "123"));
    }

    @Test
    void assertValid_respondeInvalidPixKey() {
        ApiException ex = assertThrows(
                ApiException.class, () -> PixKeyValidator.assertValid(PixKeyType.CNPJ, "11222333000182"));
        assertEquals("INVALID_PIX_KEY", ex.getCode());
    }
}
