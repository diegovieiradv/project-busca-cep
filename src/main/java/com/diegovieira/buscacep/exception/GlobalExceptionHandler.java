package com.diegovieira.buscacep.exception;

import com.diegovieira.buscacep.dto.ErrorResponse;
import com.diegovieira.buscacep.dto.ErrorResponse.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * Converte exceções de domínio e erros inesperados no contrato {@link ErrorResponse}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String MENSAGEM_ERRO_INESPERADO =
            "Ocorreu um erro inesperado. Tente novamente mais tarde.";

    @ExceptionHandler(InvalidCepException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCep(InvalidCepException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorType.ValidationError, ex.getMessage(), ex.getDetails(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(CepNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCepNotFound(CepNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ErrorType.NotFound, ex.getMessage(), ex.getDetails(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(UpstreamServiceException.class)
    public ResponseEntity<ErrorResponse> handleUpstreamService(UpstreamServiceException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(ErrorType.BadGateway, ex.getMessage(), ex.getDetails(),
                        request.getRequestURI()));
    }

    /** Rota inexistente: mantém o 404 do Spring no mesmo contrato de erro da API. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ErrorType.NotFound, "Recurso não encontrado", Map.of(),
                        request.getRequestURI()));
    }

    /** Última linha de defesa: mensagem genérica, sem vazar stacktrace para o cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro não tratado em {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorType.InternalServerError, MENSAGEM_ERRO_INESPERADO, Map.of(),
                        request.getRequestURI()));
    }
}
