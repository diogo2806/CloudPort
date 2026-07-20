package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusProgramacaoDocaCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoEventoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.ItemOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dominio.OperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.PlanoStuffUnstuffVersao;
import br.com.cloudport.servicocargageral.dominio.PlanoStuffUnstuffVersao.ItemPlano;
import br.com.cloudport.servicocargageral.dominio.PlanoStuffUnstuffVersao.StatusPlano;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CriarItemOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CriarVersaoPlanoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.ItemPlanoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.LiberarPlanoRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.PlanoVersaoResposta;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.OperacaoStuffUnstuffRepositorio;
import br.com.cloudport.servicocargageral.repositorio.PlanoStuffUnstuffVersaoRepositorio;
import br.com.cloudport.servicocargageral.repositorio.ProgramacaoDocaCargaRepositorio;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlanoStuffUnstuffServico {

    private static final List<StatusProgramacaoDocaCarga> STATUS_PROGRAMACAO_ATIVA = List.of(
            StatusProgramacaoDocaCarga.RESERVADA,
            StatusProgramacaoDocaCarga.EM_USO);

    private final PlanoStuffUnstuffVersaoRepositorio planoRepositorio;
    private final OperacaoStuffUnstuffRepositorio operacaoRepositorio;
    private final LoteCargaRepositorio loteRepositorio;
    private final ProgramacaoDocaCargaRepositorio programacaoRepositorio;

    public PlanoStuffUnstuffServico(
            PlanoStuffUnstuffVersaoRepositorio planoRepositorio,
            OperacaoStuffUnstuffRepositorio operacaoRepositorio,
            LoteCargaRepositorio loteRepositorio,
            ProgramacaoDocaCargaRepositorio programacaoRepositorio) {
        this.planoRepositorio = planoRepositorio;
        this.operacaoRepositorio = operacaoRepositorio;
        this.loteRepositorio = loteRepositorio;
        this.programacaoRepositorio = programacaoRepositorio;
    }

    @Transactional
    public PlanoVersaoResposta criarVersaoInicial(OperacaoStuffUnstuff operacao, String usuario) {
        PlanoStuffUnstuffVersao existente = planoRepositorio
                .findFirstByOperacao_IdOrderByNumeroVersaoDesc(operacao.getId())
                .orElse(null);
        if (existente != null) {
            return mapear(existente);
        }
        PlanoStuffUnstuffVersao plano = criarSnapshot(operacao, 1, usuario);
        operacao.registrarEvento(
                TipoEventoStuffUnstuff.PLANO_VERSIONADO,
                usuario,
                null,
                "Versão 1 do plano criada em rascunho.");
        operacaoRepositorio.save(operacao);
        return mapear(planoRepositorio.save(plano));
    }

    @Transactional(readOnly = true)
    public List<PlanoVersaoResposta> listar(UUID operacaoId) {
        return planoRepositorio.findByOperacao_IdOrderByNumeroVersaoDesc(operacaoId).stream()
                .map(this::mapear)
                .toList();
    }

    @Transactional
    public PlanoVersaoResposta criarNovaVersao(UUID operacaoId, CriarVersaoPlanoRequest request) {
        OperacaoStuffUnstuff operacao = buscarOperacao(operacaoId);
        if (operacao.getStatus() != StatusOperacaoStuffUnstuff.PLANEJADA || operacao.possuiExecucao()) {
            throw conflito("Uma nova versão só pode ser criada antes da execução física.");
        }
        if (programacaoRepositorio.existsByOperacao_IdAndStatusIn(operacaoId, STATUS_PROGRAMACAO_ATIVA)) {
            throw conflito("Cancele a programação de doca antes de alterar os cargo lots do plano.");
        }
        PlanoStuffUnstuffVersao atual = planoRepositorio
                .findFirstByOperacao_IdOrderByNumeroVersaoDesc(operacaoId)
                .orElseThrow(() -> conflito("A operação não possui versão inicial do plano."));
        atual.superar();
        planoRepositorio.save(atual);

        List<ItemOperacaoStuffUnstuff> novosItens = criarItensValidados(operacao.getTipo(), request.itens());
        executarComEstadoValido(() -> operacao.substituirItens(novosItens));
        operacao.registrarEvento(
                TipoEventoStuffUnstuff.PLANO_VERSIONADO,
                request.usuario(),
                request.correlationId(),
                "Plano versionado por motivo: " + request.motivo());
        operacaoRepositorio.saveAndFlush(operacao);

        PlanoStuffUnstuffVersao novaVersao = criarSnapshot(
                operacao,
                atual.getNumeroVersao() + 1,
                request.usuario());
        return mapear(planoRepositorio.save(novaVersao));
    }

    @Transactional
    public PlanoVersaoResposta liberar(UUID operacaoId, LiberarPlanoRequest request) {
        OperacaoStuffUnstuff operacao = buscarOperacao(operacaoId);
        if (operacao.getStatus() != StatusOperacaoStuffUnstuff.PLANEJADA || operacao.possuiExecucao()) {
            throw conflito("O plano só pode ser liberado antes da execução física.");
        }
        PlanoStuffUnstuffVersao plano = planoRepositorio
                .findByOperacao_IdAndNumeroVersao(operacaoId, request.versao())
                .orElseThrow(() -> naoEncontrada("Versão do plano não encontrada."));
        PlanoStuffUnstuffVersao atual = planoRepositorio
                .findFirstByOperacao_IdOrderByNumeroVersaoDesc(operacaoId)
                .orElseThrow(() -> naoEncontrada("Plano da operação não encontrado."));
        if (plano.getNumeroVersao() != atual.getNumeroVersao()) {
            throw conflito("Somente a versão mais recente pode ser liberada.");
        }
        validarSnapshotContraEstoque(operacao.getTipo(), plano);
        executarComEstadoValido(() -> plano.liberar(request.usuario(), request.motivo()));
        operacao.registrarEvento(
                TipoEventoStuffUnstuff.PLANO_LIBERADO,
                request.usuario(),
                request.correlationId(),
                "Versão " + plano.getNumeroVersao() + " liberada: " + request.motivo());
        operacaoRepositorio.save(operacao);
        return mapear(planoRepositorio.save(plano));
    }

    @Transactional(readOnly = true)
    public void exigirPlanoLiberado(UUID operacaoId) {
        if (!planoRepositorio.existsByOperacao_IdAndStatus(operacaoId, StatusPlano.LIBERADO)) {
            throw conflito("A execução física exige uma versão liberada do plano.");
        }
    }

    private PlanoStuffUnstuffVersao criarSnapshot(
            OperacaoStuffUnstuff operacao,
            int numeroVersao,
            String usuario) {
        PlanoStuffUnstuffVersao plano = new PlanoStuffUnstuffVersao();
        plano.setOperacao(operacao);
        plano.setNumeroVersao(numeroVersao);
        plano.setCriadoPor(usuario);
        operacao.getItens().forEach(item -> plano.adicionarItem(
                item.getLote().getId(),
                item.getQuantidadePlanejada(),
                item.getVolumePlanejadoM3(),
                item.getPesoPlanejadoKg()));
        return plano;
    }

    private List<ItemOperacaoStuffUnstuff> criarItensValidados(
            TipoOperacaoStuffUnstuff tipo,
            List<CriarItemOperacaoRequest> requests) {
        Set<UUID> lotesUnicos = new HashSet<>();
        List<ItemOperacaoStuffUnstuff> itens = new ArrayList<>();
        for (CriarItemOperacaoRequest request : requests) {
            if (!lotesUnicos.add(request.loteId())) {
                throw conflito("O mesmo cargo lot não pode aparecer duas vezes na versão.");
            }
            LoteCarga lote = buscarLote(request.loteId());
            validarCapacidade(tipo, lote, request.quantidadePlanejada(), request.volumePlanejadoM3(), request.pesoPlanejadoKg());
            ItemOperacaoStuffUnstuff item = new ItemOperacaoStuffUnstuff();
            item.setLote(lote);
            item.setQuantidadePlanejada(request.quantidadePlanejada());
            item.setVolumePlanejadoM3(request.volumePlanejadoM3());
            item.setPesoPlanejadoKg(request.pesoPlanejadoKg());
            itens.add(item);
        }
        return itens;
    }

    private void validarSnapshotContraEstoque(
            TipoOperacaoStuffUnstuff tipo,
            PlanoStuffUnstuffVersao plano) {
        for (ItemPlano item : plano.getItens()) {
            LoteCarga lote = buscarLote(item.getLoteId());
            validarCapacidade(
                    tipo,
                    lote,
                    item.getQuantidadePlanejada(),
                    item.getVolumePlanejadoM3(),
                    item.getPesoPlanejadoKg());
        }
    }

    private void validarCapacidade(
            TipoOperacaoStuffUnstuff tipo,
            LoteCarga lote,
            BigDecimal quantidade,
            BigDecimal volume,
            BigDecimal peso) {
        if (tipo == TipoOperacaoStuffUnstuff.STUFF) {
            if (quantidade.compareTo(lote.getQuantidadeSaldo()) > 0
                    || volume.compareTo(lote.getVolumeSaldoM3()) > 0
                    || peso.compareTo(lote.getPesoSaldoKg()) > 0) {
                throw conflito("Saldo insuficiente para liberar o lote " + lote.getCodigo() + ".");
            }
            return;
        }
        BigDecimal capacidadeQuantidade = lote.getQuantidadePrevista().subtract(lote.getQuantidadeSaldo());
        BigDecimal capacidadeVolume = lote.getVolumePrevistoM3().subtract(lote.getVolumeSaldoM3());
        BigDecimal capacidadePeso = lote.getPesoPrevistoKg().subtract(lote.getPesoSaldoKg());
        if (quantidade.compareTo(capacidadeQuantidade) > 0
                || volume.compareTo(capacidadeVolume) > 0
                || peso.compareTo(capacidadePeso) > 0) {
            throw conflito("Capacidade insuficiente para liberar o lote " + lote.getCodigo() + ".");
        }
    }

    private PlanoVersaoResposta mapear(PlanoStuffUnstuffVersao plano) {
        List<ItemPlanoResposta> itens = plano.getItens().stream()
                .map(item -> new ItemPlanoResposta(
                        item.getLoteId(),
                        loteRepositorio.findById(item.getLoteId()).map(LoteCarga::getCodigo).orElse("Lote removido"),
                        item.getQuantidadePlanejada(),
                        item.getVolumePlanejadoM3(),
                        item.getPesoPlanejadoKg()))
                .toList();
        return new PlanoVersaoResposta(
                plano.getId(),
                plano.getNumeroVersao(),
                plano.getStatus(),
                plano.getCriadoPor(),
                plano.getCriadoEm(),
                plano.getLiberadoPor(),
                plano.getLiberadoEm(),
                plano.getMotivo(),
                itens);
    }

    private OperacaoStuffUnstuff buscarOperacao(UUID id) {
        return operacaoRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Operação de stuff/unstuff não encontrada."));
    }

    private LoteCarga buscarLote(UUID id) {
        return loteRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Cargo lot não encontrado."));
    }

    private void executarComEstadoValido(Runnable acao) {
        try {
            acao.run();
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
