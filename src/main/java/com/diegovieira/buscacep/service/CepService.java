package com.diegovieira.buscacep.service;

import com.diegovieira.buscacep.dto.EnderecoResponse;
import com.diegovieira.buscacep.util.CepValidator;
import org.springframework.stereotype.Service;

/**
 * Caso de uso "buscar endereço por CEP".
 *
 * <p>Fluxo: valida e normaliza a entrada com {@link CepValidator} (aceita
 * {@code 23565100} ou {@code 23565-100}), delega a consulta ao
 * {@link ViaCepClient} com os 8 dígitos e devolve o {@link EnderecoResponse}
 * pronto (cep já formatado pelo compact constructor do record).</p>
 *
 * <p>Nada é capturado aqui: {@code InvalidCepException},
 * {@code CepNotFoundException} e {@code UpstreamServiceException} sobem
 * naturalmente para o {@code GlobalExceptionHandler}.</p>
 */
@Service
public class CepService {

    private final ViaCepClient viacepClient;

    public CepService(ViaCepClient viacepClient) {
        this.viacepClient = viacepClient;
    }

    /**
     * Consulta o endereço correspondente ao CEP informado.
     *
     * @param cep CEP com 8 dígitos, com ou sem hífen (ex.: {@code 23565-100})
     * @return o endereço encontrado, com {@code cep} no formato {@code #####-###}
     * @throws com.diegovieira.buscacep.exception.InvalidCepException se o CEP não for válido
     * @throws com.diegovieira.buscacep.exception.CepNotFoundException se o CEP não existir na base
     * @throws com.diegovieira.buscacep.exception.UpstreamServiceException se o ViaCEP falhar
     */
    public EnderecoResponse buscar(String cep) {
        String normalizado = CepValidator.normalizar(cep);
        return viacepClient.buscar(normalizado);
    }
}
