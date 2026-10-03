package com.diegovieira.buscacep.exception;

import java.util.Map;

/**
 * CEP com formato inválido (não tem 8 dígitos).
 * Mapeada para HTTP 400.
 */
public class InvalidCepException extends RuntimeException {

    private final String cep;

    public InvalidCepException(String cep) {
        this("CEP deve conter 8 dígitos numéricos", cep);
    }

    public InvalidCepException(String message, String cep) {
        super(message);
        this.cep = cep;
    }

    public String getCep() {
        return cep;
    }

    /** Detalhes expostos no corpo da resposta de erro. */
    public Map<String, String> getDetails() {
        return cep == null ? Map.of() : Map.of("cep", cep);
    }
}
