package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoEventoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoMovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.EventoOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.ItemOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dominio.MovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.OperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CancelarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.ConcluirOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CriarItemOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CriarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.EventoOperacaoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.ItemOperacaoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.OperacaoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.RegistrarExecucaoRequest;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.OperacaoStuffUnstuffRepositorio;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StuffUnstuffServico {

    private final OperacaoStuffUnstuffRepositorio operacaoRepositorio;
    private final LoteCargaRepositorio loteRepositorio;

    public StuffUnstuffServico(
            OperacaoStuffUnstuffRepositorio operacaoRepositorio,
            LoteCargaRepositorio loteRepositorio) {
        this.operacaoRepositorio = operacaoRepositorio;
        this.loteRepositorio = loteRepositorio;
    }

    @Transactional(readOnly = true)
    public List<OperacaoResposta> listar() {
        return operacaoRepositorio.findAllByOrderByCriadoEmDesc().stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OperacaoResposta obter(UUID id) {
        return mapear(operacaoRepositorio.findDetalhadaById(id)
                .orElseThrow(() -> naoEncontrada("Operação de stuff/unstuff não encontrada.")));
    }

    @Transactional
    public OperacaoResposta criarOperacaoStuffUnstuff(CriarOperacaoRequest request) {
        Set<UUID> lotesUnicos = new HashSet<>();
        OperacaoStuffUnstuff operacao = new OperacaoStuffUnstuff();
        operacao.setTipo(request.tipo());
        operacao.setConteinerId(request.conteinerId());
        operacao.setArmazemId(request.armazemId());
        operacao.setPosicaoOperacao(request.posicaoOperacao());
        operacao.setEquipeRecurso(request.equipeRecurso());
        operacao.setLacreInicial(request.lacreInicial());

        for (CriarItemOperacaoRequest itemRequest : request.itens()) {
            if (!lotesUnicos.add(itemRequest.loteId())) {
                throw conflito("O mesmo cargo lot não pode ser informado duas vezes na operação.");
            }
            LoteCarga lote = buscarLoteComBloqueio(itemRequest.loteId());
            validarPlanejamento(request.tipo(), lote, itemRequest);
            ItemOperacaoStuffUnstuff item = new ItemOperacaoStuffUnstuff();
            item.setLote(lote);
            item.setQuantidadePlanejada(itemRequest.quantidadePlanejada());
            item.setVolumePlanejadoM3(itemRequest.volumePlanejadoM3());
            item.setPesoPlanejadoKg(itemRequest.pesoPlanejadoKg());
            operacao.adicionarItem(item);
        }

        operacao.registrarEvento(TipoEventoStuffUnstuff.CRIADA, request.usuario(), request.correlationId(),
                "Operação planejada para o contêiner " + request.conteinerId() + ".");
        return mapear(operacaoRepositorio.save(operacao));
    }

    @Transactional
    public OperacaoResposta iniciar(UUID id, String usuario, String correlationId) {
        OperacaoStuffUnstuff operacao = buscarOperacaoComBloqueio(id);
        executarComEstadoValido(operacao::iniciar);
        operacao.registrarEvento(TipoEventoStuffUnstuff.INICIADA, usuario, correlationId, "Operação iniciada.");
        return mapear(operacaoRepositorio.save(operacao));
    }

    @Transactional
    public OperacaoResposta registrarExecucao(UUID id, RegistrarExecucaoRequest request) {
        OperacaoStuffUnstuff operacao = buscarOperacaoComBloqueio(id);
        if (operacao.getStatus() == StatusOperacaoStuffUnstuff.PLANEJADA) {
            executarComEstadoValido(operacao::iniciar);
            operacao.registrarEvento(TipoEventoStuffUnstuff.INICIADA, request.usuario(), request.correlationId(),
                    "Operação iniciada automaticamente no primeiro apontamento.");
        }
        if (operacao.getStatus() == StatusOperacaoStuffUnstuff.CONCLUIDA
                || operacao.getStatus() == StatusOperacaoStuffUnstuff.CANCELADA) {
            throw conflito("Operação encerrada não aceita novos apontamentos.");
        }

        ItemOperacaoStuffUnstuff item = operacao.getItens().stream()
                .filter(candidato -> candidato.getId().equals(request.itemId()))
                .findFirst()
                .orElseThrow(() -> naoEncontrada("Item da operação não encontrado."));
        LoteCarga lote = buscarLoteComBloqueio(item.getLote().getId());
        validarExecucaoNoLote(operacao.getTipo(), lote, request.quantidade(), request.volumeM3(), request.pesoKg());
        aplicarSaldo(operacao.getTipo(), lote, request.quantidade(), request.volumeM3(), request.pesoKg());
        registrarMovimentacao(operacao, lote, request);
        executarComEstadoValido(() -> item.registrarExecucao(
                request.quantidade(), request.volumeM3(), request.pesoKg(),
                request.codigoAvaria(), request.descricaoAvaria(), request.divergencia()));
        operacao.atualizarStatusExecucao();
        operacao.registrarEvento(TipoEventoStuffUnstuff.EXECUCAO_REGISTRADA, request.usuario(),
                request.correlationId(), "Execução parcial registrada para o lote " + lote.getCodigo() + ".");
        if (request.divergencia() != null && !request.divergencia().isBlank()) {
            operacao.registrarEvento(TipoEventoStuffUnstuff.DIVERGENCIA_REGISTRADA, request.usuario(),
                    request.correlationId(), request.divergencia());
        }
        if (request.codigoAvaria() != null && !request.codigoAvaria().isBlank()) {
            operacao.registrarEvento(TipoEventoStuffUnstuff.AVARIA_REGISTRADA, request.usuario(),
                    request.correlationId(), request.codigoAvaria() + ": " + request.descricaoAvaria());
        }
        loteRepositorio.save(lote);
        return mapear(operacaoRepositorio.save(operacao));
    }

    @Transactional
    public OperacaoResposta concluir(UUID id, ConcluirOperacaoRequest request) {
        OperacaoStuffUnstuff operacao = buscarOperacaoComBloqueio(id);
        executarComEstadoValido(() -> operacao.concluir(
                request.lacreFinal(), request.observacao(), request.usuario(), request.correlationId()));
        return mapear(operacaoRepositorio.save(operacao));
    }

    @Transactional
    public OperacaoResposta cancelar(UUID id, CancelarOperacaoRequest request) {
        OperacaoStuffUnstuff operacao = buscarOperacaoComBloqueio(id);
        if (operacao.getStatus() == StatusOperacaoStuffUnstuff.CONCLUIDA
                || operacao.getStatus() == StatusOperacaoStuffUnstuff.CANCELADA) {
            throw conflito("Operação já está encerrada.");
        }
        for (ItemOperacaoStuffUnstuff item : operacao.getItens()) {
            if (!item.possuiExecucao()) continue;
            LoteCarga lote = buscarLoteComBloqueio(item.getLote().getId());
            compensarSaldo(operacao.getTipo(), lote, item);
            loteRepositorio.save(lote);
        }
        executarComEstadoValido(() -> operacao.cancelar(request.motivo(), request.usuario(), request.correlationId()));
        return mapear(operacaoRepositorio.save(operacao));
    }

    private void validarPlanejamento(TipoOperacaoStuffUnstuff tipo, LoteCarga lote, CriarItemOperacaoRequest item) {
        if (tipo == TipoOperacaoStuffUnstuff.STUFF) {
            validarDisponibilidade(lote, item.quantidadePlanejada(), item.volumePlanejadoM3(), item.pesoPlanejadoKg());
            return;
        }
        BigDecimal capacidadeQuantidade = lote.getQuantidadePrevista().subtract(lote.getQuantidadeSaldo());
        BigDecimal capacidadeVolume = lote.getVolumePrevistoM3().subtract(lote.getVolumeSaldoM3());
        BigDecimal capacidadePeso = lote.getPesoPrevistoKg().subtract(lote.getPesoSaldoKg());
        if (item.quantidadePlanejada().compareTo(capacidadeQuantidade) > 0
                || item.volumePlanejadoM3().compareTo(capacidadeVolume) > 0
                || item.pesoPlanejadoKg().compareTo(capacidadePeso) > 0) {
            throw conflito("Planejamento de unstuff excede a capacidade prevista do cargo lot " + lote.getCodigo() + ".");
        }
    }

    private void validarExecucaoNoLote(TipoOperacaoStuffUnstuff tipo, LoteCarga lote,
            BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        if (tipo == TipoOperacaoStuffUnstuff.STUFF) {
            validarDisponibilidade(lote, quantidade, volume, peso);
        }
    }

    private void validarDisponibilidade(LoteCarga lote, BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        if (quantidade.compareTo(lote.getQuantidadeSaldo()) > 0
                || volume.compareTo(lote.getVolumeSaldoM3()) > 0
                || peso.compareTo(lote.getPesoSaldoKg()) > 0) {
            throw conflito("Saldo insuficiente no cargo lot " + lote.getCodigo() + ".");
        }
    }

    private void aplicarSaldo(TipoOperacaoStuffUnstuff tipo, LoteCarga lote,
            BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        executarComEstadoValido(() -> {
            if (tipo == TipoOperacaoStuffUnstuff.STUFF) lote.retirarSaldo(quantidade, volume, peso);
            else lote.adicionarSaldo(quantidade, volume, peso);
        });
    }

    private void compensarSaldo(TipoOperacaoStuffUnstuff tipo, LoteCarga lote, ItemOperacaoStuffUnstuff item) {
        executarComEstadoValido(() -> {
            if (tipo == TipoOperacaoStuffUnstuff.STUFF) {
                lote.adicionarSaldo(item.getQuantidadeRealizada(), item.getVolumeRealizadoM3(), item.getPesoRealizadoKg());
            } else {
                lote.retirarSaldo(item.getQuantidadeRealizada(), item.getVolumeRealizadoM3(), item.getPesoRealizadoKg());
            }
        });
    }

    private void registrarMovimentacao(OperacaoStuffUnstuff operacao, LoteCarga lote, RegistrarExecucaoRequest request) {
        MovimentacaoCarga movimentacao = new MovimentacaoCarga();
        movimentacao.setTipo(operacao.getTipo() == TipoOperacaoStuffUnstuff.STUFF
                ? TipoMovimentacaoCarga.CARGA_PARCIAL : TipoMovimentacaoCarga.DESCARGA_PARCIAL);
        movimentacao.setQuantidade(request.quantidade());
        movimentacao.setVolumeM3(request.volumeM3());
        movimentacao.setPesoKg(request.pesoKg());
        movimentacao.setOrigemTipo(operacao.getTipo() == TipoOperacaoStuffUnstuff.STUFF ? "CARGO_LOT" : "CONTEINER");
        movimentacao.setOrigemId(operacao.getTipo() == TipoOperacaoStuffUnstuff.STUFF
                ? lote.getId().toString() : operacao.getConteinerId());
        movimentacao.setDestinoTipo(operacao.getTipo() == TipoOperacaoStuffUnstuff.STUFF ? "CONTEINER" : "CARGO_LOT");
        movimentacao.setDestinoId(operacao.getTipo() == TipoOperacaoStuffUnstuff.STUFF
                ? operacao.getConteinerId() : lote.getId().toString());
        movimentacao.setArmazemId(operacao.getArmazemId());
        movimentacao.setUsuario(request.usuario());
        movimentacao.setCorrelationId(request.correlationId());
        movimentacao.setObservacao("Operação " + operacao.getId() + " - " + operacao.getTipo());
        lote.registrarMovimentacao(movimentacao);
    }

    private OperacaoStuffUnstuff buscarOperacaoComBloqueio(UUID id) {
        return operacaoRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Operação de stuff/unstuff não encontrada."));
    }

    private LoteCarga buscarLoteComBloqueio(UUID id) {
        return loteRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Cargo lot não encontrado."));
    }

    private OperacaoResposta mapear(OperacaoStuffUnstuff operacao) {
        List<ItemOperacaoResposta> itens = operacao.getItens().stream().map(this::mapearItem).collect(Collectors.toList());
        List<EventoOperacaoResposta> historico = operacao.getHistorico().stream().map(this::mapearEvento).collect(Collectors.toList());
        return new OperacaoResposta(operacao.getId(), operacao.getTipo(), operacao.getStatus(), operacao.getConteinerId(),
                operacao.getArmazemId(), operacao.getPosicaoOperacao(), operacao.getEquipeRecurso(), operacao.getLacreInicial(),
                operacao.getLacreFinal(), operacao.getMotivoCancelamento(), operacao.getCriadoEm(), operacao.getIniciadoEm(),
                operacao.getConcluidoEm(), operacao.getCanceladoEm(), itens, historico);
    }

    private ItemOperacaoResposta mapearItem(ItemOperacaoStuffUnstuff item) {
        return new ItemOperacaoResposta(item.getId(), item.getLote().getId(), item.getLote().getCodigo(),
                item.getQuantidadePlanejada(), item.getQuantidadeRealizada(), item.getVolumePlanejadoM3(),
                item.getVolumeRealizadoM3(), item.getPesoPlanejadoKg(), item.getPesoRealizadoKg(),
                item.getCodigoAvaria(), item.getDescricaoAvaria(), item.getDivergencia());
    }

    private EventoOperacaoResposta mapearEvento(EventoOperacaoStuffUnstuff evento) {
        return new EventoOperacaoResposta(evento.getId(), evento.getTipo(), evento.getUsuario(),
                evento.getCorrelationId(), evento.getDescricao(), evento.getOcorridoEm());
    }

    private void executarComEstadoValido(Runnable comando) {
        try {
            comando.run();
        } catch (IllegalStateException exception) {
            throw conflito(exception.getMessage());
        }
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private ResponseStatusException naoEncontrada(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }
}
