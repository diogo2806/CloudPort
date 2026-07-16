package br.com.cloudport.serviconaviosiderurgico.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import org.springframework.test.util.ReflectionTestUtils;

class ReservaPatioNavioServicoTest {

    private ReservaPosicaoPatioNavioRepositorio reservaRepositorio;
    private ItemOperacaoNavioRepositorio itemRepositorio;
    private VisitaNavioServico visitaServico;
    private PosicaoPatioYardCliente posicaoCliente;
    private ReservaPatioNavioServico servico;

    @BeforeEach
    void configurar() {
        reservaRepositorio = mock(ReservaPosicaoPatioNavioRepositorio.class);
        itemRepositorio = mock(ItemOperacaoNavioRepositorio.class);
        visitaServico = mock(VisitaNavioServico.class);
        posicaoCliente = mock(PosicaoPatioYardCliente.class);
        servico = new ReservaPatioNavioServico(
                reservaRepositorio,
                itemRepositorio,
                visitaServico,
                posicaoCliente,
                30);

        when(reservaRepositorio.findByStatusAndExpiraEmLessThanEqualOrderByExpiraEmAsc(
                eq(StatusReservaPatioNavio.ATIVA), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(reservaRepositorio.findAll()).thenReturn(List.of());
        when(reservaRepositorio.findFirstByItemOperacaoNavioIdAndStatusInOrderByCriadoEmDesc(
                any(), anyList()))
                .thenReturn(Optional.empty());
        when(reservaRepositorio.save(any(ReservaPosicaoPatioNavio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        when(itemRepositorio.save(any(ItemOperacaoNavio.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void deveRejeitarPosicaoBloqueadaInterditadaOuForaDaAreaPermitida() {
        ItemOperacaoNavio item = novoItem();

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
        ItemOperacaoNavio item = novoItem();

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
        ItemOperacaoNavio item = novoItem();
        item.setPosicaoPatioPlanejada("10");
        item.setStatusIntegracaoPatio(StatusIntegracaoPatio.RESERVADO);
        ReservaPosicaoPatioNavio reserva = novaReserva(item, 40L, "10");
        reserva.setExpiraEm(LocalDateTime.now().minusMinutes(1));

        when(reservaRepositorio.findByStatusAndExpiraEmLessThanEqualOrderByExpiraEmAsc(
                eq(StatusReservaPatioNavio.ATIVA), any(LocalDateTime.class)))
                .thenReturn(List.of(reserva));
        when(itemRepositorio.findById(item.getId())).thenReturn(Optional.of(item));

        int expiradas = servico.expirarReservasVencidas();

        assertEquals(1, expiradas);
        assertEquals(StatusReservaPatioNavio.EXPIRADA, reserva.getStatus());
        assertEquals(StatusIntegracaoPatio.NAO_GERADO, item.getStatusIntegracaoPatio());
        assertNull(item.getPosicaoPatioPlanejada());
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
        ItemOperacaoNavio item = novoItem();
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

        assertEquals(StatusReservaPatioNavio.CANCELADA, anterior.getStatus());
        assertEquals("2", nova.getPosicaoPatioId());
        assertEquals(anterior.getId(), nova.getReservaAnteriorId());
        assertEquals(TipoReservaPatioNavio.DEFINITIVA, nova.getTipoReserva());
        assertEquals(StatusReservaPatioNavio.ATIVA, nova.getStatus());
        assertEquals("2", item.getPosicaoPatioPlanejada());
        assertEquals(StatusIntegracaoPatio.RESERVADO, item.getStatusIntegracaoPatio());
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
                eq(null),
                eq(StatusReservaPatioNavio.ATIVA.name()));
    }

    private ItemOperacaoNavio novoItem() {
        VisitaNavio visita = new VisitaNavio();
        ReflectionTestUtils.setField(visita, "id", 100L);
        visita.setFase(FaseVisitaNavio.PREVISTA);

        ItemOperacaoNavio item = new ItemOperacaoNavio();
        ReflectionTestUtils.setField(item, "id", 200L);
        item.setVisitaNavio(visita);
        item.setCodigoLote("LOTE-1");
        item.setTipoMovimento(TipoMovimentoNavio.DESCARGA);
        item.setTipoCarga(TipoCargaSiderurgica.BOBINA);
        item.setQuantidade(1);
        item.setPesoUnitarioToneladas(new BigDecimal("10.000"));
        item.setPesoTotalToneladas(new BigDecimal("10.000"));
        item.setAlturaCargaMetros(new BigDecimal("2.000"));
        item.setStatus(StatusItemCarga.PLANEJADO);
        item.setStatusIntegracaoPatio(StatusIntegracaoPatio.NAO_GERADO);
        return item;
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
            ItemOperacaoNavio item,
            Long id,
            String posicaoId) {
        ReservaPosicaoPatioNavio reserva = new ReservaPosicaoPatioNavio();
        ReflectionTestUtils.setField(reserva, "id", id);
        reserva.setVisitaNavioId(item.getVisitaNavio().getId());
        reserva.setItemOperacaoNavioId(item.getId());
        reserva.setPosicaoPatioId(posicaoId);
        reserva.setStatus(StatusReservaPatioNavio.ATIVA);
        reserva.setTipoReserva(TipoReservaPatioNavio.TENTATIVA);
        reserva.setExpiraEm(LocalDateTime.now().plusMinutes(30));
        return reserva;
    }

    private void assertMensagem(String trecho, Executavel executavel) {
        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                executavel::executar);
        assertTrue(erro.getMessage().contains(trecho));
    }

    @FunctionalInterface
    private interface Executavel {
        void executar();
    }
}
