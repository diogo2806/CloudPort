package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

@DisplayName("EstabilidadeEstruturalServico - cálculo operacional versionado")
class EstabilidadeEstruturalServicoTest {

    private EstabilidadeEstruturalServico servico;

    @BeforeEach
    void setup() {
        servico = new EstabilidadeEstruturalServico();
    }

    @Test
    @DisplayName("Plano sem perfil completo é simulação não operacional e não aprova")
    void planoSemPerfilCompletoNaoAprova() {
        PlanoEstivaBulk plano = new PlanoEstivaBulk();

        EstabilidadeEstrutural resultado = servico.calcular(plano);

        assertFalse(resultado.isOperacional());
        assertFalse(resultado.isAprovado());
        assertTrue(resultado.getViolacoes().stream()
                .anyMatch(violacao -> "DADOS_ESTABILIDADE_INCOMPLETOS".equals(violacao.getTipo())));
    }

    @Test
    @DisplayName("Perfil e distribuição versionados produzem cálculo operacional")
    void perfilVersionadoProduzCalculoOperacional() {
        NavioGranel navio = criarNavioOperacional();
        PlanoEstivaBulk plano = criarPlanoComBobina(navio);

        EstabilidadeEstrutural resultado = servico.calcular(plano);

        assertTrue(resultado.isOperacional());
        assertTrue(resultado.isAprovado());
        assertTrue(resultado.getCaladoSaidaMetros() > 0.0);
        assertTrue(resultado.getGmMetros() > navio.getGmMinimo());
        assertTrue(resultado.getMemoriaCalculo().contains("hidro=HYDRO-2026-01"));
        assertTrue(resultado.getMemoriaCalculo().contains("estrutural=STRUCT-2026-01"));
    }

    @Test
    @DisplayName("GM abaixo do mínimo versionado bloqueia a aprovação")
    void gmAbaixoDoMinimoBloqueiaAprovacao() {
        NavioGranel navio = criarNavioOperacional();
        navio.setKm(8.1);
        PlanoEstivaBulk plano = criarPlanoComBobina(navio);

        EstabilidadeEstrutural resultado = servico.calcular(plano);

        assertTrue(resultado.isOperacional());
        assertFalse(resultado.isAprovado());
        assertTrue(resultado.getViolacoes().stream()
                .anyMatch(violacao -> "GM_INSUFICIENTE".equals(violacao.getTipo())
                        && "PERIGO".equals(violacao.getSeveridade())));
    }

    private NavioGranel criarNavioOperacional() {
        NavioGranel navio = new NavioGranel();
        navio.setNome("MV TEST HANDYMAX");
        navio.setClasse(ClasseNavio.HANDYMAX);
        navio.setLpp(200.0);
        navio.setBoca(32.0);
        navio.setCalado(10.0);
        navio.setDeslocamento(39000.0);
        navio.setTpc(40.0);
        navio.setLcb(100.0);
        navio.setKm(15.0);
        navio.setMct1cm(2000.0);
        navio.setCaladoMaximo(11.0);
        navio.setTrimMaximo(3.0);
        navio.setBandaMaxima(5.0);
        navio.setGmMinimo(0.5);
        navio.setPesoLeveToneladas(39000.0);
        navio.setLcgPesoLeve(100.0);
        navio.setTcgPesoLeve(0.0);
        navio.setVcgPesoLeve(8.0);
        navio.setPesoLastroToneladas(0.0);
        navio.setBmMaxPermitido(1_000_000_000.0);
        navio.setSfMaxPermitido(1_000_000_000.0);
        navio.setVersaoDadosHidrostaticos("HYDRO-2026-01");
        navio.setVersaoDadosEstruturais("STRUCT-2026-01");
        navio.setPosicoesSecoes("0;50;100;150;200");
        navio.setPesoLeveSecoes("5000;9000;11000;9000;5000");
        navio.setEmpuxoSecoes("5000;9000;11000;9000;5000");
        navio.setLimitesSfSecoes("1000000000;1000000000;1000000000;1000000000;1000000000");
        navio.setLimitesBmSecoes("1000000000;1000000000;1000000000;1000000000;1000000000");
        return navio;
    }

    private PlanoEstivaBulk criarPlanoComBobina(NavioGranel navio) {
        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);

        PoraoNavio porao = new PoraoNavio();
        porao.setNavio(navio);
        porao.setNumero(2);
        porao.setPosLongInicio(90.0);
        porao.setPosLongFim(110.0);
        porao.setLargura(20.0);
        porao.setAlturaUtil(12.0);
        navio.getPoroes().add(porao);

        BobinaManifesto bobina = new BobinaManifesto();
        bobina.setPesoKg(10000.0);
        bobina.setDiametroExternoMm(1500.0);
        bobina.setLarguraMm(2000.0);

        PosicaoBobina posicao = new PosicaoBobina();
        posicao.setPlano(plano);
        posicao.setBobina(bobina);
        posicao.setPorao(porao);
        posicao.setCamada(1);
        posicao.setPosicaoX(10.0);
        posicao.setPosicaoY(0.0);
        posicao.setEspessuraDunnageMm(50.0);
        plano.getPosicoes().add(posicao);
        return plano;
    }
}
