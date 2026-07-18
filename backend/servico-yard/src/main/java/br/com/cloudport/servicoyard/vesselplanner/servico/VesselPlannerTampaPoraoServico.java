package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.edi.repositorio.BayPlanRepositorio;
import br.com.cloudport.servicoyard.integracao.navio.IdentidadePlanejamentoNavioServico;
import br.com.cloudport.servicoyard.vesselplanner.dto.AlocacaoSlotRequisicaoDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.AlocacaoSlotRespostaDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.EstivagemPlanDto;
import br.com.cloudport.servicoyard.vesselplanner.dto.SequenciamentoGuindasteDto;
import br.com.cloudport.servicoyard.vesselplanner.mensagem.VesselPlannerEventoPublicador;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.SlotNavio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.SlotNavioRepositorio;
import javax.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Primary
@Service
public class VesselPlannerTampaPoraoServico extends VesselPlannerServico {

    private final EstivagemPlanRepositorio planRepositorioLocal;
    private final SlotNavioRepositorio slotRepositorioLocal;
    private final TampaPoraoServico tampaPoraoServico;

    public VesselPlannerTampaPoraoServico(
            EstivagemPlanRepositorio planRepositorio,
            SlotNavioRepositorio slotRepositorio,
            BayPlanRepositorio bayPlanRepositorio,
            EstabilidadeNavioServico estabilidadeServico,
            RestowCalculadorServico restowServico,
            SequenciamentoGuindasteServico sequenciamentoServico,
            AutoStowageServico autoStowageServico,
            GeometriaNavioServico geometriaServico,
            VesselPlannerEventoPublicador publicador,
            IdentidadePlanejamentoNavioServico identidadeServico,
            TampaPoraoServico tampaPoraoServico) {
        super(
                planRepositorio,
                slotRepositorio,
                bayPlanRepositorio,
                estabilidadeServico,
                restowServico,
                sequenciamentoServico,
                autoStowageServico,
                geometriaServico,
                publicador,
                identidadeServico);
        this.planRepositorioLocal = planRepositorio;
        this.slotRepositorioLocal = slotRepositorio;
        this.tampaPoraoServico = tampaPoraoServico;
    }

    @Override
    @Transactional
    public EstivagemPlanDto criarPlanoDeBayPlan(Long bayPlanId, Long visitaNavioId) {
        EstivagemPlanDto plano = super.criarPlanoDeBayPlan(bayPlanId, visitaNavioId);
        EstivagemPlan entidade = planRepositorioLocal.findById(plano.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "EstivagemPlan não encontrado após criação: " + plano.getId()));
        tampaPoraoServico.inicializarDoPlano(entidade);
        return plano;
    }

    @Override
    @Transactional
    public AlocacaoSlotRespostaDto alocarContainer(
            Long planId,
            AlocacaoSlotRequisicaoDto requisicao) {
        EstivagemPlan plan = planRepositorioLocal.findLockedById(planId)
                .orElseThrow(() -> new EntityNotFoundException("EstivagemPlan não encontrado: " + planId));
        SlotNavio destino = slotRepositorioLocal.findById(requisicao.getSlotDestinoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Slot não encontrado: " + requisicao.getSlotDestinoId()));
        if (!destino.getEstivagem().getId().equals(planId)) {
            throw new IllegalArgumentException("O slot de destino não pertence ao plano informado.");
        }
        tampaPoraoServico.validarInicioMovimento(plan, destino, requisicao.getCodigoContainer());
        return super.alocarContainer(planId, requisicao);
    }

    @Override
    @Transactional(readOnly = true)
    public SequenciamentoGuindasteDto sequenciarGuindastes(Long planId, int numGuindastes) {
        SequenciamentoGuindasteDto sequenciamento = super.sequenciarGuindastes(planId, numGuindastes);
        EstivagemPlan plan = planRepositorioLocal.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("EstivagemPlan não encontrado: " + planId));
        tampaPoraoServico.enriquecerSequenciamento(plan, sequenciamento);
        return sequenciamento;
    }
}
