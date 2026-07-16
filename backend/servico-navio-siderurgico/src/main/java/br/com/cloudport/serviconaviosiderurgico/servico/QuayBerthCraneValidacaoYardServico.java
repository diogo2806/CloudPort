package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.ConsultaWorkQueueYardPorta;
import br.com.cloudport.serviconaviosiderurgico.cliente.WorkQueueValidacaoYardDto;
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
public class QuayBerthCraneValidacaoYardServico extends QuayBerthCraneServico {

    private final ConsultaWorkQueueYardPorta consultaWorkQueueYard;

    public QuayBerthCraneValidacaoYardServico(
            VisitaNavioServico visitaNavioServico,
            ItemOperacaoNavioRepositorio itemRepositorio,
            PlanoGuindasteVisitaRepositorio planoRepositorio,
            ConsultaWorkQueueYardPorta consultaWorkQueueYard
    ) {
        super(visitaNavioServico, itemRepositorio, planoRepositorio);
        this.consultaWorkQueueYard = consultaWorkQueueYard;
    }

    @Override
    @Transactional
    public PlanoGuindasteDTO salvarPlano(Long visitaId, ComandoPlanoGuindasteDTO comando) {
        validarComandoCompleto(visitaId, comando);
        return super.salvarPlano(visitaId, comando);
    }

    private void validarComandoCompleto(Long visitaId, ComandoPlanoGuindasteDTO comando) {
        if (comando == null || comando.guindastes() == null || comando.guindastes().isEmpty()) return;

        String bercoPlano = normalizar(comando.berco());
        List<WorkQueueValidacaoYardDto> filas = consultaWorkQueueYard.listarParaValidacaoPlano(visitaId);
        Map<Long, WorkQueueValidacaoYardDto> porId = new HashMap<>();
        for (WorkQueueValidacaoYardDto fila : filas) {
            if (fila != null && fila.getId() != null) porId.put(fila.getId(), fila);
        }
        Set<Long> utilizadas = new HashSet<>();

        for (AlocacaoGuindasteDTO alocacao : comando.guindastes()) {
            if (alocacao == null) continue;
            Long workQueueId = alocacao.workQueueId();
            if (workQueueId == null) {
                throw new IllegalArgumentException("Cada alocacao deve informar uma work queue real do Yard.");
            }
            if (!utilizadas.add(workQueueId)) {
                throw new IllegalArgumentException(
                        "A work queue " + workQueueId + " foi associada mais de uma vez no mesmo plano.");
            }
            WorkQueueValidacaoYardDto fila = porId.get(workQueueId);
            if (fila == null) {
                throw new IllegalArgumentException(
                        "Work queue " + workQueueId + " nao existe ou nao pertence a visita " + visitaId + ".");
            }
            validarFila(visitaId, bercoPlano, alocacao, fila);
        }
    }

    private void validarFila(
            Long visitaId,
            String bercoPlano,
            AlocacaoGuindasteDTO alocacao,
            WorkQueueValidacaoYardDto fila
    ) {
        if (!Objects.equals(visitaId, fila.getVisitaNavioId())) {
            throw new IllegalArgumentException("Work queue " + fila.getId() + " pertence a outra visita de navio.");
        }
        if (!StringUtils.hasText(fila.getBerco()) || !normalizar(fila.getBerco()).equals(bercoPlano)) {
            throw new IllegalArgumentException(
                    "Work queue " + fila.getId() + " nao e compativel com o berco " + bercoPlano + ".");
        }
        if (!Objects.equals(alocacao.porao(), fila.getPorao())) {
            throw new IllegalArgumentException(
                    "Work queue " + fila.getId() + " nao e compativel com o porao " + alocacao.porao() + ".");
        }
        if (!"ATIVA".equals(fila.getStatus())) {
            throw new IllegalArgumentException("Work queue " + fila.getId() + " nao esta ativa.");
        }
        if (!fila.isCoberturaValida()) {
            throw new IllegalArgumentException(
                    "Work queue " + fila.getId()
                            + " nao possui cobertura completa de POW, pool, CHE, recurso de cais e job list.");
        }
        if (fila.getEquipamentoPatioId() == null
                || !StringUtils.hasText(fila.getEquipamentoIdentificador())
                || !"OPERACIONAL".equals(fila.getEquipamentoStatus())) {
            throw new IllegalArgumentException("Work queue " + fila.getId() + " nao possui CHE operacional valido.");
        }
        if (fila.getRecursoCaisId() == null) {
            throw new IllegalArgumentException("Work queue " + fila.getId() + " nao possui recurso de cais real.");
        }
        if (fila.getTotalOrdensDispatchaveis() <= 0) {
            throw new IllegalArgumentException("Work queue " + fila.getId() + " nao possui work instructions elegiveis.");
        }
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : "";
    }
}
