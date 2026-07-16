package br.com.cloudport.serviconaviosiderurgico.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente.PosicaoPatioYardDTO;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusIntegracaoPatio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ReservaPosicaoPatioNavioRepositorio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaPatioNavioServicoTest {

    @Mock
    private ReservaPosicaoPatioNavioRepositorio reservaRepositorio;
    @Mock
    private ItemOperacaoNavioRepositorio itemRepositorio;
    @Mock
    private VisitaNavioServico visitaServico;
    @Mock
    private PosicaoPatioYardCliente posicaoPatioYardCliente;

    private ReservaPatioNavioServico servico;
    private ItemOperacaoNavio item;

    @BeforeEach
    void configurar() {
        servico = new ReservaPatioNavioServico(
                reservaRepositorio,
                itemRepositorio,
                visitaServico,
                posicaoPatioYardCliente
        );
        VisitaNavio visita = new VisitaNavio();
        ReflectionTestUtils.setField(visita, "id", 42L);
        item = new ItemOperacaoNavio();
        ReflectionTestUtils.setField(item, "id", 7L);
        item.setVisitaNavio(visita);
        item.setDestinoPatio("BLOCO-A");

        when(reservaRepositorio.findFirstByItemOperacaoNavioIdAndStatusInOrderByCriadoEmDesc(any(), anyList()))
                .thenReturn(Optional.empty());
        when(reservaRepositorio.findAll()).thenReturn(List.of());
        when(reservaRepositorio.existsByPosicaoPatioIdIgnoreCaseAndStatusIn(anyString(), anyList()))
                .thenReturn(false);
        when(reservaRepositorio.save(any(ReservaPosicaoPatioNavio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(itemRepositorio.save(any(ItemOperacaoNavio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void deveRejeitarMapaRealVazio() {
        when(posicaoPatioYardCliente.listarPosicoes()).thenReturn(List.of());

        assertThatThrownBy(() -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mapa real do patio nao possui posicoes");
    }

    @Test
    void deveRejeitarPosicaoPlanejadaInexistente() {
        item.setPosicaoPatioPlanejada("999");
        when(posicaoPatioYardCliente.listarPosicoes()).thenReturn(List.of(posicao(10L, false)));

        assertThatThrownBy(() -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao existe no mapa real");
    }

    @Test
    void deveRejeitarPosicaoOcupada() {
        item.setPosicaoPatioPlanejada("10");
        when(posicaoPatioYardCliente.listarPosicoes()).thenReturn(List.of(posicao(10L, true)));

        assertThatThrownBy(() -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ocupada no mapa real");
    }

    @Test
    void deveRejeitarPosicaoJaReservada() {
        item.setPosicaoPatioPlanejada("10");
        when(posicaoPatioYardCliente.listarPosicoes()).thenReturn(List.of(posicao(10L, false)));
        when(reservaRepositorio.existsByPosicaoPatioIdIgnoreCaseAndStatusIn(anyString(), anyList()))
                .thenReturn(true);

        assertThatThrownBy(() -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ja reservada");
    }

    @Test
    void deveCriarReservaComCoordenadasDoMapaReal() {
        when(posicaoPatioYardCliente.listarPosicoes()).thenReturn(List.of(posicao(10L, false)));

        ReservaPosicaoPatioNavio reserva = servico.reservarItem(item, TipoReservaPatioNavio.DEFINITIVA);

        assertThat(reserva.getPosicaoPatioId()).isEqualTo("10");
        assertThat(reserva.getLinha()).isEqualTo(2);
        assertThat(reserva.getColuna()).isEqualTo(3);
        assertThat(reserva.getCamada()).isEqualTo("4");
        assertThat(reserva.getStatus()).isEqualTo(StatusReservaPatioNavio.ATIVA);
        assertThat(reserva.getTipoReserva()).isEqualTo(TipoReservaPatioNavio.DEFINITIVA);
        assertThat(item.getPosicaoPatioPlanejada()).isEqualTo("10");
        assertThat(item.getStatusIntegracaoPatio()).isEqualTo(StatusIntegracaoPatio.RESERVADO);
    }

    private PosicaoPatioYardDTO posicao(Long id, boolean ocupada) {
        PosicaoPatioYardDTO dto = new PosicaoPatioYardDTO();
        dto.setId(id);
        dto.setLinha(2);
        dto.setColuna(3);
        dto.setCamadaOperacional("4");
        dto.setOcupada(ocupada);
        return dto;
    }
}
