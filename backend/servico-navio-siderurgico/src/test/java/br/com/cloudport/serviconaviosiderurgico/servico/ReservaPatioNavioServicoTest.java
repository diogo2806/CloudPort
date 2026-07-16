package br.com.cloudport.serviconaviosiderurgico.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.PosicaoPatioYardCliente.PosicaoPatioYardDTO;
import br.com.cloudport.serviconaviosiderurgico.dominio.FaseVisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusIntegracaoPatio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoCargaSiderurgica;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoReservaPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ReservaPosicaoPatioNavioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private PosicaoPatioYardCliente posicaoCliente;

    private ReservaPatioNavioServico servico;
    private ItemOperacaoNavio item;

    @BeforeEach
    void configurar() {
        servico = new ReservaPatioNavioServico(
                reservaRepositorio,
                itemRepositorio,
                visitaServico,
                posicaoCliente,
                30);
        item = novoItem();

        when(reservaRepositorio.findByStatusAndExpiraEmLessThanEqualOrderByExpiraEmAsc(
                eq(StatusReservaPatioNavio.ATIVA), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(reservaRepositorio.findFirstByItemOperacaoNavioIdAndStatusInOrderByCriadoEmDesc(
                any(), anyList()))
                .thenReturn(Optional.empty());
        when(reservaRepositorio.findAll()).thenReturn(List.of());
        when(reservaRepositorio.save(any(ReservaPosicaoPatioNavio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(itemRepositorio.save(any(ItemOperacaoNavio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void deveRejeitarMapaRealVazio() {
        when(posicaoCliente.listarPosicoes()).thenReturn(List.of());

        assertThatThrownBy(() -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mapa real do patio nao possui posicoes");
    }

    @Test
    void deveRejeitarPosicaoPlanejadaInexistente() {
        item.setPosicaoPatioPlanejada("999");
        when(posicaoCliente.listarPosicoes()).thenReturn(List.of(novaPosicao(10L)));

        assertThatThrownBy(() -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao existe no mapa real");
    }

    @Test
    void deveRejeitarPosicaoOcupada() {
        item.setPosicaoPatioPlanejada("10");
        PosicaoPatioYardDTO ocupada = novaPosicao(10L);
        ocupada.setOcupada(true);
        when(posicaoCliente.listarPosicoes()).thenReturn(List.of(ocupada));

        assertThatThrownBy(() -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ocupada no mapa real");
    }

    @Test
    void deveRejeitarPosicaoJaReservada() {
        item.setPosicaoPatioPlanejada("10");
        ReservaPosicaoPatioNavio ativa = novaReserva(item, 30L, "10");
        when(reservaRepositorio.findAll()).thenReturn(List.of(ativa));
        when(posicaoCliente.listarPosicoes()).thenReturn(List.of(novaPosicao(10L)));

        assertThatThrownBy(() -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ja reservada");
    }

    @Test
    void deveCriarReservaComCoordenadasDoMapaReal() {
        PosicaoPatioYardDTO posicao = novaPosicao(10L);
        posicao.setLinha(2);
        posicao.setColuna(3);
        posicao.setCamadaOperacional("4");
        when(posicaoCliente.listarPosicoes()).thenReturn(List.of(posicao));

        ReservaPosicaoPatioNavio reserva = servico.reservarItem(
                item,
                TipoReservaPatioNavio.DEFINITIVA);

        assertThat(reserva.getPosicaoPatioId()).isEqualTo("10");
        assertThat(reserva.getLinha()).isEqualTo(2);
        assertThat(reserva.getColuna()).isEqualTo(3);
        assertThat(reserva.getCamada()).isEqualTo("4");
        assertThat(reserva.getStatus()).isEqualTo(StatusReservaPatioNavio.ATIVA);
        assertThat(reserva.getTipoReserva()).isEqualTo(TipoReservaPatioNavio.DEFINITIVA);
        assertThat(reserva.getExpiraEm()).isAfter(LocalDateTime.now());
        assertThat(item.getPosicaoPatioPlanejada()).isEqualTo("10");
        assertThat(item.getStatusIntegracaoPatio()).isEqualTo(StatusIntegracaoPatio.RESERVADO);
    }

    @Test
    void deveRejeitarPosicaoBloqueadaInterditadaOuForaDaAreaPermitida() {
        PosicaoPatioYardDTO bloqueada = novaPosicao(1L);
        bloqueada.setBloqueada(true);
        when(posicaoCliente.listarPosicoes()).thenReturn(List.of(bloqueada));
        assertMensagem("bloqueada", () -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA));

        PosicaoPatioYardDTO interditada = novaPosicao(2L);
        interditada.setInterditada(true);
        when(posicaoCliente.listarPosicoes()).thenReturn(List.of(interditada));
        assertMensagem("interditada", () -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA));

        PosicaoPatioYardDTO foraDaArea = novaPosicao(3L);
        foraDaArea.setAreaPermitida(false);
        when(posicaoCliente.listarPosicoes()).thenReturn(List.of(foraDaArea));
        assertMensagem("area permitida", () -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA));
    }

    @Test
    void deveValidarCargaPesoAlturaCamadaECapacidadeDaPilha() {
        PosicaoPatioYardDTO cargaInvalida = novaPosicao(1L);
        cargaInvalida.setTiposCargaPermitidos(List.of("CHAPA"));
        when(posicaoCliente.listarPosicoes()).thenReturn(List.of(cargaInvalida));
        assertMensagem("Tipo de carga", () -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA));

        PosicaoPatioYardDTO pesoInvalido = novaPosicao(2L);
        pesoInvalido.setPesoMaximoToneladas(new BigDecimal("9.000"));
        when(posicaoCliente.listarPosicoes()).thenReturn(List.of(pesoInvalido));
        assertMensagem("Peso da carga", () -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA));

        PosicaoPatioYardDTO alturaInvalida = novaPosicao(3L);
        alturaInvalida.setAlturaMaximaMetros(new BigDecimal("1.000"));
        when(posicaoCliente.listarPosicoes()).thenReturn(List.of(alturaInvalida));
        assertMensagem("Altura da carga", () -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA));

        PosicaoPatioYardDTO camadaInvalida = novaPosicao(4L);
        camadaInvalida.setCamadaOperacional("C4");
        camadaInvalida.setCamadaMaxima(3);
        when(posicaoCliente.listarPosicoes()).thenReturn(List.of(camadaInvalida));
        assertMensagem("Camada 4", () -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA));

        PosicaoPatioYardDTO pilhaCheia = novaPosicao(5L);
        pilhaCheia.setCapacidadePilha(2);
        pilhaCheia.setOcupacaoPilha(2L);
        when(posicaoCliente.listarPosicoes()).thenReturn(List.of(pilhaCheia));
        assertMensagem("Capacidade da pilha", () -> servico.reservarItem(item, TipoReservaPatioNavio.TENTATIVA));
    }

    @Test
    void deveExpirarReservaLimparPlanejamentoEAuditar() {
        item.setPosicaoPatioPlanejada("10");
        item.setStatusIntegracaoPatio(StatusIntegracaoPatio.RESERVADO);
        ReservaPosicaoPatioNavio reserva = novaReserva(item, 40L, "10");
        reserva.setExpiraEm(LocalDateTime.now().minusMinutes(1));

        when(reservaRepositorio.findByStatusAndExpiraEmLessThanEqualOrderByExpiraEmAsc(
                eq(StatusReservaPatioNavio.ATIVA), any(LocalDateTime.class)))
                .thenReturn(List.of(reserva));
        when(itemRepositorio.findById(item.getId())).thenReturn(Optional.of(item));

        int expiradas = servico.expirarReservasVencidas();

        assertThat(expiradas).isEqualTo(1);
        assertThat(reserva.getStatus()).isEqualTo(StatusReservaPatioNavio.EXPIRADA);
        assertThat(item.getStatusIntegracaoPatio()).isEqualTo(StatusIntegracaoPatio.NAO_GERADO);
        assertThat(item.getPosicaoPatioPlanejada()).isNull();
        verify(visitaServico).registrarEvento(
                eq(item.getVisitaNavio()),
                eq(item),
                eq("RESERVA_PATIO_EXPIRADA"),
                any(),
                eq("sistema"),
                eq(StatusReservaPatioNavio.ATIVA.name()),
                eq(StatusReservaPatioNavio.EXPIRADA.name()));
    }

    @Test
    void deveCompensarReservaAnteriorAoReplanejar() {
        item.setPosicaoPatioPlanejada("1");
        item.setStatusIntegracaoPatio(StatusIntegracaoPatio.RESERVADO);
        ReservaPosicaoPatioNavio anterior = novaReserva(item, 50L, "1");
        anterior.setLinha(1);
        anterior.setColuna(1);

        PosicaoPatioYardDTO posicaoAnterior = novaPosicao(1L);
        PosicaoPatioYardDTO novaPosicao = novaPosicao(2L);
        novaPosicao.setColuna(2);
        when(posicaoCliente.listarPosicoes()).thenReturn(List.of(posicaoAnterior, novaPosicao));
        when(reservaRepositorio.findAll()).thenReturn(List.of(anterior));
        when(reservaRepositorio.findFirstByItemOperacaoNavioIdAndStatusInOrderByCriadoEmDesc(
                eq(item.getId()), anyList()))
                .thenReturn(Optional.of(anterior));

        ReservaPosicaoPatioNavio nova = servico.replanejarItem(
                item,
                TipoReservaPatioNavio.DEFINITIVA,
                "operador");

        assertThat(anterior.getStatus()).isEqualTo(StatusReservaPatioNavio.CANCELADA);
        assertThat(nova.getPosicaoPatioId()).isEqualTo("2");
        assertThat(nova.getReservaAnteriorId()).isEqualTo(anterior.getId());
        assertThat(nova.getTipoReserva()).isEqualTo(TipoReservaPatioNavio.DEFINITIVA);
        assertThat(nova.getStatus()).isEqualTo(StatusReservaPatioNavio.ATIVA);
        assertThat(item.getPosicaoPatioPlanejada()).isEqualTo("2");
        assertThat(item.getStatusIntegracaoPatio()).isEqualTo(StatusIntegracaoPatio.RESERVADO);
        verify(visitaServico).registrarEvento(
                eq(item.getVisitaNavio()),
                eq(item),
                eq("RESERVA_PATIO_CANCELADA"),
                any(),
                eq("operador"),
                eq(StatusReservaPatioNavio.ATIVA.name()),
                eq(StatusReservaPatioNavio.CANCELADA.name()));
        verify(visitaServico).registrarEvento(
                eq(item.getVisitaNavio()),
                eq(item),
                eq("RESERVA_PATIO_CRIADA"),
                any(),
                eq("operador"),
                isNull(),
                eq(StatusReservaPatioNavio.ATIVA.name()));
    }

    private ItemOperacaoNavio novoItem() {
        VisitaNavio visita = new VisitaNavio();
        ReflectionTestUtils.setField(visita, "id", 100L);
        visita.setFase(FaseVisitaNavio.PREVISTA);

        ItemOperacaoNavio novoItem = new ItemOperacaoNavio();
        ReflectionTestUtils.setField(novoItem, "id", 200L);
        novoItem.setVisitaNavio(visita);
        novoItem.setCodigoLote("LOTE-1");
        novoItem.setTipoMovimento(TipoMovimentoNavio.DESCARGA);
        novoItem.setTipoCarga(TipoCargaSiderurgica.BOBINA);
        novoItem.setQuantidade(1);
        novoItem.setPesoUnitarioToneladas(new BigDecimal("10.000"));
        novoItem.setPesoTotalToneladas(new BigDecimal("10.000"));
        novoItem.setAlturaCargaMetros(new BigDecimal("2.000"));
        novoItem.setStatus(StatusItemCarga.PLANEJADO);
        novoItem.setStatusIntegracaoPatio(StatusIntegracaoPatio.NAO_GERADO);
        novoItem.setDestinoPatio("BLOCO-A");
        return novoItem;
    }

    private PosicaoPatioYardDTO novaPosicao(Long id) {
        PosicaoPatioYardDTO posicao = new PosicaoPatioYardDTO();
        posicao.setId(id);
        posicao.setLinha(1);
        posicao.setColuna(1);
        posicao.setCamadaOperacional("C1");
        posicao.setBloco("B1");
        posicao.setAreaPermitida(true);
        posicao.setTiposCargaPermitidos(List.of("BOBINA"));
        posicao.setPesoMaximoToneladas(new BigDecimal("20.000"));
        posicao.setAlturaMaximaMetros(new BigDecimal("3.000"));
        posicao.setCamadaMaxima(5);
        posicao.setCapacidadePilha(5);
        return posicao;
    }

    private ReservaPosicaoPatioNavio novaReserva(
            ItemOperacaoNavio itemReserva,
            Long id,
            String posicaoId) {
        ReservaPosicaoPatioNavio reserva = new ReservaPosicaoPatioNavio();
        ReflectionTestUtils.setField(reserva, "id", id);
        reserva.setVisitaNavioId(itemReserva.getVisitaNavio().getId());
        reserva.setItemOperacaoNavioId(itemReserva.getId());
        reserva.setPosicaoPatioId(posicaoId);
        reserva.setStatus(StatusReservaPatioNavio.ATIVA);
        reserva.setTipoReserva(TipoReservaPatioNavio.TENTATIVA);
        reserva.setExpiraEm(LocalDateTime.now().plusMinutes(30));
        return reserva;
    }

    private void assertMensagem(String trecho, Executavel executavel) {
        assertThatThrownBy(executavel::executar)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(trecho);
    }

    @FunctionalInterface
    private interface Executavel {
        void executar();
    }
}
