package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.repositorio.BayPlanRepositorio;
import br.com.cloudport.servicoyard.integracao.navio.ContextoPlanejamentoNavio;
import br.com.cloudport.servicoyard.integracao.navio.IdentidadePlanejamentoNavioServico;
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
    private final IdentidadePlanejamentoNavioServico identidadeServico;

    public VesselPlannerServico(EstivagemPlanRepositorio planRepositorio,
                                 SlotNavioRepositorio slotRepositorio,
                                 BayPlanRepositorio bayPlanRepositorio,
                                 EstabilidadeNavioServico estabilidadeServico,
                                 RestowCalculadorServico restowServico,
                                 SequenciamentoGuindasteServico sequenciamentoServico,
                                 AutoStowageServico autoStowageServico,
                                 VesselPlannerEventoPublicador publicador,
                                 IdentidadePlanejamentoNavioServico identidadeServico) {
        this.planRepositorio = planRepositorio;
        this.slotRepositorio = slotRepositorio;
        this.bayPlanRepositorio = bayPlanRepositorio;
        this.estabilidadeServico = estabilidadeServico;
        this.restowServico = restowServico;
        this.sequenciamentoServico = sequenciamentoServico;
        this.autoStowageServico = autoStowageServico;
        this.publicador = publicador;
        this.identidadeServico = identidadeServico;
    }

    @Transactional
    public EstivagemPlanDto criarPlanoDeBayPlan(Long bayPlanId, Long visitaNavioId) {
        BayPlan bayPlan = bayPlanRepositorio.findById(bayPlanId)
                .orElseThrow(() -> new EntityNotFoundException("BayPlan não encontrado: " + bayPlanId));
        ContextoPlanejamentoNavio contexto = identidadeServico.resolverVisita(
                visitaNavioId,
                null,
                bayPlan.getCodigoNavio(),
                bayPlan.getCodigoViagem());

        EstivagemPlan plan = new EstivagemPlan();
        plan.setBayPlanId(bayPlanId);
        plan.setNavioCadastroId(contexto.navio().identificador());
        plan.setVisitaNavioId(contexto.visita().identificador());
        plan.setCodigoVisita(contexto.visita().codigoVisita());
        plan.setVersaoNavioCanonico(contexto.navio().versao());
        plan.setVersaoVisita(contexto.visita().versao());
        plan.setCodigoNavio(contexto.navio().codigoImo());
        plan.setCodigoViagem(contexto.codigoViagem());

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
        validarFonteCanonica(plan);
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
        validarFonteCanonica(plan);
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
        EstivagemPlan plan = buscarPlan(planId);
        validarFonteCanonica(plan);
        return estabilidadeServico.calcular(plan);
    }

    @Transactional(readOnly = true)
    public RestowAnaliseDto analisarRestow(Long planId) {
        EstivagemPlan plan = buscarPlan(planId);
        validarFonteCanonica(plan);
        return restowServico.analisar(plan);
    }

    @Transactional(readOnly = true)
    public SequenciamentoGuindasteDto sequenciarGuindastes(Long planId, int numGuindastes) {
        EstivagemPlan plan = buscarPlan(planId);
        validarFonteCanonica(plan);
        return sequenciamentoServico.sequenciar(plan, numGuindastes);
    }

    @Transactional
    public EstivagemPlanDto validarEAprovar(Long planId) {
        EstivagemPlan plan = buscarPlan(planId);
        validarFonteCanonica(plan);
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

    private void validarFonteCanonica(EstivagemPlan plan) {
        identidadeServico.validarFontePersistida(
                plan.getVisitaNavioId(),
                plan.getNavioCadastroId(),
                plan.getCodigoNavio(),
                plan.getCodigoViagem(),
                plan.getVersaoNavioCanonico(),
                plan.getVersaoVisita());
    }

    private EstivagemPlanDto toDto(EstivagemPlan plan, EstabilidadeDto estabilidade) {
        EstivagemPlanDto dto = new EstivagemPlanDto();
        dto.setId(plan.getId());
        dto.setBayPlanId(plan.getBayPlanId());
        dto.setNavioCadastroId(plan.getNavioCadastroId());
        dto.setVisitaNavioId(plan.getVisitaNavioId());
        dto.setCodigoVisita(plan.getCodigoVisita());
        dto.setVersaoNavioCanonico(plan.getVersaoNavioCanonico());
        dto.setVersaoVisita(plan.getVersaoVisita());
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
