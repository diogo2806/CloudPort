package br.com.cloudport.servicocargageral.servico;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.OperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dto.OrdemLiberacaoStuffUnstuffDTOs.OrigemOperacionalRequest;
import br.com.cloudport.servicocargageral.dto.OrdemLiberacaoStuffUnstuffDTOs.TipoOrigemOperacional;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CriarItemOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CriarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.OperacaoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.RegistrarExecucaoRequest;
import br.com.cloudport.servicocargageral.repositorio.OperacaoStuffUnstuffRepositorio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FluxoStuffUnstuffServicoTest {

    @Mock
    private StuffUnstuffServico operacaoServico;

    @Mock
    private PlanoStuffUnstuffServico planoServico;

    @Mock
    private ProgramacaoDocaCargaServico programacaoDocaServico;

    @Mock
    private OrdemLiberacaoStuffUnstuffServico ordemLiberacaoServico;

    @Mock
    private OperacaoStuffUnstuffRepositorio operacaoRepositorio;

    private FluxoStuffUnstuffServico fluxoServico;

    @BeforeEach
    void configurar() {
        fluxoServico = new FluxoStuffUnstuffServico(
                operacaoServico,
                planoServico,
                programacaoDocaServico,
                ordemLiberacaoServico,
                operacaoRepositorio);
    }

    @Test
    void deveCriarVersaoInicialNaMesmaOrquestracaoDaOperacao() {
        UUID operacaoId = UUID.randomUUID();
        CriarOperacaoRequest request = requestComItem();
        OrigemOperacionalRequest origemOperacional = origemOperacional();
        OperacaoResposta resposta = resposta(operacaoId);
        OperacaoStuffUnstuff operacao = new OperacaoStuffUnstuff();
        ReflectionTestUtils.setField(operacao, "id", operacaoId);

        when(operacaoServico.criarOperacaoStuffUnstuff(request)).thenReturn(resposta);
        when(operacaoRepositorio.findDetalhadaById(operacaoId)).thenReturn(Optional.of(operacao));
        when(operacaoServico.obter(operacaoId)).thenReturn(resposta);

        OperacaoResposta resultado = fluxoServico.criar(request, origemOperacional);

        assertSame(resposta, resultado);
        InOrder ordem = inOrder(ordemLiberacaoServico, planoServico);
        ordem.verify(ordemLiberacaoServico).reservar(operacaoId, origemOperacional, BigDecimal.ONE);
        ordem.verify(planoServico).criarVersaoInicial(operacao, "planejador");
    }

    @Test
    void deveExigirLiberacaoPlanoEProgramacaoAntesDeIniciar() {
        UUID operacaoId = UUID.randomUUID();
        OperacaoResposta resposta = resposta(operacaoId);
        when(operacaoServico.iniciar(operacaoId, "operador", "corr-2")).thenReturn(resposta);

        OperacaoResposta resultado = fluxoServico.iniciar(operacaoId, "operador", "corr-2");

        assertSame(resposta, resultado);
        InOrder ordem = inOrder(ordemLiberacaoServico, planoServico, programacaoDocaServico, operacaoServico);
        ordem.verify(ordemLiberacaoServico).validarParaInicio(operacaoId);
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
        InOrder ordem = inOrder(planoServico, programacaoDocaServico, operacaoServico, ordemLiberacaoServico);
        ordem.verify(planoServico).exigirPlanoLiberado(operacaoId);
        ordem.verify(programacaoDocaServico).exigirEmUso(operacaoId);
        ordem.verify(operacaoServico).registrarExecucao(operacaoId, request);
        ordem.verify(ordemLiberacaoServico).consumir(operacaoId, request.commandId(), request.quantidade());
    }

    private CriarOperacaoRequest requestComItem() {
        return new CriarOperacaoRequest(
                TipoOperacaoStuffUnstuff.STUFF,
                "CONT-001",
                null,
                null,
                null,
                null,
                "planejador",
                "corr-1",
                List.of(new CriarItemOperacaoRequest(
                        UUID.randomUUID(),
                        BigDecimal.ONE,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO)));
    }

    private OrigemOperacionalRequest origemOperacional() {
        OffsetDateTime agora = OffsetDateTime.now();
        return new OrigemOperacionalRequest(
                TipoOrigemOperacional.BILL_OF_LADING,
                "BL-001",
                1,
                BigDecimal.TEN,
                agora.minusHours(1),
                agora.plusHours(1),
                false,
                "{\"numero\":\"BL-001\"}");
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
