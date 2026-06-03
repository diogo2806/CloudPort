package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.vesselplanner.dto.EstabilidadeDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.ViolacaoHardConstraintDto;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
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
                .filter(s -> s.getCodigoContainer() != null)
                .toList();

        if (ocupados.isEmpty()) {
            return EstabilidadeDto.vazia();
        }

        double pesoTotal = ocupados.stream()
                .mapToDouble(s -> s.getPesoKg() != null ? s.getPesoKg() : 0.0)
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

        for (SlotNavio s : ocupados) {
            double peso = s.getPesoKg() != null ? s.getPesoKg() : 0.0;
            double posLong = s.getBay() * espacamentoBay;
            double posTrans = (s.getRowBay() - rowCentro) * espacamentoRow;
            double posVert = s.getTier() * alturaAndar;
            somaMomentoLong += peso * posLong;
            somaMomentoTrans += peso * posTrans;
            somaMomentoVert += peso * posVert;
        }

        double lcg = somaMomentoLong / pesoTotal;
        double tcg = somaMomentoTrans / pesoTotal;
        double vcg = somaMomentoVert / pesoTotal;

        double trim = lcg - plan.getLcb();

        double gm = plan.getGm();
        double list = (gm > 0) ? Math.toDegrees(Math.atan(tcg / gm)) : 0.0;

        List<ViolacaoHardConstraintDto> violacoes = new ArrayList<>();
        verificarTrim(trim, violacoes);
        verificarList(list, violacoes);
        verificarSobrePeso(slots, violacoes);
        verificarSegregacaoImo(slots, violacoes);
        verificarReefer(slots, violacoes);

        boolean aprovado = violacoes.stream()
                .noneMatch(v -> "PERIGO".equals(v.getSeveridade()));

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

    public List<ViolacaoHardConstraintDto> verificarSlot(EstivagemPlan plan, SlotNavio slot, String codigoContainer, Double pesoKg, String classeImo, boolean reefer) {
        List<ViolacaoHardConstraintDto> violacoes = new ArrayList<>();

        if (slot.getMaxPesoKg() != null && pesoKg != null && pesoKg > slot.getMaxPesoKg()) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "SOBREPESO_SLOT",
                    "Peso " + pesoKg + " kg excede o limite do slot " + slot.getMaxPesoKg() + " kg",
                    slot.getId(),
                    "PERIGO"));
        }

        if (reefer && slot.getTipoSlot() != br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio.REEFER) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "REEFER_SLOT_INVALIDO",
                    "Container reefer não pode ser alocado em slot tipo " + slot.getTipoSlot(),
                    slot.getId(),
                    "PERIGO"));
        }

        if (classeImo != null && !classeImo.isBlank()
                && slot.getTipoSlot() == br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio.ESCOTILHA) {
            violacoes.add(new ViolacaoHardConstraintDto(
                    "SEGREGACAO_IMO_VIOLADA",
                    "Carga IMO classe " + classeImo + " não pode ser posicionada em escotilha",
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
        for (SlotNavio s : slots) {
            if (s.getCodigoContainer() != null
                    && s.getMaxPesoKg() != null
                    && s.getPesoKg() != null
                    && s.getPesoKg() > s.getMaxPesoKg()) {
                violacoes.add(new ViolacaoHardConstraintDto(
                        "SOBREPESO_SLOT",
                        "Slot bay=" + s.getBay() + " row=" + s.getRowBay() + " tier=" + s.getTier()
                                + ": peso " + s.getPesoKg() + " kg > máximo " + s.getMaxPesoKg() + " kg",
                        s.getId(),
                        "PERIGO"));
            }
        }
    }

    private void verificarSegregacaoImo(List<SlotNavio> slots, List<ViolacaoHardConstraintDto> violacoes) {
        List<SlotNavio> imoSlots = slots.stream()
                .filter(s -> s.getClasseImo() != null && !s.getClasseImo().isBlank())
                .toList();

        for (SlotNavio s : imoSlots) {
            if (s.getTipoSlot() == br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio.ESCOTILHA) {
                violacoes.add(new ViolacaoHardConstraintDto(
                        "SEGREGACAO_IMO_VIOLADA",
                        "Container IMO " + s.getCodigoContainer() + " (classe " + s.getClasseImo()
                                + ") em escotilha — violação de segregação",
                        s.getId(),
                        "PERIGO"));
            }
        }
    }

    private void verificarReefer(List<SlotNavio> slots, List<ViolacaoHardConstraintDto> violacoes) {
        for (SlotNavio s : slots) {
            if (s.isReefer() && s.getCodigoContainer() != null
                    && s.getTipoSlot() != br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio.REEFER) {
                violacoes.add(new ViolacaoHardConstraintDto(
                        "REEFER_SLOT_INVALIDO",
                        "Container reefer " + s.getCodigoContainer() + " em slot não reefer",
                        s.getId(),
                        "PERIGO"));
            }
        }
    }

    private int calcularNumBays(List<SlotNavio> slots) {
        return slots.stream().mapToInt(SlotNavio::getBay).max().orElse(30);
    }

    private int calcularNumRows(List<SlotNavio> slots) {
        return slots.stream().mapToInt(SlotNavio::getRowBay).max().orElse(10);
    }
}
