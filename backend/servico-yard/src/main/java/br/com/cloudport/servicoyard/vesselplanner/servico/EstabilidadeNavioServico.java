package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.vesselplanner.dto.EstabilidadeDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.ViolacaoHardConstraintDto;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EstabilidadeNavioServico {

    private static final double MAX_TRIM_METROS = 3.0;
    private static final double MAX_LIST_GRAUS = 2.0;

    public EstabilidadeDto calcular(EstivagemPlan plan) {
        List<SlotNavio> slots = plan.getSlots();
        List<SlotNavio> ocupados = slots.stream()
                .filter(slot -> slot.getCodigoContainer() != null)
                .toList();

        if (ocupados.isEmpty()) {
            return EstabilidadeDto.vazia();
        }

        double pesoTotal = ocupados.stream()
                .mapToDouble(slot -> slot.getPesoKg() != null ? slot.getPesoKg() : 0.0)
                .sum();
        if (pesoTotal == 0.0) {
            return EstabilidadeDto.vazia();
        }

        double lpp = plan.getComprimentoLpp();
        double boca = plan.getBoca();
        int numBays = calcularNumBays(slots);
        int numRows = calcularNumRows(slots);
        double espacamentoBay = lpp / Math.max(numBays, 1);
        double espacamentoRow = boca / Math.max(numRows, 1);
        double alturaAndar = 2.5;

        double somaMomentoLong = 0.0;
        double somaMomentoTrans = 0.0;
        double somaMomentoVert = 0.0;
        int rowCentro = (numRows + 1) / 2;

        for (SlotNavio slot : ocupados) {
            double peso = slot.getPesoKg() != null ? slot.getPesoKg() : 0.0;
            double posLong = slot.getBay() * espacamentoBay;
            double posTrans = (slot.getRowBay() - rowCentro) * espacamentoRow;
            double posVert = slot.getTier() * alturaAndar;
            somaMomentoLong += peso * posLong;
            somaMomentoTrans += peso * posTrans;
            somaMomentoVert += peso * posVert;
        }

        double lcg = somaMomentoLong / pesoTotal;
        double tcg = somaMomentoTrans / pesoTotal;
        double vcg = somaMomentoVert / pesoTotal;
        double trim = lcg - plan.getLcb();
        double gm = plan.getGm();
        double list = gm > 0 ? Math.toDegrees(Math.atan(tcg / gm)) : 0.0;

        List<ViolacaoHardConstraintDto> violacoes = new ArrayList<>();
        verificarTrim(trim, violacoes);
        verificarList(list, violacoes);
        verificarSobrePeso(slots, violacoes);
        verificarSegregacaoImo(slots, violacoes);
        verificarReefer(slots, violacoes);
        verificarOog(slots, violacoes);

        boolean aprovado = violacoes.stream()
                .noneMatch(violacao -> "PERIGO".equals(violacao.getSeveridade()));

        EstabilidadeDto dto = new EstabilidadeDto();
        dto.setTrimMetros(Math.round(trim * 100.0) / 100.0);
        dto.setListGraus(Math.round(list * 100.0) / 100.0);
        dto.setLcgMetros(Math.round(lcg * 100.0) / 100.0);
        dto.setTcgMetros(Math.round(tcg * 100.0) / 100.0);
        dto.setVcgMetros(Math.round(vcg * 100.0) / 100.0);
        dto.setPesoTotalToneladas(Math.round(pesoTotal / 1000.0 * 10.0) / 10.0);
        dto.setAprovado(aprovado);
        dto.setViolacoes(violacoes);
        return dto;
    }

    public List<ViolacaoHardConstraintDto> verificarSlot(EstivagemPlan plan,
                                                          SlotNavio slot,
                                                          String codigoContainer,
                                                          Double pesoKg,
                                                          String classeImo,
                                                          boolean reefer) {
        List<ViolacaoHardConstraintDto> violacoes = new ArrayList<>();

        if (slot.getMaxPesoKg() != null && pesoKg != null && pesoKg > slot.getMaxPesoKg()) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "SOBREPESO_SLOT",
                    "Peso " + pesoKg + " kg excede o limite do slot " + slot.getMaxPesoKg() + " kg",
                    slot.getId(),
                    "PERIGO"));
        }

        if (reefer && !ehSlotReefer(slot.getTipoSlot())) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "REEFER_SLOT_INVALIDO",
                    "Container reefer não pode ser alocado em slot tipo " + slot.getTipoSlot(),
                    slot.getId(),
                    "PERIGO"));
        }

        if (classeImo != null && !classeImo.isBlank() && !ehSlotPerigoso(slot.getTipoSlot())) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "SEGREGACAO_IMO_VIOLADA",
                    "Carga IMO classe " + classeImo + " exige slot perigoso dedicado",
                    slot.getId(),
                    "PERIGO"));
        }

        return violacoes;
    }

    private void verificarTrim(double trim, List<ViolacaoHardConstraintDto> violacoes) {
        if (Math.abs(trim) > MAX_TRIM_METROS) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "TRIM_EXCEDIDO",
                    String.format("Trim de %.2f m excede o limite de ±%.1f m", trim, MAX_TRIM_METROS),
                    null,
                    "PERIGO"));
        } else if (Math.abs(trim) > MAX_TRIM_METROS * 0.7) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "TRIM_EXCEDIDO",
                    String.format("Trim de %.2f m próximo ao limite de ±%.1f m", trim, MAX_TRIM_METROS),
                    null,
                    "AVISO"));
        }
    }

    private void verificarList(double list, List<ViolacaoHardConstraintDto> violacoes) {
        if (Math.abs(list) > MAX_LIST_GRAUS) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "LIST_EXCEDIDO",
                    String.format("Banda de %.2f° excede o limite de ±%.1f°", list, MAX_LIST_GRAUS),
                    null,
                    "PERIGO"));
        } else if (Math.abs(list) > MAX_LIST_GRAUS * 0.7) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "LIST_EXCEDIDO",
                    String.format("Banda de %.2f° próxima ao limite de ±%.1f°", list, MAX_LIST_GRAUS),
                    null,
                    "AVISO"));
        }
    }

    private void verificarSobrePeso(List<SlotNavio> slots, List<ViolacaoHardConstraintDto> violacoes) {
        for (SlotNavio slot : slots) {
            if (slot.getCodigoContainer() != null
                    && slot.getMaxPesoKg() != null
                    && slot.getPesoKg() != null
                    && slot.getPesoKg() > slot.getMaxPesoKg()) {
                violacoes.add(new ViolacaoHardConstraintDto(
                        "SOBREPESO_SLOT",
                        "Slot bay=" + slot.getBay() + " row=" + slot.getRowBay() + " tier=" + slot.getTier()
                                + ": peso " + slot.getPesoKg() + " kg > máximo " + slot.getMaxPesoKg() + " kg",
                        slot.getId(),
                        "PERIGO"));
            }
        }
    }

    private void verificarSegregacaoImo(List<SlotNavio> slots,
                                         List<ViolacaoHardConstraintDto> violacoes) {
        List<SlotNavio> perigosos = slots.stream()
                .filter(slot -> slot.getCodigoContainer() != null)
                .filter(slot -> slot.isPerigoso()
                        || (slot.getClasseImo() != null && !slot.getClasseImo().isBlank()))
                .toList();

        for (SlotNavio slot : perigosos) {
            if (!ehSlotPerigoso(slot.getTipoSlot())) {
                violacoes.add(new ViolacaoHardConstraintDto(
                        "SEGREGACAO_IMO_VIOLADA",
                        "Container IMO " + slot.getCodigoContainer() + " (classe " + slot.getClasseImo()
                                + ") fora de slot perigoso dedicado",
                        slot.getId(),
                        "PERIGO"));
            }
        }

        for (int primeiro = 0; primeiro < perigosos.size(); primeiro++) {
            for (int segundo = primeiro + 1; segundo < perigosos.size(); segundo++) {
                SlotNavio slotA = perigosos.get(primeiro);
                SlotNavio slotB = perigosos.get(segundo);
                if (adjacentes(slotA, slotB) && !mesmaClasseOuGrupo(slotA, slotB)) {
                    violacoes.add(new ViolacaoHardConstraintDto(
                            "SEGREGACAO_IMO_VIOLADA",
                            "Cargas perigosas incompatíveis estão em posições adjacentes: "
                                    + slotA.getCodigoContainer() + " e " + slotB.getCodigoContainer(),
                            slotB.getId(),
                            "PERIGO"));
                }
            }
        }
    }

    private void verificarReefer(List<SlotNavio> slots, List<ViolacaoHardConstraintDto> violacoes) {
        for (SlotNavio slot : slots) {
            if (slot.isReefer() && slot.getCodigoContainer() != null && !ehSlotReefer(slot.getTipoSlot())) {
                violacoes.add(new ViolacaoHardConstraintDto(
                        "REEFER_SLOT_INVALIDO",
                        "Container reefer " + slot.getCodigoContainer() + " em slot não reefer",
                        slot.getId(),
                        "PERIGO"));
            }
        }
    }

    private void verificarOog(List<SlotNavio> slots, List<ViolacaoHardConstraintDto> violacoes) {
        for (SlotNavio slot : slots) {
            if (slot.isOog() && slot.getCodigoContainer() != null && slot.getTipoSlot() != TipoSlotNavio.OOG) {
                violacoes.add(new ViolacaoHardConstraintDto(
                        "OOG_SLOT_INVALIDO",
                        "Container OOG " + slot.getCodigoContainer() + " em slot sem reserva dimensional",
                        slot.getId(),
                        "PERIGO"));
            }
        }
    }

    private boolean ehSlotReefer(TipoSlotNavio tipoSlot) {
        return tipoSlot == TipoSlotNavio.REEFER || tipoSlot == TipoSlotNavio.REEFER_PERIGOSO;
    }

    private boolean ehSlotPerigoso(TipoSlotNavio tipoSlot) {
        return tipoSlot == TipoSlotNavio.PERIGOSO || tipoSlot == TipoSlotNavio.REEFER_PERIGOSO;
    }

    private boolean adjacentes(SlotNavio primeiro, SlotNavio segundo) {
        return primeiro.getBay() == segundo.getBay()
                && Math.abs(primeiro.getRowBay() - segundo.getRowBay()) <= 1
                && Math.abs(primeiro.getTier() - segundo.getTier()) <= 1;
    }

    private boolean mesmaClasseOuGrupo(SlotNavio primeiro, SlotNavio segundo) {
        if (iguaisNaoVazios(primeiro.getClasseImo(), segundo.getClasseImo())) {
            return true;
        }
        return iguaisNaoVazios(primeiro.getGrupoSegregacao(), segundo.getGrupoSegregacao());
    }

    private boolean iguaisNaoVazios(String primeiro, String segundo) {
        return primeiro != null && !primeiro.isBlank()
                && segundo != null && !segundo.isBlank()
                && primeiro.equalsIgnoreCase(segundo);
    }

    private int calcularNumBays(List<SlotNavio> slots) {
        return slots.stream().mapToInt(SlotNavio::getBay).max().orElse(30);
    }

    private int calcularNumRows(List<SlotNavio> slots) {
        return slots.stream().mapToInt(SlotNavio::getRowBay).max().orElse(10);
    }
}
