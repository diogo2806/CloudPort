package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.OrdemPatioYardRespostaDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.WorkQueuePatioYardDTO;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.OrdemPatioDaVisitaDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.WorkQueuePatioDaVisitaDTO;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ConversorWorkQueuePatioServico {

    public WorkQueuePatioDaVisitaDTO converter(WorkQueuePatioYardDTO fila) {
        List<OrdemPatioDaVisitaDTO> jobList = Optional.ofNullable(fila.getJobList())
                .orElse(List.of()).stream()
                .map(this::converterOrdem)
                .toList();
        return new WorkQueuePatioDaVisitaDTO(
                fila.getId(),
                fila.getIdentificador(),
                fila.getAgrupamento(),
                fila.getVisitaNavioId(),
                fila.getBerco(),
                fila.getPorao(),
                fila.getBlocoZona(),
                fila.getSequenciaInicial(),
                fila.getPow(),
                fila.getPoolOperacional(),
                fila.getEquipamento(),
                fila.getStatus(),
                fila.getPrioridadeOperacional(),
                fila.getTotalOrdens(),
                jobList,
                fila.getCriadoEm(),
                fila.getAtualizadoEm()
        );
    }

    public OrdemPatioDaVisitaDTO converterOrdem(OrdemPatioYardRespostaDTO ordem) {
        TipoMovimentoNavio tipoMovimento = tipoMovimentoNavio(ordem.getTipoMovimento());
        return new OrdemPatioDaVisitaDTO(
                ordem.getId(),
                ordem.getVisitaNavioId(),
                ordem.getItemOperacaoNavioId(),
                ordem.getCodigoConteiner(),
                tipoMovimento,
                ordem.getStatusOrdem(),
                tipoMovimento == TipoMovimentoNavio.DESCARGA ? "NAVIO" : ordem.getDestino(),
                tipoMovimento == TipoMovimentoNavio.EMBARQUE ? "NAVIO" : ordem.getDestino(),
                ordem.posicaoDestinoFormatada(),
                null,
                ordem.getSequenciaNavio(),
                ordem.getPrioridadeOperacional()
        );
    }

    private TipoMovimentoNavio tipoMovimentoNavio(String tipoMovimentoPatio) {
        if (!StringUtils.hasText(tipoMovimentoPatio)) {
            return TipoMovimentoNavio.DESCARGA;
        }
        return switch (tipoMovimentoPatio.toUpperCase(Locale.ROOT)) {
            case "TRANSFERENCIA" -> TipoMovimentoNavio.EMBARQUE;
            case "REMANEJAMENTO" -> TipoMovimentoNavio.RESTOW;
            default -> TipoMovimentoNavio.DESCARGA;
        };
    }
}
