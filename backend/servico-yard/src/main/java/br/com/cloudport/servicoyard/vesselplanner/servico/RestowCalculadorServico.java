package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.vesselplanner.dto.RestowAnaliseDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.RestowMovimentoDto;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RestowCalculadorServico {

    public RestowAnaliseDto analisar(EstivagemPlan plan) {
        List<SlotNavio> ocupados = plan.getSlots().stream()
                .filter(s -> s.getCodigoContainer() != null)
                .toList();

        Map<String, List<SlotNavio>> stacks = new HashMap<>();
        for (SlotNavio s : ocupados) {
            String chave = s.getBay() + "_" + s.getRowBay();
            stacks.computeIfAbsent(chave, k -> new ArrayList<>()).add(s);
        }

        List<RestowMovimentoDto> movimentos = new ArrayList<>();

        for (Map.Entry<String, List<SlotNavio>> entry : stacks.entrySet()) {
            List<SlotNavio> pilha = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(SlotNavio::getTier))
                    .toList();

            for (int i = 0; i < pilha.size() - 1; i++) {
                SlotNavio abaixo = pilha.get(i);
                SlotNavio acima = pilha.get(i + 1);

                if (abaixo.getPortoDescarga() != null && acima.getPortoDescarga() != null
                        && abaixo.getPortoDescarga().compareTo(acima.getPortoDescarga()) < 0) {
                    RestowMovimentoDto mov = new RestowMovimentoDto();
                    mov.setCodigoContainer(abaixo.getCodigoContainer());
                    mov.setBayAtual(abaixo.getBay());
                    mov.setRowAtual(abaixo.getRowBay());
                    mov.setTierAtual(abaixo.getTier());
                    mov.setBayDestino(abaixo.getBay() + 2);
                    mov.setRowDestino(abaixo.getRowBay());
                    mov.setTierDestino(1);
                    mov.setMotivoRestow("ORDEM_PORTO_INVALIDA: "
                            + abaixo.getPortoDescarga() + " abaixo de "
                            + acima.getPortoDescarga());
                    movimentos.add(mov);
                }
            }
        }

        RestowAnaliseDto dto = new RestowAnaliseDto();
        dto.setTotalRestows(movimentos.size());
        dto.setMovimentos(movimentos);
        dto.setDescricao(movimentos.isEmpty()
                ? "Nenhum re-estivamento necessário"
                : movimentos.size() + " contêiner(s) precisam ser re-estivados para otimizar a ordem de descarga");
        return dto;
    }
}
