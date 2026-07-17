package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.EstadoCargaContainer;
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
import br.com.cloudport.servicoyard.vesselplanner.modelo.PerfilGeometriaNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusEstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.SlotNavioRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private final GeometriaNavioServico geometriaServico;
    private final VesselPlannerEventoPublicador publicador;
    private final IdentidadePlanejamentoNavioServico identidadeServico;

    public VesselPlannerServico(EstivagemPlanRepositorio planRepositorio,
                                  SlotNavioRepositorio slotRepositorio,
                                  BayPlanRepositorio bayPlanRepositorio,
                                  EstabilidadeNavioServico estabilidadeServico,
                                  RestowCalculadorServico restowServico,
                                  SequenciamentoGuindasteServico sequenciamentoServico,
                                  AutoStowageServico autoStowageServico,
                                  GeometriaNavioServico geometriaServico,
                                  VesselPlannerEventoPublicador publicador,
                                  IdentidadePlanejamentoNavioServico identidadeServico) {
        this.planRepositorio = planRepositorio;
        this.slotRepositorio = slotRepositorio;
        this.bayPlanRepositorio = bayPlanRepositorio;
        this.estabilidadeServico = estabilidadeServico;
        this.restowServico = restowServico;
        this.sequenciamentoServico = sequenciamentoServico;
        this.autoStowageServico = autoStowageServico;
        this.geometriaServico = geometriaServico;
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
        PerfilGeometriaNavio perfil = geometriaServico.carregarPerfilAprovado(contexto.navio().codigoImo());

        EstivagemPlan plan = new EstivagemPlan();
        plan.setBayPlanId(bayPlanId);
        plan.setNavioCadastroId(contexto.navio().identificador());
        plan.setVisitaNavioId(contexto.visita().identificador());
        plan.setCodigoVisita(contexto.visita().codigoVisita());
        plan.setVersaoNavioCanonico(contexto.navio().versao());
        plan.setVersaoVisita(contexto.visita().versao());
        plan.setCodigoNavio(contexto.navio().codigoImo());
        plan.setCodigoViagem(contexto.codigoViagem());
        geometriaServico.aplicarPerfil(plan, perfil);
        geometriaServico.posicionarConteineresImportados(plan, bayPlan.getContainers());

        EstabilidadeDto estabilidade = recalcularEstabilidade(plan);
        plan = planRepositorio.save(plan);
        return toDto(plan, estabilidade);
    }

    @Transactional
    public AlocacaoSlotRespostaDto alocarContainer(Long planId, AlocacaoSlotRequisicaoDto requisicao) {
        EstivagemPlan plan = buscarPlanOperacional(planId);
        validarFonteCanonica(plan);
        exigirPlanoEditavel(plan);
        SlotNavio slot = slotRepositorio.findById(requisicao.getSlotDestinoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Slot não encontrado: " + requisicao.getSlotDestinoId()));

        if (!slot.getEstivagem().getId().equals(planId)) {
            return AlocacaoSlotRespostaDto.falha("Slot não pertence ao plano informado", List.of());
        }
        if (slot.getCodigoContainer() != null
                && !slot.getCodigoContainer().equalsIgnoreCase(requisicao.getCodigoContainer())) {
            return AlocacaoSlotRespostaDto.falha("Slot já está ocupado por outro contêiner", List.of());
        }

        Double pesoOperacional = requisicao.getPesoVgmKg() != null
                ? requisicao.getPesoVgmKg()
                : requisicao.getPesoKg();
        List<ViolacaoHardConstraintDto> violacoes = new ArrayList<>(
                geometriaServico.verificarAlocacao(
                        plan,
                        slot,
                        requisicao.getCodigoContainer(),
                        requisicao.getIsoCode(),
                        pesoOperacional,
                        requisicao.isReefer(),
                        requisicao.isPerigoso(),
                        requisicao.isOog()));
        violacoes.addAll(estabilidadeServico.verificarSlot(
                plan,
                slot,
                requisicao.getCodigoContainer(),
                pesoOperacional,
                requisicao.getClasseImo(),
                requisicao.isReefer()));

        boolean temPerigo = violacoes.stream()
                .anyMatch(violacao -> "PERIGO".equals(violacao.getSeveridade()));
        if (temPerigo) {
            return AlocacaoSlotRespostaDto.falha(
                    "Alocação bloqueada por violação de Hard Constraint", violacoes);
        }

        plan.getSlots().stream()
                .filter(item -> requisicao.getCodigoContainer().equals(item.getCodigoContainer()))
                .forEach(this::limparSlot);

        slot.setCodigoContainer(requisicao.getCodigoContainer());
        slot.setIsoCode(requisicao.getIsoCode());
        slot.setPesoKg(pesoOperacional);
        slot.setPesoVgmKg(requisicao.getPesoVgmKg());
        slot.setEstadoCarga(requisicao.getEstadoCarga() != null
                ? requisicao.getEstadoCarga()
                : EstadoCargaContainer.DESCONHECIDO);
        slot.setPortoCarga(requisicao.getPortoCarga());
        slot.setPortoDescarga(requisicao.getPortoDescarga());
        slot.setClasseImo(requisicao.getClasseImo());
        slot.setNumeroOnu(requisicao.getNumeroOnu());
        slot.setGrupoSegregacao(requisicao.getGrupoSegregacao());
        slot.setPerigoso(requisicao.isPerigoso());
        slot.setReefer(requisicao.isReefer());
        slot.setTemperaturaRequeridaC(requisicao.getTemperaturaRequeridaC());
        slot.setTemperaturaMinimaC(requisicao.getTemperaturaMinimaC());
        slot.setTemperaturaMaximaC(requisicao.getTemperaturaMaximaC());
        slot.setOog(requisicao.isOog());
        slot.setExcessoFrontalCm(requisicao.getExcessoFrontalCm());
        slot.setExcessoTraseiroCm(requisicao.getExcessoTraseiroCm());
        slot.setExcessoEsquerdoCm(requisicao.getExcessoEsquerdoCm());
        slot.setExcessoDireitoCm(requisicao.getExcessoDireitoCm());
        slot.setExcessoAlturaCm(requisicao.getExcessoAlturaCm());
        slot.setStatusAlertas(violacoes.isEmpty() ? "OK" : "AVISO");

        EstabilidadeDto estabilidade = recalcularEstabilidade(plan);
        planRepositorio.save(plan);

        SlotNavioDto slotDto = toSlotDto(slot);
        publicador.publicarAtualizacaoSlot(planId, slotDto, estabilidade);
        return AlocacaoSlotRespostaDto.ok(estabilidade, slotDto);
    }

    @Transactional
    public EstivagemPlanDto autoEstivar(Long planId) {
        EstivagemPlan plan = buscarPlanOperacional(planId);
        validarFonteCanonica(plan);
        exigirPlanoEditavel(plan);
        autoStowageServico.limparEstivagem(plan);
        List<BayPlanContainer> containers = bayPlanRepositorio
                .findById(plan.getBayPlanId())
                .map(BayPlan::getContainers)
                .orElseThrow(() -> new EntityNotFoundException(
                        "BayPlan não encontrado: " + plan.getBayPlanId()));
        int esperados = (int) containers.stream().filter(Objects::nonNull).count();
        int alocados = autoStowageServico.sugerirEstivagem(plan, containers);
        if (alocados != esperados) {
            throw new IllegalStateException(
                    "Autoestivagem bloqueada: " + (esperados - alocados)
                            + " contêiner(es) incompatível(is) com o perfil geométrico");
        }

        EstabilidadeDto estabilidade = recalcularEstabilidade(plan);
        planRepositorio.save(plan);
        return toDto(plan, estabilidade);
    }

    @Transactional(readOnly = true)
    public EstivagemPlanDto buscarPorId(Long planId) {
        EstivagemPlan plan = buscarPlanOperacional(planId);
        return toDto(plan, estabilidadeServico.calcular(plan));
    }

    @Transactional(readOnly = true)
    public EstabilidadeDto calcularEstabilidade(Long planId) {
        EstivagemPlan plan = buscarPlanOperacional(planId);
        validarFonteCanonica(plan);
        return estabilidadeServico.calcular(plan);
    }

    @Transactional(readOnly = true)
    public RestowAnaliseDto analisarRestow(Long planId) {
        EstivagemPlan plan = buscarPlanOperacional(planId);
        validarFonteCanonica(plan);
        return restowServico.analisar(plan);
    }

    @Transactional(readOnly = true)
    public SequenciamentoGuindasteDto sequenciarGuindastes(Long planId, int numGuindastes) {
        EstivagemPlan plan = buscarPlanOperacional(planId);
        validarFonteCanonica(plan);
        return sequenciamentoServico.sequenciar(plan, numGuindastes);
    }

    @Transactional
    public EstivagemPlanDto validarEAprovar(Long planId) {
        EstivagemPlan plan = buscarPlanOperacional(planId);
        validarFonteCanonica(plan);
        if (plan.getStatus() == StatusEstivagemPlan.APROVADO) {
            return toDto(plan, estabilidadeServico.calcular(plan));
        }
        if (plan.getStatus() == StatusEstivagemPlan.TRANSMITIDO) {
            throw new IllegalStateException("Plano transmitido não pode ser aprovado novamente");
        }
        geometriaServico.validarPlanoParaAprovacao(plan);
        EstabilidadeDto estabilidade = estabilidadeServico.calcular(plan);
        if (!estabilidade.isAprovado()) {
            throw new IllegalStateException("Plano possui violações de Hard Constraint e não pode ser aprovado");
        }
        plan.setStatus(StatusEstivagemPlan.APROVADO);
        planRepositorio.save(plan);
        return toDto(plan, estabilidade);
    }

    private EstivagemPlan buscarPlanOperacional(Long planId) {
        EstivagemPlan plan = planRepositorio.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("EstivagemPlan não encontrado: " + planId));
        geometriaServico.validarPlanoOperacional(plan);
        return plan;
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

    private void exigirPlanoEditavel(EstivagemPlan plan) {
        if (plan.getStatus() == StatusEstivagemPlan.APROVADO
                || plan.getStatus() == StatusEstivagemPlan.TRANSMITIDO) {
            throw new IllegalStateException("Plano aprovado ou transmitido não pode ser alterado por este comando");
        }
    }

    private EstabilidadeDto recalcularEstabilidade(EstivagemPlan plan) {
        geometriaServico.validarPlanoOperacional(plan);
        EstabilidadeDto estabilidade = estabilidadeServico.calcular(plan);
        plan.setTrimCalculado(estabilidade.getTrimMetros());
        plan.setListCalculado(estabilidade.getListGraus());
        plan.setLcgCalculado(estabilidade.getLcgMetros());
        plan.setTcgCalculado(estabilidade.getTcgMetros());
        return estabilidade;
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
        dto.setPerfilGeometriaId(plan.getPerfilGeometriaId());
        dto.setPerfilGeometriaVersao(plan.getPerfilGeometriaVersao());
        dto.setCondicaoCarregamento(plan.getCondicaoCarregamento());
        dto.setStatus(plan.getStatus() != null ? plan.getStatus().name() : null);
        dto.setEstabilidade(estabilidade);
        List<SlotNavioDto> slots = plan.getSlots().stream()
                .map(this::toSlotDto)
                .collect(Collectors.toList());
        dto.setSlots(slots);
        dto.setTotalSlotsOcupados((int) slots.stream()
                .filter(item -> item.getCodigoContainer() != null)
                .count());
        dto.setTotalContainers(dto.getTotalSlotsOcupados());
        return dto;
    }

    private SlotNavioDto toSlotDto(SlotNavio slot) {
        SlotNavioDto dto = new SlotNavioDto();
        dto.setId(slot.getId());
        dto.setBay(slot.getBay());
        dto.setRowBay(slot.getRowBay());
        dto.setTier(slot.getTier());
        dto.setTipoSlot(slot.getTipoSlot() != null ? slot.getTipoSlot().name() : null);
        dto.setCodigoHatchCover(slot.getCodigoHatchCover());
        dto.setSobreHatchCover(slot.isSobreHatchCover());
        dto.setRestrito(slot.isRestrito());
        dto.setMotivoRestricao(slot.getMotivoRestricao());
        dto.setTomadaReefer(slot.isTomadaReefer());
        dto.setAceita20Pes(slot.isAceita20Pes());
        dto.setAceita40Pes(slot.isAceita40Pes());
        dto.setAceita45Pes(slot.isAceita45Pes());
        dto.setMaxPesoKg(slot.getMaxPesoKg());
        dto.setMaxPesoPilhaKg(slot.getMaxPesoPilhaKg());
        dto.setCodigoContainer(slot.getCodigoContainer());
        dto.setIsoCode(slot.getIsoCode());
        dto.setPesoKg(slot.getPesoKg());
        dto.setPesoVgmKg(slot.getPesoVgmKg());
        dto.setEstadoCarga(slot.getEstadoCarga());
        dto.setPortoCarga(slot.getPortoCarga());
        dto.setPortoDescarga(slot.getPortoDescarga());
        dto.setClasseImo(slot.getClasseImo());
        dto.setNumeroOnu(slot.getNumeroOnu());
        dto.setGrupoSegregacao(slot.getGrupoSegregacao());
        dto.setPerigoso(slot.isPerigoso());
        dto.setReefer(slot.isReefer());
        dto.setTemperaturaRequeridaC(slot.getTemperaturaRequeridaC());
        dto.setTemperaturaMinimaC(slot.getTemperaturaMinimaC());
        dto.setTemperaturaMaximaC(slot.getTemperaturaMaximaC());
        dto.setOog(slot.isOog());
        dto.setExcessoFrontalCm(slot.getExcessoFrontalCm());
        dto.setExcessoTraseiroCm(slot.getExcessoTraseiroCm());
        dto.setExcessoEsquerdoCm(slot.getExcessoEsquerdoCm());
        dto.setExcessoDireitoCm(slot.getExcessoDireitoCm());
        dto.setExcessoAlturaCm(slot.getExcessoAlturaCm());
        dto.setStatusAlertas(slot.getStatusAlertas());
        return dto;
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
}
