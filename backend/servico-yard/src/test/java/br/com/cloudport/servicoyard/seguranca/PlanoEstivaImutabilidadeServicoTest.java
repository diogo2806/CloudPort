package br.com.cloudport.servicoyard.seguranca;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.edi.repositorio.BayPlanRepositorio;
import br.com.cloudport.servicoyard.estivagembulk.modelo.BobinaManifesto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.StatusPlanoEstiva;
import br.com.cloudport.servicoyard.estivagembulk.repositorio.NavioGranelRepositorio;
import br.com.cloudport.servicoyard.estivagembulk.repositorio.PlanoEstivaBulkRepositorio;
import br.com.cloudport.servicoyard.estivagembulk.servico.EmpilhamentoBobinaServico;
import br.com.cloudport.servicoyard.estivagembulk.servico.EstabilidadeEstruturalServico;
import br.com.cloudport.servicoyard.estivagembulk.servico.PlanoEstivaBulkServico;
import br.com.cloudport.servicoyard.estivagembulk.servico.TacktopServico;
import br.com.cloudport.servicoyard.estivagembulk.servico.TanktopCalculadorServico;
import br.com.cloudport.servicoyard.vesselplanner.dto.AlocacaoSlotRequisicaoDto;
import br.com.cloudport.servicoyard.vesselplanner.mensagem.VesselPlannerEventoPublicador;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.modelo.StatusEstivagemPlan;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EstivagemPlanRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.SlotNavioRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.servico.AutoStowageServico;
import br.com.cloudport.servicoyard.vesselplanner.servico.EstabilidadeNavioServico;
import br.com.cloudport.servicoyard.vesselplanner.servico.RestowCalculadorServico;
import br.com.cloudport.servicoyard.vesselplanner.servico.SequenciamentoGuindasteServico;
import br.com.cloudport.servicoyard.vesselplanner.servico.VesselPlannerServico;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanoEstivaImutabilidadeServicoTest {

    @Mock
    private EstivagemPlanRepositorio estivagemPlanRepositorio;

    @Mock
    private SlotNavioRepositorio slotNavioRepositorio;

    @Mock
    private BayPlanRepositorio bayPlanRepositorio;

    @Mock
    private EstabilidadeNavioServico estabilidadeNavioServico;

    @Mock
    private RestowCalculadorServico restowCalculadorServico;

    @Mock
    private SequenciamentoGuindasteServico sequenciamentoGuindasteServico;

    @Mock
    private AutoStowageServico autoStowageServico;

    @Mock
    private VesselPlannerEventoPublicador vesselPlannerEventoPublicador;

    @Mock
    private NavioGranelRepositorio navioGranelRepositorio;

    @Mock
    private PlanoEstivaBulkRepositorio planoEstivaBulkRepositorio;

    @Mock
    private TanktopCalculadorServico tanktopCalculadorServico;

    @Mock
    private EstabilidadeEstruturalServico estabilidadeEstruturalServico;

    @Mock
    private EmpilhamentoBobinaServico empilhamentoBobinaServico;

    @Mock
    private TacktopServico tacktopServico;

    @Test
    void naoDeveRealocarContainerEmPlanoAprovado() {
        EstivagemPlan plano = new EstivagemPlan();
        plano.setStatus(StatusEstivagemPlan.APROVADO);
        when(estivagemPlanRepositorio.findById(1L)).thenReturn(Optional.of(plano));
        VesselPlannerServico servico = new VesselPlannerServico(
                estivagemPlanRepositorio,
                slotNavioRepositorio,
                bayPlanRepositorio,
                estabilidadeNavioServico,
                restowCalculadorServico,
                sequenciamentoGuindasteServico,
                autoStowageServico,
                vesselPlannerEventoPublicador);

        assertThatThrownBy(() -> servico.alocarContainer(1L, new AlocacaoSlotRequisicaoDto()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não pode ser alterado");

        verifyNoInteractions(slotNavioRepositorio);
        verify(estivagemPlanRepositorio, never()).save(any(EstivagemPlan.class));
    }

    @Test
    void naoDeveAdicionarBobinaEmPlanoBulkAprovado() {
        PlanoEstivaBulk plano = new PlanoEstivaBulk();
        plano.setStatus(StatusPlanoEstiva.APROVADO);
        when(planoEstivaBulkRepositorio.findById(1L)).thenReturn(Optional.of(plano));
        PlanoEstivaBulkServico servico = new PlanoEstivaBulkServico(
                navioGranelRepositorio,
                planoEstivaBulkRepositorio,
                tanktopCalculadorServico,
                estabilidadeEstruturalServico,
                empilhamentoBobinaServico,
                tacktopServico);

        assertThatThrownBy(() -> servico.adicionarBobina(1L, new BobinaManifesto()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não pode ser alterado");

        verify(planoEstivaBulkRepositorio, never()).save(any(PlanoEstivaBulk.class));
    }
}
