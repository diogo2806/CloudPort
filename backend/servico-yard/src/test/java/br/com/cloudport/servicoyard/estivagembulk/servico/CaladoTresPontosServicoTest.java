package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.*;

import br.com.cloudport.servicoyard.estivagembulk.dto.CaladoTresPontosDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PoraoNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CaladoTresPontosServico - Cálculo de calado nos três pontos")
class CaladoTresPontosServicoTest {

    private CaladoTresPontosServico servico;

    @BeforeEach
    void setup() {
        servico = new CaladoTresPontosServico();
    }

    @Test
    @DisplayName("Plano sem navio retorna DTO aprovado com zeros")
    void planoSemNavioRetornaAprovado() {
        PlanoEstivaBulk plano = new PlanoEstivaBulk();

        CaladoTresPontosDto dto = servico.calcular(plano);

        assertTrue(dto.isAprovado());
    }

    @Test
    @DisplayName("Calado médio positivo para carga real")
    void caladoMedioPositivoParaCargaReal() {
        PlanoEstivaBulk plano = criarPlanoComCarga(10000.0);

        CaladoTresPontosDto dto = servico.calcular(plano);

        assertTrue(dto.getCaladoMeioM() > 0, "Calado médio deve ser positivo");
        assertTrue(dto.getPesoCarregadoToneladas() > 0);
    }

    @Test
    @DisplayName("Carga centrada no meio-navio gera trim próximo de zero")
    void cargaCentradaGeraTrimProximoZero() {
        NavioGranel navio = criarNavio(200.0, 32.0, 45000.0);
        PoraoNavio porao = criarPorao(navio, 1, 90.0, 110.0);
        navio.getPoroes().add(porao);

        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);

        BobinaManifesto bob = new BobinaManifesto();
        bob.setPesoKg(20000.0);
        PosicaoBobina pos = new PosicaoBobina();
        pos.setBobina(bob);
        pos.setPorao(porao);
        pos.setCamada(1);
        pos.setPosicaoX(0.0);
        pos.setPosicaoY(0.0);
        plano.getPosicoes().add(pos);

        CaladoTresPontosDto dto = servico.calcular(plano);

        // Centro do porão = (90+110)/2 = 100m = LCB para Lpp=200m → trim ≈ 0
        assertEquals(0.0, dto.getTrimM(), 0.01, "Trim deve ser próximo de zero para carga centrada");
    }

    @Test
    @DisplayName("Trim excessivo (>3m) gera violação PERIGO")
    void trimExcessivoGeraViolacaoPerigo() {
        // Small ship: 500t displacement, Lpp=50m → lightship = 175t at LCB=25m
        // 100t cargo concentrated at x=2.5m → LCG shifts far from LCB
        NavioGranel navio = criarNavio(50.0, 15.0, 500.0);
        PoraoNavio poraoExtremal = criarPorao(navio, 1, 0.0, 5.0);
        navio.getPoroes().add(poraoExtremal);

        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);

        // 10 × 10t = 100t in extremal porão (centre = 2.5m vs LCB = 25m)
        for (int i = 0; i < 10; i++) {
            BobinaManifesto bob = new BobinaManifesto();
            bob.setPesoKg(10000.0);
            PosicaoBobina pos = new PosicaoBobina();
            pos.setBobina(bob);
            pos.setPorao(poraoExtremal);
            pos.setCamada(1);
            pos.setPosicaoX(0.0);
            pos.setPosicaoY(0.0);
            plano.getPosicoes().add(pos);
        }

        // lcgLightship=25m, lcgCargo=2.5m, pesoLightship=175t, pesoCargo=100t
        // lcg = (175×25 + 100×2.5)/(275) = 4625/275 ≈ 16.8m  →  trim = 16.8-25 = -8.2m  > 3m ✓
        CaladoTresPontosDto dto = servico.calcular(plano);

        assertTrue(dto.getViolacoes().stream()
                .anyMatch(v -> "TRIM_EXCESSIVO".equals(v.getTipo()) && "PERIGO".equals(v.getSeveridade())),
                "Deve detectar trim excessivo");
        assertFalse(dto.isAprovado());
    }

    @Test
    @DisplayName("Calado de popa é maior que calado de proa quando LCG > LCB (by the stern)")
    void caladoPopaEmRelacaoAProa() {
        // Small ship: lightship has small weight so cargo LCG dominates
        NavioGranel navio = criarNavio(50.0, 15.0, 500.0);
        // Porão at high-x end [45-50m], centre = 47.5m > LCB = 25m → trim = positive → popa (caladoPopa) deeper
        PoraoNavio poraoHighX = criarPorao(navio, 1, 45.0, 50.0);
        navio.getPoroes().add(poraoHighX);

        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);

        // 10 × 10t = 100t concentrated at high-x extremity (centre ≈ 47.5m)
        // lightship = 175t at 25m; lcg = (175×25 + 100×47.5)/275 = 9125/275 ≈ 33.2m > LCB=25m
        // trim = 33.2 - 25 = +8.2m → caladoPopa = caladoMedio + 4.1 > caladoProa ✓
        for (int i = 0; i < 10; i++) {
            BobinaManifesto bob = new BobinaManifesto();
            bob.setPesoKg(10000.0);
            PosicaoBobina pos = new PosicaoBobina();
            pos.setBobina(bob);
            pos.setPorao(poraoHighX);
            pos.setCamada(1);
            pos.setPosicaoX(0.0);
            pos.setPosicaoY(0.0);
            plano.getPosicoes().add(pos);
        }

        CaladoTresPontosDto dto = servico.calcular(plano);

        assertTrue(dto.getCaladoPopaM() > dto.getCaladoProaM(),
                "caladoPopa deve ser maior quando LCG > LCB (trim positivo)");
    }

    private NavioGranel criarNavio(double lpp, double boca, double deslocamento) {
        NavioGranel n = new NavioGranel();
        n.setNome("MV TEST");
        n.setLpp(lpp);
        n.setBoca(boca);
        n.setCalado(12.0);
        n.setDeslocamento(deslocamento);
        n.setGm(1.5);
        return n;
    }

    private PoraoNavio criarPorao(NavioGranel navio, int num, double inicio, double fim) {
        PoraoNavio p = new PoraoNavio();
        p.setNavio(navio);
        p.setNumero(num);
        p.setPosLongInicio(inicio);
        p.setPosLongFim(fim);
        p.setLargura(20.0);
        p.setAlturaUtil(12.0);
        return p;
    }

    private PlanoEstivaBulk criarPlanoComCarga(double pesoKg) {
        NavioGranel navio = criarNavio(200.0, 32.0, 45000.0);
        PoraoNavio porao = criarPorao(navio, 1, 80.0, 120.0);
        navio.getPoroes().add(porao);

        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);

        BobinaManifesto bob = new BobinaManifesto();
        bob.setPesoKg(pesoKg);
        PosicaoBobina pos = new PosicaoBobina();
        pos.setBobina(bob);
        pos.setPorao(porao);
        pos.setCamada(1);
        pos.setPosicaoX(0.0);
        pos.setPosicaoY(0.0);
        plano.getPosicoes().add(pos);
        return plano;
    }
}
