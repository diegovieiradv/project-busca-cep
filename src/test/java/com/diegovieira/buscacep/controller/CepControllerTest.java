package com.diegovieira.buscacep.controller;

import com.diegovieira.buscacep.dto.EnderecoResponse;
import com.diegovieira.buscacep.exception.CepNotFoundException;
import com.diegovieira.buscacep.exception.InvalidCepException;
import com.diegovieira.buscacep.exception.UpstreamServiceException;
import com.diegovieira.buscacep.service.CepService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de fatia MVC da {@link CepController}: só o controller entra no
 * contexto, o {@link CepService} é mockado e o {@code GlobalExceptionHandler}
 * (ambos já existentes) é carregado normalmente pela fatia, o que permite
 * validar o contrato HTTP completo (status + corpo JSON) de 200/400/404/502.
 *
 * <p>Spring Boot 3.5: {@code @MockBean} está deprecated, usa-se
 * {@link MockitoBean}.</p>
 */
@WebMvcTest(CepController.class)
class CepControllerTest {

    private static final String ROTA = "/api/cep";
    private static final String CEP_COM_HIFEN = "23565-100";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CepService cepService;

    private static EnderecoResponse enderecoDeExemplo() {
        return new EnderecoResponse(CEP_COM_HIFEN, "Rua Doutor Cerqueira Cesar", "Leblon",
                "RJ", "Rio de Janeiro", "ate 999/1000");
    }

    @Test
    void cepValidoRetorna200ComJsonDoEndereco() throws Exception {
        when(cepService.buscar(CEP_COM_HIFEN)).thenReturn(enderecoDeExemplo());

        mockMvc.perform(get(ROTA + "/" + CEP_COM_HIFEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cep").value(CEP_COM_HIFEN))
                .andExpect(jsonPath("$.logradouro").value("Rua Doutor Cerqueira Cesar"))
                .andExpect(jsonPath("$.bairro").value("Leblon"))
                .andExpect(jsonPath("$.uf").value("RJ"))
                .andExpect(jsonPath("$.localidade").value("Rio de Janeiro"))
                .andExpect(jsonPath("$.complemento").value("ate 999/1000"));

        verify(cepService).buscar(CEP_COM_HIFEN);
    }

    @Test
    void cepInvalidoRetorna400NoContratoDeErro() throws Exception {
        when(cepService.buscar("123")).thenThrow(new InvalidCepException("123"));

        mockMvc.perform(get(ROTA + "/123"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.message").value("CEP deve conter 8 dígitos numéricos"))
                .andExpect(jsonPath("$.details.cep").value("123"))
                .andExpect(jsonPath("$.path").value("/api/cep/123"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void cepNaoEncontradoRetorna404NoContratoDeErro() throws Exception {
        when(cepService.buscar("99999-999")).thenThrow(new CepNotFoundException("99999-999"));

        mockMvc.perform(get(ROTA + "/99999-999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("NotFound"))
                .andExpect(jsonPath("$.message").value("CEP não encontrado"))
                .andExpect(jsonPath("$.details.cep").value("99999-999"))
                .andExpect(jsonPath("$.path").value("/api/cep/99999-999"));
    }

    @Test
    void falhaNoUpstreamRetorna502NoContratoDeErro() throws Exception {
        when(cepService.buscar(CEP_COM_HIFEN))
                .thenThrow(new UpstreamServiceException("Serviço de consulta indisponível", CEP_COM_HIFEN));

        mockMvc.perform(get(ROTA + "/" + CEP_COM_HIFEN))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("BadGateway"))
                .andExpect(jsonPath("$.message").value("Serviço de consulta indisponível"))
                .andExpect(jsonPath("$.details.cep").value(CEP_COM_HIFEN))
                .andExpect(jsonPath("$.path").value("/api/cep/" + CEP_COM_HIFEN));
    }

    @Test
    void rotaSemParametroRetorna404NoContratoDeErro() throws Exception {
        // Sem parâmetro {cep} o Spring não encontra handler e o recurso não existe:
        // o comportamento natural é 404, devolvido pelo GlobalExceptionHandler.
        mockMvc.perform(get(ROTA + "/"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("NotFound"));
    }
}
