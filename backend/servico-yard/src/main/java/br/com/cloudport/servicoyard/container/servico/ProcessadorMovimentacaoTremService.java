package br.com.cloudport.servicoyard.container.servico;

import br.com.cloudport.servicoyard.container.dto.MovimentacaoTremConcluidaEventoDto;
import br.com.cloudport.servicoyard.container.entidade.Conteiner;
import br.com.cloudport.servicoyard.container.entidade.HistoricoOperacaoConteiner;
import br.com.cloudport.servicoyard.container.entidade.StatusOperacionalConteiner;
import br.com.cloudport.servicoyard.container.entidade.TipoOperacaoConteiner;
import br.com.cloudport.servicoyard.container.enumeracao.TipoMovimentacaoFerroviaEnum;
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
        if (!StringUtils.hasText(evento.getTipoMovimentacao())) {
            LOGGER.warn("event=movimentacao_trem.tipo_ausente ordem={} visita={}",
                    evento.getIdOrdemMovimentacao(), evento.getIdVisitaTrem());
            return;
        }
        TipoMovimentacaoFerroviaEnum tipoMovimentacao;
        try {
            tipoMovimentacao = TipoMovimentacaoFerroviaEnum.fromString(evento.getTipoMovimentacao());
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("event=movimentacao_trem.tipo_invalido tipo={} ordem={} visita={}",
                    evento.getTipoMovimentacao(), evento.getIdOrdemMovimentacao(), evento.getIdVisitaTrem(), ex);
            return;
        }
        if (tipoMovimentacao == null) {
            LOGGER.warn("event=movimentacao_trem.tipo_desconhecido tipo={} ordem={} visita={}",
                    evento.getTipoMovimentacao(), evento.getIdOrdemMovimentacao(), evento.getIdVisitaTrem());
            return;
        }
        TipoOperacaoConteiner tipoOperacao = tipoMovimentacao.getTipoOperacaoConteiner();
        Conteiner conteiner = conteinerRepositorio.findByIdentificacaoIgnoreCase(codigoConteiner)
                .orElse(null);
        if (conteiner == null) {
            LOGGER.warn("event=movimentacao_trem.conteiner_nao_localizado codigo={} ordem={} visita={}",
                    codigoConteiner, evento.getIdOrdemMovimentacao(), evento.getIdVisitaTrem());
            return;
        }
        StatusOperacionalConteiner statusAnterior = conteiner.getStatusOperacional();
        StatusOperacionalConteiner novoStatus = definirStatusPosMovimentacao(tipoMovimentacao, statusAnterior);
        if (novoStatus != null && novoStatus != statusAnterior) {
            conteiner.setStatusOperacional(novoStatus);
            conteinerRepositorio.save(conteiner);
        }
        registrarHistorico(conteiner, tipoMovimentacao, tipoOperacao, evento, statusAnterior, novoStatus);
    }

    private void registrarHistorico(Conteiner conteiner,
                                    TipoMovimentacaoFerroviaEnum tipoMovimentacao,
                                    TipoOperacaoConteiner tipoOperacao,
                                    MovimentacaoTremConcluidaEventoDto evento,
                                    StatusOperacionalConteiner statusAnterior,
                                    StatusOperacionalConteiner novoStatus) {
        HistoricoOperacaoConteiner historico = new HistoricoOperacaoConteiner();
        historico.setConteiner(conteiner);
        historico.setTipoOperacao(tipoOperacao);
        historico.setDescricao(montarDescricao(tipoMovimentacao, statusAnterior, novoStatus));
        historico.setPosicaoAnterior(null);
        historico.setPosicaoAtual(conteiner.getPosicaoPatio());
        historico.setResponsavel(sanitizadorEntrada.limparTexto(RESPONSAVEL_INTEGRACAO));
        OffsetDateTime dataRegistro = Optional.ofNullable(evento.getConcluidoEm())
                .orElse(OffsetDateTime.now(ZoneOffset.UTC));
        historico.setDataRegistro(dataRegistro);
        historicoRepositorio.save(historico);
    }

    private StatusOperacionalConteiner definirStatusPosMovimentacao(TipoMovimentacaoFerroviaEnum tipoMovimentacao,
                                                                    StatusOperacionalConteiner statusAtual) {
        if (tipoMovimentacao == null) {
            return statusAtual;
        }
        StatusOperacionalConteiner novoStatus = tipoMovimentacao.getNovoStatus();
        if (novoStatus != null) {
            return novoStatus;
        }
        return statusAtual;
    }

    private String montarDescricao(TipoMovimentacaoFerroviaEnum tipoMovimentacao,
                                   StatusOperacionalConteiner statusAnterior,
                                   StatusOperacionalConteiner novoStatus) {
        if (tipoMovimentacao != null) {
            return tipoMovimentacao.getDescricaoHistorico();
        }
        return String.format(Locale.ROOT,
                "Movimentação atualizada de %s para %s.",
                statusAnterior, novoStatus);
    }
}
