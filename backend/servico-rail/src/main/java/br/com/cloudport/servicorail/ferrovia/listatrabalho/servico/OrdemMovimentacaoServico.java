package br.com.cloudport.servicorail.ferrovia.listatrabalho.servico;

import br.com.cloudport.servicorail.ferrovia.listatrabalho.dto.OrdemMovimentacaoRespostaDto;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.OrdemMovimentacao;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.StatusOrdemMovimentacao;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.TipoMovimentacaoOrdem;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.repositorio.OrdemMovimentacaoRepositorio;
import br.com.cloudport.servicorail.ferrovia.modelo.OperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrdemMovimentacaoServico {

    private final OrdemMovimentacaoRepositorio ordemMovimentacaoRepositorio;

    public OrdemMovimentacaoServico(OrdemMovimentacaoRepositorio ordemMovimentacaoRepositorio) {
        this.ordemMovimentacaoRepositorio = ordemMovimentacaoRepositorio;
    }

    @Transactional(readOnly = true)
    public List<OrdemMovimentacaoRespostaDto> listarOrdensParaExecucao(Long idVisita,
                                                                       StatusOrdemMovimentacao statusFiltro) {
        if (idVisita == null || idVisita <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador da visita inválido.");
        }
        List<StatusOrdemMovimentacao> filtros = statusFiltro != null
                ? List.of(statusFiltro)
                : new ArrayList<>(EnumSet.of(StatusOrdemMovimentacao.PENDENTE, StatusOrdemMovimentacao.EM_EXECUCAO));
        List<OrdemMovimentacao> ordens = ordemMovimentacaoRepositorio
                .findByVisitaTremIdAndStatusMovimentacaoInOrderByCriadoEmAsc(idVisita, filtros);
        return ordens.stream()
                .map(OrdemMovimentacaoRespostaDto::deEntidade)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrdemMovimentacaoRespostaDto atualizarStatus(Long idVisita,
                                                         Long idOrdem,
                                                         StatusOrdemMovimentacao novoStatus) {
        if (idVisita == null || idVisita <= 0 || idOrdem == null || idOrdem <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador informado é inválido.");
        }
        StatusOrdemMovimentacao statusValidado = Optional.ofNullable(novoStatus)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O status da movimentação deve ser informado."));
        OrdemMovimentacao ordem = ordemMovimentacaoRepositorio.findByIdAndVisitaTremId(idOrdem, idVisita)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ordem de movimentação não encontrada para a visita informada."));
        validarTransicaoStatus(ordem.getStatusMovimentacao(), statusValidado);
        ordem.setStatusMovimentacao(statusValidado);
        OrdemMovimentacao atualizada = ordemMovimentacaoRepositorio.save(ordem);
        return OrdemMovimentacaoRespostaDto.deEntidade(atualizada);
    }

    @Transactional
    public void gerarOrdensPendentesParaVisita(VisitaTrem visita) {
        if (visita == null || visita.getId() == null) {
            return;
        }
        if (visita.getStatusVisita() != StatusVisitaTrem.CHEGOU) {
            return;
        }
        List<OperacaoConteinerVisita> descarga = Optional.ofNullable(visita.getListaDescarga())
                .orElseGet(List::of);
        List<OperacaoConteinerVisita> carga = Optional.ofNullable(visita.getListaCarga())
                .orElseGet(List::of);

        Set<ChaveOrdem> chavesExistentes = ordemMovimentacaoRepositorio.findByVisitaTremId(visita.getId())
                .stream()
                .map(ordem -> new ChaveOrdem(ordem.getCodigoConteiner(), ordem.getTipoMovimentacao()))
                .collect(Collectors.toCollection(HashSet::new));

        List<OrdemMovimentacao> novasOrdens = new ArrayList<>();
        adicionarOrdensParaOperacoes(visita, descarga, TipoMovimentacaoOrdem.DESCARGA_TREM, chavesExistentes, novasOrdens);
        adicionarOrdensParaOperacoes(visita, carga, TipoMovimentacaoOrdem.CARGA_TREM, chavesExistentes, novasOrdens);

        if (!novasOrdens.isEmpty()) {
            ordemMovimentacaoRepositorio.saveAll(novasOrdens);
        }
    }

    @Transactional
    public void registrarOrdemParaOperacaoSeNecessario(VisitaTrem visita,
                                                        String codigoConteiner,
                                                        TipoMovimentacaoOrdem tipoMovimentacao) {
        if (visita == null || visita.getId() == null) {
            return;
        }
        if (visita.getStatusVisita() != StatusVisitaTrem.CHEGOU) {
            return;
        }
        if (!StringUtils.hasText(codigoConteiner)) {
            return;
        }
        boolean existe = ordemMovimentacaoRepositorio
                .existsByVisitaTremIdAndCodigoConteinerIgnoreCaseAndTipoMovimentacao(visita.getId(),
                        codigoConteiner, tipoMovimentacao);
        if (!existe) {
            OrdemMovimentacao ordem = new OrdemMovimentacao(visita, codigoConteiner, tipoMovimentacao,
                    StatusOrdemMovimentacao.PENDENTE);
            ordemMovimentacaoRepositorio.save(ordem);
        }
    }

    @Transactional
    public void removerOrdemSeExistir(Long idVisita,
                                      String codigoConteiner,
                                      TipoMovimentacaoOrdem tipoMovimentacao) {
        if (idVisita == null || idVisita <= 0) {
            return;
        }
        if (!StringUtils.hasText(codigoConteiner)) {
            return;
        }
        ordemMovimentacaoRepositorio
                .findByVisitaTremIdAndCodigoConteinerIgnoreCaseAndTipoMovimentacao(idVisita, codigoConteiner,
                        tipoMovimentacao)
                .filter(ordem -> ordem.getStatusMovimentacao() != StatusOrdemMovimentacao.CONCLUIDA)
                .ifPresent(ordemMovimentacaoRepositorio::delete);
    }

    private void adicionarOrdensParaOperacoes(VisitaTrem visita,
                                              List<OperacaoConteinerVisita> operacoes,
                                              TipoMovimentacaoOrdem tipoMovimentacao,
                                              Set<ChaveOrdem> chavesExistentes,
                                              List<OrdemMovimentacao> novasOrdens) {
        if (CollectionUtils.isEmpty(operacoes)) {
            return;
        }
        for (OperacaoConteinerVisita operacao : operacoes) {
            if (operacao == null || !StringUtils.hasText(operacao.getCodigoConteiner())) {
                continue;
            }
            ChaveOrdem chave = new ChaveOrdem(operacao.getCodigoConteiner(), tipoMovimentacao);
            if (chavesExistentes.contains(chave)) {
                continue;
            }
            chavesExistentes.add(chave);
            novasOrdens.add(new OrdemMovimentacao(visita, operacao.getCodigoConteiner(), tipoMovimentacao,
                    StatusOrdemMovimentacao.PENDENTE));
        }
    }

    private void validarTransicaoStatus(StatusOrdemMovimentacao statusAtual,
                                        StatusOrdemMovimentacao novoStatus) {
        if (Objects.equals(statusAtual, novoStatus)) {
            return;
        }
        if (statusAtual == StatusOrdemMovimentacao.PENDENTE
                && (novoStatus == StatusOrdemMovimentacao.EM_EXECUCAO
                || novoStatus == StatusOrdemMovimentacao.CONCLUIDA)) {
            return;
        }
        if (statusAtual == StatusOrdemMovimentacao.EM_EXECUCAO
                && novoStatus == StatusOrdemMovimentacao.CONCLUIDA) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                String.format(Locale.ROOT,
                        "A transição de status de %s para %s não é permitida.",
                        statusAtual, novoStatus));
    }

    private static final class ChaveOrdem {
        private final String codigoConteiner;
        private final TipoMovimentacaoOrdem tipoMovimentacao;

        private ChaveOrdem(String codigoConteiner, TipoMovimentacaoOrdem tipoMovimentacao) {
            this.codigoConteiner = codigoConteiner != null ? codigoConteiner.trim().toUpperCase() : null;
            this.tipoMovimentacao = tipoMovimentacao;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ChaveOrdem outra = (ChaveOrdem) obj;
            return Objects.equals(codigoConteiner, outra.codigoConteiner)
                    && tipoMovimentacao == outra.tipoMovimentacao;
        }

        @Override
        public int hashCode() {
            return Objects.hash(codigoConteiner, tipoMovimentacao);
        }
    }
}
