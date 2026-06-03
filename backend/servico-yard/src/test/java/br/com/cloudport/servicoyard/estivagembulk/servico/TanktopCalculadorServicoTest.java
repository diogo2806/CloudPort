package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.*;

import br.com.cloudport.servicoyard.estivagembulk.dto.PressaoTanktopDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.SetorTanktop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TanktopCalculadorServico - Cálculo de pressão no tanktop")
class TanktopCalculadorServicoTest {

    private TanktopCalculadorServico servico;

    @BeforeEach
    void setup() {
        servico = new TanktopCalculadorServico();
    }

    @Test
    @DisplayName("Pressão dentro do limite não gera violação")
    void pressaoDentroDoLimite() {
        // 5t on area 0.60m² = 8.33 t/m² < 15.0 t/m² → dentro do limite
        BobinaManifesto bobina = criarBobina("BOB001", 5000.0, 1500.0, 2000.0);
        SetorTanktop setor = criarSetor("CENTRO", 15.0);

        PressaoTanktopDto dto = servico.calcularPressao(bobina, setor, 50.0);

        assertFalse(dto.isExcedido());
        assertTrue(dto.getViolacoes().isEmpty());
        assertTrue(dto.getPressaoCalculadaTM2() > 0);
        assertTrue(dto.getPressaoCalculadaTM2() < 15.0);
    }

    @Test
    @DisplayName("Pressão acima da capacidade gera violação PERIGO")
    void pressaoExcedeLimite() {
        BobinaManifesto bobina = criarBobina("BOB002", 50000.0, 1500.0, 1500.0);
        SetorTanktop setor = criarSetor("PROA", 12.0);

        PressaoTanktopDto dto = servico.calcularPressao(bobina, setor, 50.0);

        assertTrue(dto.isExcedido());
        assertFalse(dto.getViolacoes().isEmpty());
        assertEquals("PERIGO", dto.getViolacoes().get(0).getSeveridade());
        assertEquals("SOBRECARGA_TANKTOP", dto.getViolacoes().get(0).getTipo());
    }

    @Test
    @DisplayName("Pressão entre 80% e 100% da capacidade gera AVISO")
    void pressaoAvisoPerto80Percent() {
        // capacidade = 10.0 t/m², alvo = 85% = 8.5 t/m²
        // contact area = 2 × (50/1000 × 3) × (largura/1000)
        // pressao = pesoT / area → pesoT = pressao × area
        // largura = 2000mm → area = 2 × 0.15 × 2.0 = 0.60 m²
        // pesoT para 8.5 t/m² = 8.5 × 0.60 = 5.1t = 5100kg
        BobinaManifesto bobina = criarBobina("BOB003", 5100.0, 1500.0, 2000.0);
        SetorTanktop setor = criarSetor("POPA", 10.0);

        PressaoTanktopDto dto = servico.calcularPressao(bobina, setor, 50.0);

        assertFalse(dto.isExcedido());
        assertFalse(dto.getViolacoes().isEmpty());
        assertEquals("AVISO", dto.getViolacoes().get(0).getSeveridade());
    }

    @Test
    @DisplayName("Área de contato calculada corretamente: 2 × (0.05×3) × 2.0 = 0.60 m²")
    void contactAreaCalculadaCorretamente() {
        // contact area = 2 × (espessura/1000 × 3) × (largura/1000)
        // = 2 × (50/1000 × 3) × (2000/1000) = 2 × 0.15 × 2.0 = 0.60 m²
        // pressao = (pesoKg/1000) / 0.60
        BobinaManifesto bobina = criarBobina("BOB004", 6000.0, 1500.0, 2000.0);
        SetorTanktop setor = criarSetor("CENTRO", 20.0);

        PressaoTanktopDto dto = servico.calcularPressao(bobina, setor, 50.0);

        double esperado = (6000.0 / 1000.0) / 0.60;
        assertEquals(esperado, dto.getPressaoCalculadaTM2(), 0.01);
    }

    private BobinaManifesto criarBobina(String codigo, double pesoKg, double diametroMm, double larguraMm) {
        BobinaManifesto b = new BobinaManifesto();
        b.setCodigo(codigo);
        b.setPesoKg(pesoKg);
        b.setDiametroExternoMm(diametroMm);
        b.setLarguraMm(larguraMm);
        return b;
    }

    private SetorTanktop criarSetor(String nome, double capacidade) {
        SetorTanktop s = new SetorTanktop();
        s.setNome(nome);
        s.setCapacidadeTM2(capacidade);
        return s;
    }
}
