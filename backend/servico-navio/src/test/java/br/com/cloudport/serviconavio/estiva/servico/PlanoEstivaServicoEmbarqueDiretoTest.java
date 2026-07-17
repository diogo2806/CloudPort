package br.com.cloudport.serviconavio.estiva.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import br.com.cloudport.serviconavio.escala.repositorio.EscalaRepositorio;
import br.com.cloudport.serviconavio.estiva.dto.EmbarqueDiretoGateResultadoDTO;
import br.com.cloudport.serviconavio.estiva.entidade.AtribuicaoEstiva;
import br.com.cloudport.serviconavio.estiva.entidade.PlanoEstiva;
import br.com.cloudport.serviconavio.estiva.entidade.StatusPlanoEstiva;
import br.com.cloudport.serviconavio.estiva.repositorio.AtribuicaoEstivaRepositorio;
import br.com.cloudport.serviconavio.estiva.repositorio.PlanoEstivaRepositorio;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PlanoEstivaServicoEmbarqueDiretoTest {

    @Mock private PlanoEstivaRepositorio planoEstivaRepositorio;
    @Mock private AtribuicaoEstivaRepositorio atribuicaoEstivaRepositorio;
    @Mock private EscalaRepositorio escalaRepositorio;

    private PlanoEstivaServico service;
    private AtribuicaoEstiva atribuicao;

    @BeforeEach
    void setUp() {
        service = new PlanoEstivaServico(
                planoEstivaRepositorio,
                atribuicaoEstivaRepositorio,
                escalaRepositorio,
                new SanitizadorEntrada());

        PlanoEstiva plano = new PlanoEstiva();
        plano.setId(20L);
        plano.setStatus(StatusPlanoEstiva.CONFIRMADO);
        plano.setBaias(10);
        plano.setFileiras(8);
        plano.setCamadas(6);

        atribuicao = new AtribuicaoEstiva();
        atribuicao.setId(30L);
        atribuicao.setCodigoConteiner("MSCU1234567");
        atribuicao.setBaia(2);
        atribuicao.setFileira(4);
        atribuicao.setCamada(6);
        atribuicao.setEmbarcado(false);
        plano.adicionarAtribuicao(atribuicao);
        when(atribuicaoEstivaRepositorio.findById(30L)).thenReturn(Optional.of(atribuicao));
    }

    @Test
    void deveEmbarcarSemCriarOrigemNoPatio() {
        LocalDateTime horario = LocalDateTime.of(2026, 7, 17, 19, 30);

        EmbarqueDiretoGateResultadoDTO resultado = service.embarcarDiretoDoGate(
                30L, "mscu1234567", horario);

        assertTrue(atribuicao.isEmbarcado());
        assertEquals(horario, atribuicao.getEmbarcadoEm());
        assertEquals(20L, resultado.getPlanoEstivaId());
        assertEquals(StatusPlanoEstiva.CONCLUIDO, atribuicao.getPlano().getStatus());
        verify(atribuicaoEstivaRepositorio).save(atribuicao);
        verify(planoEstivaRepositorio).save(atribuicao.getPlano());
    }

    @Test
    void deveRejeitarAtribuicaoQueVeioDoPatio() {
        atribuicao.setPosicaoPatioOrigem("A01-02-03");
        assertThrows(ResponseStatusException.class,
                () -> service.embarcarDiretoDoGate(30L, "MSCU1234567", LocalDateTime.now()));
        assertFalse(atribuicao.isEmbarcado());
    }

    @Test
    void deveRejeitarConteinerDiferenteDaAtribuicao() {
        assertThrows(ResponseStatusException.class,
                () -> service.embarcarDiretoDoGate(30L, "TGHU7654321", LocalDateTime.now()));
        assertFalse(atribuicao.isEmbarcado());
    }
}
