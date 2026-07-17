package br.com.cloudport.servicoyard.vesselplanner.servico;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.servicoyard.vesselplanner.dto.EstabilidadeDto;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EstabilidadeNavioServico - cálculo operacional versionado")
class EstabilidadeNavioServicoTest {

    private EstabilidadeNavioServico servico;

    @BeforeEach
    void setup() {
        servico = new EstabilidadeNavioServico();
    }

    @Test
    @DisplayName("Plano com dados ausentes não é aprovado como operacional")
    void planoSemDadosOperacionaisNaoAprova() {
        EstivagemPlan plano = new EstivagemPlan();
        plano.getSlots().add(criarSlotOcupado(true));

        EstabilidadeDto resultado = servico.calcular(plano);

        assertFalse(resultado.isOperacional());
        assertFalse(resultado.isAprovado());
        assertTrue(resultado.getViolacoes().stream()
                .anyMatch(violacao -> "DADOS_ESTABILIDADE_INCOMPLETOS".equals(violacao.getTipo())));
    }

    @Test
    @DisplayName("Dados e coordenadas versionados produzem cálculo operacional")
    void dadosVersionadosProduzemCalculoOperacional() {
        EstivagemPlan plano = criarPlanoOperacional();
        plano.getSlots().add(criarSlotOcupado(true));

        EstabilidadeDto resultado = servico.calcular(plano);

        assertTrue(resultado.isOperacional());
        assertTrue(resultado.isAprovado());
        assertTrue(resultado.getCaladoMedioMetros() > 0.0);
        assertTrue(resultado.getGmMetros() > plano.getGmMinimo());
        assertTrue(resultado.getMemoriaCalculo().contains("hidro=HYDRO-CONTAINER-01"));
        assertTrue(resultado.getMemoriaCalculo().contains("estrutural=STRUCT-CONTAINER-01"));
    }

    @Test
    @DisplayName("Slot sem coordenada física mantém o resultado não operacional")
    void slotSemCoordenadaFisicaNaoAprova() {
        EstivagemPlan plano = criarPlanoOperacional();
        plano.getSlots().add(criarSlotOcupado(false));

        EstabilidadeDto resultado = servico.calcular(plano);

        assertFalse(resultado.isOperacional());
        assertFalse(resultado.isAprovado());
        assertTrue(resultado.getViolacoes().stream()
                .anyMatch(violacao -> violacao.getDescricao().contains("coordenadas físicas")));
    }

    private EstivagemPlan criarPlanoOperacional() {
        EstivagemPlan plano = new EstivagemPlan();
        plano.setCodigoNavio("IMO1234567");
        plano.setCodigoViagem("V001");
        plano.setComprimentoLpp(200.0);
        plano.setBoca(32.0);
        plano.setCalado(10.0);
        plano.setDeslocamento(39000.0);
        plano.setTpc(40.0);
        plano.setLcb(100.0);
        plano.setKm(15.0);
        plano.setMct1cm(2000.0);
        plano.setCaladoMaximo(11.0);
        plano.setTrimMaximo(3.0);
        plano.setBandaMaxima(5.0);
        plano.setGmMinimo(0.5);
        plano.setPesoLeveToneladas(39000.0);
        plano.setLcgPesoLeve(100.0);
        plano.setTcgPesoLeve(0.0);
        plano.setVcgPesoLeve(8.0);
        plano.setPesoLastroToneladas(0.0);
        plano.setVersaoDadosHidrostaticos("HYDRO-CONTAINER-01");
        plano.setVersaoDadosEstruturais("STRUCT-CONTAINER-01");
        plano.setPosicoesSecoes("0;50;100;150;200");
        plano.setPesoLeveSecoes("5000;9000;11000;9000;5000");
        plano.setEmpuxoSecoes("5000;9000;11000;9000;5000");
        plano.setLimitesSfSecoes("1000000000;1000000000;1000000000;1000000000;1000000000");
        plano.setLimitesBmSecoes("1000000000;1000000000;1000000000;1000000000;1000000000");
        return plano;
    }

    private SlotNavio criarSlotOcupado(boolean comCoordenadas) {
        SlotNavio slot = new SlotNavio();
        slot.setBay(10);
        slot.setRowBay(5);
        slot.setTier(2);
        slot.setTipoSlot(TipoSlotNavio.NORMAL);
        slot.setMaxPesoKg(30000.0);
        slot.setCodigoContainer("CPRT1234567");
        slot.setPesoKg(10000.0);
        if (comCoordenadas) {
            slot.setPosLongitudinalMetros(100.0);
            slot.setPosTransversalMetros(0.0);
            slot.setPosVerticalMetros(10.0);
        }
        return slot;
    }
}
