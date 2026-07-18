package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dominio.LoteCarga.HistoricoCustodiaFerroviaria;
import br.com.cloudport.servicocargageral.dominio.StatusOrdemFerroviariaCarga;
import br.com.cloudport.servicocargageral.dto.FerroviaCargaGeralDTOs.AtualizarStatusOrdemFerroviariaRequest;
import br.com.cloudport.servicocargageral.dto.FerroviaCargaGeralDTOs.HistoricoCustodiaResposta;
import br.com.cloudport.servicocargageral.dto.FerroviaCargaGeralDTOs.OrdemFerroviariaCargaResposta;
import br.com.cloudport.servicocargageral.dto.FerroviaCargaGeralDTOs.PlanejarOrdemFerroviariaRequest;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrdemMovimentacaoFerroviariaCargaGeralServico {

    private final LoteCargaRepositorio loteCargaRepositorio;

    public OrdemMovimentacaoFerroviariaCargaGeralServico(LoteCargaRepositorio loteCargaRepositorio) {
        this.loteCargaRepositorio = loteCargaRepositorio;
    }

    @Transactional
    public OrdemFerroviariaCargaResposta planejar(
            String visitaTremId,
            PlanejarOrdemFerroviariaRequest request) {
        validarVisita(visitaTremId);
        LoteCarga lote = loteCargaRepositorio.findComBloqueioById(request.loteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo lot não encontrado."));
        executarRegraDominio(() -> lote.planejarFerrovia(
                visitaTremId,
                request.vagaoId(),
                request.posicao(),
                request.sequencia(),
                request.capacidadePesoKg(),
                request.incompatibilidades(),
                request.custodia(),
                request.responsavel()));
        return mapear(loteCargaRepositorio.save(lote));
    }

    @Transactional(readOnly = true)
    public List<OrdemFerroviariaCargaResposta> listar(
            String visitaTremId,
            StatusOrdemFerroviariaCarga status) {
        validarVisita(visitaTremId);
        List<LoteCarga> lotes = status == null
                ? loteCargaRepositorio.findByVisitaTremIdOrderBySequenciaFerroviariaAsc(visitaTremId)
                : loteCargaRepositorio.findByVisitaTremIdAndStatusOrdemFerroviariaOrderBySequenciaFerroviariaAsc(
                        visitaTremId,
                        status);
        return lotes.stream().map(this::mapear).toList();
    }

    @Transactional
    public OrdemFerroviariaCargaResposta atualizarStatus(
            String visitaTremId,
            UUID loteId,
            AtualizarStatusOrdemFerroviariaRequest request) {
        validarVisita(visitaTremId);
        LoteCarga lote = loteCargaRepositorio.findByIdAndVisitaTremId(loteId, visitaTremId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ordem ferroviária de carga geral não encontrada para a visita informada."));
        executarRegraDominio(() -> lote.atualizarStatusFerroviario(
                request.status(),
                request.custodia(),
                request.motivo(),
                request.responsavel()));
        return mapear(loteCargaRepositorio.save(lote));
    }

    private OrdemFerroviariaCargaResposta mapear(LoteCarga lote) {
        return new OrdemFerroviariaCargaResposta(
                lote.getId(),
                lote.getCodigo(),
                lote.getNatureza(),
                lote.getStatusOrdemFerroviaria(),
                lote.getVisitaTremId(),
                lote.getVagaoId(),
                lote.getPosicaoFerroviaria(),
                lote.getSequenciaFerroviaria(),
                lote.getPesoPrevistoKg(),
                lote.getPesoSaldoKg(),
                lote.getCapacidadeVagaoPesoKg(),
                lote.getIncompatibilidadesFerroviarias(),
                lote.getCustodiaFerroviaria(),
                lote.getHistoricoCustodiaFerroviaria().stream()
                        .map(this::mapearHistorico)
                        .toList());
    }

    private HistoricoCustodiaResposta mapearHistorico(HistoricoCustodiaFerroviaria historico) {
        return new HistoricoCustodiaResposta(
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getCustodiaAnterior(),
                historico.getCustodiaNova(),
                historico.getEvento(),
                historico.getMotivo(),
                historico.getResponsavel(),
                historico.getOcorridoEm());
    }

    private void validarVisita(String visitaTremId) {
        if (visitaTremId == null || visitaTremId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador da visita de trem inválido.");
        }
    }

    private void executarRegraDominio(Runnable acao) {
        try {
            acao.run();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }
}
