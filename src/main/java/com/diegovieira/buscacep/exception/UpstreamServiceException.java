package com.diegovieira.buscacep.exception;

import java.util.Map;

/**
 * Falha no serviço externo de consulta de CEP (timeout, 5xx, resposta inválida).
 * Mapeada para HTTP 502.
 */
public class UpstreamServiceException extends RuntimeException {

    private final String cep;

    public UpstreamServiceException(String message) {
        this(message, null, null);
    }

    public UpstreamServiceException(String message, String cep) {
        this(message, cep, null);
    }

    public UpstreamServiceException(String message, String cep, Throwable cause) {
        super(message, cause);
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
