package br.com.cloudport.servicoyard.container.servico;

import br.com.cloudport.servicoyard.container.dto.MovimentacaoTremConcluidaEventoDto;
import br.com.cloudport.servicoyard.container.entidade.Conteiner;
import br.com.cloudport.servicoyard.container.entidade.HistoricoOperacaoConteiner;
import br.com.cloudport.servicoyard.container.entidade.StatusOperacionalConteiner;
import br.com.cloudport.servicoyard.container.entidade.TipoOperacaoConteiner;
import br.com.cloudport.servicoyard.container.repositorio.ConteinerRepositorio;
import br.com.cloudport.servicoyard.container.repositorio.HistoricoOperacaoConteinerRepositorio;
import br.com.cloudport.servicoyard.container.validacao.SanitizadorEntrada;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProcessadorMovimentacaoTremService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessadorMovimentacaoTremService.class);
    private static final String RESPONSAVEL_INTEGRACAO = "Integração Ferrovia";

    private final ConteinerRepositorio conteinerRepositorio;
    private final HistoricoOperacaoConteinerRepositorio historicoRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public ProcessadorMovimentacaoTremService(ConteinerRepositorio conteinerRepositorio,
                                              HistoricoOperacaoConteinerRepositorio historicoRepositorio,
                                              SanitizadorEntrada sanitizadorEntrada) {
        this.conteinerRepositorio = conteinerRepositorio;
        this.historicoRepositorio = historicoRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional
    public void processar(MovimentacaoTremConcluidaEventoDto evento) {
        if (evento == null) {
            LOGGER.warn("event=movimentacao_trem.evento_nulo");
            return;
        }
        String codigoConteiner = Optional.ofNullable(evento.getCodigoConteiner())
                .map(sanitizadorEntrada::limparTexto)
                .map(valor -> valor.toUpperCase(Locale.ROOT))
                .orElse(null);
        if (!StringUtils.hasText(codigoConteiner)) {
            LOGGER.warn("event=movimentacao_trem.codigo_ausente ordem={} visita={}",
                    evento.getIdOrdemMovimentacao(), evento.getIdVisitaTrem());
            return;
        }
        TipoOperacaoConteiner tipoOperacao = mapearTipoOperacao(evento.getTipoMovimentacao());
        if (tipoOperacao == null) {
            LOGGER.warn("event=movimentacao_trem.tipo_desconhecido tipo={} ordem={} visita={}",
                    evento.getTipoMovimentacao(), evento.getIdOrdemMovimentacao(), evento.getIdVisitaTrem());
            return;
        }
        Conteiner conteiner = conteinerRepositorio.findByIdentificacaoIgnoreCase(codigoConteiner)
                .orElse(null);
        if (conteiner == null) {
            LOGGER.warn("event=movimentacao_trem.conteiner_nao_localizado codigo={} ordem={} visita={}",
                    codigoConteiner, evento.getIdOrdemMovimentacao(), evento.getIdVisitaTrem());
            return;
        }
        StatusOperacionalConteiner statusAnterior = conteiner.getStatusOperacional();
        StatusOperacionalConteiner novoStatus = definirStatusPosMovimentacao(tipoOperacao, statusAnterior);
        if (novoStatus != null && novoStatus != statusAnterior) {
            conteiner.setStatusOperacional(novoStatus);
            conteinerRepositorio.save(conteiner);
        }
        registrarHistorico(conteiner, tipoOperacao, evento, statusAnterior, novoStatus);
    }

    private void registrarHistorico(Conteiner conteiner,
                                    TipoOperacaoConteiner tipoOperacao,
                                    MovimentacaoTremConcluidaEventoDto evento,
                                    StatusOperacionalConteiner statusAnterior,
                                    StatusOperacionalConteiner novoStatus) {
        HistoricoOperacaoConteiner historico = new HistoricoOperacaoConteiner();
        historico.setConteiner(conteiner);
        historico.setTipoOperacao(tipoOperacao);
        historico.setDescricao(montarDescricao(tipoOperacao, statusAnterior, novoStatus));
        historico.setPosicaoAnterior(null);
        historico.setPosicaoAtual(conteiner.getPosicaoPatio());
        historico.setResponsavel(sanitizadorEntrada.limparTexto(RESPONSAVEL_INTEGRACAO));
        OffsetDateTime dataRegistro = Optional.ofNullable(evento.getConcluidoEm())
                .orElse(OffsetDateTime.now(ZoneOffset.UTC));
        historico.setDataRegistro(dataRegistro);
        historicoRepositorio.save(historico);
    }

    private TipoOperacaoConteiner mapearTipoOperacao(String tipoMovimentacao) {
        if (!StringUtils.hasText(tipoMovimentacao)) {
            return null;
        }
        String tipoNormalizado = tipoMovimentacao.trim().toUpperCase(Locale.ROOT);
        if ("DESCARGA_TREM".equals(tipoNormalizado)) {
            return TipoOperacaoConteiner.DESCARGA_TREM;
        }
        if ("CARGA_TREM".equals(tipoNormalizado)) {
            return TipoOperacaoConteiner.CARGA_TREM;
        }
        return null;
    }

    private StatusOperacionalConteiner definirStatusPosMovimentacao(TipoOperacaoConteiner tipoOperacao,
                                                                    StatusOperacionalConteiner statusAtual) {
        if (tipoOperacao == TipoOperacaoConteiner.DESCARGA_TREM) {
            return StatusOperacionalConteiner.ALOCADO;
        }
        if (tipoOperacao == TipoOperacaoConteiner.CARGA_TREM) {
            return StatusOperacionalConteiner.EM_TRANSFERENCIA;
        }
        return statusAtual;
    }

    private String montarDescricao(TipoOperacaoConteiner tipoOperacao,
                                   StatusOperacionalConteiner statusAnterior,
                                   StatusOperacionalConteiner novoStatus) {
        if (tipoOperacao == TipoOperacaoConteiner.DESCARGA_TREM) {
            return "Descarga do trem concluída e contêiner disponível no pátio.";
        }
        if (tipoOperacao == TipoOperacaoConteiner.CARGA_TREM) {
            return "Carga no trem concluída e contêiner encaminhado para o modal ferroviário.";
        }
        return String.format(Locale.ROOT,
                "Movimentação atualizada de %s para %s.",
                statusAnterior, novoStatus);
    }
}
