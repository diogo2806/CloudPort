package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.*;

import br.com.cloudport.servicoyard.estivagembulk.dto.DocumentoCargoManifestDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PoraoNavio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DocumentoEstivaServico - Geração de documentos")
class DocumentoEstivaServicoTest {

    private DocumentoEstivaServico servico;

    @BeforeEach
    void setup() {
        servico = new DocumentoEstivaServico();
    }

    @Test
    @DisplayName("Cargo Manifest gerado com tipo de documento correto")
    void cargoManifestTipoCorreto() {
        PlanoEstivaBulk plano = criarPlanoComBobinas();

        DocumentoCargoManifestDto doc = servico.gerarCargoManifest(plano);

        assertEquals("CARGO_MANIFEST", doc.getTipoDocumento());
        assertNotNull(doc.getDataGeracao());
    }

    @Test
    @DisplayName("Stowage Plan gerado com tipo de documento correto")
    void stowagePlanTipoCorreto() {
        PlanoEstivaBulk plano = criarPlanoComBobinas();

        DocumentoCargoManifestDto doc = servico.gerarPlanilhaEstivagem(plano);

        assertEquals("STOWAGE_PLAN", doc.getTipoDocumento());
    }

    @Test
    @DisplayName("Cargo Manifest lista todos os itens do plano")
    void manifestListaTodosItens() {
        PlanoEstivaBulk plano = criarPlanoComBobinas();

        DocumentoCargoManifestDto doc = servico.gerarCargoManifest(plano);

        assertEquals(3, doc.getTotalItens());
        assertFalse(doc.getItens().isEmpty());
    }

    @Test
    @DisplayName("Peso total calculado corretamente no manifesto")
    void pesoTotalCalculadoCorretamente() {
        PlanoEstivaBulk plano = criarPlanoComBobinas();

        DocumentoCargoManifestDto doc = servico.gerarCargoManifest(plano);

        // 3 bobinas x 10000kg = 30000kg = 30t
        assertEquals(30.0, doc.getPesoTotalToneladas(), 0.1);
    }

    @Test
    @DisplayName("Observações SOLAS incluídas no documento")
    void observacoesSolasIncluidas() {
        PlanoEstivaBulk plano = criarPlanoComBobinas();

        DocumentoCargoManifestDto doc = servico.gerarCargoManifest(plano);

        assertFalse(doc.getObservacoesSolas().isEmpty());
        assertTrue(doc.getObservacoesSolas().stream().anyMatch(s -> s.contains("IMSBC")));
    }

    private PlanoEstivaBulk criarPlanoComBobinas() {
        NavioGranel navio = new NavioGranel();
        navio.setNome("MV TEST");
        navio.setImo("IMO9999999");
        navio.setGm(1.5);

        PoraoNavio porao = new PoraoNavio();
        porao.setNumero(1);
        porao.setPosLongInicio(50.0);
        porao.setPosLongFim(100.0);
        navio.getPoroes().add(porao);

        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);
        plano.setCodigoViagem("VYG-2026-001");
        plano.setPortoCarga("BRSSV");
        plano.setPortoDescarga("CNSHA");

        for (int i = 1; i <= 3; i++) {
            BobinaManifesto bob = new BobinaManifesto();
            bob.setCodigo("BOB-00" + i);
            bob.setPesoKg(10000.0);
            bob.setPortoDescarga("CNSHA");
            bob.setGrauAco("SAE1020");
            plano.getBobinas().add(bob);

            PosicaoBobina pos = new PosicaoBobina();
            pos.setBobina(bob);
            pos.setPorao(porao);
            pos.setCamada(1);
            plano.getPosicoes().add(pos);
        }

        return plano;
    }
}
