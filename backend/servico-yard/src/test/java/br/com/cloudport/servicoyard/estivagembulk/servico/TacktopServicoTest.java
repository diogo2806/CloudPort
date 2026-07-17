package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.servicoyard.estivagembulk.dto.TacktopDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.MaterialLashingBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TipoLashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TacktopServico - Validação de lashing e securing")
class TacktopServicoTest {

    private TacktopServico servico;

    @BeforeEach
    void setup() {
        servico = new TacktopServico();
    }

    @Test
    @DisplayName("Validação usa materiais certificados e não altera o plano")
    void validarMateriaisSemMutarPosicao() {
        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        PosicaoBobina posicao = criarPosicao(plano);
        posicao.setAnguloInclinacao(7.5);
        plano.getPosicoes().add(posicao);
        plano.getMateriais().add(criarMaterial(plano, posicao, "PA-01", 25.0));
        plano.getMateriais().add(criarMaterial(plano, posicao, "PA-02", 25.0));

        TacktopDto dto = servico.validarSecuring(plano);

        assertTrue(dto.isAprovado());
        assertEquals(50.0, dto.getCapacidadeDisponivelTotalKn(), 0.01);
        assertEquals(40.0, dto.getForcaRequeridaTotalKn(), 0.01);
        assertEquals(7.5, posicao.getAnguloInclinacao(), 0.01);
        assertEquals(2, plano.getMateriais().size());
    }

    @Test
    @DisplayName("Ausência de certificado e capacidade reprova sem criar material estimado")
    void materialIncompletoReprovaSemEstimativa() {
        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        PosicaoBobina posicao = criarPosicao(plano);
        plano.getPosicoes().add(posicao);

        MaterialLashingBulk material = new MaterialLashingBulk();
        material.setPlano(plano);
        material.setPosicao(posicao);
        material.setTipo(TipoLashing.CINTA_ACO);
        material.setQuantidade(1);
        material.setPontoAmarracao("PA-01");
        plano.getMateriais().add(material);

        TacktopDto dto = servico.validarSecuring(plano);

        assertFalse(dto.isAprovado());
        assertEquals(1, plano.getMateriais().size());
        assertTrue(dto.getViolacoes().stream()
                .anyMatch(violacao -> "CAPACIDADE_MATERIAL_AUSENTE".equals(violacao.getTipo())));
        assertTrue(dto.getViolacoes().stream()
                .anyMatch(violacao -> "CERTIFICADO_MATERIAL_AUSENTE".equals(violacao.getTipo())));
    }

    private PosicaoBobina criarPosicao(PlanoEstivaBulk plano) {
        PosicaoBobina posicao = new PosicaoBobina();
        posicao.setPlano(plano);
        posicao.setCamada(1);
        posicao.setPosicaoX(2.0);
        posicao.setPosicaoY(3.0);
        posicao.setTipoLashing(TipoLashing.CINTA_ACO);
        posicao.setForcaRequeridaLashingKn(40.0);
        posicao.setVersaoEspecificacao("TATA-2016-01");
        posicao.setReferenciaRegra("SECURING-COILS");
        posicao.setResponsavelValidacao("oficial-carga");
        return posicao;
    }

    private MaterialLashingBulk criarMaterial(PlanoEstivaBulk plano, PosicaoBobina posicao,
            String pontoAmarracao, double cargaTrabalhoSeguraKn) {
        MaterialLashingBulk material = new MaterialLashingBulk();
        material.setPlano(plano);
        material.setPosicao(posicao);
        material.setTipo(TipoLashing.CINTA_ACO);
        material.setQuantidade(1);
        material.setComprimentoM(4.0);
        material.setPesoUnitarioKg(8.0);
        material.setPontoAmarracao(pontoAmarracao);
        material.setCapacidadeNominalKn(38.2);
        material.setCargaTrabalhoSeguraKn(cargaTrabalhoSeguraKn);
        material.setCertificado("CERT-001");
        material.setVersaoEspecificacao("TATA-2016-01");
        material.setReferenciaRegra("SECURING-COILS");
        material.setResponsavelValidacao("oficial-carga");
        return material;
    }
}
