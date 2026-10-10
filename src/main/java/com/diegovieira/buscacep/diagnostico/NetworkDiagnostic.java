package com.diegovieira.buscacep.diagnostico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.HttpURLConnection;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "diagnostico.rede", havingValue = "habilitado", matchIfMissing = false)
public class NetworkDiagnostic {

    private static final Logger log = LoggerFactory.getLogger(NetworkDiagnostic.class);
    private static final String HOST = "viacep.com.br";
    private static final int HTTPS_PORT = 443;
    private static final int TCP_TIMEOUT_MS = 3000;
    private static final int HTTPS_TIMEOUT_MS = 5000;

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void executarDiagnostico() {
        Thread.ofVirtual().start(this::realizarDiagnostico);
    }

    private void realizarDiagnostico() {
        log.info("[DIAGNOSTICO-REDE] Iniciando diagnostico com {} as {}", HOST, Instant.now());
        diagnosticarDNS();
        diagnosticarTCP();
        diagnosticarHTTPS();
        log.info("[DIAGNOSTICO-REDE] Diagnostico concluido as {}", Instant.now());
    }

    private void diagnosticarDNS() {
        long inicio = System.currentTimeMillis();
        try {
            InetAddress[] enderecos = InetAddress.getAllByName(HOST);
            for (InetAddress addr : enderecos) {
                String tipo = addr.getHostAddress().contains(":") ? "IPv6" : "IPv4";
                log.info("[DIAGNOSTICO-REDE] DNS {} -> {} (tipo: {})", HOST, addr.getHostAddress(), tipo);
            }
        } catch (Exception ex) {
            log.warn("[DIAGNOSTICO-REDE] Falha na resolucao DNS para {}: {}", HOST, ex.getMessage());
        }
        log.info("[DIAGNOSTICO-REDE] DNS tempo: {}ms", System.currentTimeMillis() - inicio);
    }

    private void diagnosticarTCP() {
        long inicio = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, HTTPS_PORT), TCP_TIMEOUT_MS);
            log.info("[DIAGNOSTICO-REDE] TCP {}:{} conectou ({}ms)", HOST, HTTPS_PORT, System.currentTimeMillis() - inicio);
        } catch (Exception ex) {
            log.warn("[DIAGNOSTICO-REDE] TCP {}:{} falhou: {} ({}ms)", HOST, HTTPS_PORT, ex.getMessage(), System.currentTimeMillis() - inicio);
        }
    }

    private void diagnosticarHTTPS() {
        long inicio = System.currentTimeMillis();
        try {
            URL url = new URL("https://" + HOST + "/ws/23565100/json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(HTTPS_TIMEOUT_MS);
            conn.setReadTimeout(HTTPS_TIMEOUT_MS);
            conn.setRequestMethod("GET");
            int status = conn.getResponseCode();
            log.info("[DIAGNOSTICO-REDE] HTTPS {} status={} tempo={}ms", url, status, System.currentTimeMillis() - inicio);
        } catch (Exception ex) {
            log.warn("[DIAGNOSTICO-REDE] HTTPS falhou: {} tempo={}ms", ex.getMessage(), System.currentTimeMillis() - inicio);
        }
    }
}
