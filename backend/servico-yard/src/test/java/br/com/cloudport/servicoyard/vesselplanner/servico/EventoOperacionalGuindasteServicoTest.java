package br.com.cloudport.servicoyard.vesselplanner.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.vesselplanner.dto.EventoOperacionalGuindasteDtos.EventoOperacionalGuindasteResponse;
import br.com.cloudport.servicoyard.vesselplanner.dto.EventoOperacionalGuindasteDtos.RegistrarHandoverRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.EventoOperacionalGuindasteDtos.RegistrarParalisacaoRequest;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EventoOperacionalGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.ExecucaoSequenciaGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.MovimentoExecucaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.NaturezaParalisacaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoEventoOperacionalGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EventoOperacionalGuindasteRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.ExecucaoSequenciaGuindasteRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class EventoOperacionalGuindasteServicoTest {

    private ExecucaoSequenciaGuindasteRepositorio execucaoRepositorio;
    private EventoOperacionalGuindasteRepositorio eventoRepositorio;
    private EventoOperacionalGuindasteServico servico;
    private ExecucaoSequenciaGuindaste execucao;

    @BeforeEach
    void preparar() {
        execucaoRepositorio = mock(ExecucaoSequenciaGuindasteRepositorio.class);
        eventoRepositorio = mock(EventoOperacionalGuindasteRepositorio.class);
        servico = new EventoOperacionalGuindasteServico(execucaoRepositorio, eventoRepositorio);

        EstivagemPlan plano = new EstivagemPlan();
        plano.setId(20L);
        execucao = new ExecucaoSequenciaGuindaste();
        execucao.setId(10L);
        execucao.setEstivagem(plano);
        MovimentoExecucaoGuindaste movimento = new MovimentoExecucaoGuindaste();
        movimento.setId(30L);
        movimento.setGuindasteId(1);
        execucao.adicionarMovimento(movimento);

        when(execucaoRepositorio.findById(10L)).thenReturn(Optional.of(execucao));
        when(eventoRepositorio.findByExecucaoIdAndGuindasteIdAndTipoOrderByInicioAsc(
                10L,
                1,
                TipoEventoOperacionalGuindaste.PARALISACAO))
                .thenReturn(List.of());
        when(eventoRepositorio.saveAndFlush(any(EventoOperacionalGuindaste.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void deveRegistrarParalisacaoPlanejadaSemAlterarPlano() {
        LocalDateTime inicio = LocalDateTime.of(2026, 7, 20, 14, 0);
        RegistrarParalisacaoRequest request = new RegistrarParalisacaoRequest(
                1,
                NaturezaParalisacaoGuindaste.PLANEJADA,
                inicio,
                inicio.plusMinutes(30),
                "Manutenção preventiva",
                "Guindaste indisponível durante a janela",
                "TURNO_A",
                "Retomar a sequência após liberação",
                "Equipe de manutenção avisada");

        EventoOperacionalGuindasteResponse resposta = servico.registrarParalisacao(
                10L,
                request,
                "operador-01");

        assertThat(resposta.tipo()).isEqualTo("PARALISACAO");
        assertThat(resposta.natureza()).isEqualTo("PLANEJADA");
        assertThat(resposta.planId()).isEqualTo(20L);
        assertThat(resposta.estado()).isEqualTo("ENCERRADA");
        assertThat(execucao.getEstivagem().getId()).isEqualTo(20L);
    }

    @Test
    void deveBloquearMovimentoDuranteParalisacaoAberta() {
        EventoOperacionalGuindaste evento = new EventoOperacionalGuindaste();
        evento.setExecucao(execucao);
        evento.setGuindasteId(1);
        evento.setTipo(TipoEventoOperacionalGuindaste.PARALISACAO);
        evento.setNatureza(NaturezaParalisacaoGuindaste.OPERACIONAL);
        evento.setInicio(LocalDateTime.of(2026, 7, 20, 14, 0));
        evento.setMotivo("Falha no spreader");
        when(eventoRepositorio.findByExecucaoIdAndGuindasteIdAndTipoOrderByInicioAsc(
                10L,
                1,
                TipoEventoOperacionalGuindaste.PARALISACAO))
                .thenReturn(List.of(evento));

        assertThatThrownBy(() -> servico.validarGuindasteDisponivel(
                10L,
                1,
                LocalDateTime.of(2026, 7, 20, 14, 10)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("está paralisado");
    }

    @Test
    void deveRegistrarHandoverComPendencias() {
        RegistrarHandoverRequest request = new RegistrarHandoverRequest(
                1,
                LocalDateTime.of(2026, 7, 20, 15, 0),
                "TURNO_A",
                "TURNO_B",
                "operador-02",
                "Concluir bay 14 e conferir tampa do porão",
                "Sem avarias abertas");

        EventoOperacionalGuindasteResponse resposta = servico.registrarHandover(
                10L,
                request,
                "operador-01");

        assertThat(resposta.tipo()).isEqualTo("HANDOVER");
        assertThat(resposta.estado()).isEqualTo("REGISTRADO");
        assertThat(resposta.responsavelDestino()).isEqualTo("operador-02");
        assertThat(resposta.pendencias()).contains("bay 14");
    }
}
