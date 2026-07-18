package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoMovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dominio.MovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.OperacaoTransload;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusTransload;
import br.com.cloudport.servicocargageral.dto.TransloadDTOs.ExecutarTransloadRequest;
import br.com.cloudport.servicocargageral.dto.TransloadDTOs.ItemTransloadRequest;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.OperacaoTransloadRepositorio;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransloadTransacaoServico {

    private static final String TIPO_UNIDADE = "CONTEINER";

    private final LoteCargaRepositorio loteRepositorio;
    private final OperacaoTransloadRepositorio transloadRepositorio;

    public TransloadTransacaoServico(
            LoteCargaRepositorio loteRepositorio,
            OperacaoTransloadRepositorio transloadRepositorio) {
        this.loteRepositorio = loteRepositorio;
        this.transloadRepositorio = transloadRepositorio;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OperacaoTransload iniciar(ExecutarTransloadRequest request) {
        OperacaoTransload existente = transloadRepositorio.findByCommandId(request.commandId()).orElse(null);
        if (existente != null) {
            return existente;
        }

        OperacaoTransload operacao = new OperacaoTransload();
        operacao.setCommandId(request.commandId());
        operacao.setUnidadeOrigem(request.unidadeOrigem());
        operacao.setUnidadeDestino(request.unidadeDestino());
        operacao.setReservaOrigemId(UUID.randomUUID());
        operacao.setReservaDestinoId(UUID.randomUUID());
        operacao.setLacreOrigem(request.lacreOrigem());
        operacao.setLacreDestino(request.lacreDestino());
        operacao.setDivergencia(limpar(request.divergencia()));
        operacao.setCodigoAvaria(request.codigoAvaria());
        operacao.setDescricaoAvaria(limpar(request.descricaoAvaria()));
        operacao.setUsuario(request.usuario());
        operacao.setCorrelationId(correlationId(request));
        request.itens().forEach(item -> operacao.adicionarItem(
                item.loteOrigemId(),
                item.loteDestinoId(),
                item.quantidade(),
                item.volumeM3(),
                item.pesoKg()));
        operacao.iniciar();
        return transloadRepositorio.saveAndFlush(operacao);
    }

    @Transactional
    public OperacaoTransload aplicar(UUID operacaoId, ExecutarTransloadRequest request) {
        OperacaoTransload operacao = buscarComBloqueio(operacaoId);
        if (operacao.getStatus() == StatusTransload.CONCLUIDO) {
            return operacao;
        }
        if (operacao.getStatus() == StatusTransload.CANCELADO) {
            throw conflito("Transload cancelado não pode ser executado novamente com o mesmo commandId.");
        }

        Map<UUID, LoteCarga> lotes = bloquearLotes(request.itens());
        validarLotesEQuantidades(request.itens(), lotes);
        aplicarMovimentos(operacao, request, lotes);
        loteRepositorio.saveAll(lotes.values());
        operacao.concluir();
        return transloadRepositorio.save(operacao);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OperacaoTransload cancelar(UUID operacaoId, String motivo) {
        OperacaoTransload operacao = buscarComBloqueio(operacaoId);
        if (operacao.getStatus() == StatusTransload.EM_EXECUCAO) {
            operacao.cancelar(limitar(motivo, 1000));
            return transloadRepositorio.save(operacao);
        }
        return operacao;
    }

    @Transactional(readOnly = true)
    public OperacaoTransload buscarPorCommandId(UUID commandId) {
        return transloadRepositorio.findByCommandId(commandId)
                .orElseThrow(() -> naoEncontrada("Operação de transload não encontrada."));
    }

    @Transactional(readOnly = true)
    public OperacaoTransload obter(UUID id) {
        return transloadRepositorio.findDetalhadoById(id)
                .orElseThrow(() -> naoEncontrada("Operação de transload não encontrada."));
    }

    private Map<UUID, LoteCarga> bloquearLotes(List<ItemTransloadRequest> itens) {
        List<UUID> ids = itens.stream()
                .flatMap(item -> List.of(item.loteOrigemId(), item.loteDestinoId()).stream())
                .distinct()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        Map<UUID, LoteCarga> lotes = new HashMap<>();
        ids.forEach(id -> lotes.put(id, loteRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Cargo lot não encontrado: " + id + "."))));
        return lotes;
    }

    private void validarLotesEQuantidades(
            List<ItemTransloadRequest> itens,
            Map<UUID, LoteCarga> lotes) {
        Set<UUID> origens = new HashSet<>();
        Set<UUID> destinos = new HashSet<>();
        Map<UUID, Totais> saidas = new HashMap<>();
        Map<UUID, Totais> entradas = new HashMap<>();

        for (ItemTransloadRequest item : itens) {
            if (item.loteOrigemId().equals(item.loteDestinoId())) {
                throw conflito("Transload exige lotes distintos para origem e destino.");
            }
            origens.add(item.loteOrigemId());
            destinos.add(item.loteDestinoId());
            LoteCarga origem = lotes.get(item.loteOrigemId());
            LoteCarga destino = lotes.get(item.loteDestinoId());
            validarCompatibilidade(origem, destino);
            saidas.computeIfAbsent(item.loteOrigemId(), id -> new Totais()).adicionar(item);
            entradas.computeIfAbsent(item.loteDestinoId(), id -> new Totais()).adicionar(item);
        }

        Set<UUID> intersecao = new HashSet<>(origens);
        intersecao.retainAll(destinos);
        if (!intersecao.isEmpty()) {
            throw conflito("Um cargo lot não pode atuar simultaneamente como origem e destino no mesmo transload.");
        }

        saidas.forEach((loteId, totais) -> validarSaldoDisponivel(lotes.get(loteId), totais));
        entradas.forEach((loteId, totais) -> validarCapacidadeDestino(lotes.get(loteId), totais));
    }

    private void validarCompatibilidade(LoteCarga origem, LoteCarga destino) {
        if (!origem.getUnidadeMedida().equalsIgnoreCase(destino.getUnidadeMedida())) {
            throw conflito("Lotes do transload possuem unidades de medida incompatíveis.");
        }
        if (!origem.getItem().getId().equals(destino.getItem().getId())) {
            throw conflito("Lotes do transload devem pertencer ao mesmo item do Bill of Lading.");
        }
    }

    private void validarSaldoDisponivel(LoteCarga lote, Totais totais) {
        if (totais.quantidade.compareTo(lote.getQuantidadeDisponivel()) > 0
                || totais.volumeM3.compareTo(lote.getVolumeDisponivelM3()) > 0
                || totais.pesoKg.compareTo(lote.getPesoDisponivelKg()) > 0) {
            throw conflito("Saldo disponível insuficiente no cargo lot " + lote.getCodigo() + ".");
        }
    }

    private void validarCapacidadeDestino(LoteCarga lote, Totais totais) {
        if (lote.getQuantidadeSaldo().add(totais.quantidade).compareTo(lote.getQuantidadePrevista()) > 0
                || lote.getVolumeSaldoM3().add(totais.volumeM3).compareTo(lote.getVolumePrevistoM3()) > 0
                || lote.getPesoSaldoKg().add(totais.pesoKg).compareTo(lote.getPesoPrevistoKg()) > 0) {
            throw conflito("Capacidade prevista excedida no cargo lot " + lote.getCodigo() + ".");
        }
    }

    private void aplicarMovimentos(
            OperacaoTransload operacao,
            ExecutarTransloadRequest request,
            Map<UUID, LoteCarga> lotes) {
        for (ItemTransloadRequest item : request.itens()) {
            LoteCarga origem = lotes.get(item.loteOrigemId());
            LoteCarga destino = lotes.get(item.loteDestinoId());
            executarEstado(() -> origem.retirarSaldo(item.quantidade(), item.volumeM3(), item.pesoKg()));
            destino.adicionarSaldo(item.quantidade(), item.volumeM3(), item.pesoKg());
            origem.getItem().getConhecimento().iniciarOperacao();
            destino.getItem().getConhecimento().iniciarOperacao();
            registrarMovimentacao(
                    origem,
                    destino,
                    TipoMovimentacaoCarga.CARGA_PARCIAL,
                    item,
                    operacao,
                    "Saída no transload " + request.commandId());
            registrarMovimentacao(
                    destino,
                    origem,
                    TipoMovimentacaoCarga.DESCARGA_PARCIAL,
                    item,
                    operacao,
                    "Entrada no transload " + request.commandId());
        }
    }

    private void registrarMovimentacao(
            LoteCarga lote,
            LoteCarga loteRelacionado,
            TipoMovimentacaoCarga tipo,
            ItemTransloadRequest item,
            OperacaoTransload operacao,
            String observacao) {
        MovimentacaoCarga movimentacao = new MovimentacaoCarga();
        movimentacao.setTipo(tipo);
        movimentacao.setQuantidade(item.quantidade());
        movimentacao.setVolumeM3(item.volumeM3());
        movimentacao.setPesoKg(item.pesoKg());
        movimentacao.setLoteRelacionadoId(loteRelacionado.getId());
        movimentacao.setOrigemTipo(TIPO_UNIDADE);
        movimentacao.setOrigemId(operacao.getUnidadeOrigem());
        movimentacao.setDestinoTipo(TIPO_UNIDADE);
        movimentacao.setDestinoId(operacao.getUnidadeDestino());
        movimentacao.setUsuario(operacao.getUsuario());
        movimentacao.setCorrelationId(operacao.getCorrelationId());
        movimentacao.setObservacao(observacao);
        lote.registrarMovimentacao(movimentacao);
    }

    private OperacaoTransload buscarComBloqueio(UUID id) {
        return transloadRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Operação de transload não encontrada."));
    }

    private void executarEstado(Runnable acao) {
        try {
            acao.run();
        } catch (IllegalStateException exception) {
            throw conflito(exception.getMessage());
        }
    }

    private String correlationId(ExecutarTransloadRequest request) {
        return request.correlationId() == null || request.correlationId().isBlank()
                ? request.commandId().toString()
                : request.correlationId().trim();
    }

    private String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String limitar(String valor, int tamanho) {
        String texto = limpar(valor);
        if (texto == null || texto.length() <= tamanho) {
            return texto;
        }
        return texto.substring(0, tamanho);
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private ResponseStatusException naoEncontrada(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }

    private static final class Totais {
        private BigDecimal quantidade = BigDecimal.ZERO;
        private BigDecimal volumeM3 = BigDecimal.ZERO;
        private BigDecimal pesoKg = BigDecimal.ZERO;

        private void adicionar(ItemTransloadRequest item) {
            quantidade = quantidade.add(item.quantidade());
            volumeM3 = volumeM3.add(item.volumeM3());
            pesoKg = pesoKg.add(item.pesoKg());
        }
    }
}
