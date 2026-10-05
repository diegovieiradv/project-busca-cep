package com.diegovieira.buscacep.service;

import com.diegovieira.buscacep.dto.EnderecoResponse;
import com.diegovieira.buscacep.exception.CepNotFoundException;
import com.diegovieira.buscacep.exception.InvalidCepException;
import com.diegovieira.buscacep.exception.UpstreamServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teste unitário do {@link CepService} sem contexto Spring: o
 * {@link ViaCepClient} é substituído por um mock criado manualmente com
 * {@link org.mockito.Mockito#mock} (mesmo sem framework de mock nos testes
 * existentes, o mockito-core já vem no starter-test).
 *
 * <p>O ponto central aqui é a fronteira de normalização: entrada "suja" do
 * usuário → 8 dígitos para o client, e nenhuma chamada ao client quando a
 * entrada é inválida.</p>
 */
class CepServiceTest {

    private static final String CEP_SEM_HIFEN = "23565100";
    private static final String CEP_COM_HIFEN = "23565-100";
    private static final String CEP_NORMALIZADO = "23565100";

    private ViaCepClient viacepClient;
    private CepService cepService;

    @BeforeEach
    void setUp() {
        viacepClient = mock(ViaCepClient.class);
        cepService = new CepService(viacepClient);
    }

    private static EnderecoResponse enderecoDeExemplo() {
        return new EnderecoResponse(CEP_SEM_HIFEN, "Rua Doutor Cerqueira Cesar", "Leblon",
                "RJ", "Rio de Janeiro", "ate 999/1000");
    }

    @Test
    void cepValidoSemHifenEhNormalizadoPara8DigitosAntesDeDelegar() {
        when(viacepClient.buscar(CEP_NORMALIZADO)).thenReturn(enderecoDeExemplo());

        EnderecoResponse endereco = cepService.buscar(CEP_SEM_HIFEN);

        assertEquals("23565-100", endereco.cep());
        assertEquals("Leblon", endereco.bairro());
        // O client só aceita 8 dígitos: o argumento comprovado acima prova a normalização.
        verify(viacepClient).buscar(CEP_NORMALIZADO);
    }

    @Test
    void cepValidoComHifenEhNormalizadoAntesDeDelegar() {
        when(viacepClient.buscar(CEP_NORMALIZADO)).thenReturn(enderecoDeExemplo());

        EnderecoResponse endereco = cepService.buscar(CEP_COM_HIFEN);

        assertEquals("23565-100", endereco.cep());
        // Hífen é removido: o mock só responde se o argumento for exatamente os 8 dígitos.
        verify(viacepClient).buscar(CEP_NORMALIZADO);
    }

    @Test
    void cepInvalidoLancaInvalidCepExceptionSemChamarOClient() {
        String[] entradasInvalidas = {null, "", "123", "abcdefgh", "23565 100"};

        for (String entrada : entradasInvalidas) {
            assertThrows(InvalidCepException.class, () -> cepService.buscar(entrada),
                    "entrada " + entrada + " deveria ser rejeitada antes do client");
        }

        verify(viacepClient, never()).buscar(any());
    }

    @Test
    void cepNaoEncontradoDoClientPropagaSemSerTransformado() {
        when(viacepClient.buscar(CEP_NORMALIZADO)).thenThrow(new CepNotFoundException(CEP_COM_HIFEN));

        CepNotFoundException ex = assertThrows(CepNotFoundException.class,
                () -> cepService.buscar(CEP_COM_HIFEN));

        assertEquals(CEP_COM_HIFEN, ex.getCep());
        assertEquals("CEP não encontrado", ex.getMessage());
    }

    @Test
    void falhaDeUpstreamDoClientPropagaSemSerTransformada() {
        UpstreamServiceException falha = new UpstreamServiceException("ViaCEP indisponível", CEP_COM_HIFEN);
        when(viacepClient.buscar(CEP_NORMALIZADO)).thenThrow(falha);

        UpstreamServiceException ex = assertThrows(UpstreamServiceException.class,
                () -> cepService.buscar(CEP_COM_HIFEN));

        // O handler global depende do TIPO exato da exceção: o service não pode embrulhá-la.
        assertEquals("ViaCEP indisponível", ex.getMessage());
        assertEquals(CEP_COM_HIFEN, ex.getCep());
    }
}
