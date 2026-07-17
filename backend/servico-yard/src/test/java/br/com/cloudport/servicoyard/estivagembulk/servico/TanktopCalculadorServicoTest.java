package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.servicoyard.estivagembulk.dto.PressaoTanktopDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.SetorTanktop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TanktopCalculadorServico - Cálculo de pressão no tank top")
class TanktopCalculadorServicoTest {

    private TanktopCalculadorServico servico;

    @BeforeEach
    void setup() {
        servico = new TanktopCalculadorServico();
    }

    @Test
    @DisplayName("Pressão dentro do limite usa área real do dunnage")
    void pressaoDentroDoLimite() {
        BobinaManifesto bobina = criarBobina("BOB001", 5000.0, 1500.0, 2000.0);
        SetorTanktop setor = criarSetor("CENTRO", 15.0);

        PressaoTanktopDto dto = servico.calcularPressao(bobina, setor, 2, 150.0, 2000.0);

        assertFalse(dto.isExcedido());
        assertTrue(dto.getViolacoes().isEmpty());
        assertEquals(0.60, dto.getAreaContatoM2(), 0.001);
        assertEquals(8.33, dto.getPressaoCalculadaTM2(), 0.01);
    }

    @Test
    @DisplayName("Pressão acima da capacidade gera violação PERIGO")
    void pressaoExcedeLimite() {
        BobinaManifesto bobina = criarBobina("BOB002", 50000.0, 1500.0, 1500.0);
        SetorTanktop setor = criarSetor("PROA", 12.0);

        PressaoTanktopDto dto = servico.calcularPressao(bobina, setor, 2, 150.0, 1500.0);

        assertTrue(dto.isExcedido());
        assertTrue(dto.getViolacoes().stream()
                .anyMatch(violacao -> "SOBRECARGA_TANKTOP".equals(violacao.getTipo())
                        && "PERIGO".equals(violacao.getSeveridade())));
    }

    @Test
    @DisplayName("Pressão entre 80% e 100% da capacidade gera AVISO")
    void pressaoAvisoPerto80Percent() {
        BobinaManifesto bobina = criarBobina("BOB003", 5100.0, 1500.0, 2000.0);
        SetorTanktop setor = criarSetor("POPA", 10.0);

        PressaoTanktopDto dto = servico.calcularPressao(bobina, setor, 2, 150.0, 2000.0);

        assertFalse(dto.isExcedido());
        assertTrue(dto.getViolacoes().stream()
                .anyMatch(violacao -> "MARGEM_TANKTOP_REDUZIDA".equals(violacao.getTipo())
                        && "AVISO".equals(violacao.getSeveridade())));
    }

    @Test
    @DisplayName("Dunnage sem dimensões reais reprova sem aplicar valor padrão")
    void dunnageSemDimensoesReprova() {
        BobinaManifesto bobina = criarBobina("BOB004", 6000.0, 1500.0, 2000.0);
        SetorTanktop setor = criarSetor("CENTRO", 20.0);

        PressaoTanktopDto dto = servico.calcularPressao(bobina, setor, null, null, null);

        assertTrue(dto.isExcedido());
        assertEquals(0.0, dto.getAreaContatoM2());
        assertTrue(dto.getViolacoes().stream()
                .anyMatch(violacao -> "DUNNAGE_INSUFICIENTE".equals(violacao.getTipo())));
    }

    private BobinaManifesto criarBobina(String codigo, double pesoKg, double diametroMm, double larguraMm) {
        BobinaManifesto bobina = new BobinaManifesto();
        bobina.setCodigo(codigo);
        bobina.setPesoKg(pesoKg);
        bobina.setDiametroExternoMm(diametroMm);
        bobina.setLarguraMm(larguraMm);
        return bobina;
    }

    private SetorTanktop criarSetor(String nome, double capacidade) {
        SetorTanktop setor = new SetorTanktop();
        setor.setNome(nome);
        setor.setCapacidadeTM2(capacidade);
        return setor;
    }
}
