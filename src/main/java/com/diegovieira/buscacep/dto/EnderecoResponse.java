package com.diegovieira.buscacep.dto;

import com.diegovieira.buscacep.util.CepValidator;

/**
 * Contrato de saída (HTTP 200) da consulta de CEP.
 *
 * <p>Campos são {@code String} e podem ser nulos: o ViaCEP omite
 * {@code complemento} e, em alguns casos, {@code logradouro}.</p>
 *
 * <p>O compact constructor garante que {@code cep} saia sempre formatado
 * como {@code #####-###}, independentemente de o ViaCEP devolver com ou sem hífen.</p>
 */
public record EnderecoResponse(
        String cep,
        String logradouro,
        String bairro,
        String uf,
        String localidade,
        String complemento) {

    public EnderecoResponse {
        if (cep != null && CepValidator.isValido(cep)) {
            cep = CepValidator.formatar(cep);
        }
    }
}
