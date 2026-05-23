package br.com.cloudport.serviconavio.escala.listatrabalho.servico;

import br.com.cloudport.serviconavio.escala.dto.EventoMovimentacaoNavioConcluidaDto;
import br.com.cloudport.serviconavio.escala.entidade.Escala;
import br.com.cloudport.serviconavio.escala.entidade.OperacaoConteinerEscala;
import br.com.cloudport.serviconavio.escala.evento.PublicadorEventoMovimentacaoNavio;
import br.com.cloudport.serviconavio.escala.listatrabalho.dto.OrdemMovimentacaoNavioRespostaDTO;
import br.com.cloudport.serviconavio.escala.listatrabalho.modelo.OrdemMovimentacaoNavio;
import br.com.cloudport.serviconavio.escala.listatrabalho.modelo.StatusOrdemMovimentacaoNavio;
import br.com.cloudport.serviconavio.escala.listatrabalho.modelo.TipoMovimentacaoOrdemNavio;
import br.com.cloudport.serviconavio.escala.listatrabalho.repositorio.OrdemMovimentacaoNavioRepositorio;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
public class OrdemMovimentacaoNavioServico {

    private final OrdemMovimentacaoNavioRepositorio ordemRepositorio;
    private final PublicadorEventoMovimentacaoNavio publicador;

    public OrdemMovimentacaoNavioServico(OrdemMovimentacaoNavioRepositorio ordemRepositorio,
                                         PublicadorEventoMovimentacaoNavio publicador) {
        this.ordemRepositorio = ordemRepositorio;
        this.publicador = publicador;
    }

    @Transactional(readOnly = true)
    public List<OrdemMovimentacaoNavioRespostaDTO> listarOrdensParaExecucao(Long idEscala,
                                                                            StatusOrdemMovimentacaoNavio statusFiltro) {
        if (idEscala == null || idEscala <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador da escala inválido.");
        }
        List<StatusOrdemMovimentacaoNavio> filtros = statusFiltro != null
                ? List.of(statusFiltro)
                : new ArrayList<>(EnumSet.of(StatusOrdemMovimentacaoNavio.PENDENTE,
                StatusOrdemMovimentacaoNavio.EM_EXECUCAO));
        return ordemRepositorio.findByEscalaIdAndStatusMovimentacaoInOrderByCriadoEmAsc(idEscala, filtros).stream()
                .map(OrdemMovimentacaoNavioRespostaDTO::deEntidade)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrdemMovimentacaoNavioRespostaDTO atualizarStatus(Long idEscala,
                                                             Long idOrdem,
                                                             StatusOrdemMovimentacaoNavio novoStatus) {
        if (idEscala == null || idEscala <= 0 || idOrdem == null || idOrdem <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador informado é inválido.");
        }
        StatusOrdemMovimentacaoNavio statusValidado = Optional.ofNullable(novoStatus)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O status da movimentação deve ser informado."));
        OrdemMovimentacaoNavio ordem = ordemRepositorio.findByIdAndEscalaId(idOrdem, idEscala)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ordem de movimentação não encontrada para a escala informada."));
        StatusOrdemMovimentacaoNavio statusAnterior = ordem.getStatusMovimentacao();
        validarTransicaoStatus(statusAnterior, statusValidado);
        ordem.setStatusMovimentacao(statusValidado);
        OrdemMovimentacaoNavio atualizada = ordemRepositorio.save(ordem);
        if (statusValidado == StatusOrdemMovimentacaoNavio.CONCLUIDA
                && statusAnterior != StatusOrdemMovimentacaoNavio.CONCLUIDA) {
            publicarEventoConclusao(atualizada);
        }
        return OrdemMovimentacaoNavioRespostaDTO.deEntidade(atualizada);
    }

    @Transactional
    public void gerarOrdensPendentesParaEscala(Escala escala) {
        if (escala == null || escala.getId() == null) {
            return;
        }
        if (!escala.getFase().permiteOperacaoConteiner()) {
            return;
        }
        List<OperacaoConteinerEscala> descarga = Optional.ofNullable(escala.getListaDescarga()).orElseGet(List::of);
        List<OperacaoConteinerEscala> carga = Optional.ofNullable(escala.getListaCarga()).orElseGet(List::of);

        Set<ChaveOrdem> chavesExistentes = ordemRepositorio.findByEscalaId(escala.getId()).stream()
                .map(ordem -> new ChaveOrdem(ordem.getCodigoConteiner(), ordem.getTipoMovimentacao()))
                .collect(Collectors.toCollection(HashSet::new));

        List<OrdemMovimentacaoNavio> novasOrdens = new ArrayList<>();
        adicionarOrdens(escala, descarga, TipoMovimentacaoOrdemNavio.DESCARGA_NAVIO, chavesExistentes, novasOrdens);
        adicionarOrdens(escala, carga, TipoMovimentacaoOrdemNavio.CARGA_NAVIO, chavesExistentes, novasOrdens);

        if (!novasOrdens.isEmpty()) {
            ordemRepositorio.saveAll(novasOrdens);
        }
    }

    @Transactional
    public void registrarOrdemSeNecessario(Escala escala,
                                           String codigoConteiner,
                                           TipoMovimentacaoOrdemNavio tipoMovimentacao) {
        if (escala == null || escala.getId() == null || !escala.getFase().permiteOperacaoConteiner()) {
            return;
        }
        if (!StringUtils.hasText(codigoConteiner)) {
            return;
        }
        boolean existe = ordemRepositorio
                .existsByEscalaIdAndCodigoConteinerIgnoreCaseAndTipoMovimentacao(escala.getId(),
                        codigoConteiner, tipoMovimentacao);
        if (!existe) {
            ordemRepositorio.save(new OrdemMovimentacaoNavio(escala, codigoConteiner, tipoMovimentacao,
                    StatusOrdemMovimentacaoNavio.PENDENTE));
        }
    }

    @Transactional
    public void removerOrdemSeExistir(Long idEscala,
                                      String codigoConteiner,
                                      TipoMovimentacaoOrdemNavio tipoMovimentacao) {
        if (idEscala == null || idEscala <= 0 || !StringUtils.hasText(codigoConteiner)) {
            return;
        }
        ordemRepositorio
                .findByEscalaIdAndCodigoConteinerIgnoreCaseAndTipoMovimentacao(idEscala, codigoConteiner, tipoMovimentacao)
                .filter(ordem -> ordem.getStatusMovimentacao() != StatusOrdemMovimentacaoNavio.CONCLUIDA)
                .ifPresent(ordemRepositorio::delete);
    }

    private void adicionarOrdens(Escala escala,
                                 List<OperacaoConteinerEscala> operacoes,
                                 TipoMovimentacaoOrdemNavio tipoMovimentacao,
                                 Set<ChaveOrdem> chavesExistentes,
                                 List<OrdemMovimentacaoNavio> novasOrdens) {
        if (CollectionUtils.isEmpty(operacoes)) {
            return;
        }
        for (OperacaoConteinerEscala operacao : operacoes) {
            if (operacao == null || !StringUtils.hasText(operacao.getCodigoConteiner())) {
                continue;
            }
            ChaveOrdem chave = new ChaveOrdem(operacao.getCodigoConteiner(), tipoMovimentacao);
            if (chavesExistentes.contains(chave)) {
                continue;
            }
            chavesExistentes.add(chave);
            novasOrdens.add(new OrdemMovimentacaoNavio(escala, operacao.getCodigoConteiner(), tipoMovimentacao,
                    StatusOrdemMovimentacaoNavio.PENDENTE));
        }
    }

    private void validarTransicaoStatus(StatusOrdemMovimentacaoNavio statusAtual,
                                        StatusOrdemMovimentacaoNavio novoStatus) {
        if (Objects.equals(statusAtual, novoStatus)) {
            return;
        }
        if (statusAtual == StatusOrdemMovimentacaoNavio.PENDENTE
                && (novoStatus == StatusOrdemMovimentacaoNavio.EM_EXECUCAO
                || novoStatus == StatusOrdemMovimentacaoNavio.CONCLUIDA)) {
            return;
        }
        if (statusAtual == StatusOrdemMovimentacaoNavio.EM_EXECUCAO
                && novoStatus == StatusOrdemMovimentacaoNavio.CONCLUIDA) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                String.format(Locale.ROOT,
                        "A transição de status de %s para %s não é permitida.", statusAtual, novoStatus));
    }

    private void publicarEventoConclusao(OrdemMovimentacaoNavio ordem) {
        if (ordem == null) {
            return;
        }
        EventoMovimentacaoNavioConcluidaDto evento = new EventoMovimentacaoNavioConcluidaDto(
                ordem.getEscala() != null ? ordem.getEscala().getId() : null,
                ordem.getId(),
                ordem.getCodigoConteiner(),
                ordem.getTipoMovimentacao() != null ? ordem.getTipoMovimentacao().name() : null,
                OffsetDateTime.now(ZoneOffset.UTC),
                "MovimentacaoNavioConcluidaEvent");
        publicador.publicar(evento);
    }

    private static final class ChaveOrdem {
        private final String codigoConteiner;
        private final TipoMovimentacaoOrdemNavio tipoMovimentacao;

        private ChaveOrdem(String codigoConteiner, TipoMovimentacaoOrdemNavio tipoMovimentacao) {
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
