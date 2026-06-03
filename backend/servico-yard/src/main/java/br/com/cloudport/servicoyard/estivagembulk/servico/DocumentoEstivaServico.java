package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.DocumentoCargoManifestDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DocumentoEstivaServico {

    public DocumentoCargoManifestDto gerarCargoManifest(PlanoEstivaBulk plano) {
        DocumentoCargoManifestDto doc = new DocumentoCargoManifestDto();
        doc.setTipoDocumento("CARGO_MANIFEST");
        doc.setDataGeracao(LocalDateTime.now());

        if (plano.getNavio() != null) {
            doc.setNomeNavio(plano.getNavio().getNome());
            doc.setImoNavio(plano.getNavio().getImo());
        }
        doc.setCodigoViagem(plano.getCodigoViagem());
        doc.setPortoCarga(plano.getPortoCarga());
        doc.setPortoDescarga(plano.getPortoDescarga());

        Map<Long, PosicaoBobina> bobinaParaPosicao = plano.getPosicoes().stream()
                .filter(p -> p.getBobina() != null)
                .collect(Collectors.toMap(
                        p -> p.getBobina().getId(),
                        p -> p,
                        (a, b) -> a));

        List<DocumentoCargoManifestDto.ItemManifestDto> itens = new ArrayList<>();
        double pesoTotal = 0.0;

        for (BobinaManifesto bobina : plano.getBobinas().stream()
                .sorted(Comparator.comparing(b -> b.getPortoDescarga() != null ? b.getPortoDescarga() : ""))
                .toList()) {

            DocumentoCargoManifestDto.ItemManifestDto item = new DocumentoCargoManifestDto.ItemManifestDto();
            item.setCodigo(bobina.getCodigo());
            item.setTipoCarga("BOBINA_ACO");
            item.setPesoKg(bobina.getPesoKg() != null ? bobina.getPesoKg() : 0.0);
            item.setPortoDescarga(bobina.getPortoDescarga());
            item.setGrauAco(bobina.getGrauAco());
            pesoTotal += item.getPesoKg();

            PosicaoBobina pos = bobina.getId() != null ? bobinaParaPosicao.get(bobina.getId()) : null;
            if (pos != null) {
                item.setCamada(pos.getCamada());
                if (pos.getPorao() != null) item.setPoraoNumero(pos.getPorao().getNumero());
            }

            itens.add(item);
        }

        doc.setItens(itens);
        doc.setTotalItens(itens.size());
        doc.setPesoTotalToneladas(Math.round(pesoTotal / 10.0) / 100.0);

        List<String> obsSolas = new ArrayList<>();
        obsSolas.add("Carga siderúrgica — IMSBC Grupo C (carga sólida a granel)");
        obsSolas.add("Amarrio conforme CSS Code (Code of Safe Practice for Cargo Stowage and Securing)");
        obsSolas.add("Tanktop não excedido conforme plano de estivagem aprovado");
        if (plano.getNavio() != null && plano.getNavio().getGm() != null) {
            obsSolas.add(String.format("GM do navio na saída: %.2fm (SOLAS mínimo: 0.15m)", plano.getNavio().getGm()));
        }
        doc.setObservacoesSolas(obsSolas);

        return doc;
    }

    public DocumentoCargoManifestDto gerarPlanilhaEstivagem(PlanoEstivaBulk plano) {
        DocumentoCargoManifestDto doc = gerarCargoManifest(plano);
        doc.setTipoDocumento("STOWAGE_PLAN");

        List<DocumentoCargoManifestDto.ItemManifestDto> itensPorPoraoECamada = doc.getItens().stream()
                .sorted(Comparator.comparingInt(DocumentoCargoManifestDto.ItemManifestDto::getPoraoNumero)
                        .thenComparingInt(DocumentoCargoManifestDto.ItemManifestDto::getCamada))
                .toList();

        doc.setItens(itensPorPoraoECamada);
        return doc;
    }
}
