package br.com.cloudport.servicoyard.container.servico;

import br.com.cloudport.servicoyard.container.dto.MovimentacaoTremConcluidaEventoDto;
import br.com.cloudport.servicoyard.container.enumeracao.TipoMovimentacaoFerroviaEnum;
import br.com.cloudport.servicoyard.container.validacao.SanitizadorEntrada;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.MovimentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.MovimentoPatioRepositorio;
import java.time.LocalDateTime;
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

    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final MovimentoPatioRepositorio movimentoRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public ProcessadorMovimentacaoTremService(ConteinerPatioRepositorio conteinerRepositorio,
                                              MovimentoPatioRepositorio movimentoRepositorio,
                                              SanitizadorEntrada sanitizadorEntrada) {
        this.conteinerRepositorio = conteinerRepositorio;
        this.movimentoRepositorio = movimentoRepositorio;
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
        TipoMovimentoPatio tipoMovimento = tipoMovimentacao.getTipoOperacaoConteiner();
        ConteinerPatio conteiner = conteinerRepositorio.findByCodigoIgnoreCase(codigoConteiner).orElse(null);
        if (conteiner == null) {
            LOGGER.warn("event=movimentacao_trem.conteiner_nao_localizado codigo={} ordem={} visita={}",
                    codigoConteiner, evento.getIdOrdemMovimentacao(), evento.getIdVisitaTrem());
            return;
        }
        StatusConteiner statusAnterior = conteiner.getStatus();
        StatusConteiner novoStatus = tipoMovimentacao.getNovoStatus();
        if (novoStatus != null && novoStatus != statusAnterior) {
            conteiner.setStatus(novoStatus);
            conteinerRepositorio.save(conteiner);
        }
        registrarMovimento(conteiner, tipoMovimentacao, tipoMovimento, evento, statusAnterior, novoStatus);
    }

    private void registrarMovimento(ConteinerPatio conteiner,
                                    TipoMovimentacaoFerroviaEnum tipoMovimentacao,
                                    TipoMovimentoPatio tipoMovimento,
                                    MovimentacaoTremConcluidaEventoDto evento,
                                    StatusConteiner statusAnterior,
                                    StatusConteiner novoStatus) {
        MovimentoPatio movimento = new MovimentoPatio();
        movimento.setConteiner(conteiner);
        movimento.setTipoMovimento(tipoMovimento);
        movimento.setDescricao(montarDescricao(tipoMovimentacao, statusAnterior, novoStatus));
        movimento.setPosicaoAnterior(null);
        movimento.setPosicaoAtual(formatarPosicao(conteiner));
        movimento.setResponsavel(sanitizadorEntrada.limparTexto(RESPONSAVEL_INTEGRACAO));
        LocalDateTime registradoEm = Optional.ofNullable(evento.getConcluidoEm())
                .map(odt -> odt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime())
                .orElseGet(LocalDateTime::now);
        movimento.setRegistradoEm(registradoEm);
        movimentoRepositorio.save(movimento);
    }

    private String formatarPosicao(ConteinerPatio conteiner) {
        PosicaoPatio posicao = conteiner.getPosicao();
        if (posicao == null) {
            return null;
        }
        return posicao.getLinha() + "-" + posicao.getColuna() + "-" + posicao.getCamadaOperacional();
    }

    private String montarDescricao(TipoMovimentacaoFerroviaEnum tipoMovimentacao,
                                   StatusConteiner statusAnterior,
                                   StatusConteiner novoStatus) {
        if (tipoMovimentacao != null) {
            return tipoMovimentacao.getDescricaoHistorico();
        }
        return String.format(Locale.ROOT,
                "Movimentação atualizada de %s para %s.",
                statusAnterior, novoStatus);
    }
}
