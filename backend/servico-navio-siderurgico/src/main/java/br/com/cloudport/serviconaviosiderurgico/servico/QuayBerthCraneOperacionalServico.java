package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.ConsultaWorkQueueYardCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.ConsultaWorkQueueYardCliente.WorkQueueValidacaoYardDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.AlocacaoGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoPlanoGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.PlanoGuindasteVisitaRepositorio;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Primary
public class QuayBerthCraneOperacionalServico extends QuayBerthCraneServico {

    private final ConsultaWorkQueueYardCliente consultaWorkQueueYardCliente;

    public QuayBerthCraneOperacionalServico(
            VisitaNavioServico visitaNavioServico,
            ItemOperacaoNavioRepositorio itemRepositorio,
            PlanoGuindasteVisitaRepositorio planoRepositorio,
            ConsultaWorkQueueYardCliente consultaWorkQueueYardCliente
    ) {
        super(visitaNavioServico, itemRepositorio, planoRepositorio);
        this.consultaWorkQueueYardCliente = consultaWorkQueueYardCliente;
    }

    @Override
    @Transactional
    public PlanoGuindasteDTO salvarPlano(Long visitaId, ComandoPlanoGuindasteDTO comando) {
        validarWorkQueuesReais(visitaId, comando);
        return super.salvarPlano(visitaId, comando);
    }

    private void validarWorkQueuesReais(Long visitaId, ComandoPlanoGuindasteDTO comando) {
        if (comando == null || comando.guindastes() == null || comando.guindastes().isEmpty()) return;
        String bercoPlano = normalizar(comando.berco());
        List<WorkQueueValidacaoYardDTO> filas = consultaWorkQueueYardCliente.listarParaValidacaoPlano(visitaId);
        Map<Long, WorkQueueValidacaoYardDTO> porId = new HashMap<>();
        for (WorkQueueValidacaoYardDTO fila : filas) {
            if (fila != null && fila.getId() != null) porId.put(fila.getId(), fila);
        }
        Set<Long> utilizadas = new HashSet<>();

        for (AlocacaoGuindasteDTO alocacao : comando.guindastes()) {
            if (alocacao == null) continue;
            Long workQueueId = alocacao.workQueueId();
            if (workQueueId == null) {
                throw new IllegalArgumentException("Cada alocacao do plano deve informar uma work queue real do Yard.");
            }
            if (!utilizadas.add(workQueueId)) {
                throw new IllegalArgumentException("A work queue " + workQueueId + " foi associada mais de uma vez no mesmo plano.");
            }
            WorkQueueValidacaoYardDTO fila = porId.get(workQueueId);
            if (fila == null) {
                throw new IllegalArgumentException("Work queue " + workQueueId
                        + " nao existe ou nao pertence a visita " + visitaId + ".");
            }
            validarFila(visitaId, bercoPlano, alocacao, fila);
        }
    }

    private void validarFila(
            Long visitaId,
            String bercoPlano,
            AlocacaoGuindasteDTO alocacao,
            WorkQueueValidacaoYardDTO fila
    ) {
        if (!Objects.equals(visitaId, fila.getVisitaNavioId())) {
            throw new IllegalArgumentException("Work queue " + fila.getId() + " pertence a outra visita de navio.");
        }
        if (!StringUtils.hasText(fila.getBerco()) || !normalizar(fila.getBerco()).equals(bercoPlano)) {
            throw new IllegalArgumentException("Work queue " + fila.getId()
                    + " nao e compativel com o berco " + bercoPlano + ".");
        }
        if (!Objects.equals(alocacao.porao(), fila.getPorao())) {
            throw new IllegalArgumentException("Work queue " + fila.getId()
                    + " nao e compativel com o porao " + alocacao.porao() + ".");
        }
        if (!"ATIVA".equals(fila.getStatus())) {
            throw new IllegalArgumentException("Work queue " + fila.getId()
                    + " nao esta ativa para o plano de guindastes.");
        }
        if (!fila.isCoberturaValida()) {
            throw new IllegalArgumentException("Work queue " + fila.getId()
                    + " nao possui cobertura operacional completa de POW, pool, CHE, recurso de cais e job list.");
        }
        if (fila.getEquipamentoPatioId() == null
                || !StringUtils.hasText(fila.getEquipamentoIdentificador())
                || !"OPERACIONAL".equals(fila.getEquipamentoStatus())) {
            throw new IllegalArgumentException("Work queue " + fila.getId()
                    + " nao possui CHE operacional valido.");
        }
        if (fila.getRecursoCaisId() == null) {
            throw new IllegalArgumentException("Work queue " + fila.getId()
                    + " nao possui recurso de cais real associado.");
        }
        if (fila.getTotalOrdensDispatchaveis() <= 0) {
            throw new IllegalArgumentException("Work queue " + fila.getId()
                    + " nao possui work instructions elegiveis.");
        }
    }

    private String normalizar(String valor) {
        if (!StringUtils.hasText(valor)) return "";
        return valor.trim().toUpperCase(Locale.ROOT);
    }
}
