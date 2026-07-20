package br.com.cloudport.servicoyard.scheduler.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.scheduler.modelo.EstadoPlanoPosicaoOperacional;
import br.com.cloudport.servicoyard.scheduler.modelo.HistoricoPlanoPosicaoOperacional;
import br.com.cloudport.servicoyard.scheduler.modelo.PlanoPosicaoOperacional;
import br.com.cloudport.servicoyard.scheduler.repositorio.HistoricoPlanoPosicaoOperacionalRepositorio;
import br.com.cloudport.servicoyard.scheduler.repositorio.PlanoPosicaoOperacionalRepositorio;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PlanoPosicaoOperacionalServicoTest {

    @Mock
    private PlanoPosicaoOperacionalRepositorio repositorio;
    @Mock
    private HistoricoPlanoPosicaoOperacionalRepositorio historicoRepositorio;

    private PlanoPosicaoOperacionalServico servico;

    @BeforeEach
    void configurar() {
        servico = new PlanoPosicaoOperacionalServico(repositorio, historicoRepositorio);
        when(repositorio.saveAndFlush(any(PlanoPosicaoOperacional.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveConverterPlanoTentativoValidoDuranteDispatch() {
        PlanoPosicaoOperacional plano = plano(EstadoPlanoPosicaoOperacional.TENTATIVO, LocalDateTime.now().plusHours(1));
        OrdemTrabalhoPatio ordem = ordem();
        when(repositorio.findFirstByCodigoContainerIgnoreCaseAndEstadoInOrderByAtualizadoEmDesc(
                eq("MSCU1000001"), anyCollection())).thenReturn(Optional.of(plano));

        servico.revalidarParaDispatch(ordem, "planejador");

        assertEquals(EstadoPlanoPosicaoOperacional.DEFINITIVO, plano.getEstado());
        assertEquals(50L, plano.getOrdemTrabalhoPatioId());
        verify(historicoRepositorio).save(any(HistoricoPlanoPosicaoOperacional.class));
        verify(repositorio).save(plano);
    }

    @Test
    void deveBloquearDispatchQuandoPlanoExpirou() {
        PlanoPosicaoOperacional plano = plano(EstadoPlanoPosicaoOperacional.TENTATIVO, LocalDateTime.now().minusMinutes(1));
        when(repositorio.findFirstByCodigoContainerIgnoreCaseAndEstadoInOrderByAtualizadoEmDesc(
                eq("MSCU1000001"), anyCollection())).thenReturn(Optional.of(plano));

        assertThrows(ResponseStatusException.class, () -> servico.revalidarParaDispatch(ordem(), "planejador"));
    }

    @Test
    void deveBloquearDispatchQuandoDestinoDiverge() {
        PlanoPosicaoOperacional plano = plano(EstadoPlanoPosicaoOperacional.DEFINITIVO, LocalDateTime.now().plusHours(1));
        OrdemTrabalhoPatio ordem = ordem();
        ordem.setColunaDestino(99);
        when(repositorio.findFirstByCodigoContainerIgnoreCaseAndEstadoInOrderByAtualizadoEmDesc(
                eq("MSCU1000001"), anyCollection())).thenReturn(Optional.of(plano));

        assertThrows(ResponseStatusException.class, () -> servico.revalidarParaDispatch(ordem, "planejador"));
    }

    private PlanoPosicaoOperacional plano(
            EstadoPlanoPosicaoOperacional estado,
            LocalDateTime validade) {
        PlanoPosicaoOperacional plano = new PlanoPosicaoOperacional();
        plano.setId(10L);
        plano.setCodigoContainer("MSCU1000001");
        plano.setBloco("A01");
        plano.setLinha(1);
        plano.setColuna(2);
        plano.setCamada("3");
        plano.setEstado(estado);
        plano.setHorizonteInicio(LocalDateTime.now());
        plano.setHorizonteFim(LocalDateTime.now().plusHours(6));
        plano.setValidoAte(validade);
        plano.setOrigem("SCHEDULER_PREDITIVO");
        plano.setMotivo("Plano de teste");
        plano.setAssinaturaEntrada("assinatura");
        plano.setAlteradoPor("teste");
        return plano;
    }

    private OrdemTrabalhoPatio ordem() {
        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio();
        ordem.setId(50L);
        ordem.setCodigoConteiner("MSCU1000001");
        ordem.setLinhaDestino(1);
        ordem.setColunaDestino(2);
        ordem.setCamadaDestino("3");
        return ordem;
    }
}
