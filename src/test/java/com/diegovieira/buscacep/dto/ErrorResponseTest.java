package com.diegovieira.buscacep.dto;

import com.diegovieira.buscacep.dto.ErrorResponse.ErrorType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorResponseTest {

    @Test
    void factoryPreencheTimestampEDetalhesVazioQuandoNulos() {
        ErrorResponse response = ErrorResponse.of(ErrorType.ValidationError, "Mensagem", null, "/api/cep/123");

        assertEquals("ValidationError", response.error());
        assertEquals("Mensagem", response.message());
        assertEquals("/api/cep/123", response.path());
        assertNotNull(response.timestamp());
        assertEquals(Map.of(), response.details());
    }

    @Test
    void factoryPreservaOsDetailsInformados() {
        ErrorResponse response = ErrorResponse.of(ErrorType.NotFound, "CEP não encontrado",
                Map.of("cep", "99999-999"), "/api/cep/99999-999");

        assertEquals(Map.of("cep", "99999-999"), response.details());
    }

    @Test
    void detailsEDefinidoPorPadraoENuncaNulo() {
        ErrorResponse response = new ErrorResponse("NotFound", "msg", null, Instant.now(), "/x");

        assertEquals(Map.of(), response.details());
        assertThrows(UnsupportedOperationException.class, () -> response.details().put("k", "v"));
    }

    @Test
    void serializaNoFormatoDoContrato() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ErrorResponse response = ErrorResponse.of(ErrorType.BadGateway, "Serviço indisponível",
                Map.of("cep", "23565-100"), "/api/cep/23565-100");

        JsonNode json = mapper.readTree(mapper.writeValueAsString(response));

        assertEquals("BadGateway", json.get("error").asText());
        assertEquals("Serviço indisponível", json.get("message").asText());
        assertEquals("23565-100", json.get("details").get("cep").asText());
        assertEquals("/api/cep/23565-100", json.get("path").asText());
        assertTrue(json.get("timestamp").isTextual(), "timestamp deve ser string ISO-8601");
        assertNotNull(Instant.parse(json.get("timestamp").asText()), "timestamp deve ser parseável como Instant");
    }
}
