package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.ItemOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dominio.OperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dto.OperacaoStuffUnstuffDTOs.CancelarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoStuffUnstuffDTOs.ConcluirOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoStuffUnstuffDTOs.CriarItemOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoStuffUnstuffDTOs.CriarOperacaoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoStuffUnstuffDTOs.ItemOperacaoResposta;
import br.com.cloudport.servicocargageral.dto.OperacaoStuffUnstuffDTOs.OperacaoResposta;
import br.com.cloudport.servicocargageral.dto.OperacaoStuffUnstuffDTOs.RegistrarExecucaoRequest;
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
public class OperacaoStuffUnstuffServico {

    private final OperacaoStuffUnstuffRepositorio operacaoRepositorio;
    private final LoteCargaRepositorio loteRepositorio;

    public OperacaoStuffUnstuffServico(
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
                .orElseThrow(() -> naoEncontrada("Operação não encontrada.")));
    }

    @Transactional
    public OperacaoResposta criarOperacaoStuffUnstuff(CriarOperacaoRequest request) {
        if (operacaoRepositorio.existsByNumeroIgnoreCase(request.numero())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma operação com esse número.");
        }
        validarPlanejamento(request.itens());
        OperacaoStuffUnstuff operacao = new OperacaoStuffUnstuff();
        operacao.setNumero(request.numero());
        operacao.setTipo(request.tipo());
        operacao.setContainerId(request.containerId());
        operacao.setArmazemId(request.armazemId());
        operacao.setPosicaoOperacao(request.posicaoOperacao());
        operacao.setEquipeRecurso(request.equipeRecurso());
        operacao.setLacreInicial(request.lacreInicial());
        operacao.setUsuario(request.usuario());
        operacao.setObservacao(request.observacao());

        for (CriarItemOperacaoRequest itemRequest : request.itens()) {
            LoteCarga lote = buscarLoteComBloqueio(itemRequest.loteId());
            if (request.tipo() == TipoOperacaoStuffUnstuff.STUFF) {
                validarSaldoPlanejado(lote, itemRequest);
            }
            ItemOperacaoStuffUnstuff item = new ItemOperacaoStuffUnstuff();
            item.setLote(lote);
            item.setQuantidadePlanejada(itemRequest.quantidadePlanejada());
            item.setVolumePlanejadoM3(itemRequest.volumePlanejadoM3());
            item.setPesoPlanejadoKg(itemRequest.pesoPlanejadoKg());
            operacao.adicionarItem(item);
        }
        return mapear(operacaoRepositorio.save(operacao));
    }

    @Transactional
    public OperacaoResposta iniciar(UUID id) {
        OperacaoStuffUnstuff operacao = buscarOperacaoComBloqueio(id);
        executarDominio(operacao::iniciar);
        return mapear(operacaoRepositorio.save(operacao));
    }

    @Transactional
    public OperacaoResposta registrarExecucaoParcial(UUID id, RegistrarExecucaoRequest request) {
        validarExecucao(request);
        OperacaoStuffUnstuff operacao = buscarOperacaoComBloqueio(id);
        ItemOperacaoStuffUnstuff item = operacao.getItens().stream()
                .filter(candidato -> candidato.getId().equals(request.itemId()))
                .findFirst()
                .orElseThrow(() -> naoEncontrada("Item da operação não encontrado."));
        LoteCarga lote = buscarLoteComBloqueio(item.getLote().getId());
        try {
            if (operacao.getTipo() == TipoOperacaoStuffUnstuff.STUFF) {
                lote.retirarSaldo(request.quantidade(), request.volumeM3(), request.pesoKg());
            } else {
                lote.adicionarSaldo(request.quantidade(), request.volumeM3(), request.pesoKg());
            }
            item.registrarExecucao(
                    request.quantidade(), request.volumeM3(), request.pesoKg(),
                    request.divergencia(), request.codigoAvaria(), request.descricaoAvaria());
            operacao.marcarParcial();
        } catch (IllegalStateException exception) {
            throw conflito(exception);
        }
        if (request.codigoAvaria() != null && !request.codigoAvaria().isBlank()) {
            lote.setCodigoAvaria(request.codigoAvaria().trim().toUpperCase());
            lote.setDescricaoAvaria(request.descricaoAvaria());
        }
        lote.atualizarLocalizacao(operacao.getArmazemId(), operacao.getPosicaoOperacao(), null, null, null);
        loteRepositorio.save(lote);
        return mapear(operacaoRepositorio.save(operacao));
    }

    @Transactional
    public OperacaoResposta concluir(UUID id, ConcluirOperacaoRequest request) {
        OperacaoStuffUnstuff operacao = buscarOperacaoComBloqueio(id);
        executarDominio(() -> operacao.concluir(request.lacreFinal()));
        return mapear(operacaoRepositorio.save(operacao));
    }

    @Transactional
    public OperacaoResposta cancelar(UUID id, CancelarOperacaoRequest request) {
        OperacaoStuffUnstuff operacao = buscarOperacaoComBloqueio(id);
        try {
            for (ItemOperacaoStuffUnstuff item : operacao.getItens()) {
                LoteCarga lote = buscarLoteComBloqueio(item.getLote().getId());
                if (operacao.getTipo() == TipoOperacaoStuffUnstuff.STUFF) {
                    lote.adicionarSaldo(item.getQuantidadeRealizada(), item.getVolumeRealizadoM3(), item.getPesoRealizadoKg());
                } else {
                    lote.retirarSaldo(item.getQuantidadeRealizada(), item.getVolumeRealizadoM3(), item.getPesoRealizadoKg());
                }
                loteRepositorio.save(lote);
            }
            operacao.cancelar(request.motivo());
        } catch (IllegalStateException exception) {
            throw conflito(exception);
        }
        return mapear(operacaoRepositorio.save(operacao));
    }

    private void validarPlanejamento(List<CriarItemOperacaoRequest> itens) {
        Set<UUID> lotes = new HashSet<>();
        for (CriarItemOperacaoRequest item : itens) {
            if (!lotes.add(item.loteId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O mesmo cargo lot não pode ser repetido na operação.");
            }
            if (todosZero(item.quantidadePlanejada(), item.volumePlanejadoM3(), item.pesoPlanejadoKg())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada item deve possuir quantidade, volume ou peso planejado.");
            }
        }
    }

    private void validarExecucao(RegistrarExecucaoRequest request) {
        if (todosZero(request.quantidade(), request.volumeM3(), request.pesoKg())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe quantidade, volume ou peso executado.");
        }
    }

    private void validarSaldoPlanejado(LoteCarga lote, CriarItemOperacaoRequest item) {
        if (item.quantidadePlanejada().compareTo(lote.getQuantidadeSaldo()) > 0
                || item.volumePlanejadoM3().compareTo(lote.getVolumeSaldoM3()) > 0
                || item.pesoPlanejadoKg().compareTo(lote.getPesoSaldoKg()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Planejamento de stuff excede o saldo do cargo lot " + lote.getCodigo() + ".");
        }
    }

    private boolean todosZero(BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        return quantidade.signum() == 0 && volume.signum() == 0 && peso.signum() == 0;
    }

    private OperacaoStuffUnstuff buscarOperacaoComBloqueio(UUID id) {
        return operacaoRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Operação não encontrada."));
    }

    private LoteCarga buscarLoteComBloqueio(UUID id) {
        return loteRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Cargo lot não encontrado."));
    }

    private void executarDominio(Runnable comando) {
        try {
            comando.run();
        } catch (IllegalStateException exception) {
            throw conflito(exception);
        }
    }

    private ResponseStatusException conflito(IllegalStateException exception) {
        return new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
    }

    private ResponseStatusException naoEncontrada(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }

    private OperacaoResposta mapear(OperacaoStuffUnstuff operacao) {
        return new OperacaoResposta(
                operacao.getId(), operacao.getNumero(), operacao.getTipo(), operacao.getStatus(),
                operacao.getContainerId(), operacao.getArmazemId(), operacao.getPosicaoOperacao(),
                operacao.getEquipeRecurso(), operacao.getLacreInicial(), operacao.getLacreFinal(),
                operacao.getUsuario(), operacao.getObservacao(), operacao.getMotivoCancelamento(),
                operacao.getIniciadaEm(), operacao.getConcluidaEm(), operacao.getCanceladaEm(),
                operacao.getCriadoEm(), operacao.getAtualizadoEm(),
                operacao.getItens().stream().map(this::mapearItem).collect(Collectors.toList()));
    }

    private ItemOperacaoResposta mapearItem(ItemOperacaoStuffUnstuff item) {
        return new ItemOperacaoResposta(
                item.getId(), item.getLote().getId(), item.getLote().getCodigo(),
                item.getQuantidadePlanejada(), item.getVolumePlanejadoM3(), item.getPesoPlanejadoKg(),
                item.getQuantidadeRealizada(), item.getVolumeRealizadoM3(), item.getPesoRealizadoKg(),
                item.getDivergencia(), item.getCodigoAvaria(), item.getDescricaoAvaria());
    }
}
