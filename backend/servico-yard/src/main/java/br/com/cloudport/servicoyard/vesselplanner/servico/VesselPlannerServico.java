package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.repositorio.BayPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.dto.AlocacaoSlotRequisicaoDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.AlocacaoSlotRespostaDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.EstabilidadeDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.EstivagemPlanDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.RestowAnaliseDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.SequenciamentoGuindasteDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.SlotNavioDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.ViolacaoHardConstraintDto;
import br.com.cloudport.servicoyard.vesselplanner.mensagem.VesselPlannerEventoPublicador;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusEstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoSlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.SlotNavioRepositorio;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VesselPlannerServico {

    private final EstivagemPlanRepositorio planRepositorio;
    private final SlotNavioRepositorio slotRepositorio;
    private final BayPlanRepositorio bayPlanRepositorio;
    private final EstabilidadeNavioServico estabilidadeServico;
    private final RestowCalculadorServico restowServico;
    private final SequenciamentoGuindasteServico sequenciamentoServico;
    private final AutoStowageServico autoStowageServico;
    private final VesselPlannerEventoPublicador publicador;

    public VesselPlannerServico(EstivagemPlanRepositorio planRepositorio,
                                  SlotNavioRepositorio slotRepositorio,
                                  BayPlanRepositorio bayPlanRepositorio,
                                  EstabilidadeNavioServico estabilidadeServico,
                                  RestowCalculadorServico restowServico,
                                  SequenciamentoGuindasteServico sequenciamentoServico,
                                  AutoStowageServico autoStowageServico,
                                  VesselPlannerEventoPublicador publicador) {
        this.planRepositorio = planRepositorio;
        this.slotRepositorio = slotRepositorio;
        this.bayPlanRepositorio = bayPlanRepositorio;
        this.estabilidadeServico = estabilidadeServico;
        this.restowServico = restowServico;
        this.sequenciamentoServico = sequenciamentoServico;
        this.autoStowageServico = autoStowageServico;
        this.publicador = publicador;
    }

    @Transactional
    public EstivagemPlanDto criarPlanoDeBayPlan(Long bayPlanId) {
        BayPlan bayPlan = bayPlanRepositorio.findById(bayPlanId)
                .orElseThrow(() -> new EntityNotFoundException("BayPlan não encontrado: " + bayPlanId));

        EstivagemPlan plan = new EstivagemPlan();
        plan.setBayPlanId(bayPlanId);
        plan.setCodigoNavio(bayPlan.getCodigoNavio());
        plan.setCodigoViagem(bayPlan.getCodigoViagem());

        for (int bay = 1; bay <= 30; bay++) {
            for (int row = 1; row <= 10; row++) {
                for (int tier = 1; tier <= 8; tier++) {
                    SlotNavio slot = new SlotNavio();
                    slot.setEstivagem(plan);
                    slot.setBay(bay);
                    slot.setRowBay(row);
                    slot.setTier(tier);
                    slot.setTipoSlot(TipoSlotNavio.NORMAL);
                    slot.setMaxPesoKg(30000.0);
                    slot.setStatusAlertas("OK");
                    plan.getSlots().add(slot);
                }
            }
        }

        autoStowageServico.sugerirEstivagem(plan, bayPlan.getContainers());

        EstabilidadeDto estabilidade = estabilidadeServico.calcular(plan);
        plan.setTrimCalculado(estabilidade.getTrimMetros());
        plan.setListCalculado(estabilidade.getListGraus());
        plan.setLcgCalculado(estabilidade.getLcgMetros());
        plan.setTcgCalculado(estabilidade.getTcgMetros());

        plan = planRepositorio.save(plan);
        return toDto(plan, estabilidade);
    }

    @Transactional
    public AlocacaoSlotRespostaDto alocarContainer(Long planId, AlocacaoSlotRequisicaoDto req) {
        EstivagemPlan plan = buscarPlan(planId);
        exigirPlanoEditavel(plan);
        SlotNavio slot = slotRepositorio.findById(req.getSlotDestinoId())
                .orElseThrow(() -> new EntityNotFoundException("Slot não encontrado: " + req.getSlotDestinoId()));

        if (!slot.getEstivagem().getId().equals(planId)) {
            return AlocacaoSlotRespostaDto.falha("Slot não pertence ao plano informado", List.of());
        }

        List<ViolacaoHardConstraintDto> violacoes = estabilidadeServico.verificarSlot(
                plan, slot, req.getCodigoContainer(), req.getPesoKg(), req.getClasseImo(), req.isReefer());

        boolean temPerigo = violacoes.stream().anyMatch(v -> "PERIGO".equals(v.getSeveridade()));
        if (temPerigo) {
            return AlocacaoSlotRespostaDto.falha(
                    "Alocação bloqueada por violação de Hard Constraint", violacoes);
        }

        plan.getSlots().stream()
                .filter(s -> req.getCodigoContainer().equals(s.getCodigoContainer()))
                .forEach(s -> {
                    s.setCodigoContainer(null);
                    s.setIsoCode(null);
                    s.setPesoKg(null);
                    s.setPortoCarga(null);
                    s.setPortoDescarga(null);
                    s.setClasseImo(null);
                    s.setReefer(false);
                    s.setStatusAlertas("OK");
                });

        slot.setCodigoContainer(req.getCodigoContainer());
        slot.setIsoCode(req.getIsoCode());
        slot.setPesoKg(req.getPesoKg());
        slot.setPortoCarga(req.getPortoCarga());
        slot.setPortoDescarga(req.getPortoDescarga());
        slot.setClasseImo(req.getClasseImo());
        slot.setReefer(req.isReefer());
        slot.setStatusAlertas(violacoes.isEmpty() ? "OK" : "AVISO");

        EstabilidadeDto estabilidade = estabilidadeServico.calcular(plan);
        plan.setTrimCalculado(estabilidade.getTrimMetros());
        plan.setListCalculado(estabilidade.getListGraus());
        plan.setLcgCalculado(estabilidade.getLcgMetros());
        plan.setTcgCalculado(estabilidade.getTcgMetros());
        planRepositorio.save(plan);

        SlotNavioDto slotDto = toSlotDto(slot);
        publicador.publicarAtualizacaoSlot(planId, slotDto, estabilidade);

        return AlocacaoSlotRespostaDto.ok(estabilidade, slotDto);
    }

    @Transactional
    public EstivagemPlanDto autoEstivar(Long planId) {
        EstivagemPlan plan = buscarPlan(planId);
        exigirPlanoEditavel(plan);
        autoStowageServico.limparEstivagem(plan);
        autoStowageServico.sugerirEstivagem(plan,
                bayPlanRepositorio.findById(plan.getBayPlanId())
                        .map(BayPlan::getContainers)
                        .orElse(List.of()));

        EstabilidadeDto estabilidade = estabilidadeServico.calcular(plan);
        plan.setTrimCalculado(estabilidade.getTrimMetros());
        plan.setListCalculado(estabilidade.getListGraus());
        plan.setLcgCalculado(estabilidade.getLcgMetros());
        plan.setTcgCalculado(estabilidade.getTcgMetros());
        planRepositorio.save(plan);
        return toDto(plan, estabilidade);
    }

    @Transactional(readOnly = true)
    public EstivagemPlanDto buscarPorId(Long planId) {
        EstivagemPlan plan = buscarPlan(planId);
        EstabilidadeDto estabilidade = estabilidadeServico.calcular(plan);
        return toDto(plan, estabilidade);
    }

    @Transactional(readOnly = true)
    public EstabilidadeDto calcularEstabilidade(Long planId) {
        return estabilidadeServico.calcular(buscarPlan(planId));
    }

    @Transactional(readOnly = true)
    public RestowAnaliseDto analisarRestow(Long planId) {
        return restowServico.analisar(buscarPlan(planId));
    }

    @Transactional(readOnly = true)
    public SequenciamentoGuindasteDto sequenciarGuindastes(Long planId, int numGuindastes) {
        return sequenciamentoServico.sequenciar(buscarPlan(planId), numGuindastes);
    }

    @Transactional
    public EstivagemPlanDto validarEAprovar(Long planId) {
        EstivagemPlan plan = buscarPlan(planId);
        if (plan.getStatus() == StatusEstivagemPlan.APROVADO) {
            return toDto(plan, estabilidadeServico.calcular(plan));
        }
        if (plan.getStatus() == StatusEstivagemPlan.TRANSMITIDO) {
            throw new IllegalStateException("Plano transmitido não pode ser aprovado novamente");
        }
        EstabilidadeDto estabilidade = estabilidadeServico.calcular(plan);
        if (!estabilidade.isAprovado()) {
            throw new IllegalStateException("Plano possui violações de Hard Constraint e não pode ser aprovado");
        }
        plan.setStatus(StatusEstivagemPlan.APROVADO);
        planRepositorio.save(plan);
        return toDto(plan, estabilidade);
    }

    private EstivagemPlan buscarPlan(Long planId) {
        return planRepositorio.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("EstivagemPlan não encontrado: " + planId));
    }

    private void exigirPlanoEditavel(EstivagemPlan plan) {
        if (plan.getStatus() == StatusEstivagemPlan.APROVADO
                || plan.getStatus() == StatusEstivagemPlan.TRANSMITIDO) {
            throw new IllegalStateException("Plano aprovado ou transmitido não pode ser alterado por este comando");
        }
    }

    private EstivagemPlanDto toDto(EstivagemPlan plan, EstabilidadeDto estabilidade) {
        EstivagemPlanDto dto = new EstivagemPlanDto();
        dto.setId(plan.getId());
        dto.setBayPlanId(plan.getBayPlanId());
        dto.setCodigoNavio(plan.getCodigoNavio());
        dto.setCodigoViagem(plan.getCodigoViagem());
        dto.setStatus(plan.getStatus() != null ? plan.getStatus().name() : null);
        dto.setEstabilidade(estabilidade);
        List<SlotNavioDto> slots = plan.getSlots().stream()
                .map(this::toSlotDto)
                .collect(Collectors.toList());
        dto.setSlots(slots);
        dto.setTotalSlotsOcupados((int) slots.stream().filter(s -> s.getCodigoContainer() != null).count());
        dto.setTotalContainers(dto.getTotalSlotsOcupados());
        return dto;
    }

    private SlotNavioDto toSlotDto(SlotNavio s) {
        SlotNavioDto dto = new SlotNavioDto();
        dto.setId(s.getId());
        dto.setBay(s.getBay());
        dto.setRowBay(s.getRowBay());
        dto.setTier(s.getTier());
        dto.setTipoSlot(s.getTipoSlot() != null ? s.getTipoSlot().name() : null);
        dto.setMaxPesoKg(s.getMaxPesoKg());
        dto.setCodigoContainer(s.getCodigoContainer());
        dto.setIsoCode(s.getIsoCode());
        dto.setPesoKg(s.getPesoKg());
        dto.setPortoCarga(s.getPortoCarga());
        dto.setPortoDescarga(s.getPortoDescarga());
        dto.setClasseImo(s.getClasseImo());
        dto.setReefer(s.isReefer());
        dto.setStatusAlertas(s.getStatusAlertas());
        return dto;
    }
}
