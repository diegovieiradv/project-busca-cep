package com.diegovieira.buscacep.config;

import com.diegovieira.buscacep.controller.CepController;
import com.diegovieira.buscacep.dto.EnderecoResponse;
import com.diegovieira.buscacep.service.CepService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova que a origem configurada em {@code app.cors.allowed-origins} é
 * refletida nas respostas da API: fatia MVC com a {@link CorsConfig} importada,
 * duas origens permitidas via property de teste (dev local + Vercel) e uma
 * origem fora da lista.
 *
 * <p>São verificadas as duas superfícies que o navegador usa: o preflight
 * ({@code OPTIONS}) e a requisição real ({@code GET}).</p>
 */
@WebMvcTest(CepController.class)
@Import(CorsConfig.class)
@TestPropertySource(properties =
        "app.cors.allowed-origins=http://localhost:3000,https://busca-cep.vercel.app")
class CorsConfigTest {

    private static final String ROTA = "/api/cep/23565-100";
    private static final String ORIGEM_DEV = "http://localhost:3000";
    private static final String ORIGEM_VERCEL = "https://busca-cep.vercel.app";
    private static final String ORIGEM_NAO_PERMITIDA = "https://exemplo-invalido.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CepService cepService;

    @Test
    void preflightDeOrigemPermitidaRetornaCabecalhosCors() throws Exception {
        mockMvc.perform(options(ROTA)
                        .header(HttpHeaders.ORIGIN, ORIGEM_VERCEL)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_VERCEL))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")));
    }

    @Test
    void getComOrigemPermitidaRefleteOrigemNaResposta() throws Exception {
        when(cepService.buscar("23565-100")).thenReturn(
                new EnderecoResponse("23565-100", "Rua Doutor Cerqueira Cesar", "Leblon",
                        "RJ", "Rio de Janeiro", "ate 999/1000"));

        mockMvc.perform(get(ROTA).header(HttpHeaders.ORIGIN, ORIGEM_DEV))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DEV));
    }

    @Test
    void getDeOrigemNaoPermitidaNaoRecebeCabecalhoCors() throws Exception {
        when(cepService.buscar("23565-100")).thenReturn(
                new EnderecoResponse("23565-100", "Rua Doutor Cerqueira Cesar", "Leblon",
                        "RJ", "Rio de Janeiro", "ate 999/1000"));

        // Origem fora da lista: a requisição é recusada pelo Spring e nenhum
        // header de CORS é devolvido.
        mockMvc.perform(get(ROTA).header(HttpHeaders.ORIGIN, ORIGEM_NAO_PERMITIDA))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void preflightDeOrigemNaoPermitidaEhRejeitado() throws Exception {
        mockMvc.perform(options(ROTA)
                        .header(HttpHeaders.ORIGIN, ORIGEM_NAO_PERMITIDA)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void requisicaoSemOriginNaoLevaCabecalhoCors() throws Exception {
        // Requisição same-origin (curl/local) não é CORS: processada normalmente.
        when(cepService.buscar("23565-100")).thenReturn(
                new EnderecoResponse("23565-100", "Rua Doutor Cerqueira Cesar", "Leblon",
                        "RJ", "Rio de Janeiro", "ate 999/1000"));

        mockMvc.perform(get(ROTA))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
