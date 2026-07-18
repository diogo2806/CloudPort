package br.com.cloudport.serviconaviosiderurgico.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.contracts.api.ComandoMotivado;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardComandoCliente;
import br.com.cloudport.serviconaviosiderurgico.dominio.PlanoEstivaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.PosicaoEstivaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusPlanoEstivaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.PlanoEstivaNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.PosicaoEstivaNavioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OperacoesAdministrativasNavioServicoTest {

    @Mock
    private VisitaNavioServico visitaServico;
    @Mock
    private ItemOperacaoNavioServico itemServico;
    @Mock
    private PlanoEstivaNavioServico planoServico;
    @Mock
    private PlanoEstivaNavioRepositorio planoRepositorio;
    @Mock
    private PosicaoEstivaNavioRepositorio posicaoRepositorio;
    @Mock
    private OrdemPatioYardCliente ordemConsultaCliente;
    @Mock
    private OrdemPatioYardComandoCliente ordemComandoCliente;
    @Mock
    private SincronizadorStatusNavioPatioServico sincronizadorStatus;

    private OperacoesAdministrativasNavioServico servico;
    private ComandoMotivado comando;

    @BeforeEach
    void configurar() {
        servico = new OperacoesAdministrativasNavioServico(
                visitaServico,
                itemServico,
                planoServico,
                planoRepositorio,
                posicaoRepositorio,
                ordemConsultaCliente,
                ordemComandoCliente,
                sincronizadorStatus);
        comando = new ComandoMotivado(
                "Mudança operacional aprovada",
                "planejador",
                "TESTE",
                "correlation-123");
    }

    @Test
    void deveConcluirEPublicarPlanoValidadoComAuditoria() {
        VisitaNavio visita = mock(VisitaNavio.class);
        PlanoEstivaNavio plano = plano(StatusPlanoEstivaNavio.VALIDADO, visita);
        PlanoEstivaNavioDTO publicado = dto(StatusPlanoEstivaNavio.CONCLUIDO);
        when(planoRepositorio.findById(7L)).thenReturn(Optional.of(plano));
        when(planoServico.concluir(3L, 7L)).thenReturn(publicado);

        PlanoEstivaNavioDTO resposta = servico.publicarPlano(3L, 7L, comando);

        assertThat(resposta.status()).isEqualTo(StatusPlanoEstivaNavio.CONCLUIDO);
        verify(planoServico).concluir(3L, 7L);
        verify(visitaServico).registrarEvento(
                eq(visita),
                eq(null),
                eq("PLANO_PUBLICADO"),
                any(String.class),
                eq("planejador"),
                eq(StatusPlanoEstivaNavio.VALIDADO.name()),
                eq(StatusPlanoEstivaNavio.CONCLUIDO.name()));
    }

    @Test
    void deveInvalidarPlanoPublicadoPreservandoHistorico() {
        VisitaNavio visita = mock(VisitaNavio.class);
        PlanoEstivaNavio plano = plano(StatusPlanoEstivaNavio.CONCLUIDO, visita);
        AtomicReference<StatusPlanoEstivaNavio> status = new AtomicReference<>(StatusPlanoEstivaNavio.CONCLUIDO);
        when(plano.getStatus()).thenAnswer(ignored -> status.get());
        doAnswer(invocation -> {
            status.set(invocation.getArgument(0));
            return null;
        }).when(plano).setStatus(any(StatusPlanoEstivaNavio.class));
        when(planoRepositorio.findById(7L)).thenReturn(Optional.of(plano));
        when(planoRepositorio.save(plano)).thenReturn(plano);
        when(posicaoRepositorio.findByPlanoEstivaIdOrderBySequenciaAscIdAsc(7L)).thenReturn(List.of());

        PlanoEstivaNavioDTO resposta = servico.invalidarPlano(3L, 7L, comando);

        assertThat(resposta.status()).isEqualTo(StatusPlanoEstivaNavio.INVALIDADO);
        verify(visitaServico).registrarEvento(
                eq(visita),
                eq(null),
                eq("PLANO_INVALIDADO"),
                any(String.class),
                eq("planejador"),
                eq(StatusPlanoEstivaNavio.CONCLUIDO.name()),
                eq(StatusPlanoEstivaNavio.INVALIDADO.name()));
    }

    @Test
    void naoDeveCancelarPlanoComMovimentoJaOperado() {
        VisitaNavio visita = mock(VisitaNavio.class);
        PlanoEstivaNavio plano = plano(StatusPlanoEstivaNavio.RASCUNHO, visita);
        PosicaoEstivaNavio posicao = mock(PosicaoEstivaNavio.class);
        when(posicao.getStatus()).thenReturn("OPERADO");
        when(planoRepositorio.findById(7L)).thenReturn(Optional.of(plano));
        when(posicaoRepositorio.findByPlanoEstivaIdOrderBySequenciaAscIdAsc(7L))
                .thenReturn(List.of(posicao));

        assertThatThrownBy(() -> servico.cancelarPlano(3L, 7L, comando))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ja operado");
    }

    private PlanoEstivaNavio plano(StatusPlanoEstivaNavio status, VisitaNavio visita) {
        PlanoEstivaNavio plano = mock(PlanoEstivaNavio.class);
        when(visita.getId()).thenReturn(3L);
        when(plano.getId()).thenReturn(7L);
        when(plano.getVisitaNavio()).thenReturn(visita);
        when(plano.getVersao()).thenReturn(2);
        when(plano.getStatus()).thenReturn(status);
        when(plano.getPesoTotalPlanejado()).thenReturn(BigDecimal.TEN);
        when(plano.getPesoTotalRealizado()).thenReturn(BigDecimal.ZERO);
        when(plano.getCriadoEm()).thenReturn(LocalDateTime.of(2026, 7, 18, 10, 0));
        return plano;
    }

    private PlanoEstivaNavioDTO dto(StatusPlanoEstivaNavio status) {
        return new PlanoEstivaNavioDTO(
                7L,
                3L,
                2,
                status,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                LocalDateTime.of(2026, 7, 18, 10, 0),
                LocalDateTime.of(2026, 7, 18, 10, 30),
                List.of());
    }
}
