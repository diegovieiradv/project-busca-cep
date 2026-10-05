package com.diegovieira.buscacep.controller;

import com.diegovieira.buscacep.dto.EnderecoResponse;
import com.diegovieira.buscacep.service.CepService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de consulta de CEP.
 *
 * <p>Controller fino: só encaminha para o {@link CepService}. Validação e
 * tradução de erros ficam no service e no {@code GlobalExceptionHandler}.</p>
 *
 * <p><strong>Rota:</strong> {@code GET /api/cep/{cep}} (ex.:
 * {@code GET /api/cep/23565-100}).</p>
 *
 * <p><strong>Códigos de resposta:</strong> 200 com {@link EnderecoResponse};
 * 400 CEP inválido; 404 CEP não encontrado; 502 falha no ViaCEP.</p>
 */
@RestController
@RequestMapping("/api/cep")
public class CepController {

    private final CepService cepService;

    public CepController(CepService cepService) {
        this.cepService = cepService;
    }

    /**
     * @param cep CEP com 8 dígitos, com ou sem hífen
     * @return 200 com o endereço; erros são tratados globalmente (400/404/502)
     */
    @GetMapping("/{cep}")
    public ResponseEntity<EnderecoResponse> buscar(@PathVariable String cep) {
        return ResponseEntity.ok(cepService.buscar(cep));
    }
}
