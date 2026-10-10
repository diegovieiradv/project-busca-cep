package com.diegovieira.buscacep.diagnostico;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "diagnostico.rede=habilitado")
class NetworkDiagnosticTest {

    @Test
    void componenteCarregaQuandoHabilitado() {
        assertNotNull(NetworkDiagnostic.class.getAnnotation(ConditionalOnProperty.class));
    }
}
