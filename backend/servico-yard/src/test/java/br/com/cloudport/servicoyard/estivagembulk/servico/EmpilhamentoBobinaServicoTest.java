package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.*;

import br.com.cloudport.servicoyard.estivagembulk.dto.AnaliseEmpilhamentoDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PoraoNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmpilhamentoBobinaServico - Análise de empilhamento de bobinas")
class EmpilhamentoBobinaServicoTest {

    private EmpilhamentoBobinaServico servico;

    @BeforeEach
    void setup() {
        servico = new EmpilhamentoBobinaServico();
    }

    @Test
    @DisplayName("Altura da segunda camada calculada corretamente")
    void alturaSegundaCamadaCalculadaCorretamente() {
        double d1 = 1500.0;
        double d2 = 1500.0;

        double altura = servico.calcularAlturaCamada(d1, d2);

        double r1 = d1 / 2000.0;
        double r2 = d2 / 2000.0;
        double esperado = Math.sqrt(r2 * r2 + 2.0 * r1 * r2);
        assertEquals(esperado, altura, 0.001);
        assertEquals(1.299, altura, 0.001);
    }

    @Test
    @DisplayName("Intertravamento inválido (bobina topo > 1.2× base) gera violação PERIGO")
    void intertravamentoInvalidoGeraViolacao() {
        NavioGranel navio = criarNavio();
        PoraoNavio porao = criarPorao(navio, 1, 0.0, 50.0, 10.0);
        navio.getPoroes().add(porao);
        porao.setId(1L);

        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);

        BobinaManifesto bobBase = criarBobina("BOB-BASE", 20000.0, 1000.0, 2000.0);
        BobinaManifesto bobTopo = criarBobina("BOB-TOPO", 15000.0, 2000.0, 2000.0);

        PosicaoBobina posBase = criarPosicao(plano, porao, bobBase, 1, 5.0, 5.0);
        PosicaoBobina posTopo = criarPosicao(plano, porao, bobTopo, 2, 5.0, 5.0);
        plano.getPosicoes().add(posBase);
        plano.getPosicoes().add(posTopo);

        AnaliseEmpilhamentoDto dto = servico.analisarEmpilhamento(plano, 1L);

        assertTrue(dto.getViolacoes().stream()
                .anyMatch(v -> "INTERTRAVAMENTO_INVALIDO".equals(v.getTipo())
                        && "PERIGO".equals(v.getSeveridade())),
                "Deve detectar intertravamento inválido");
    }

    @Test
    @DisplayName("Corredor bloqueado quando soma das bobinas excede largura do porão")
    void corredorBloqueadoGeraViolacao() {
        NavioGranel navio = criarNavio();
        PoraoNavio porao = criarPorao(navio, 1, 0.0, 30.0, 10.0);
        navio.getPoroes().add(porao);
        porao.setId(2L);

        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);

        for (int i = 0; i < 3; i++) {
            BobinaManifesto bob = criarBobina("BOB-" + i, 20000.0, 3500.0, 2000.0);
            PosicaoBobina pos = criarPosicao(plano, porao, bob, 1, i * 4.0, 5.0);
            plano.getPosicoes().add(pos);
        }

        AnaliseEmpilhamentoDto dto = servico.analisarEmpilhamento(plano, 2L);

        assertFalse(dto.isCorredorOperacaoLivre());
        assertTrue(dto.getViolacoes().stream()
                .anyMatch(v -> "CORREDOR_BLOQUEADO".equals(v.getTipo())
                        && "PERIGO".equals(v.getSeveridade())),
                "Deve detectar corredor bloqueado");
    }

    @Test
    @DisplayName("Empilhamento válido não gera violações e corredor está livre")
    void empilhamentoValidoSemViolacoes() {
        NavioGranel navio = criarNavio();
        PoraoNavio porao = criarPorao(navio, 1, 0.0, 50.0, 20.0);
        navio.getPoroes().add(porao);
        porao.setId(3L);

        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);

        BobinaManifesto bobBase = criarBobina("BOB-A", 20000.0, 1500.0, 2000.0);
        BobinaManifesto bobTopo = criarBobina("BOB-B", 15000.0, 1500.0, 2000.0);

        PosicaoBobina posBase = criarPosicao(plano, porao, bobBase, 1, 5.0, 5.0);
        PosicaoBobina posTopo = criarPosicao(plano, porao, bobTopo, 2, 5.0, 5.0);
        plano.getPosicoes().add(posBase);
        plano.getPosicoes().add(posTopo);

        AnaliseEmpilhamentoDto dto = servico.analisarEmpilhamento(plano, 3L);

        assertTrue(dto.isCorredorOperacaoLivre(), "Corredor deve estar livre");
        assertTrue(dto.getViolacoes().stream()
                .noneMatch(v -> "PERIGO".equals(v.getSeveridade())),
                "Não deve haver violações PERIGO");
        assertTrue(dto.getAlturaFinalM() > 0);
        assertEquals(2, dto.getTotalCamadas());
    }

    private NavioGranel criarNavio() {
        NavioGranel n = new NavioGranel();
        n.setNome("MV TEST");
        n.setLpp(200.0);
        n.setBoca(32.0);
        n.setCalado(10.0);
        return n;
    }

    private PoraoNavio criarPorao(NavioGranel navio, int numero, double inicio, double fim, double largura) {
        PoraoNavio p = new PoraoNavio();
        p.setNavio(navio);
        p.setNumero(numero);
        p.setPosLongInicio(inicio);
        p.setPosLongFim(fim);
        p.setLargura(largura);
        p.setAlturaUtil(12.0);
        return p;
    }

    private BobinaManifesto criarBobina(String codigo, double pesoKg, double diametroMm, double larguraMm) {
        BobinaManifesto b = new BobinaManifesto();
        b.setCodigo(codigo);
        b.setPesoKg(pesoKg);
        b.setDiametroExternoMm(diametroMm);
        b.setLarguraMm(larguraMm);
        return b;
    }

    private PosicaoBobina criarPosicao(PlanoEstivaBulk plano, PoraoNavio porao,
                                        BobinaManifesto bobina, int camada,
                                        double posX, double posY) {
        PosicaoBobina pos = new PosicaoBobina();
        pos.setPlano(plano);
        pos.setPorao(porao);
        pos.setBobina(bobina);
        pos.setCamada(camada);
        pos.setPosicaoX(posX);
        pos.setPosicaoY(posY);
        return pos;
    }
}
