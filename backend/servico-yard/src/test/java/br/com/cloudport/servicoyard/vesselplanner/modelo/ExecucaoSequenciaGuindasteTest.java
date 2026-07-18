package br.com.cloudport.servicoyard.vesselplanner.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ExecucaoSequenciaGuindaste")
class ExecucaoSequenciaGuindasteTest {

    @Test
    @DisplayName("Deve registrar início e quantidade realizada na conclusão")
    void deveRegistrarPlanejadoVersusRealizado() {
        MovimentoExecucaoGuindaste movimento = criarMovimento(1, 1);
        LocalDateTime inicio = LocalDateTime.of(2026, 7, 18, 8, 0);
        LocalDateTime conclusao = inicio.plusMinutes(4);

        movimento.iniciar(inicio, "operador-01");
        movimento.concluir(new BigDecimal("0.750"), conclusao, "operador-01");

        assertEquals(StatusMovimentoExecucaoGuindaste.CONCLUIDO, movimento.getStatus());
        assertEquals(new BigDecimal("0.750"), movimento.getQuantidadeRealizada());
        assertEquals(inicio, movimento.getIniciadoEm());
        assertEquals(conclusao, movimento.getConcluidoEm());
    }

    @Test
    @DisplayName("Deve impedir replanejamento depois do início")
    void deveImpedirReplanejamentoDepoisDoInicio() {
        MovimentoExecucaoGuindaste movimento = criarMovimento(1, 1);
        movimento.iniciar(LocalDateTime.now(), "operador-01");

        assertThrows(IllegalStateException.class, () -> movimento.replanejar(
                2,
                3,
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusMinutes(20),
                "Mudança de prioridade operacional",
                "planejador-01"));
    }

    @Test
    @DisplayName("Deve reconciliar somente após todos os movimentos finalizarem")
    void deveReconciliarSomenteMovimentosFinalizados() {
        ExecucaoSequenciaGuindaste execucao = new ExecucaoSequenciaGuindaste();
        execucao.setNumeroGuindastes(2);
        execucao.setJanelaBaseInicio(LocalDateTime.now());
        execucao.setDuracaoMovimentoMinutos(5);
        MovimentoExecucaoGuindaste primeiro = criarMovimento(1, 1);
        MovimentoExecucaoGuindaste segundo = criarMovimento(2, 2);
        execucao.adicionarMovimento(primeiro);
        execucao.adicionarMovimento(segundo);

        primeiro.iniciar(LocalDateTime.now(), "operador-01");
        primeiro.concluir(BigDecimal.ONE, LocalDateTime.now(), "operador-01");
        assertThrows(IllegalStateException.class, () -> execucao.reconciliar(null, "planejador-01"));

        segundo.falhar("Parada do equipamento", BigDecimal.ZERO, LocalDateTime.now(), "operador-02");
        execucao.atualizarStatus();
        assertEquals(StatusExecucaoSequenciaGuindaste.AGUARDANDO_RECONCILIACAO, execucao.getStatus());

        execucao.reconciliar("Falha mantida na reconciliação", "planejador-01");
        assertEquals(StatusExecucaoSequenciaGuindaste.RECONCILIADA, execucao.getStatus());
        assertNotNull(execucao.getReconciliadoEm());
        assertEquals("planejador-01", execucao.getReconciliadoPor());
    }

    private MovimentoExecucaoGuindaste criarMovimento(int ordem, int guindaste) {
        LocalDateTime inicio = LocalDateTime.of(2026, 7, 18, 8, 0).plusMinutes((long) ordem * 5);
        MovimentoExecucaoGuindaste movimento = new MovimentoExecucaoGuindaste();
        movimento.setOrdemPlanejada(ordem);
        movimento.setGuindasteId(guindaste);
        movimento.setCodigoContainer("MSCU000000" + ordem);
        movimento.setBay(ordem);
        movimento.setRowBay(1);
        movimento.setTier(82);
        movimento.setTipoOperacao("DESCARGA");
        movimento.setJanelaInicioPlanejada(inicio);
        movimento.setJanelaFimPlanejada(inicio.plusMinutes(5));
        movimento.setQuantidadePlanejada(BigDecimal.ONE);
        movimento.setQuantidadeRealizada(BigDecimal.ZERO);
        return movimento;
    }
}
