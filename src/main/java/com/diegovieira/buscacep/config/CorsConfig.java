package com.diegovieira.buscacep.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Habilita CORS para a API ({@code GET /api/cep/{cep}}) de forma configurável.
 *
 * <p><strong>Origens</strong>: lidas de {@code app.cors.allowed-origins}
 * (lista separada por vírgula). O default ({@code http://localhost:3000}, o
 * front Next.js em dev) fica no {@code application.yml}; em produção a origem
 * da Vercel é injetada pela env var {@code APP_CORS_ALLOWED_ORIGINS} — o
 * Spring converte automaticamente para {@code app.cors.allowed-origins}
 * (relaxed binding).</p>
 *
 * <p><strong>Segurança</strong>: a lista é <em>sempre explícita</em> — nunca se
 * usa {@code allowedOriginPatterns("*")}, porque esse API não usa cookies nem
 * credenciais e devolver {@code *} ampliaria o acesso sem necessidade. Como o
 * {@code allowCredentials} fica falso, a origem autorizada é ecoada no header
 * {@code Access-Control-Allow-Origin} por requisição.</p>
 *
 * <p><strong>Preflight</strong>: o {@code OPTIONS} é tratado pelo próprio
 * Spring MVC (sem chegar ao controller); só {@code GET} e {@code OPTIONS} são
 * liberados porque é tudo o que a API expõe.</p>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /** Origens autorizadas, já separadas, aparadas e sem entradas vazias. */
    private final String[] allowedOrigins;

    /**
     * @param allowedOrigins valor bruto de {@code app.cors.allowed-origins},
     *                       ex.: {@code http://localhost:3000,https://app.vercel.app}
     */
    public CorsConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("Content-Type")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
