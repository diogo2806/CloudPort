package br.com.cloudport.servicoyard.scheduler.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.DispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.servico.AvisoEstivagemPatioServico;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class ValidacaoPlanejamentoDispatchServico {

    private static final Set<StatusOrdemTrabalhoPatio> ESTADOS_DISPATCHAVEIS = Set.of(
            StatusOrdemTrabalhoPatio.PENDENTE,
            StatusOrdemTrabalhoPatio.SUSPENSA);

    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final PlanoPosicaoOperacionalServico planoServico;
    private final AvisoEstivagemPatioServico avisoEstivagemServico;

    public ValidacaoPlanejamentoDispatchServico(
            OrdemTrabalhoPatioRepositorio ordemRepositorio,
            PlanoPosicaoOperacionalServico planoServico,
            AvisoEstivagemPatioServico avisoEstivagemServico) {
        this.ordemRepositorio = ordemRepositorio;
        this.planoServico = planoServico;
        this.avisoEstivagemServico = avisoEstivagemServico;
    }

    @Transactional
    public void revalidar(Long workQueueId, DispatchWorkQueueDto dto) {
        DispatchWorkQueueDto comando = dto == null ? new DispatchWorkQueueDto() : dto;
        Set<Long> idsSelecionados = CollectionUtils.isEmpty(comando.getOrdemIds())
                ? Set.of()
                : comando.getOrdemIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        int limite = comando.limiteOrdensEfetivo();
        int validadas = 0;

        List<OrdemTrabalhoPatio> ordens = ordemRepositorio
                .findByWorkQueueIdOrderByPrioridadeBuscaDescPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(
                        workQueueId);
        for (OrdemTrabalhoPatio ordem : ordens) {
            if (validadas >= limite) {
                break;
            }
            if (!idsSelecionados.isEmpty() && !idsSelecionados.contains(ordem.getId())) {
                continue;
            }
            if (comando.somentePendentesEfetivo()
                    && ordem.getStatusOrdem() != StatusOrdemTrabalhoPatio.PENDENTE) {
                continue;
            }
            if (!ESTADOS_DISPATCHAVEIS.contains(ordem.getStatusOrdem())) {
                continue;
            }
            avisoEstivagemServico.validarOperacaoSemAvisoCritico(
                    ordem.getCodigoConteiner(), ordem.getDestino());
            planoServico.revalidarParaDispatch(ordem, comando.usuarioEfetivo());
            validadas++;
        }
    }
}
