package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AutoStowageServico {

    public int sugerirEstivagem(EstivagemPlan plan, List<BayPlanContainer> containers) {
        List<SlotNavio> slotsLivres = plan.getSlots().stream()
                .filter(s -> s.getCodigoContainer() == null)
                .collect(Collectors.toList());

        List<BayPlanContainer> ordenados = containers.stream()
                .sorted(Comparator.comparing(
                        c -> c.getPortoDescarga() != null ? c.getPortoDescarga() : "ZZZZ",
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());

        int alocados = 0;

        for (BayPlanContainer c : ordenados) {
            boolean isReefer = "RE".equals(c.getIsoCode() != null && c.getIsoCode().length() >= 2
                    ? c.getIsoCode().substring(2, Math.min(4, c.getIsoCode().length())) : "");
            boolean isImo = c.getStatusOperacao() != null && c.getStatusOperacao().startsWith("IMO");
            boolean isPesado = c.getPesoKg() != null && c.getPesoKg() > 20000.0;

            Optional<SlotNavio> slotOpt = slotsLivres.stream()
                    .filter(s -> {
                        if (isReefer && s.getTipoSlot() != TipoSlotNavio.REEFER) return false;
                        if (!isReefer && s.getTipoSlot() == TipoSlotNavio.REEFER) return false;
                        if (isImo && s.getTipoSlot() == TipoSlotNavio.ESCOTILHA) return false;
                        if (isPesado && s.getTier() > 3) return false;
                        if (s.getMaxPesoKg() != null && c.getPesoKg() != null
                                && c.getPesoKg() > s.getMaxPesoKg()) return false;
                        return true;
                    })
                    .min(Comparator
                            .comparingInt(SlotNavio::getTier)
                            .thenComparingInt(SlotNavio::getBay));

            if (slotOpt.isPresent()) {
                SlotNavio slot = slotOpt.get();
                slot.setCodigoContainer(c.getCodigoContainer());
                slot.setIsoCode(c.getIsoCode());
                slot.setPesoKg(c.getPesoKg());
                slot.setPortoCarga(c.getPortoCarga());
                slot.setPortoDescarga(c.getPortoDescarga());
                slot.setReefer(isReefer);
                slot.setStatusAlertas("OK");
                slotsLivres.remove(slot);
                alocados++;
            }
        }

        return alocados;
    }

    public int limparEstivagem(EstivagemPlan plan) {
        int count = 0;
        for (SlotNavio s : plan.getSlots()) {
            if (s.getCodigoContainer() != null) {
                s.setCodigoContainer(null);
                s.setIsoCode(null);
                s.setPesoKg(null);
                s.setPortoCarga(null);
                s.setPortoDescarga(null);
                s.setClasseImo(null);
                s.setReefer(false);
                s.setStatusAlertas("OK");
                count++;
            }
        }
        return count;
    }
}
