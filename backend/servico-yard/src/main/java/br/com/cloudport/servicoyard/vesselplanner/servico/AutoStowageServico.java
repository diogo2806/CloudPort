package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.EstadoCargaContainer;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AutoStowageServico {

    private static final double LIMITE_CONTAINER_PESADO_KG = 20_000.0;
    private static final int TIER_MAXIMO_CONTAINER_PESADO = 3;

    private final GeometriaNavioServico geometriaServico;

    public AutoStowageServico(GeometriaNavioServico geometriaServico) {
        this.geometriaServico = geometriaServico;
    }

    public int sugerirEstivagem(EstivagemPlan plan, List<BayPlanContainer> containers) {
        geometriaServico.validarPlanoOperacional(plan);
        List<SlotNavio> slotsLivres = plan.getSlots().stream()
                .filter(slot -> slot.getCodigoContainer() == null)
                .filter(slot -> !slot.isRestrito())
                .collect(Collectors.toList());

        List<BayPlanContainer> ordenados = containers.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(BayPlanContainer::getPortoDescarga,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BayPlanContainer::getPesoOperacionalKg,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        int alocados = 0;
        for (BayPlanContainer container : ordenados) {
            Optional<SlotNavio> destino = slotsLivres.stream()
                    .filter(slot -> compativel(plan, slot, container))
                    .sorted(comparadorSlots(container))
                    .findFirst();

            if (destino.isPresent()) {
                geometriaServico.preencherSlot(destino.get(), container);
                slotsLivres.remove(destino.get());
                alocados++;
            }
        }
        return alocados;
    }

    public int limparEstivagem(EstivagemPlan plan) {
        int removidos = 0;
        for (SlotNavio slot : plan.getSlots()) {
            if (slot.getCodigoContainer() != null) {
                limparSlot(slot);
                removidos++;
            }
        }
        return removidos;
    }

    private boolean compativel(EstivagemPlan plan, SlotNavio slot, BayPlanContainer container) {
        Double pesoOperacional = container.getPesoOperacionalKg();
        boolean possuiViolacaoGeometrica = geometriaServico.verificarAlocacao(
                        plan,
                        slot,
                        container.getCodigoContainer(),
                        container.getIsoCode(),
                        pesoOperacional,
                        container.isReefer(),
                        container.isPerigoso(),
                        container.isOog())
                .stream()
                .anyMatch(violacao -> "PERIGO".equals(violacao.getSeveridade()));
        if (possuiViolacaoGeometrica) {
            return false;
        }
        if (pesoOperacional > LIMITE_CONTAINER_PESADO_KG
                && slot.getTier() > TIER_MAXIMO_CONTAINER_PESADO) {
            return false;
        }
        if (container.isOog() && existeCargaAdjacente(plan, slot)) {
            return false;
        }
        if (!container.isOog() && existeOogAdjacente(plan, slot)) {
            return false;
        }
        return !container.isPerigoso() || segregacaoCompativel(plan, slot, container);
    }

    private boolean segregacaoCompativel(EstivagemPlan plan,
                                           SlotNavio destino,
                                           BayPlanContainer container) {
        return plan.getSlots().stream()
                .filter(SlotNavio::isPerigoso)
                .filter(slot -> slot.getCodigoContainer() != null)
                .filter(slot -> adjacente(destino, slot))
                .allMatch(slot -> mesmaClasseOuGrupo(slot, container));
    }

    private boolean mesmaClasseOuGrupo(SlotNavio slot, BayPlanContainer container) {
        if (iguaisNaoVazios(slot.getClasseImo(), container.getClasseImo())) {
            return true;
        }
        return iguaisNaoVazios(slot.getGrupoSegregacao(), container.getGrupoSegregacao());
    }

    private boolean existeCargaAdjacente(EstivagemPlan plan, SlotNavio destino) {
        return plan.getSlots().stream()
                .filter(slot -> slot.getCodigoContainer() != null)
                .anyMatch(slot -> adjacente(destino, slot));
    }

    private boolean existeOogAdjacente(EstivagemPlan plan, SlotNavio destino) {
        return plan.getSlots().stream()
                .filter(SlotNavio::isOog)
                .filter(slot -> slot.getCodigoContainer() != null)
                .anyMatch(slot -> adjacente(destino, slot));
    }

    private boolean adjacente(SlotNavio primeiro, SlotNavio segundo) {
        return primeiro.getBay() == segundo.getBay()
                && Math.abs(primeiro.getRowBay() - segundo.getRowBay()) <= 1
                && Math.abs(primeiro.getTier() - segundo.getTier()) <= 1;
    }

    private Comparator<SlotNavio> comparadorSlots(BayPlanContainer container) {
        Comparator<SlotNavio> porTier = Comparator.comparingInt(SlotNavio::getTier);
        if (container.getPesoOperacionalKg() != null
                && container.getPesoOperacionalKg() > LIMITE_CONTAINER_PESADO_KG) {
            return porTier.thenComparingInt(SlotNavio::getBay).thenComparingInt(SlotNavio::getRowBay);
        }
        return Comparator.comparingInt(SlotNavio::getBay)
                .thenComparingInt(SlotNavio::getRowBay)
                .thenComparing(porTier);
    }

    private void limparSlot(SlotNavio slot) {
        slot.setCodigoContainer(null);
        slot.setIsoCode(null);
        slot.setPesoKg(null);
        slot.setPesoVgmKg(null);
        slot.setEstadoCarga(EstadoCargaContainer.DESCONHECIDO);
        slot.setPortoCarga(null);
        slot.setPortoDescarga(null);
        slot.setClasseImo(null);
        slot.setNumeroOnu(null);
        slot.setGrupoSegregacao(null);
        slot.setPerigoso(false);
        slot.setReefer(false);
        slot.setTemperaturaRequeridaC(null);
        slot.setTemperaturaMinimaC(null);
        slot.setTemperaturaMaximaC(null);
        slot.setOog(false);
        slot.setExcessoFrontalCm(null);
        slot.setExcessoTraseiroCm(null);
        slot.setExcessoEsquerdoCm(null);
        slot.setExcessoDireitoCm(null);
        slot.setExcessoAlturaCm(null);
        slot.setStatusAlertas(slot.isRestrito() ? "RESTRITO" : "OK");
    }

    private boolean iguaisNaoVazios(String primeiro, String segundo) {
        return primeiro != null && !primeiro.isBlank()
                && segundo != null && !segundo.isBlank()
                && primeiro.equalsIgnoreCase(segundo);
    }
}
