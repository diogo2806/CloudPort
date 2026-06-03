package br.com.cloudport.servicoyard.vesselplanner.servico;

import static org.junit.jupiter.api.Assertions.*;

import br.com.cloudport.servicoyard.vesselplanner.dto.EstabilidadeDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.ViolacaoHardConstraintDto;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EstabilidadeNavioServico - Cálculos de estabilidade e hard constraints")
class EstabilidadeNavioServicoTest {

    private EstabilidadeNavioServico servico;

    @BeforeEach
    void setup() {
        servico = new EstabilidadeNavioServico();
    }

    @Test
    @DisplayName("Plano vazio deve retornar estabilidade zero e aprovado")
    void planoVazioRetornaVazio() {
        EstivagemPlan plan = criarPlanoVazio();
        EstabilidadeDto dto = servico.calcular(plan);
        assertTrue(dto.isAprovado());
        assertEquals(0.0, dto.getTrimMetros());
        assertEquals(0.0, dto.getListGraus());
    }

    @Test
    @DisplayName("Trim deve ser positivo quando LCG > LCB (carga ré)")
    void trimPositivoQuandoCarregadoNaRe() {
        EstivagemPlan plan = criarPlanoVazio();
        SlotNavio s = criarSlot(plan, 25, 5, 1, 20000.0);
        plan.getSlots().add(s);

        EstabilidadeDto dto = servico.calcular(plan);
        assertTrue(dto.getTrimMetros() > 0, "Trim deve ser positivo (carga ré)");
    }

    @Test
    @DisplayName("Violação de trim deve ser detectada como PERIGO")
    void trimExcedidoDeveGerarViolacaoPerigo() {
        EstivagemPlan plan = criarPlanoVazio();
        for (int i = 0; i < 5; i++) {
            plan.getSlots().add(criarSlot(plan, 30, i + 1, 1, 200000.0));
        }

        EstabilidadeDto dto = servico.calcular(plan);
        boolean temViolacaoTrim = dto.getViolacoes().stream()
                .anyMatch(v -> "TRIM_EXCEDIDO".equals(v.getTipo()) && "PERIGO".equals(v.getSeveridade()));
        assertTrue(temViolacaoTrim, "Deve detectar violação de trim PERIGO");
        assertFalse(dto.isAprovado());
    }

    @Test
    @DisplayName("Violação de banda (list) deve ser detectada como PERIGO")
    void listExcedidoDeveGerarViolacaoPerigo() {
        EstivagemPlan plan = criarPlanoVazio();
        for (int bay = 1; bay <= 10; bay++) {
            plan.getSlots().add(criarSlot(plan, bay, 10, 1, 100000.0));
        }

        EstabilidadeDto dto = servico.calcular(plan);
        boolean temViolacaoBanda = dto.getViolacoes().stream()
                .anyMatch(v -> "LIST_EXCEDIDO".equals(v.getTipo()));
        assertTrue(temViolacaoBanda, "Deve detectar violação de banda");
    }

    @Test
    @DisplayName("Sobrepeso em slot deve gerar violação PERIGO")
    void sobrepesoSlotDeveGerarViolacao() {
        EstivagemPlan plan = criarPlanoVazio();
        SlotNavio s = criarSlot(plan, 10, 5, 1, 35000.0);
        s.setMaxPesoKg(30000.0);
        plan.getSlots().add(s);

        EstabilidadeDto dto = servico.calcular(plan);
        assertTrue(dto.getViolacoes().stream()
                .anyMatch(v -> "SOBREPESO_SLOT".equals(v.getTipo()) && "PERIGO".equals(v.getSeveridade())));
        assertFalse(dto.isAprovado());
    }

    @Test
    @DisplayName("Container reefer em slot não-reefer deve gerar violação PERIGO")
    void reeferSlotInvalidoDeveGerarViolacao() {
        EstivagemPlan plan = criarPlanoVazio();
        SlotNavio s = criarSlot(plan, 5, 5, 1, 10000.0);
        s.setReefer(true);
        s.setTipoSlot(TipoSlotNavio.NORMAL);
        plan.getSlots().add(s);

        EstabilidadeDto dto = servico.calcular(plan);
        assertTrue(dto.getViolacoes().stream()
                .anyMatch(v -> "REEFER_SLOT_INVALIDO".equals(v.getTipo())));
        assertFalse(dto.isAprovado());
    }

    @Test
    @DisplayName("verificarSlot deve bloquear alocação com sobrepeso")
    void verificarSlotBloqueiaSobrepeso() {
        EstivagemPlan plan = criarPlanoVazio();
        SlotNavio slot = criarSlot(plan, 10, 5, 1, null);
        slot.setMaxPesoKg(20000.0);

        List<ViolacaoHardConstraintDto> violacoes = servico.verificarSlot(
                plan, slot, "CONT001", 25000.0, null, false);

        assertFalse(violacoes.isEmpty());
        assertEquals("SOBREPESO_SLOT", violacoes.get(0).getTipo());
        assertEquals("PERIGO", violacoes.get(0).getSeveridade());
    }

    @Test
    @DisplayName("Plano balanceado deve ter trimMetros e listGraus próximos de zero")
    void planoBalanceadoTemEstabilidadeProximaDeZero() {
        EstivagemPlan plan = criarPlanoVazio();
        // rows 1 and 3 → numRows=3, rowCentro=(3+1)/2=2; (1-2)+(3-2)+(1-2)+(3-2)=0 → TCG=0
        plan.getSlots().add(criarSlot(plan, 14, 1, 1, 10000.0));
        plan.getSlots().add(criarSlot(plan, 16, 3, 1, 10000.0));
        plan.getSlots().add(criarSlot(plan, 15, 1, 1, 10000.0));
        plan.getSlots().add(criarSlot(plan, 15, 3, 1, 10000.0));

        EstabilidadeDto dto = servico.calcular(plan);
        assertEquals(0.0, dto.getListGraus(), 0.01, "List deve ser ~0 para carga simétrica transversalmente");
    }

    private EstivagemPlan criarPlanoVazio() {
        EstivagemPlan plan = new EstivagemPlan();
        return plan;
    }

    private SlotNavio criarSlot(EstivagemPlan plan, int bay, int row, int tier, Double pesoKg) {
        SlotNavio s = new SlotNavio();
        s.setEstivagem(plan);
        s.setBay(bay);
        s.setRowBay(row);
        s.setTier(tier);
        s.setTipoSlot(TipoSlotNavio.NORMAL);
        s.setMaxPesoKg(50000.0);
        s.setPesoKg(pesoKg);
        s.setCodigoContainer(pesoKg != null ? "CONT_" + bay + "_" + row + "_" + tier : null);
        s.setStatusAlertas("OK");
        return s;
    }
}
