package com.diegovieira.buscacep.exception;

import com.diegovieira.buscacep.dto.ErrorResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste unitário puro do handler: como ainda não existe controller,
 * invocar os métodos diretamente com {@link MockHttpServletRequest} evita
 * subir contexto Spring/WebMvc sem motivo (mais rápido e suficiente para
 * validar status, corpo e serialização).
 */
class GlobalExceptionHandlerTest {

    private static final String URI = "/api/cep/123";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private static MockHttpServletRequest request() {
        return new MockHttpServletRequest("GET", URI);
    }

    @Test
    void cepInvalidoRetorna400ValidationError() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidCep(new InvalidCepException("123"), request());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("ValidationError", body.error());
        assertEquals("CEP deve conter 8 dígitos numéricos", body.message());
        assertEquals(Map.of("cep", "123"), body.details());
        assertEquals(URI, body.path());
        assertNotNull(body.timestamp());
    }

    @Test
    void cepNaoEncontradoRetorna404NotFound() {
        ResponseEntity<ErrorResponse> response = handler.handleCepNotFound(new CepNotFoundException("99999-999"), request());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("NotFound", body.error());
        assertEquals("CEP não encontrado", body.message());
        assertEquals(Map.of("cep", "99999-999"), body.details());
        assertEquals(URI, body.path());
    }

    @Test
    void falhaNoUpstreamRetorna502BadGateway() {
        ResponseEntity<ErrorResponse> response = handler.handleUpstreamService(
                new UpstreamServiceException("Serviço de consulta indisponível", "23565-100"), request());

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("BadGateway", body.error());
        assertEquals(Map.of("cep", "23565-100"), body.details());
        assertEquals(URI, body.path());
    }

    @Test
    void rotaInexistenteRetorna404NotFound() {
        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/api/inexistente"), request());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("NotFound", body.error());
        assertEquals(Map.of(), body.details());
        assertEquals(URI, body.path());
    }

    @Test
    void erroInesperadoRetorna500SemVazarStacktrace() throws Exception {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
                new IllegalStateException("detalhe interno sensível"), request());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("InternalServerError", body.error());
        assertEquals(Map.of(), body.details());

        String json = mapper.writeValueAsString(body);
        assertFalse(json.contains("detalhe interno sensível"), "mensagem interna não pode vazar");
        assertFalse(json.contains("IllegalStateException"), "tipo da exceção não pode vazar");
        assertFalse(json.contains("\tat "), "stacktrace não pode vazar");
    }

    @Test
    void corpoSerializadoSegueOContrato() throws Exception {
        ResponseEntity<ErrorResponse> response = handler.handleCepNotFound(
                new CepNotFoundException("00000-000"), request());

        JsonNode json = mapper.readTree(mapper.writeValueAsString(response.getBody()));

        assertEquals("NotFound", json.get("error").asText());
        assertEquals("CEP não encontrado", json.get("message").asText());
        assertEquals("00000-000", json.get("details").get("cep").asText());
        assertEquals(URI, json.get("path").asText());
        assertTrue(json.get("details").isObject(), "details deve ser objeto JSON");
        assertTrue(json.get("timestamp").isTextual(), "timestamp deve ser string ISO-8601");
        assertNotNull(Instant.parse(json.get("timestamp").asText()));
    }
}
