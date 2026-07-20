package br.com.cloudport.servicoyard.patio.dispatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Configuracao;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Rota;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchEnums.ModoDispatch;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchEnums.TipoEscopo;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DispatchSchedulerTest {

    private final DispatchSchedulerRegistry registry = new DispatchSchedulerRegistry();

    @Test
    void devePriorizarBuscaERegistrarMemoriaDeCalculo() {
        OrdemTrabalhoPatio ordem = ordem();
        ordem.setPrioridadeBusca(true);
        ordem.setPrioridadeOperacional(1);
        EquipamentoPatio equipamento = equipamento(TipoEquipamento.TRATOR_PORTUARIO);
        Configuracao configuracao = configuracao(ModoDispatch.SEMIAUTOMATICO);
        Rota rota = new Rota("A", "B", 100.0, 10.0, 120,
                false, false, LocalDateTime.now(), "rota de teste");

        DispatchScheduler.Avaliacao avaliacao = registry.obter(equipamento.getTipoEquipamento())
                .avaliar(ordem, equipamento, configuracao, rota, LocalDateTime.now());

        assertTrue(avaliacao.elegivel());
        assertTrue(avaliacao.score() > 0);
        assertTrue(avaliacao.memoriaCalculo().contains("prioridade="));
        assertTrue(avaliacao.memoriaCalculo().contains("familia=TRATOR_PORTUARIO"));
    }

    @Test
    void deveBloquearAutomaticoComTelemetriaAtrasada() {
        OrdemTrabalhoPatio ordem = ordem();
        EquipamentoPatio equipamento = equipamento(TipoEquipamento.ASC);
        Configuracao configuracao = configuracao(ModoDispatch.AUTOMATICO);
        Rota rota = new Rota("A", "B", 50.0, 0.0, 90,
                false, true, LocalDateTime.now().minusMinutes(10), "telemetria atrasada");

        DispatchScheduler.Avaliacao avaliacao = registry.obter(equipamento.getTipoEquipamento())
                .avaliar(ordem, equipamento, configuracao, rota, LocalDateTime.now());

        assertFalse(avaliacao.elegivel());
        assertTrue(avaliacao.motivosBloqueio().stream()
                .anyMatch(motivo -> motivo.contains("Telemetria")));
    }

    private OrdemTrabalhoPatio ordem() {
        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio();
        ordem.setId(10L);
        ordem.setCodigoConteiner("TEST1234567");
        ordem.setTipoMovimento(TipoMovimentoPatio.TRANSFERENCIA);
        ordem.setCriadoEm(LocalDateTime.now().minusHours(2));
        ordem.setPrioridadeOperacional(10);
        return ordem;
    }

    private EquipamentoPatio equipamento(TipoEquipamento tipo) {
        return new EquipamentoPatio(20L, "CHE-20", tipo, 1, 1, StatusEquipamento.OPERACIONAL);
    }

    private Configuracao configuracao(ModoDispatch modo) {
        return new Configuracao(
                1L,
                TipoEscopo.TERMINAL,
                "PADRAO",
                TipoEquipamento.TRATOR_PORTUARIO,
                1L,
                "ATIVA",
                modo,
                10.0,
                1.0,
                3.0,
                2.0,
                20.0,
                60,
                60,
                120,
                1,
                8,
                false,
                true,
                LocalDateTime.now().minusDays(1),
                null,
                "teste",
                "teste",
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
