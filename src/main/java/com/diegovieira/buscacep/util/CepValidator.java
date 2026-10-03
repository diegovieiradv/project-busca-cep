package com.diegovieira.buscacep.util;

import com.diegovieira.buscacep.exception.InvalidCepException;

import java.util.regex.Pattern;

/**
 * Valida e formata CEP brasileiro.
 *
 * <p>Sem estado e sem efeitos colaterais: todos os métodos são estáticos e puros,
 * o que os torna fáceis de testar e reutilizar em qualquer camada.</p>
 */
public final class CepValidator {

    /** Aceita {@code 23565100} ou {@code 23565-100}. */
    private static final Pattern PADRAO_CEP = Pattern.compile("^\\d{5}-?\\d{3}$");

    private CepValidator() {
    }

    /**
     * Verifica se o CEP está no formato aceito (8 dígitos, hífen opcional após o 5º dígito).
     *
     * @return {@code true} se válido; {@code false} para nulo, vazio ou formato inválido
     */
    public static boolean isValido(String cep) {
        return cep != null && PADRAO_CEP.matcher(cep).matches();
    }

    /**
     * Devolve somente os 8 dígitos do CEP, para chamadas à API ViaCEP.
     *
     * @throws InvalidCepException se o CEP não for válido
     */
    public static String normalizar(String cep) {
        exigirValido(cep);
        return cep.replace("-", "");
    }

    /**
     * Devolve o CEP no formato {@code #####-###}, para a resposta da API.
     *
     * @throws InvalidCepException se o CEP não for válido
     */
    public static String formatar(String cep) {
        exigirValido(cep);
        String digitos = cep.replace("-", "");
        return digitos.substring(0, 5) + "-" + digitos.substring(5);
    }

    private static void exigirValido(String cep) {
        if (!isValido(cep)) {
            throw new InvalidCepException(cep);
        }
    }
}
