package com.diegovieira.buscacep.service;

import com.diegovieira.buscacep.dto.EnderecoResponse;
import com.diegovieira.buscacep.exception.CepNotFoundException;
import com.diegovieira.buscacep.exception.InvalidCepException;
import com.diegovieira.buscacep.exception.UpstreamServiceException;
import com.diegovieira.buscacep.util.CepValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.regex.Pattern;

/**
 * Cliente HTTP síncrono do ViaCEP.
 *
 * <p><strong>Contrato do ViaCEP:</strong> CEP existente responde 200 com o JSON do
 * endereço; CEP inexistente responde <em>200</em> com {@code {"erro": true}}
 * (o ViaCEP não usa 404 HTTP para esse caso).</p>
 *
 * <p><strong>Mapeamento de erros:</strong></p>
 * <ul>
 *   <li>200 + JSON de endereço → {@link EnderecoResponse} (cep formatado pelo compact constructor do record)</li>
 *   <li>200 + campo {@code erro} → {@link CepNotFoundException} (sem retry)</li>
 *   <li>timeout / falha de conexão / 5xx → retry até 2x; esgotado → {@link UpstreamServiceException} com causa preservada</li>
 *   <li>404 HTTP real do upstream → {@link CepNotFoundException} (sem retry). Decisão: se um 404
 *       HTTP aparecer, ele carrega a semântica "este CEP não tem representação", a mesma que o
 *       ViaCEP transmite hoje via {@code {"erro": true}}; traduzi-la para 404 mantém a resposta da
 *       nossa API coerente com o resultado real. Caso o ViaCEP passe a devolver 404 para URL
 *       inexistente (erro de configuração), o efeito é o mesmo: recurso não encontrado.</li>
 *   <li>outros 4xx inesperados → {@link UpstreamServiceException} (sem retry)</li>
 *   <li>200 com corpo vazio/inválido → {@link UpstreamServiceException} (sem retry: contrato quebrado)</li>
 * </ul>
 *
 * <p><strong>Retry manual:</strong> loop de no máximo {@value #MAX_TENTATIVAS} tentativas
 * (1 inicial + 2 adicionais) exclusivamente para falhas transitórias (timeout/IO e 5xx),
 * com backoff curto e exponencial ({@value #BACKOFF_INICIAL_MS}ms, depois o dobro).
 * Nenhuma dependência extra (ex.: spring-retry) foi necessária.</p>
 */
@Component
public class ViaCepClient {

    private static final Logger log = LoggerFactory.getLogger(ViaCepClient.class);

    /** 1 chamada inicial + até 2 retries para falhas transitórias. */
    private static final int MAX_TENTATIVAS = 3;

    /** Backoff antes da 2ª tentativa; antes da 3ª é o dobro. */
    private static final long BACKOFF_INICIAL_MS = 200L;

    /** Caminho do recurso de consulta, com {cep} como variável de URI. */
    private static final String CAMINHO_CONSULTA = "/ws/{cep}/json";

    /** Contrato defensivo: aqui só entra CEP já normalizado (8 dígitos, sem hífen). */
    private static final Pattern CEP_NORMALIZADO = Pattern.compile("\\d{8}");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ViaCepClient(RestClient viacepRestClient, ObjectMapper objectMapper) {
        this.restClient = viacepRestClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Consulta o ViaCEP e devolve o endereço correspondente.
     *
     * @param cepNormalizado CEP com exatamente 8 dígitos (sem hífen) — normalizado
     *                       pelo {@code CepValidator} na camada de serviço
     * @return o endereço encontrado, com {@code cep} no formato {@code #####-###}
     * @throws InvalidCepException se o parâmetro não estiver no formato normalizado
     * @throws CepNotFoundException se o ViaCEP responder que o CEP não existe
     * @throws UpstreamServiceException se o ViaCEP estiver indisponível (retry esgotado,
     *                                  status inesperado ou corpo inválido), com a causa preservada
     */
    public EnderecoResponse buscar(String cepNormalizado) {
        exigirCepNormalizado(cepNormalizado);

        String cepFormatado = CepValidator.formatar(cepNormalizado);
        Throwable ultimaFalhaTransitoria = null;

        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            if (tentativa > 1) {
                aguardarBackoff(tentativa, cepFormatado);
            }
            try {
                String corpo = restClient.get()
                        .uri(CAMINHO_CONSULTA, cepNormalizado)
                        .retrieve()
                        .body(String.class);
                EnderecoResponse endereco = interpretarCorpo(corpo, cepFormatado);
                log.info("ViaCEP respondeu com sucesso para o CEP {}", cepFormatado);
                return endereco;
            } catch (ResourceAccessException ex) {
                // Timeout ou falha de conexão: transitória, vale tentar de novo.
                ultimaFalhaTransitoria = ex;
                log.warn("ViaCEP inacessível (tentativa {}/{}): {}", tentativa, MAX_TENTATIVAS, ex.getMessage());
            } catch (HttpServerErrorException ex) {
                // 5xx: transitória, vale tentar de novo.
                ultimaFalhaTransitoria = ex;
                log.warn("ViaCEP devolveu {} (tentativa {}/{}): {}",
                        ex.getStatusCode().value(), tentativa, MAX_TENTATIVAS, ex.getMessage());
            } catch (HttpClientErrorException ex) {
                // 4xx: resposta definitiva do upstream, sem retry.
                log.warn("ViaCEP devolveu {} para o CEP {}", ex.getStatusCode().value(), cepFormatado);
                if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                    throw new CepNotFoundException(cepFormatado);
                }
                throw new UpstreamServiceException(
                        "ViaCEP respondeu com erro " + ex.getStatusCode().value(), cepFormatado, ex);
            }
        }

        log.warn("ViaCEP esgotou as {} tentativas para o CEP {}", MAX_TENTATIVAS, cepFormatado);
        throw new UpstreamServiceException(
                "ViaCEP indisponível após " + MAX_TENTATIVAS + " tentativas", cepFormatado, ultimaFalhaTransitoria);
    }

    /**
     * Interpreta um corpo 200: se vier {@code {"erro": ...}} o CEP não existe;
     * caso contrário o JSON é mapeado para {@link EnderecoResponse}.
     *
     * <p>Campos extras do ViaCEP (ibge, ddd, siafi...) são ignorados: o reader é
     * criado sem {@code FAIL_ON_UNKNOWN_PROPERTIES} para não depender da
     * configuração global do ObjectMapper.</p>
     */
    private EnderecoResponse interpretarCorpo(String corpo, String cepFormatado) {
        if (corpo == null || corpo.isBlank()) {
            throw new UpstreamServiceException("ViaCEP respondeu com corpo vazio", cepFormatado);
        }

        JsonNode node;
        try {
            node = objectMapper.readTree(corpo);
        } catch (JsonProcessingException ex) {
            throw new UpstreamServiceException("ViaCEP respondeu um corpo que não é JSON válido", cepFormatado, ex);
        }

        if (node == null || !node.isObject()) {
            throw new UpstreamServiceException("ViaCEP devolveu payload inesperado", cepFormatado);
        }

        if (contemMarcadorDeErro(node)) {
            throw new CepNotFoundException(cepFormatado);
        }

        try {
            return objectMapper.readerFor(EnderecoResponse.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .treeToValue(node, EnderecoResponse.class);
        } catch (JsonProcessingException ex) {
            throw new UpstreamServiceException("ViaCEP devolveu payload sem os campos esperados", cepFormatado, ex);
        }
    }

    /** O ViaCEP sinaliza "CEP não existe" com o campo {@code erro} (normalmente {@code true}). */
    private boolean contemMarcadorDeErro(JsonNode node) {
        JsonNode erro = node.get("erro");
        if (erro == null || erro.isNull()) {
            return false;
        }
        if (erro.isBoolean()) {
            return erro.asBoolean();
        }
        String texto = erro.asText().trim();
        return !texto.isEmpty() && !"false".equalsIgnoreCase(texto);
    }

    /** Validação defensiva: a normalização é responsabilidade da camada de serviço (Etapa 5). */
    private void exigirCepNormalizado(String cepNormalizado) {
        if (cepNormalizado == null || !CEP_NORMALIZADO.matcher(cepNormalizado).matches()) {
            throw new InvalidCepException(cepNormalizado);
        }
    }

    /** Backoff curto: 200ms antes da 2ª tentativa, 400ms antes da 3ª. */
    private void aguardarBackoff(int tentativa, String cepFormatado) {
        long atrasoMs = BACKOFF_INICIAL_MS << (tentativa - 2);
        try {
            Thread.sleep(atrasoMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new UpstreamServiceException("Thread interrompida durante backoff", cepFormatado, ex);
        }
    }
}
