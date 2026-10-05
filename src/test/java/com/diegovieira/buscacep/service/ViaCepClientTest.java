package com.diegovieira.buscacep.service;

import com.diegovieira.buscacep.dto.EnderecoResponse;
import com.diegovieira.buscacep.exception.CepNotFoundException;
import com.diegovieira.buscacep.exception.InvalidCepException;
import com.diegovieira.buscacep.exception.UpstreamServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Testes do {@link ViaCepClient} 100% offline: o {@link RestClient} é amarrado ao
 * {@link MockRestServiceServer} do spring-test (já presente no starter-test), sem
 * nenhuma chamada de rede real.
 *
 * <p>A contagem de expectativas do servidor é a prova do comportamento de retry:
 * menos requests que o esperado = retry faltando; mais = retry indevido.</p>
 */
class ViaCepClientTest {

    private static final String BASE_URL = "https://viacep.com.br";
    private static final String CEP = "23565100";
    private static final String URL_CONSULTA = BASE_URL + "/ws/23565100/json";

    /** JSON do ViaCEP inclui campos além do nosso DTO (ibge, ddd, siafi...): devem ser ignorados. */
    private static final String JSON_ENDERECO = """
            {
              "cep": "23565100",
              "logradouro": "Rua Doutor Cerqueira Cesar",
              "complemento": "ate 999/1000",
              "bairro": "Leblon",
              "localidade": "Rio de Janeiro",
              "uf": "RJ",
              "ibge": "3304557",
              "gia": "3321",
              "ddd": "21",
              "siafi": "3707"
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockRestServiceServer server;
    private ViaCepClient viaCepClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        viaCepClient = new ViaCepClient(builder.build(), objectMapper);
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void resposta200ComJsonValidoRetornaEnderecoComCepFormatado() {
        server.expect(requestTo(URL_CONSULTA))
                .andRespond(withSuccess(JSON_ENDERECO, MediaType.APPLICATION_JSON));

        EnderecoResponse endereco = viaCepClient.buscar(CEP);

        // O compact constructor do record formata o cep vindo "23565100" para "23565-100".
        assertEquals("23565-100", endereco.cep());
        assertEquals("Rua Doutor Cerqueira Cesar", endereco.logradouro());
        assertEquals("Leblon", endereco.bairro());
        assertEquals("Rio de Janeiro", endereco.localidade());
        assertEquals("RJ", endereco.uf());
        assertEquals("ate 999/1000", endereco.complemento());
        // verify() no tearDown confirma exatamente UMA chamada: sucesso não repete.
    }

    @Test
    void resposta200ComErroLancaCepNotFoundSemRetry() {
        server.expect(ExpectedCount.times(1), requestTo(URL_CONSULTA))
                .andRespond(withSuccess("{\"erro\": true}", MediaType.APPLICATION_JSON));

        assertThrows(CepNotFoundException.class, () -> viaCepClient.buscar(CEP));

        // times(1) + verify(): se houvesse retry, o servidor reportaria request inesperada.
    }

    @Test
    void retryApos500RetornaSucessoNaSegundaTentativa() {
        server.expect(ExpectedCount.times(1), requestTo(URL_CONSULTA))
                .andRespond(withServerError());
        server.expect(ExpectedCount.times(1), requestTo(URL_CONSULTA))
                .andRespond(withSuccess(JSON_ENDERECO, MediaType.APPLICATION_JSON));

        EnderecoResponse endereco = viaCepClient.buscar(CEP);

        assertEquals("23565-100", endereco.cep());
        // verify() confirma exatamente 2 requests: 1 falha + 1 retry (não mais que isso).
    }

    @Test
    void esgotaRetryCom500SempreLancaUpstreamServiceException() {
        server.expect(ExpectedCount.times(3), requestTo(URL_CONSULTA))
                .andRespond(withServerError());

        UpstreamServiceException ex = assertThrows(
                UpstreamServiceException.class, () -> viaCepClient.buscar(CEP));

        assertEquals("23565-100", ex.getCep());
        assertNotNull(ex.getCause(), "a causa do upstream deve ser preservada");
        // times(3) + verify(): exatamente 1 tentativa + 2 retries, nem uma a mais.
    }

    @Test
    void timeoutEsgotaRetryEUpstreamServiceExceptionComCausa() {
        AtomicInteger chamadas = new AtomicInteger();
        ClientHttpRequestFactory fabricaComTimeout = (uri, httpMethod) -> {
            chamadas.incrementAndGet();
            throw new SocketTimeoutException("Read timed out");
        };
        RestClient comTimeout = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(fabricaComTimeout)
                .build();
        ViaCepClient clienteComTimeout = new ViaCepClient(comTimeout, objectMapper);

        UpstreamServiceException ex = assertThrows(
                UpstreamServiceException.class, () -> clienteComTimeout.buscar(CEP));

        assertEquals(3, chamadas.get(), "timeout é transitório: 1 tentativa + 2 retries");
        assertInstanceOf(ResourceAccessException.class, ex.getCause());
        assertTrue(ex.getCause().getCause() instanceof SocketTimeoutException,
                "o SocketTimeoutException original deve ficar na cadeia de causas");
        assertEquals("23565-100", ex.getCep());
    }

    @Test
    void resposta404HttpDoUpstreamLancaCepNotFoundSemRetry() {
        // Decisão documentada no ViaCepClient: 404 HTTP real = "CEP não existe",
        // mesma semântica do 200 {"erro": true} -> CepNotFoundException, sem retry.
        server.expect(ExpectedCount.times(1), requestTo(URL_CONSULTA))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(CepNotFoundException.class, () -> viaCepClient.buscar(CEP));
    }

    @Test
    void resposta4xxInesperadaLancaUpstreamServiceExceptionSemRetry() {
        server.expect(ExpectedCount.times(1), requestTo(URL_CONSULTA))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        UpstreamServiceException ex = assertThrows(
                UpstreamServiceException.class, () -> viaCepClient.buscar(CEP));

        assertNotNull(ex.getCause(), "a exceção do upstream deve ser preservada como causa");
    }

    @Test
    void corpo200NaoJsonLancaUpstreamServiceExceptionSemRetry() {
        server.expect(ExpectedCount.times(1), requestTo(URL_CONSULTA))
                .andRespond(withSuccess("<html>proxy error</html>", MediaType.TEXT_HTML));

        assertThrows(UpstreamServiceException.class, () -> viaCepClient.buscar(CEP));
    }

    @Test
    void cepNaoNormalizadoLancaInvalidCepSemNenhumaChamadaHttp() {
        assertThrows(InvalidCepException.class, () -> viaCepClient.buscar("23565-100"));
        assertThrows(InvalidCepException.class, () -> viaCepClient.buscar("123"));
        assertThrows(InvalidCepException.class, () -> viaCepClient.buscar(null));

        // Nenhuma expectativa registrada: verify() falharia se alguma request saísse.
    }
}
