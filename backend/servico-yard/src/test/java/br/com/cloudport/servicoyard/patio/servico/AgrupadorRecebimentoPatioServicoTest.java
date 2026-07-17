package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.ContainerOtimizacaoDto;
import br.com.cloudport.servicoyard.patio.dto.GrupoRecebimentoPatioDto;
import br.com.cloudport.servicoyard.patio.dto.PlanoRecebimentoPatioDto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AgrupadorRecebimentoPatioServico")
class AgrupadorRecebimentoPatioServicoTest {

    private AgrupadorRecebimentoPatioServico servico;

    @BeforeEach
    void configurar() {
        servico = new AgrupadorRecebimentoPatioServico();
    }

    @Test
    @DisplayName("Deve manter contêineres operacionalmente compatíveis no mesmo grupo")
    void deveAgruparConteineresCompativeis() {
        ContainerOtimizacaoDto primeiro = criarConteiner(1L, "MSCU0000001", LocalDateTime.of(2026, 7, 18, 9, 10));
        ContainerOtimizacaoDto segundo = criarConteiner(2L, "MSCU0000002", LocalDateTime.of(2026, 7, 18, 11, 40));

        PlanoRecebimentoPatioDto plano = servico.planejar(List.of(primeiro, segundo));

        assertEquals(1, plano.getTotalGrupos());
        assertEquals(2, plano.getTotalConteineres());
        assertEquals(4, plano.getTotalTeus());
        assertEquals(new BigDecimal("48.000"), plano.getPesoTotalToneladas());
        assertEquals(2, plano.getGrupos().get(0).getQuantidadeConteineres());
        assertEquals(LocalDateTime.of(2026, 7, 18, 8, 0),
                plano.getGrupos().get(0).getInicioJanelaRecebimento());
        assertEquals(LocalDateTime.of(2026, 7, 18, 12, 0),
                plano.getGrupos().get(0).getFimJanelaRecebimento());
    }

    @Test
    @DisplayName("Deve separar reefer de contêiner dry")
    void deveSepararReeferDeDry() {
        ContainerOtimizacaoDto dry = criarConteiner(1L, "DRYU0000001", LocalDateTime.of(2026, 7, 18, 9, 0));
        ContainerOtimizacaoDto reefer = criarConteiner(2L, "REFU0000001", LocalDateTime.of(2026, 7, 18, 9, 30));
        reefer.setTipoEquipamento("REEFER");
        reefer.setRefrigerado(true);

        PlanoRecebimentoPatioDto plano = servico.planejar(List.of(dry, reefer));

        assertEquals(2, plano.getTotalGrupos());
        assertTrue(plano.getGrupos().stream().anyMatch(GrupoRecebimentoPatioDto::getRefrigerado));
        assertTrue(plano.getGrupos().stream().anyMatch(grupo -> !grupo.getRefrigerado()));
        assertTrue(plano.getGrupos().get(0).getRefrigerado(),
                "O grupo reefer deve possuir prioridade operacional superior ao dry");
    }

    @Test
    @DisplayName("Deve separar cargas perigosas por classe IMO")
    void deveSepararCargasPerigosasPorClasseImo() {
        ContainerOtimizacaoDto imo3 = criarConteiner(1L, "IMO30000001", LocalDateTime.of(2026, 7, 18, 13, 0));
        imo3.setPerigoso(true);
        imo3.setClasseImo("3");
        ContainerOtimizacaoDto imo8 = criarConteiner(2L, "IMO80000001", LocalDateTime.of(2026, 7, 18, 13, 30));
        imo8.setPerigoso(true);
        imo8.setClasseImo("8");

        PlanoRecebimentoPatioDto plano = servico.planejar(List.of(imo3, imo8));

        assertEquals(2, plano.getTotalGrupos());
        assertTrue(plano.getGrupos().stream().allMatch(GrupoRecebimentoPatioDto::getPerigoso));
        assertTrue(plano.getGrupos().stream().anyMatch(grupo -> "3".equals(grupo.getClasseImo())));
        assertTrue(plano.getGrupos().stream().anyMatch(grupo -> "8".equals(grupo.getClasseImo())));
    }

    @Test
    @DisplayName("Deve separar grupos por janela de recebimento de quatro horas")
    void deveSepararPorJanelaDeRecebimento() {
        ContainerOtimizacaoDto manha = criarConteiner(1L, "MANH0000001", LocalDateTime.of(2026, 7, 18, 7, 59));
        ContainerOtimizacaoDto tarde = criarConteiner(2L, "TARD0000001", LocalDateTime.of(2026, 7, 18, 8, 0));

        PlanoRecebimentoPatioDto plano = servico.planejar(List.of(manha, tarde));

        assertEquals(2, plano.getTotalGrupos());
        assertEquals(LocalDateTime.of(2026, 7, 18, 4, 0), plano.getPrimeiraChegada().withHour(4).withMinute(0));
        assertEquals(LocalDateTime.of(2026, 7, 18, 7, 59), plano.getPrimeiraChegada());
        assertEquals(LocalDateTime.of(2026, 7, 18, 8, 0), plano.getUltimaChegada());
    }

    @Test
    @DisplayName("Deve emitir alertas para dados críticos ausentes")
    void deveEmitirAlertasParaDadosCriticosAusentes() {
        ContainerOtimizacaoDto conteiner = new ContainerOtimizacaoDto();
        conteiner.setId(1L);
        conteiner.setCodigo("ALRT0000001");
        conteiner.setCategoria("EXPORTACAO");
        conteiner.setPerigoso(true);

        PlanoRecebimentoPatioDto plano = servico.planejar(List.of(conteiner));

        assertFalse(plano.getAlertas().isEmpty());
        assertTrue(plano.getAlertas().stream().anyMatch(alerta -> alerta.contains("ETA")));
        assertTrue(plano.getAlertas().stream().anyMatch(alerta -> alerta.contains("classe IMO")));
        assertTrue(plano.getAlertas().stream().anyMatch(alerta -> alerta.contains("visita de saída")));
        assertTrue(plano.getGrupos().get(0).getAlertas().stream()
                .anyMatch(alerta -> alerta.contains("posição definitiva")));
    }

    @Test
    @DisplayName("Deve rejeitar código duplicado independentemente da formatação")
    void deveRejeitarCodigoDuplicado() {
        ContainerOtimizacaoDto primeiro = criarConteiner(1L, "mscu 000001", LocalDateTime.of(2026, 7, 18, 9, 0));
        ContainerOtimizacaoDto segundo = criarConteiner(2L, "MSCU-000001", LocalDateTime.of(2026, 7, 18, 9, 30));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> servico.planejar(List.of(primeiro, segundo)));

        assertTrue(exception.getMessage().contains("duplicado"));
    }

    private ContainerOtimizacaoDto criarConteiner(Long id, String codigo, LocalDateTime etaChegada) {
        ContainerOtimizacaoDto conteiner = new ContainerOtimizacaoDto();
        conteiner.setId(id);
        conteiner.setCodigo(codigo);
        conteiner.setEtaChegada(etaChegada);
        conteiner.setEtaPartida(LocalDateTime.of(2026, 7, 20, 18, 0));
        conteiner.setCategoria("EXPORTACAO");
        conteiner.setArmador("MSC");
        conteiner.setVisitaSaida("MSC-2026-0718");
        conteiner.setDestino("BERCO_1");
        conteiner.setComprimentoPes(40);
        conteiner.setTipoEquipamento("DRY");
        conteiner.setEstadoCarga("CHEIO");
        conteiner.setPesoToneladas(new BigDecimal("24.000"));
        return conteiner;
    }
}
