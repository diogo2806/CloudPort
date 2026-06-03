package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.*;

import br.com.cloudport.servicoyard.estivagembulk.dto.EstabilidadeEstrutural;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.ClasseNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PoraoNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EstabilidadeEstruturalServico - Momento Fletor e Força de Cisalhamento")
class EstabilidadeEstruturalServicoTest {

    private EstabilidadeEstruturalServico servico;

    @BeforeEach
    void setup() {
        servico = new EstabilidadeEstruturalServico();
    }

    @Test
    @DisplayName("Plano sem navio retorna estabilidade vazia e aprovado")
    void planoVazioRetornaAprovado() {
        PlanoEstivaBulk plano = new PlanoEstivaBulk();

        EstabilidadeEstrutural dto = servico.calcular(plano);

        assertTrue(dto.isAprovado());
        assertEquals(0.0, dto.getBmMaxKnm());
        assertEquals(0.0, dto.getSfMaxKn());
    }

    @Test
    @DisplayName("Peso concentrado no centro do navio gera sagging")
    void pesoConcentradoNoCentroGeraSagging() {
        NavioGranel navio = criarNavioHandymax();
        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);

        // Place heavy load in midship hold (pos 100m on a 200m ship)
        PoraoNavio poraoMeio = criarPorao(navio, 2, 90.0, 110.0);
        navio.getPoroes().add(poraoMeio);
        for (int i = 0; i < 5; i++) {
            PosicaoBobina pos = criarPosicao(plano, poraoMeio, 30000.0);
            plano.getPosicoes().add(pos);
        }

        EstabilidadeEstrutural dto = servico.calcular(plano);

        assertTrue(dto.getPesoTotalToneladas() > 0);
        assertTrue(dto.getCaladoSaidaMetros() > 0);
    }

    @Test
    @DisplayName("BM excedido gera violação PERIGO")
    void bmExcedidoGeraViolacaoPerigo() {
        NavioGranel navio = criarNavioHandymax();
        navio.setBmMaxPermitido(1.0); // absurdly low limit to force violation
        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);

        PoraoNavio porao = criarPorao(navio, 1, 50.0, 100.0);
        navio.getPoroes().add(porao);
        for (int i = 0; i < 10; i++) {
            plano.getPosicoes().add(criarPosicao(plano, porao, 50000.0));
        }

        EstabilidadeEstrutural dto = servico.calcular(plano);

        assertTrue(dto.getViolacoes().stream()
                .anyMatch(v -> "MOMENTO_FLETOR_EXCEDIDO".equals(v.getTipo())
                        && "PERIGO".equals(v.getSeveridade())));
        assertFalse(dto.isAprovado());
    }

    @Test
    @DisplayName("Calado de saída calculado é positivo para carga real")
    void caladoSaidaPositivoParaCargaReal() {
        NavioGranel navio = criarNavioHandymax();
        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);

        PoraoNavio porao = criarPorao(navio, 1, 20.0, 80.0);
        navio.getPoroes().add(porao);
        plano.getPosicoes().add(criarPosicao(plano, porao, 10000.0));

        EstabilidadeEstrutural dto = servico.calcular(plano);

        assertTrue(dto.getCaladoSaidaMetros() > 0, "Calado de saída deve ser positivo");
    }

    private NavioGranel criarNavioHandymax() {
        NavioGranel n = new NavioGranel();
        n.setNome("MV TEST HANDYMAX");
        n.setClasse(ClasseNavio.HANDYMAX);
        n.setLpp(200.0);
        n.setBoca(32.0);
        n.setCalado(10.0);
        n.setDeslocamento(45000.0);
        n.setGm(1.5);
        n.setBmMaxPermitido(250000.0);
        n.setSfMaxPermitido(50000.0);
        return n;
    }

    private PoraoNavio criarPorao(NavioGranel navio, int numero, double inicio, double fim) {
        PoraoNavio p = new PoraoNavio();
        p.setNavio(navio);
        p.setNumero(numero);
        p.setPosLongInicio(inicio);
        p.setPosLongFim(fim);
        p.setLargura(20.0);
        p.setAlturaUtil(12.0);
        return p;
    }

    private PosicaoBobina criarPosicao(PlanoEstivaBulk plano, PoraoNavio porao, double pesoKg) {
        BobinaManifesto bobina = new BobinaManifesto();
        bobina.setPesoKg(pesoKg);
        bobina.setDiametroExternoMm(1500.0);
        bobina.setLarguraMm(2000.0);

        PosicaoBobina pos = new PosicaoBobina();
        pos.setPlano(plano);
        pos.setBobina(bobina);
        pos.setPorao(porao);
        pos.setCamada(1);
        pos.setPosicaoX(5.0);
        pos.setPosicaoY(5.0);
        return pos;
    }
}
