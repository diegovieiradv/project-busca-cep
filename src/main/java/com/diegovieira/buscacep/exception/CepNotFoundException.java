package com.diegovieira.buscacep.exception;

import java.util.Map;

/**
 * CEP com formato válido, mas sem correspondência na base de consulta.
 * Mapeada para HTTP 404.
 */
public class CepNotFoundException extends RuntimeException {

    private final String cep;

    public CepNotFoundException(String cep) {
        this("CEP não encontrado", cep);
    }

    public CepNotFoundException(String message, String cep) {
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
