package com.diegovieira.buscacep.util;

import com.diegovieira.buscacep.exception.InvalidCepException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CepValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"23565100", "23565-100", "01310-100", "00000000"})
    void isValidoAceitaCepsValidosComESemHifen(String cep) {
        assertTrue(CepValidator.isValido(cep));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "2356510",      // 7 dígitos
            "235651000",    // 9 dígitos
            "abcdefgh",     // letras
            "2356-100",     // hífen no lugar errado (4 dígitos antes)
            "2356510-0",    // hífen no lugar errado (depois do 7º dígito)
            "235-65100",    // hífen no lugar errado (depois do 3º dígito)
            "23565--100",   // hífen duplicado
            "23565-10",     // 7 dígitos com hífen
            " 23565100",    // espaço no início
            "23565100 "     // espaço no fim
    })
    void isValidoRejeitaCepsInvalidos(String cep) {
        assertFalse(CepValidator.isValido(cep));
    }

    @Test
    void normalizarDevolveApenasOsOitoDigitos() {
        assertEquals("23565100", CepValidator.normalizar("23565-100"));
        assertEquals("23565100", CepValidator.normalizar("23565100"));
    }

    @Test
    void formatarDevolvePadraoComHifen() {
        assertEquals("23565-100", CepValidator.formatar("23565100"));
        assertEquals("23565-100", CepValidator.formatar("23565-100"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123", "2356510", "abcdefgh"})
    void normalizarLancaExcecaoQuandoInvalido(String cep) {
        assertThrows(InvalidCepException.class, () -> CepValidator.normalizar(cep));
    }

    @Test
    void formatarLancaExcecaoQuandoInvalido() {
        InvalidCepException ex = assertThrows(InvalidCepException.class, () -> CepValidator.formatar("123"));
        assertEquals("CEP deve conter 8 dígitos numéricos", ex.getMessage());
        assertEquals("123", ex.getCep());
    }
}
