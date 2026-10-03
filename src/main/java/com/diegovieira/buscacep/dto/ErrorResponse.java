package com.diegovieira.buscacep.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Map;

/**
 * Contrato de saída dos erros da API (HTTP 400/404/502/500).
 *
 * <p>{@code details} nunca é nulo (mapa vazio quando não há detalhes) e
 * {@code timestamp} é ISO-8601 em UTC.</p>
 */
public record ErrorResponse(
        String error,
        String message,
        Map<String, String> details,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant timestamp,
        String path) {

    /** Erro legível por máquina; o nome do enum é o valor serializado. */
    public enum ErrorType {
        ValidationError,
        NotFound,
        BadGateway,
        InternalServerError
    }

    public ErrorResponse {
        details = details == null ? Map.of() : Map.copyOf(details);
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * Cria a resposta de erro com {@code timestamp} UTC preenchido automaticamente.
     *
     * @param details mapa de detalhes; {@code null} vira mapa vazio
     */
    public static ErrorResponse of(ErrorType error, String message, Map<String, String> details, String path) {
        return new ErrorResponse(error.name(), message, details, Instant.now(), path);
    }
}
