package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.contracts.api.ComandoMotivado;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardCliente.OrdemPatioYardRespostaDTO;
import br.com.cloudport.serviconaviosiderurgico.cliente.OrdemPatioYardComandoCliente;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandoPrioridadeOrdemPatioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.OrdemPatioDaVisitaDTO;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OperacaoOrdemPatioNavioServico {

    private final VisitaNavioServico visitaServico;
    private final SincronizadorStatusNavioPatioServico sincronizador;
    private final OrdemPatioYardComandoCliente comandoCliente;

    public OperacaoOrdemPatioNavioServico(
            VisitaNavioServico visitaServico,
            SincronizadorStatusNavioPatioServico sincronizador,
            OrdemPatioYardComandoCliente comandoCliente
    ) {
        this.visitaServico = visitaServico;
        this.sincronizador = sincronizador;
        this.comandoCliente = comandoCliente;
    }

    @Transactional
    public OrdemPatioDaVisitaDTO atualizarPrioridade(Long visitaId, Long ordemId, ComandoPrioridadeOrdemPatioDTO comando) {
        var visita = visitaServico.buscarEntidade(visitaId);
        visitaServico.validarVisitaEditavel(visita);
        OrdemPatioYardRespostaDTO resposta = comandoCliente.atualizarPrioridade(ordemId, comando);
        validarResposta(visitaId, ordemId, resposta);
        visitaServico.registrarEvento(
                visita,
                null,
                "PRIORIDADE_ORDEM_PATIO_ATUALIZADA",
                "Prioridade da ordem de patio " + ordemId + " atualizada para "
                        + comando.prioridadeOperacional() + ". Motivo: " + comando.motivo().trim(),
                comando.usuarioEfetivo(),
                null,
                String.valueOf(comando.prioridadeOperacional())
        );
        return converter(resposta);
    }

    @Transactional
    public OrdemPatioDaVisitaDTO suspender(Long visitaId, Long ordemId, ComandoMotivado comando) {
        var visita = visitaServico.buscarEntidade(visitaId);
        visitaServico.validarVisitaEditavel(visita);
        OrdemPatioYardRespostaDTO resposta = comandoCliente.suspender(ordemId, comando);
        validarResposta(visitaId, ordemId, resposta);
        sincronizador.sincronizarStatus(visitaId);
        visitaServico.registrarEvento(
                visita,
                null,
                "ORDEM_PATIO_SUSPENSA",
                "Ordem de patio " + ordemId + " suspensa. Motivo: " + comando.motivoNormalizado(),
                comando.usuarioEfetivo("sistema"),
                null,
                resposta.getStatusOrdem()
        );
        return converter(resposta);
    }

    @Transactional
    public OrdemPatioDaVisitaDTO retomar(Long visitaId, Long ordemId, ComandoMotivado comando) {
        var visita = visitaServico.buscarEntidade(visitaId);
        visitaServico.validarVisitaEditavel(visita);
        OrdemPatioYardRespostaDTO resposta = comandoCliente.retomar(ordemId, comando);
        validarResposta(visitaId, ordemId, resposta);
        sincronizador.sincronizarStatus(visitaId);
        visitaServico.registrarEvento(
                visita,
                null,
                "ORDEM_PATIO_RETOMADA",
                "Ordem de patio " + ordemId + " retomada. Motivo: " + comando.motivoNormalizado(),
                comando.usuarioEfetivo("sistema"),
                null,
                resposta.getStatusOrdem()
        );
        return converter(resposta);
    }

    private void validarResposta(Long visitaId, Long ordemId, OrdemPatioYardRespostaDTO resposta) {
        if (resposta == null || resposta.getId() == null) {
            throw new IllegalStateException("Servico-yard nao retornou a ordem de patio atualizada.");
        }
        if (!Objects.equals(ordemId, resposta.getId())) {
            throw new IllegalStateException("Servico-yard retornou uma ordem diferente da solicitada.");
        }
        if (resposta.getVisitaNavioId() != null && !Objects.equals(visitaId, resposta.getVisitaNavioId())) {
            throw new IllegalArgumentException("Ordem de patio nao pertence a visita informada.");
        }
    }

    private OrdemPatioDaVisitaDTO converter(OrdemPatioYardRespostaDTO ordem) {
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
