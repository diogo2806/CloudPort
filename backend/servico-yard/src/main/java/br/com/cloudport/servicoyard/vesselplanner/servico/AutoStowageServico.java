package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.vesselplanner.modelo.ComprimentoConteiner;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AutoStowageServico {

    public int sugerirEstivagem(EstivagemPlan plan, List<BayPlanContainer> containers) {
        List<SlotNavio> slotsLivres = plan.getSlots().stream()
                .filter(slot -> slot.getCodigoContainer() == null)
                .filter(slot -> !slot.isRestrito())
                .collect(Collectors.toList());

        List<BayPlanContainer> ordenados = containers.stream()
                .sorted(Comparator.comparing(
                        container -> container.getPortoDescarga() != null
                                ? container.getPortoDescarga()
                                : "ZZZZ",
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());

        int alocados = 0;
        for (BayPlanContainer container : ordenados) {
            int comprimentoPes = ComprimentoConteiner.exigirComprimentoPes(container.getIsoCode());
            boolean reefer = ComprimentoConteiner.isReefer(container.getIsoCode());
            boolean imo = container.getStatusOperacao() != null
                    && container.getStatusOperacao().startsWith("IMO");

            Optional<SlotNavio> slotOpt = slotsLivres.stream()
                    .filter(slot -> compativel(plan, slot, container, comprimentoPes, reefer, imo))
                    .min(Comparator
                            .comparingInt(SlotNavio::getTier)
                            .thenComparingInt(SlotNavio::getBay)
                            .thenComparingInt(SlotNavio::getRowBay));

            if (slotOpt.isPresent()) {
                SlotNavio slot = slotOpt.get();
                slot.setCodigoContainer(container.getCodigoContainer());
                slot.setIsoCode(container.getIsoCode());
                slot.setPesoKg(container.getPesoKg());
                slot.setPortoCarga(container.getPortoCarga());
                slot.setPortoDescarga(container.getPortoDescarga());
                slot.setReefer(reefer);
                slot.setStatusAlertas("OK");
                slotsLivres.remove(slot);
                alocados++;
            }
        }

        return alocados;
    }

    public int limparEstivagem(EstivagemPlan plan) {
        int count = 0;
        for (SlotNavio slot : plan.getSlots()) {
            if (slot.getCodigoContainer() != null) {
                slot.setCodigoContainer(null);
                slot.setIsoCode(null);
                slot.setPesoKg(null);
                slot.setPortoCarga(null);
                slot.setPortoDescarga(null);
                slot.setClasseImo(null);
                slot.setReefer(false);
                slot.setStatusAlertas(slot.isRestrito() ? "RESTRITO" : "OK");
                count++;
            }
        }
        return count;
    }

    private boolean compativel(EstivagemPlan plan,
                               SlotNavio slot,
                               BayPlanContainer container,
                               int comprimentoPes,
                               boolean reefer,
                               boolean imo) {
        if (slot.isRestrito() || !slot.aceitaComprimentoPes(comprimentoPes)) {
            return false;
        }
        if (reefer && !slot.isTomadaReefer()) {
            return false;
        }
        if (imo && slot.isSobreHatchCover()) {
            return false;
        }
        if (container.getPesoKg() == null || !Double.isFinite(container.getPesoKg())) {
            return false;
        }
        if (slot.getMaxPesoKg() == null || container.getPesoKg() > slot.getMaxPesoKg()) {
            return false;
        }
        if (slot.getMaxPesoPilhaKg() == null) {
            return false;
        }

        double pesoAtualPilha = plan.getSlots().stream()
                .filter(outro -> outro.getBay() == slot.getBay())
                .filter(outro -> outro.getRowBay() == slot.getRowBay())
                .filter(outro -> outro.getCodigoContainer() != null)
                .map(SlotNavio::getPesoKg)
                .filter(peso -> peso != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        return pesoAtualPilha + container.getPesoKg() <= slot.getMaxPesoPilhaKg();
    }
}
