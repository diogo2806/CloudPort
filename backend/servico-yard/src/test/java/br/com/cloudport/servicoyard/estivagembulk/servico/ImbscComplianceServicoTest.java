package br.com.cloudport.servicoyard.estivagembulk.servico;

import static org.junit.jupiter.api.Assertions.*;

import br.com.cloudport.servicoyard.estivagembulk.dto.ImbscComplianceDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.NavioGranel;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TipoLashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ImbscComplianceServico - Verificação SOLAS/IMSBC")
class ImbscComplianceServicoTest {

    private ImbscComplianceServico servico;

    @BeforeEach
    void setup() {
        servico = new ImbscComplianceServico();
    }

    @Test
    @DisplayName("Plano conforme gera Grupo C e documentos mínimos")
    void planoConformeGrupoC() {
        PlanoEstivaBulk plano = criarPlanoBasico(1.5);

        ImbscComplianceDto dto = servico.verificar(plano);

        assertEquals("C", dto.getGrupoImbsc());
        assertTrue(dto.isConforme());
        assertFalse(dto.getDocumentosRequeridos().isEmpty());
    }

    @Test
    @DisplayName("GM abaixo de 0.15m gera não-conformidade PERIGO")
    void gmAbaixoMinimoGeraPerigo() {
        PlanoEstivaBulk plano = criarPlanoBasico(0.10);

        ImbscComplianceDto dto = servico.verificar(plano);

        assertFalse(dto.isConforme());
        assertTrue(dto.getNaoConformidades().stream()
                .anyMatch(v -> "GM_INSUFICIENTE_SOLAS".equals(v.getTipo()) && "PERIGO".equals(v.getSeveridade())),
                "Deve detectar GM insuficiente");
    }

    @Test
    @DisplayName("Sem amarrio em camadas superiores gera aviso de segregação")
    void semAmarrioEmCamadasSuperioresGeraAviso() {
        PlanoEstivaBulk plano = criarPlanoBasico(1.5);

        BobinaManifesto bob = new BobinaManifesto();
        bob.setPesoKg(10000.0);
        bob.setPortoDescarga("BRSSV");
        PosicaoBobina pos = new PosicaoBobina();
        pos.setBobina(bob);
        pos.setCamada(2);
        pos.setTipoLashing(TipoLashing.SEM_LASHING);
        plano.getPosicoes().add(pos);
        plano.getBobinas().add(bob);

        ImbscComplianceDto dto = servico.verificar(plano);

        assertTrue(dto.getNaoConformidades().stream()
                .anyMatch(v -> "SEGREGACAO_INSUFICIENTE".equals(v.getTipo())));
    }

    @Test
    @DisplayName("Bobina >25t requer certificado de reforço e gera aviso")
    void bobinaPesadaRequercertificadoReforco() {
        PlanoEstivaBulk plano = criarPlanoBasico(1.5);

        BobinaManifesto bob = new BobinaManifesto();
        bob.setPesoKg(30000.0);
        bob.setPortoDescarga("BRSSV");
        plano.getBobinas().add(bob);

        ImbscComplianceDto dto = servico.verificar(plano);

        assertTrue(dto.getNaoConformidades().stream()
                .anyMatch(v -> "REFORCO_ESTRUTURAL_REQUERIDO".equals(v.getTipo()) && "AVISO".equals(v.getSeveridade())));
        assertTrue(dto.getDocumentosRequeridos().stream()
                .anyMatch(d -> d.contains("Reforço Estrutural")));
    }

    @Test
    @DisplayName("Itens sem porto de descarga geram aviso PORTO_DESCARGA_AUSENTE")
    void itensSemPortoGeramAviso() {
        PlanoEstivaBulk plano = criarPlanoBasico(1.5);

        BobinaManifesto bob = new BobinaManifesto();
        bob.setPesoKg(5000.0);
        plano.getBobinas().add(bob);

        ImbscComplianceDto dto = servico.verificar(plano);

        assertTrue(dto.getNaoConformidades().stream()
                .anyMatch(v -> "PORTO_DESCARGA_AUSENTE".equals(v.getTipo())));
    }

    private PlanoEstivaBulk criarPlanoBasico(double gm) {
        NavioGranel navio = new NavioGranel();
        navio.setNome("MV TEST");
        navio.setGm(gm);
        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setNavio(navio);
        return plano;
    }
}
