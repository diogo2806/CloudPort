package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.SequenciaEmbarqueDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PortoViagem;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TipoOperacaoPorto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SequenciaEmbarqueServico {

    public SequenciaEmbarqueDto analisarSequencia(PlanoEstivaBulk plano) {
        SequenciaEmbarqueDto dto = new SequenciaEmbarqueDto();

        List<PortoViagem> portosOrdenados = plano.getPortosViagem().stream()
                .filter(p -> p.getTipoOperacao() == TipoOperacaoPorto.DESCARGA
                        || p.getTipoOperacao() == TipoOperacaoPorto.CARGA_DESCARGA)
                .sorted(Comparator.comparingInt(PortoViagem::getSequencia))
                .toList();

        List<SequenciaEmbarqueDto.PortoSequenciaDto> sequencia = new ArrayList<>();
        List<ViolacaoEstivaDto> violacoes = new ArrayList<>();
        int totalRestow = 0;

        for (int i = 0; i < portosOrdenados.size(); i++) {
            PortoViagem porto = portosOrdenados.get(i);
            SequenciaEmbarqueDto.PortoSequenciaDto pDto = new SequenciaEmbarqueDto.PortoSequenciaDto();
            pDto.setCodigoPorto(porto.getCodigoPorto());
            pDto.setSequencia(porto.getSequencia());

            List<PosicaoBobina> itensDestePorto = plano.getPosicoes().stream()
                    .filter(p -> porto.getCodigoPorto().equals(
                            p.getBobina() != null ? p.getBobina().getPortoDescarga() : null))
                    .toList();

            pDto.setTotalItensDescarga(itensDestePorto.size());

            List<String> restow = calcularRestowParaPorto(plano, porto.getCodigoPorto(), portosOrdenados, i);
            pDto.setItensRestow(restow);
            pDto.setTotalItensRestow(restow.size());
            totalRestow += restow.size();

            if (!restow.isEmpty()) {
                violacoes.add(new ViolacaoEstivaDto("RESTOW_NECESSARIO",
                        "Porto " + porto.getCodigoPorto() + " requer restow de " + restow.size() + " item(ns)",
                        null, "AVISO"));
            }

            sequencia.add(pDto);
        }

        dto.setSequenciaPortos(sequencia);
        dto.setTotalItensRestow(totalRestow);
        dto.setLifoValido(violacoes.isEmpty());
        dto.setViolacoesLifo(violacoes);
        return dto;
    }

    private List<String> calcularRestowParaPorto(PlanoEstivaBulk plano, String codigoPorto,
                                                   List<PortoViagem> portosOrdenados, int indiceAtual) {
        List<String> restow = new ArrayList<>();

        Map<String, List<PosicaoBobina>> pilhas = new HashMap<>();
        for (PosicaoBobina pos : plano.getPosicoes()) {
            String chave = (pos.getPorao() != null ? pos.getPorao().getId() : 0)
                    + "_" + pos.getPosicaoX() + "_" + pos.getPosicaoY();
            pilhas.computeIfAbsent(chave, k -> new ArrayList<>()).add(pos);
        }

        for (Map.Entry<String, List<PosicaoBobina>> entry : pilhas.entrySet()) {
            List<PosicaoBobina> pilha = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(PosicaoBobina::getCamada))
                    .toList();

            for (int i = 0; i < pilha.size(); i++) {
                PosicaoBobina item = pilha.get(i);
                if (item.getBobina() == null || item.getBobina().getPortoDescarga() == null) continue;
                if (!codigoPorto.equals(item.getBobina().getPortoDescarga())) continue;

                for (int j = i + 1; j < pilha.size(); j++) {
                    PosicaoBobina acima = pilha.get(j);
                    if (acima.getBobina() == null) continue;
                    String portoAcima = acima.getBobina().getPortoDescarga();
                    if (portoAcima == null) continue;

                    boolean portoAcimaDescarregaDepois = portosOrdenados.stream()
                            .filter(p -> p.getCodigoPorto().equals(portoAcima))
                            .findFirst()
                            .map(p -> p.getSequencia() > indiceAtual)
                            .orElse(false);

                    if (portoAcimaDescarregaDepois) {
                        String cod = acima.getBobina().getCodigo();
                        if (!restow.contains(cod)) restow.add(cod);
                    }
                }
            }
        }

        return restow;
    }
}
