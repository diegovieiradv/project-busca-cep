package com.diegovieira.buscacep.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuração do {@link RestClient} usado para falar com o ViaCEP.
 *
 * <p><strong>Timeouts</strong>: connect 3s e read 5s, aplicados pela
 * {@link JdkClientHttpRequestFactory} sobre o {@link HttpClient} do próprio JDK
 * (java.net.http). Observação: as classes {@code ClientHttpRequestFactorySettings}
 * e {@code ClientHttpRequestFactoryBuilder} citadas no plano da etapa não existem
 * na versão em uso (Spring Framework 6.2.19 / Boot 3.5.16 — foram introduzidas em
 * Spring 7), portanto o caminho equivalente desta versão é configurar o
 * connect timeout no {@link HttpClient} builder e o read timeout na factory.</p>
 *
 * <p>O retry NÃO é feito aqui: fica na {@code ViaCepClient}, que sabe distinguir
 * falha transitória (5xx/timeout) de resultado definitivo (CEP inexistente).</p>
 */
@Configuration
public class ViaCepClientConfig {

    /** Tempo máximo aguardando a conexão TCP com o ViaCEP. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);

    /** Tempo máximo aguardando a resposta (read) do ViaCEP. */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    /**
     * RestClient com baseUrl do ViaCEP e timeouts de rede.
     *
     * @param baseUrl valor de {@code app.viacep.base-url} (ex.: https://viacep.com.br)
     */
    @Bean
    public RestClient viacepRestClient(@Value("${app.viacep.base-url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
