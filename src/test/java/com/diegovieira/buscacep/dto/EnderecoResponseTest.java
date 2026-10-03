package com.diegovieira.buscacep.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnderecoResponseTest {

    @Test
    void formataCepQueChegaSemHifen() {
        EnderecoResponse response = new EnderecoResponse("23565100", "Rua das Flores", "Centro", "RJ",
                "Rio de Janeiro", "");

        assertEquals("23565-100", response.cep());
    }

    @Test
    void mantemCepJaFormatado() {
        EnderecoResponse response = new EnderecoResponse("23565-100", "Rua das Flores", "Centro", "RJ",
                "Rio de Janeiro", "");

        assertEquals("23565-100", response.cep());
    }

    @Test
    void toleraCamposOpcionaisNulos() {
        EnderecoResponse response = new EnderecoResponse("23565100", null, "Centro", "RJ",
                "Rio de Janeiro", null);

        assertEquals("23565-100", response.cep());
        assertNull(response.logradouro());
        assertNull(response.complemento());
    }
}
