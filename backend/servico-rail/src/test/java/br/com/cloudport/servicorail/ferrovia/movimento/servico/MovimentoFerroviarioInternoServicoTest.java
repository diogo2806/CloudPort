package br.com.cloudport.servicorail.ferrovia.movimento.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicorail.comum.sanitizacao.SanitizadorEntrada;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicorail.ferrovia.movimento.dto.CancelarMovimentoFerroviarioInternoDto;
import br.com.cloudport.servicorail.ferrovia.movimento.dto.PlanejarMovimentoFerroviarioInternoDto;
import br.com.cloudport.servicorail.ferrovia.movimento.modelo.EstadoMovimentoFerroviarioInterno;
import br.com.cloudport.servicorail.ferrovia.movimento.modelo.MovimentoFerroviarioInterno;
import br.com.cloudport.servicorail.ferrovia.movimento.modelo.TipoRecursoFerroviario;
import br.com.cloudport.servicorail.ferrovia.movimento.repositorio.MovimentoFerroviarioInternoRepositorio;
import br.com.cloudport.servicorail.ferrovia.movimento.repositorio.ReservaRecursoFerroviarioRepositorio;
import br.com.cloudport.servicorail.ferrovia.repositorio.VisitaTremRepositorio;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MovimentoFerroviarioInternoServicoTest {

    @Mock
    private MovimentoFerroviarioInternoRepositorio movimentoRepositorio;

    @Mock
    private ReservaRecursoFerroviarioRepositorio reservaRepositorio;

    @Mock
    private VisitaTremRepositorio visitaTremRepositorio;

    private MovimentoFerroviarioInternoServico servico;

    @BeforeEach
    void configurar() {
        servico = new MovimentoFerroviarioInternoServico(
                movimentoRepositorio,
                reservaRepositorio,
                visitaTremRepositorio,
                new SanitizadorEntrada());
        lenient().when(movimentoRepositorio.save(any(MovimentoFerroviarioInterno.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(movimentoRepositorio.saveAndFlush(any(MovimentoFerroviarioInterno.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void devePlanejarAutorizarIniciarEConcluirLiberandoRecursos() {
        VisitaTrem visita = visita();
        when(visitaTremRepositorio.findById(1L)).thenReturn(Optional.of(visita));

        MovimentoFerroviarioInterno movimento = movimentoPlanejado(visita);
        when(movimentoRepositorio.findOneById(10L)).thenReturn(Optional.of(movimento));
        when(movimentoRepositorio
                .findFirstByVisitaTrem_IdAndReservaAtivaTrueAndInicioPlanejadoLessThanAndFimPlanejadoGreaterThanOrderByInicioPlanejadoAsc(
                        any(), any(), any()))
                .thenReturn(Optional.empty());
        when(reservaRepositorio
                .findFirstByTipoRecursoAndCodigoRecursoIgnoreCaseAndAtivoTrueAndInicioReservaLessThanAndFimReservaGreaterThanOrderByInicioReservaAsc(
                        any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        servico.autorizar(10L, "planejador");
        assertEquals(EstadoMovimentoFerroviarioInterno.AUTORIZADO, movimento.getEstado());
        assertTrue(movimento.getRecursos().stream().allMatch(recurso -> recurso.isAtivo()));

        servico.iniciar(10L, "operador");
        assertEquals(EstadoMovimentoFerroviarioInterno.EM_EXECUCAO, movimento.getEstado());

        servico.concluir(10L, "operador");
        assertEquals(EstadoMovimentoFerroviarioInterno.CONCLUIDO, movimento.getEstado());
        assertFalse(movimento.isReservaAtiva());
        assertTrue(movimento.getRecursos().stream().noneMatch(recurso -> recurso.isAtivo()));
        assertEquals("LINHA-B", visita.getPosicaoFerroviariaAtual());
    }

    @Test
    void deveBloquearAutorizacaoQuandoRecursoEstaOcupado() {
        VisitaTrem visita = visita();
        MovimentoFerroviarioInterno movimento = movimentoPlanejado(visita);
        MovimentoFerroviarioInterno conflito = movimentoPlanejado(visita());

        when(movimentoRepositorio.findOneById(10L)).thenReturn(Optional.of(movimento));
        when(movimentoRepositorio
                .findFirstByVisitaTrem_IdAndReservaAtivaTrueAndInicioPlanejadoLessThanAndFimPlanejadoGreaterThanOrderByInicioPlanejadoAsc(
                        any(), any(), any()))
                .thenReturn(Optional.empty());
        when(reservaRepositorio
                .findFirstByTipoRecursoAndCodigoRecursoIgnoreCaseAndAtivoTrueAndInicioReservaLessThanAndFimReservaGreaterThanOrderByInicioReservaAsc(
                        any(), any(), any(), any()))
                .thenReturn(Optional.of(conflito.getRecursos().get(0)));

        assertThrows(ResponseStatusException.class, () -> servico.autorizar(10L, "planejador"));
    }

    @Test
    void deveCancelarMovimentoAutorizadoELiberarRecursos() {
        MovimentoFerroviarioInterno movimento = movimentoPlanejado(visita());
        movimento.autorizar("planejador");
        when(movimentoRepositorio.findOneById(10L)).thenReturn(Optional.of(movimento));

        CancelarMovimentoFerroviarioInternoDto dto = new CancelarMovimentoFerroviarioInternoDto();
        dto.setMotivo("Interdição operacional");

        servico.cancelar(10L, dto, "operador");

        assertEquals(EstadoMovimentoFerroviarioInterno.CANCELADO, movimento.getEstado());
        assertFalse(movimento.isReservaAtiva());
        assertEquals("Interdição operacional", movimento.getMotivoCancelamento());
    }

    @Test
    void deveBloquearOrigemDiferenteDaPosicaoAtual() {
        VisitaTrem visita = visita();
        visita.setPosicaoFerroviariaAtual("LINHA-X");
        when(visitaTremRepositorio.findById(1L)).thenReturn(Optional.of(visita));

        assertThrows(
                ResponseStatusException.class,
                () -> servico.planejar(dtoPlanejamento(), "planejador"));
    }

    private MovimentoFerroviarioInterno movimentoPlanejado(VisitaTrem visita) {
        LocalDateTime inicio = LocalDateTime.of(2026, 7, 18, 12, 0);
        MovimentoFerroviarioInterno movimento = new MovimentoFerroviarioInterno(
                visita,
                "LINHA-A",
                "LINHA-B",
                inicio,
                inicio.plusMinutes(30),
                "planejador");
        movimento.adicionarRecurso(TipoRecursoFerroviario.ROTA, "ROTA-01");
        movimento.adicionarRecurso(TipoRecursoFerroviario.LINHA, "LINHA-01");
        movimento.adicionarRecurso(TipoRecursoFerroviario.TRECHO, "TRECHO-01");
        movimento.adicionarRecurso(TipoRecursoFerroviario.SWITCH, "SW-01");
        return movimento;
    }

    private PlanejarMovimentoFerroviarioInternoDto dtoPlanejamento() {
        PlanejarMovimentoFerroviarioInternoDto dto = new PlanejarMovimentoFerroviarioInternoDto();
        dto.setVisitaTremId(1L);
        dto.setOrigem("LINHA-A");
        dto.setDestino("LINHA-B");
        dto.setInicioPlanejado(LocalDateTime.of(2026, 7, 18, 12, 0));
        dto.setFimPlanejado(LocalDateTime.of(2026, 7, 18, 12, 30));
        dto.setRotas(Collections.singletonList("ROTA-01"));
        dto.setLinhas(Collections.singletonList("LINHA-01"));
        dto.setTrechos(Arrays.asList("TRECHO-01", "TRECHO-02"));
        dto.setSwitches(Collections.singletonList("SW-01"));
        return dto;
    }

    private VisitaTrem visita() {
        VisitaTrem visita = new VisitaTrem();
        visita.setId(1L);
        visita.setIdentificadorTrem("TREM-001");
        visita.setOperadoraFerroviaria("Operadora Teste");
        visita.setStatusVisita(StatusVisitaTrem.CHEGOU);
        return visita;
    }
}
