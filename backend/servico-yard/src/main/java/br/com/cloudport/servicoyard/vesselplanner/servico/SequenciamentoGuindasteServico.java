package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.vesselplanner.dto.GuindasteOperacaoDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.SequenciamentoGuindasteDto;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SequenciamentoGuindasteServico {

    public SequenciamentoGuindasteDto sequenciar(EstivagemPlan plan, int numGuindastes) {
        if (numGuindastes < 1) {
            numGuindastes = 1;
        }

        List<SlotNavio> ocupados = plan.getSlots().stream()
                .filter(slot -> slot.getCodigoContainer() != null)
                .sorted(Comparator
                        .comparingInt(SlotNavio::getBay)
                        .thenComparingInt(SlotNavio::getRowBay)
                        .thenComparing(Comparator.comparingInt(SlotNavio::getTier).reversed()))
                .collect(Collectors.toList());

        List<GuindasteOperacaoDto> sequencia = new ArrayList<>();
        int ordem = 1;
        int guindasteAtual = 1;

        for (SlotNavio slot : ocupados) {
            GuindasteOperacaoDto operacao = new GuindasteOperacaoDto();
            operacao.setOrdem(ordem++);
            operacao.setCodigoContainer(slot.getCodigoContainer());
            operacao.setBay(slot.getBay());
            operacao.setRowBay(slot.getRowBay());
            operacao.setTier(slot.getTier());
            operacao.setTipoOperacao("DESCARGA");
            operacao.setGuindasteId(guindasteAtual);
            operacao.setCodigoHatchCover(slot.getCodigoHatchCover());
            operacao.setSobreHatchCover(slot.isSobreHatchCover());
            sequencia.add(operacao);

            guindasteAtual = (guindasteAtual % numGuindastes) + 1;
        }

        SequenciamentoGuindasteDto dto = new SequenciamentoGuindasteDto();
        dto.setTotalOperacoes(sequencia.size());
        dto.setNumGuindastes(numGuindastes);
        dto.setSequencia(sequencia);
        return dto;
    }
}
