package br.com.cloudport.servicocargageral.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.ResultadoAvaria;
import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.StatusAvariaOperacional;
import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.StatusPlanoOperacional;
import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.StatusReservaGate;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class OperacaoIntermodalDominioTest {

    @Test
    void deveControlarPlanejadoVersusRealizado() {
        PlanoOperacionalCarga plano = planoValido();
        ItemPlanoOperacionalCarga item = item("10.000", "5.000", "100.000");
        plano.adicionarItem(item);

        plano.liberar("planejador");
        plano.iniciar("operador");
        item.registrarExecucao(decimal("4.000"), decimal("2.000"), decimal("40.000"),
                "ARMAZEM-A", "CONTEINER-1", null, null, null);
        plano.atualizarStatusExecucao();

        assertEquals(StatusPlanoOperacional.PARCIAL, plano.getStatus());
        assertThrows(IllegalStateException.class, () -> item.registrarExecucao(
                decimal("7.000"), decimal("3.000"), decimal("60.000"),
                "ARMAZEM-A", "CONTEINER-1", null, null, null));

        item.registrarExecucao(decimal("6.000"), decimal("3.000"), decimal("60.000"),
                "ARMAZEM-A", "CONTEINER-1", null, null, null);
        plano.atualizarStatusExecucao();
        plano.concluir(false, "operador", "Executado integralmente.");

        assertTrue(item.estaCompleto());
        assertEquals(StatusPlanoOperacional.CONCLUIDO, plano.getStatus());
    }

    @Test
    void deveConfirmarReservaEmEtapas() {
        ReservaGateCarga reserva = new ReservaGateCarga();
        reserva.setTransacaoId("gate-1");
        reserva.setBlNumero("bl-1");
        reserva.setVeiculoId("abc1d23");
        reserva.setUsuario("operador");
        reserva.setQuantidadeReservada(decimal("10.000"));
        reserva.setVolumeReservadoM3(decimal("5.000"));
        reserva.setPesoReservadoKg(decimal("100.000"));

        reserva.confirmar("conf-1", decimal("4.000"), decimal("2.000"), decimal("40.000"),
                "operador", "corr-1");
        assertEquals(StatusReservaGate.PARCIAL, reserva.getStatus());
        assertEquals(decimal("6.000"), reserva.getQuantidadePendente());

        reserva.confirmar("conf-2", decimal("6.000"), decimal("3.000"), decimal("60.000"),
                "operador", "corr-2");
        assertEquals(StatusReservaGate.CONFIRMADA, reserva.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(3), reserva.getQuantidadePendente());
        assertThrows(IllegalStateException.class, () -> reserva.liberar("indevido"));
    }

    @Test
    void deveRegistrarCicloCompletoDaAvaria() {
        AvariaOperacionalCarga avaria = avariaValida();

        avaria.segregar("inspetor");
        avaria.iniciarInspecao("Reparo possível", "inspetor");
        avaria.encerrar(ResultadoAvaria.REINTEGRAR, "Carga reparada", "supervisor");

        assertEquals(StatusAvariaOperacional.REINTEGRADA, avaria.getStatus());
        assertTrue(avaria.getHistoricoOperacional().contains("REINTEGRAR"));
    }

    @Test
    void deveExigirInspecaoAntesDeEncerrarAvaria() {
        AvariaOperacionalCarga avaria = avariaValida();
        avaria.segregar("inspetor");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> avaria.encerrar(ResultadoAvaria.BAIXAR, "Carga sem recuperação", "supervisor"));

        assertEquals("Avaria deve estar em tratamento após inspeção para ser encerrada.", exception.getMessage());
        assertEquals(StatusAvariaOperacional.SEGREGADA, avaria.getStatus());
    }

    @Test
    void devePermitirManterAvariaBloqueadaDepoisDaInspecao() {
        AvariaOperacionalCarga avaria = avariaValida();
        avaria.segregar("inspetor");
        avaria.iniciarInspecao("Necessária decisão externa", "inspetor");

        avaria.encerrar(ResultadoAvaria.MANTER_BLOQUEADA, "Aguardar seguradora", "supervisor");

        assertEquals(StatusAvariaOperacional.BLOQUEADA, avaria.getStatus());
        assertEquals(ResultadoAvaria.MANTER_BLOQUEADA, avaria.getResultadoTratamento());
        assertTrue(avaria.getHistoricoOperacional().contains("MANTER_BLOQUEADA"));
    }

    private AvariaOperacionalCarga avariaValida() {
        AvariaOperacionalCarga avaria = new AvariaOperacionalCarga();
        avaria.setCodigo("av-1");
        avaria.setDescricao("Embalagem danificada");
        avaria.setQuantidadeAfetada(decimal("2.000"));
        avaria.setVolumeAfetadoM3(decimal("1.000"));
        avaria.setPesoAfetadoKg(decimal("20.000"));
        avaria.setResponsavel("inspetor");
        return avaria;
    }

    private PlanoOperacionalCarga planoValido() {
        PlanoOperacionalCarga plano = new PlanoOperacionalCarga();
        plano.setNumero("plano-1");
        plano.setTipo(TipoServicoOrdemCarga.TRANSLOAD);
        plano.setPrioridade(100);
        plano.setJanelaInicio(OffsetDateTime.now());
        plano.setJanelaFim(OffsetDateTime.now().plusHours(2));
        plano.setLocal("patio-a");
        return plano;
    }

    private ItemPlanoOperacionalCarga item(String quantidade, String volume, String peso) {
        ItemPlanoOperacionalCarga item = new ItemPlanoOperacionalCarga();
        item.setSequencia(1);
        item.setQuantidadePlanejada(decimal(quantidade));
        item.setVolumePlanejadoM3(decimal(volume));
        item.setPesoPlanejadoKg(decimal(peso));
        return item;
    }

    private BigDecimal decimal(String valor) {
        return new BigDecimal(valor);
    }
}
