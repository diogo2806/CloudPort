package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.estivagembulk.dto.EstabilidadeEstrutural;
import br.com.cloudport.servicoyard.estivagembulk.dto.PlanoEstivaBulkDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.MaterialLashingBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PoraoNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import br.com.cloudport.servicoyard.estivagembulk.modelo.ResultadoValidacaoSeguranca;
import br.com.cloudport.servicoyard.estivagembulk.modelo.SetorTanktop;
import br.com.cloudport.servicoyard.estivagembulk.modelo.StatusPlanoEstiva;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TipoLashing;
import br.com.cloudport.servicoyard.estivagembulk.repositorio.NavioGranelRepositorio;
import br.com.cloudport.servicoyard.estivagembulk.repositorio.PlanoEstivaBulkRepositorio;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlanoEstivaBulkServico - Barreira única de aprovação BUS30")
class PlanoEstivaBulkServicoTest {

    private PlanoEstivaBulkRepositorio planoRepositorio;
    private EstabilidadeEstruturalServico estabilidadeServico;
    private PlanoEstivaBulkServico servico;

    @BeforeEach
    void setup() {
        NavioGranelRepositorio navioRepositorio = mock(NavioGranelRepositorio.class);
        planoRepositorio = mock(PlanoEstivaBulkRepositorio.class);
        estabilidadeServico = mock(EstabilidadeEstruturalServico.class);
        servico = new PlanoEstivaBulkServico(
                navioRepositorio,
                planoRepositorio,
                new TanktopCalculadorServico(),
                estabilidadeServico,
                new EmpilhamentoBobinaServico(),
                new TacktopServico());
    }

    @Test
    @DisplayName("Aprova somente quando todos os blocos usam o mesmo snapshot")
    void aprovarPlanoCompleto() {
        PlanoEstivaBulk plano = criarPlanoCompleto();
        when(planoRepositorio.findById(1L)).thenReturn(Optional.of(plano));
        when(estabilidadeServico.calcular(plano)).thenReturn(estabilidadeAprovada());

        PlanoEstivaBulkDto dto = servico.validarEAprovar(1L);

        assertEquals(StatusPlanoEstiva.APROVADO, plano.getStatus());
        assertEquals(ResultadoValidacaoSeguranca.APROVADO, plano.getResultadoValidacaoSeguranca());
        assertTrue(dto.getValidacaoSeguranca().isAprovado());
        assertNotNull(dto.getValidacaoSeguranca().getValidadoEm());
        assertEquals("TATA-2016-01", dto.getValidacaoSeguranca().getVersaoEspecificacao());
        verify(planoRepositorio).save(plano);
    }

    @Test
    @DisplayName("Reprova e persiste evidência quando o dunnage real está incompleto")
    void reprovarPlanoSemDunnageCompleto() {
        PlanoEstivaBulk plano = criarPlanoCompleto();
        plano.getPosicoes().get(0).setLarguraDunnageMm(null);
        when(planoRepositorio.findById(1L)).thenReturn(Optional.of(plano));
        when(estabilidadeServico.calcular(plano)).thenReturn(estabilidadeAprovada());

        ValidacaoPlanoBulkException exception = assertThrows(
                ValidacaoPlanoBulkException.class,
                () -> servico.validarEAprovar(1L));

        assertEquals(StatusPlanoEstiva.RASCUNHO, plano.getStatus());
        assertEquals(ResultadoValidacaoSeguranca.REPROVADO, plano.getResultadoValidacaoSeguranca());
        assertTrue(exception.getValidacao().getViolacoes().stream()
                .anyMatch(violacao -> "DUNNAGE_NAO_COMPROVADO".equals(violacao.getTipo())));
        verify(planoRepositorio).save(plano);
    }

    private PlanoEstivaBulk criarPlanoCompleto() {
        NavioGranel navio = new NavioGranel();
        navio.setId(10L);
        navio.setNome("MV VALIDATION");
        navio.setLpp(180.0);
        navio.setBoca(30.0);
        navio.setCalado(9.0);
        navio.setPoroes(new ArrayList<>());

        PoraoNavio porao = new PoraoNavio();
        porao.setId(20L);
        porao.setNavio(navio);
        porao.setNumero(1);
        porao.setComprimento(50.0);
        porao.setLargura(10.0);
        porao.setAlturaUtil(12.0);
        porao.setPosLongInicio(0.0);
        porao.setPosLongFim(50.0);
        porao.setSetores(new ArrayList<>());
        navio.getPoroes().add(porao);

        SetorTanktop setor = new SetorTanktop();
        setor.setId(30L);
        setor.setPorao(porao);
        setor.setNome("CENTRO");
        setor.setCapacidadeTM2(20.0);
        setor.setAreaM2(100.0);
        porao.getSetores().add(setor);

        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setId(1L);
        plano.setVersao(4L);
        plano.setNavio(navio);
        plano.setCodigoViagem("V001");
        plano.setPortoCarga("BRIGI");
        plano.setPortoDescarga("NLRTM");

        BobinaManifesto bobina = new BobinaManifesto();
        bobina.setId(40L);
        bobina.setPlano(plano);
        bobina.setCodigo("BOB-001");
        bobina.setPesoKg(6000.0);
        bobina.setDiametroExternoMm(1500.0);
        bobina.setDiametroInternoMm(500.0);
        bobina.setLarguraMm(2000.0);
        bobina.setPortoDescarga("NLRTM");
        plano.getBobinas().add(bobina);

        PosicaoBobina posicao = new PosicaoBobina();
        posicao.setId(50L);
        posicao.setPlano(plano);
        posicao.setBobina(bobina);
        posicao.setPorao(porao);
        posicao.setSetor(setor);
        posicao.setCamada(1);
        posicao.setPosicaoX(5.0);
        posicao.setPosicaoY(5.0);
        posicao.setAnguloInclinacao(0.0);
        posicao.setEspessuraDunnageMm(30.0);
        posicao.setQuantidadeLinhasDunnage(2);
        posicao.setLarguraDunnageMm(150.0);
        posicao.setComprimentoContatoDunnageMm(2000.0);
        posicao.setQuantidadeCalcos(2);
        posicao.setEspacamentoFileirasMm(120.0);
        posicao.setTipoLashing(TipoLashing.CINTA_ACO);
        posicao.setForcaRequeridaLashingKn(20.0);
        posicao.setSequenciaDescarga(1);
        posicao.setReferenciaRegra("SECURING-COILS");
        posicao.setVersaoEspecificacao("TATA-2016-01");
        posicao.setResponsavelValidacao("oficial-carga");
        plano.getPosicoes().add(posicao);

        MaterialLashingBulk material = new MaterialLashingBulk();
        material.setId(60L);
        material.setPlano(plano);
        material.setPosicao(posicao);
        material.setTipo(TipoLashing.CINTA_ACO);
        material.setQuantidade(1);
        material.setComprimentoM(4.0);
        material.setPesoUnitarioKg(8.0);
        material.setPontoAmarracao("PA-01");
        material.setCapacidadeNominalKn(38.2);
        material.setCargaTrabalhoSeguraKn(25.0);
        material.setCertificado("CERT-001");
        material.setReferenciaRegra("SECURING-COILS");
        material.setVersaoEspecificacao("TATA-2016-01");
        material.setResponsavelValidacao("oficial-carga");
        plano.getMateriais().add(material);
        return plano;
    }

    private EstabilidadeEstrutural estabilidadeAprovada() {
        return new EstabilidadeEstrutural(
                100.0,
                80.0,
                0.2,
                9.3,
                6.0,
                false,
                false,
                true,
                new ArrayList<>());
    }
}
