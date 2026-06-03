package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.*;

import br.com.cloudport.servicoyard.estivagembulk.dto.AnaliseEmpilhamentoDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PoraoNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmpilhamentoBobinaServico - Validação de empilhamento e corredor")
class EmpilhamentoBobinaServicoTest {

    private EmpilhamentoBobinaServico servico;

    @BeforeEach
    void setup() {
        servico = new EmpilhamentoBobinaServico();
    }

    @Test
    @DisplayName("Altura da segunda camada calculada corretamente via fórmula de travamento")
    void alturaSegundaCamadaCalculadaCorretamente() {
        // r1 = 1500/2 = 750mm = 0.75m, r2 = 0.75m
        // h = sqrt(r2² + 2×r1×r2) = sqrt(0.5625 + 1.125) = sqrt(1.6875) ≈ 1.299m
        double altura = servico.calcularAlturaCamada(1500.0, 1500.0);

        assertEquals(1.299, altura, 0.001);
    }

    @Test
    @DisplayName("Bobina maior acima de bobina menor gera violação de intertravamento PERIGO")
    void intertravamentoInvalidoGeraViolacao() {
        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        PoraoNavio porao = criarPorao(1L, 20.0, 12.0);

        // Camada 1: Ø1000mm, Camada 2: Ø2000mm (> 1000 × 1.2 = 1200mm → invalid)
        PosicaoBobina pos1 = criarPosicaoBobina(plano, porao, 1, 5.0, 5.0, 1000.0);
        PosicaoBobina pos2 = criarPosicaoBobina(plano, porao, 2, 5.0, 5.0, 2000.0);
        plano.getPosicoes().add(pos1);
        plano.getPosicoes().add(pos2);

        AnaliseEmpilhamentoDto dto = servico.analisarEmpilhamento(plano, 1L);

        assertTrue(dto.getViolacoes().stream()
                .anyMatch(v -> "INTERTRAVAMENTO_INVALIDO".equals(v.getTipo())
                        && "PERIGO".equals(v.getSeveridade())));
    }

    @Test
    @DisplayName("Bobinas ocupando toda a largura do porão bloqueiam corredor")
    void corredorBloqueadoGeraViolacao() {
        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        PoraoNavio porao = criarPorao(1L, 8.0, 12.0); // largura = 8m

        // 3 bobinas Ø3000mm = 3m each → total 9m > 8m → no corridor
        for (int i = 0; i < 3; i++) {
            plano.getPosicoes().add(criarPosicaoBobina(plano, porao, 1, i * 3.0, 2.0, 3000.0));
        }

        AnaliseEmpilhamentoDto dto = servico.analisarEmpilhamento(plano, 1L);

        assertTrue(dto.getViolacoes().stream()
                .anyMatch(v -> "CORREDOR_BLOQUEADO".equals(v.getTipo())));
        assertFalse(dto.isCorredorOperacaoLivre());
    }

    @Test
    @DisplayName("Empilhamento válido com corredor livre não gera violações")
    void empilhamentoValidoSemViolacoes() {
        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        PoraoNavio porao = criarPorao(1L, 20.0, 12.0); // largura = 20m, altura = 12m

        // 1 bobina Ø1500mm = 1.5m total → 20 - 1.5 = 18.5m corredor → valid
        plano.getPosicoes().add(criarPosicaoBobina(plano, porao, 1, 2.0, 5.0, 1500.0));

        AnaliseEmpilhamentoDto dto = servico.analisarEmpilhamento(plano, 1L);

        assertTrue(dto.isCorredorOperacaoLivre());
        assertTrue(dto.getViolacoes().stream()
                .noneMatch(v -> "PERIGO".equals(v.getSeveridade())));
    }

    private PoraoNavio criarPorao(Long id, double largura, double alturaUtil) {
        PoraoNavio p = new PoraoNavio();
        try {
            java.lang.reflect.Field f = PoraoNavio.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, id);
        } catch (Exception ignored) {}
        p.setNumero(1);
        p.setLargura(largura);
        p.setAlturaUtil(alturaUtil);
        p.setComprimento(30.0);
        return p;
    }

    private PosicaoBobina criarPosicaoBobina(PlanoEstivaBulk plano, PoraoNavio porao,
                                               int camada, double x, double y, double diametroMm) {
        BobinaManifesto bobina = new BobinaManifesto();
        bobina.setCodigo("BOB_" + (int) x + "_" + camada);
        bobina.setDiametroExternoMm(diametroMm);
        bobina.setLarguraMm(2000.0);
        bobina.setPesoKg(15000.0);

        PosicaoBobina pos = new PosicaoBobina();
        pos.setPlano(plano);
        pos.setBobina(bobina);
        pos.setPorao(porao);
        pos.setCamada(camada);
        pos.setPosicaoX(x);
        pos.setPosicaoY(y);
        return pos;
    }
}
