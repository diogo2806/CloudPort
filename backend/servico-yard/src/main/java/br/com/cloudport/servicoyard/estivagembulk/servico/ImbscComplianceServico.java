package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.ImbscComplianceDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.MaterialLashingBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TipoLashing;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ImbscComplianceServico {

    private static final double GM_MINIMO_SIDERURGICO = 0.15;
    private static final double PESO_POR_UNIDADE_MAX_SEM_REFORCO_KG = 25000.0;

    public ImbscComplianceDto verificar(PlanoEstivaBulk plano) {
        ImbscComplianceDto dto = new ImbscComplianceDto();
        dto.setGrupoImbsc("C");
        dto.setCertificadoUmidadeRequerido(false);

        List<ViolacaoEstivaDto> naoConformidades = new ArrayList<>();
        List<String> documentos = new ArrayList<>();

        documentos.add("Plano de Estivagem (Stowage Plan)");
        documentos.add("Manifesto de Carga (Cargo Manifest)");
        documentos.add("Certificado de Estabilidade do Navio (Stability Booklet)");
        documentos.add("Lista de Materiais de Amarrio (Lashing List)");
        documentos.add("Nota Fiscal / Bill of Lading");

        if (plano.getNavio() != null && plano.getNavio().getGm() != null) {
            double gm = plano.getNavio().getGm();
            dto.setFatorEstabilidadeGm(gm);
            if (gm < GM_MINIMO_SIDERURGICO) {
                naoConformidades.add(new ViolacaoEstivaDto("GM_INSUFICIENTE_SOLAS",
                        String.format("GM de %.2fm abaixo do mínimo SOLAS de %.2fm para carga siderúrgica",
                                gm, GM_MINIMO_SIDERURGICO),
                        null, "PERIGO"));
            }
        }

        boolean temLashing = plano.getPosicoes().stream()
                .anyMatch(p -> p.getTipoLashing() != null && p.getTipoLashing() != TipoLashing.SEM_LASHING);

        dto.setRequerimentoAmarrioAtendido(temLashing || plano.getMateriais() != null && !plano.getMateriais().isEmpty());

        if (!dto.isRequerimentoAmarrioAtendido() && !plano.getPosicoes().isEmpty()) {
            naoConformidades.add(new ViolacaoEstivaDto("AMARRIO_AUSENTE",
                    "Nenhum sistema de amarrio (lashing) definido para as posições de carga",
                    null, "AVISO"));
        }

        boolean sobrepesoPorUnidade = plano.getBobinas().stream()
                .anyMatch(b -> b.getPesoKg() != null && b.getPesoKg() > PESO_POR_UNIDADE_MAX_SEM_REFORCO_KG);

        if (sobrepesoPorUnidade) {
            documentos.add("Certificado de Reforço Estrutural de Porão (Hold Reinforcement Certificate)");
            naoConformidades.add(new ViolacaoEstivaDto("REFORCO_ESTRUTURAL_REQUERIDO",
                    "Unidades acima de " + (PESO_POR_UNIDADE_MAX_SEM_REFORCO_KG / 1000) + "t requerem certificado de reforço de porão",
                    null, "AVISO"));
        }

        verificarPortoDescargaDefinido(plano, naoConformidades);

        boolean temItensNaoCamada1 = plano.getPosicoes().stream().anyMatch(p -> p.getCamada() > 1);
        dto.setSegregacaoAtendida(!temItensNaoCamada1 || temLashing);

        if (!dto.isSegregacaoAtendida()) {
            naoConformidades.add(new ViolacaoEstivaDto("SEGREGACAO_INSUFICIENTE",
                    "Itens em camada superior sem amarrio — risco de deslocamento SOLAS II-2",
                    null, "AVISO"));
        }

        dto.setNaoConformidades(naoConformidades);
        dto.setDocumentosRequeridos(documentos);
        dto.setConforme(naoConformidades.stream().noneMatch(v -> "PERIGO".equals(v.getSeveridade())));
        return dto;
    }

    private void verificarPortoDescargaDefinido(PlanoEstivaBulk plano, List<ViolacaoEstivaDto> naoConformidades) {
        long semPorto = plano.getBobinas().stream()
                .filter(b -> b.getPortoDescarga() == null || b.getPortoDescarga().isBlank())
                .count();
        if (semPorto > 0) {
            naoConformidades.add(new ViolacaoEstivaDto("PORTO_DESCARGA_AUSENTE",
                    semPorto + " item(ns) sem porto de descarga definido — exigido pela SOLAS para Cargo Manifest",
                    null, "AVISO"));
        }
    }
}
