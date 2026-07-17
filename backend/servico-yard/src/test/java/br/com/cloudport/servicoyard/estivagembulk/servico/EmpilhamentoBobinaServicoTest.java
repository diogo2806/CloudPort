package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @DisplayName("Altura da segunda camada usa diâmetros reais")
    void alturaSegundaCamadaCalculadaCorretamente() {
        double diametroBase = 1500.0;
        double diametroCima = 1500.0;

        double altura = servico.calcularAlturaCamada(diametroBase, diametroCima);

        double raioBase = diametroBase / 2000.0;
        double raioCima = diametroCima / 2000.0;
        double esperado = Math.sqrt(raioCima * raioCima + 2.0 * raioBase * raioCima);
        assertEquals(esperado, altura, 0.001);
        assertEquals(1.299, altura, 0.001);
    }

    @Test
    @DisplayName("Intertravamento inválido gera violação PERIGO")
    void intertravamentoInvalidoGeraViolacao() {
        NavioGranel navio = criarNavio();
        PoraoNavio porao = criarPorao(navio, 1, 50.0, 10.0);
        navio.getPoroes().add(porao);
        porao.setId(1L);

        PlanoEstivaBulk plano = criarPlano(navio);
        BobinaManifesto bobinaBase = criarBobina("BOB-BASE", 20000.0, 1000.0, 2000.0);
        BobinaManifesto bobinaTopo = criarBobina("BOB-TOPO", 15000.0, 2000.0, 2000.0);

        plano.getPosicoes().add(criarPosicao(plano, porao, bobinaBase, 1, 5.0, 5.0, 2));
        plano.getPosicoes().add(criarPosicao(plano, porao, bobinaTopo, 2, 5.0, 5.0, 1));

        AnaliseEmpilhamentoDto dto = servico.analisarEmpilhamento(plano, 1L);

        assertTrue(dto.getViolacoes().stream()
                .anyMatch(violacao -> "INTERTRAVAMENTO_INVALIDO".equals(violacao.getTipo())
                        && "PERIGO".equals(violacao.getSeveridade())));
    }

    @Test
    @DisplayName("Corredor bloqueado quando a base ocupa a largura do porão")
    void corredorBloqueadoGeraViolacao() {
        NavioGranel navio = criarNavio();
        PoraoNavio porao = criarPorao(navio, 1, 50.0, 10.0);
        navio.getPoroes().add(porao);
        porao.setId(2L);

        PlanoEstivaBulk plano = criarPlano(navio);
        for (int indice = 0; indice < 3; indice++) {
            BobinaManifesto bobina = criarBobina("BOB-" + indice, 20000.0, 3500.0, 2000.0);
            plano.getPosicoes().add(criarPosicao(
                    plano, porao, bobina, 1, indice * 4.0, 5.0, indice + 1));
        }

        AnaliseEmpilhamentoDto dto = servico.analisarEmpilhamento(plano, 2L);

        assertFalse(dto.isCorredorOperacaoLivre());
        assertTrue(dto.getViolacoes().stream()
                .anyMatch(violacao -> "CORREDOR_BLOQUEADO".equals(violacao.getTipo())
                        && "PERIGO".equals(violacao.getSeveridade())));
    }

    @Test
    @DisplayName("Empilhamento completo e rastreável não gera perigo")
    void empilhamentoValidoSemViolacoes() {
        NavioGranel navio = criarNavio();
        PoraoNavio porao = criarPorao(navio, 1, 50.0, 20.0);
        navio.getPoroes().add(porao);
        porao.setId(3L);

        PlanoEstivaBulk plano = criarPlano(navio);
        BobinaManifesto bobinaBase = criarBobina("BOB-A", 20000.0, 1500.0, 2000.0);
        BobinaManifesto bobinaTopo = criarBobina("BOB-B", 15000.0, 1500.0, 2000.0);

        plano.getPosicoes().add(criarPosicao(plano, porao, bobinaBase, 1, 5.0, 5.0, 2));
        plano.getPosicoes().add(criarPosicao(plano, porao, bobinaTopo, 2, 5.0, 5.0, 1));

        AnaliseEmpilhamentoDto dto = servico.analisarEmpilhamento(plano, 3L);

        assertTrue(dto.isCorredorOperacaoLivre());
        assertTrue(dto.getViolacoes().stream()
                .noneMatch(violacao -> "PERIGO".equals(violacao.getSeveridade())));
        assertTrue(dto.getAlturaFinalM() > 0.0);
        assertEquals(2, dto.getTotalCamadas());
    }

    @Test
    @DisplayName("Ausência de dimensões reais não recebe fallback")
    void dimensoesAusentesReprovam() {
        NavioGranel navio = criarNavio();
        PoraoNavio porao = criarPorao(navio, 1, 50.0, 20.0);
        navio.getPoroes().add(porao);
        porao.setId(4L);

        PlanoEstivaBulk plano = criarPlano(navio);
        BobinaManifesto bobina = criarBobina("BOB-SEM-DIAMETRO", 20000.0, 0.0, 2000.0);
        plano.getPosicoes().add(criarPosicao(plano, porao, bobina, 1, 5.0, 5.0, 1));

        AnaliseEmpilhamentoDto dto = servico.analisarEmpilhamento(plano, 4L);

        assertTrue(dto.getViolacoes().stream()
                .anyMatch(violacao -> "DADOS_BOBINA_INCOMPLETOS".equals(violacao.getTipo())));
        assertEquals(0.0, dto.getAlturaFinalM());
    }

    private PlanoEstivaBulk criarPlano(NavioGranel navio) {
        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);
        return plano;
    }

    private NavioGranel criarNavio() {
        NavioGranel navio = new NavioGranel();
        navio.setNome("MV TEST");
        navio.setLpp(200.0);
        navio.setBoca(32.0);
        navio.setCalado(10.0);
        return navio;
    }

    private PoraoNavio criarPorao(NavioGranel navio, int numero, double comprimento, double largura) {
        PoraoNavio porao = new PoraoNavio();
        porao.setNavio(navio);
        porao.setNumero(numero);
        porao.setComprimento(comprimento);
        porao.setLargura(largura);
        porao.setAlturaUtil(12.0);
        porao.setPosLongInicio(0.0);
        porao.setPosLongFim(comprimento);
        return porao;
    }

    private BobinaManifesto criarBobina(String codigo, double pesoKg, double diametroMm, double larguraMm) {
        BobinaManifesto bobina = new BobinaManifesto();
        bobina.setCodigo(codigo);
        bobina.setPesoKg(pesoKg);
        bobina.setDiametroExternoMm(diametroMm);
        bobina.setLarguraMm(larguraMm);
        return bobina;
    }

    private PosicaoBobina criarPosicao(PlanoEstivaBulk plano, PoraoNavio porao,
            BobinaManifesto bobina, int camada, double posicaoX, double posicaoY,
            int sequenciaDescarga) {
        PosicaoBobina posicao = new PosicaoBobina();
        posicao.setPlano(plano);
        posicao.setPorao(porao);
        posicao.setBobina(bobina);
        posicao.setCamada(camada);
        posicao.setPosicaoX(posicaoX);
        posicao.setPosicaoY(posicaoY);
        posicao.setEspacamentoFileirasMm(120.0);
        posicao.setSequenciaDescarga(sequenciaDescarga);
        return posicao;
    }
}
