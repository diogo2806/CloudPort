package br.com.cloudport.servicocargageral.servico;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.OperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CriarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.OperacaoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.RegistrarExecucaoRequest;
import br.com.cloudport.servicocargageral.repositorio.OperacaoStuffUnstuffRepositorio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FluxoStuffUnstuffServicoTest {

    @Mock
    private StuffUnstuffServico operacaoServico;

    @Mock
    private PlanoStuffUnstuffServico planoServico;

    @Mock
    private ProgramacaoDocaCargaServico programacaoDocaServico;

    @Mock
    private OperacaoStuffUnstuffRepositorio operacaoRepositorio;

    private FluxoStuffUnstuffServico fluxoServico;

    @BeforeEach
    void configurar() {
        fluxoServico = new FluxoStuffUnstuffServico(
                operacaoServico,
                planoServico,
                programacaoDocaServico,
                operacaoRepositorio);
    }

    @Test
    void deveCriarVersaoInicialNaMesmaOrquestracaoDaOperacao() {
        UUID operacaoId = UUID.randomUUID();
        CriarOperacaoRequest request = new CriarOperacaoRequest(
                TipoOperacaoStuffUnstuff.STUFF,
                "CONT-001",
                null,
                null,
                null,
                null,
                "planejador",
                "corr-1",
                List.of());
        OperacaoResposta resposta = resposta(operacaoId);
        OperacaoStuffUnstuff operacao = new OperacaoStuffUnstuff();

        when(operacaoServico.criarOperacaoStuffUnstuff(request)).thenReturn(resposta);
        when(operacaoRepositorio.findDetalhadaById(operacaoId)).thenReturn(Optional.of(operacao));
        when(operacaoServico.obter(operacaoId)).thenReturn(resposta);

        OperacaoResposta resultado = fluxoServico.criar(request);

        assertSame(resposta, resultado);
        verify(planoServico).criarVersaoInicial(operacao, "planejador");
    }

    @Test
    void deveExigirPlanoEProgramacaoAntesDeIniciar() {
        UUID operacaoId = UUID.randomUUID();
        OperacaoResposta resposta = resposta(operacaoId);
        when(operacaoServico.iniciar(operacaoId, "operador", "corr-2")).thenReturn(resposta);

        OperacaoResposta resultado = fluxoServico.iniciar(operacaoId, "operador", "corr-2");

        assertSame(resposta, resultado);
        InOrder ordem = inOrder(planoServico, programacaoDocaServico, operacaoServico);
        ordem.verify(planoServico).exigirPlanoLiberado(operacaoId);
        ordem.verify(programacaoDocaServico).iniciarParaOperacao(operacaoId, "operador", "corr-2");
        ordem.verify(operacaoServico).iniciar(operacaoId, "operador", "corr-2");
    }

    @Test
    void deveExigirPlanoEProgramacaoEmUsoAntesDoApontamento() {
        UUID operacaoId = UUID.randomUUID();
        RegistrarExecucaoRequest request = new RegistrarExecucaoRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                null,
                "operador",
                "corr-3");
        OperacaoResposta resposta = resposta(operacaoId);
        when(operacaoServico.registrarExecucao(operacaoId, request)).thenReturn(resposta);

        OperacaoResposta resultado = fluxoServico.registrarExecucao(operacaoId, request);

        assertSame(resposta, resultado);
        InOrder ordem = inOrder(planoServico, programacaoDocaServico, operacaoServico);
        ordem.verify(planoServico).exigirPlanoLiberado(operacaoId);
        ordem.verify(programacaoDocaServico).exigirEmUso(operacaoId);
        ordem.verify(operacaoServico).registrarExecucao(operacaoId, request);
    }

    private OperacaoResposta resposta(UUID id) {
        return new OperacaoResposta(
                id,
                TipoOperacaoStuffUnstuff.STUFF,
                StatusOperacaoStuffUnstuff.PLANEJADA,
                "CONT-001",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of());
    }
}
