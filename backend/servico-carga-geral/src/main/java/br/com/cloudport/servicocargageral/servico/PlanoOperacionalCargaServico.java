package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoMovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.ComandoPlanoOperacionalCarga;
import br.com.cloudport.servicocargageral.dominio.ItemPlanoOperacionalCarga;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dominio.MovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.StatusPlanoOperacional;
import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.TipoUnidadeIntermodal;
import br.com.cloudport.servicocargageral.dominio.PlanoOperacionalCarga;
import br.com.cloudport.servicocargageral.dominio.TipoServicoOrdemCarga;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.AtribuirRecursosRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.CancelarPlanoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.ComandoPlanoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.ConcluirPlanoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.CriarItemPlanoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.CriarPlanoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.ExecutarItemPlanoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.ItemPlanoResposta;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.NovaVersaoPlanoRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.PlanoResposta;
import br.com.cloudport.servicocargageral.repositorio.ComandoPlanoOperacionalCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.PlanoOperacionalCargaRepositorio;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlanoOperacionalCargaServico {

    private static final EnumSet<TipoServicoOrdemCarga> SAIDAS = EnumSet.of(
            TipoServicoOrdemCarga.ENTREGA, TipoServicoOrdemCarga.STUFF,
            TipoServicoOrdemCarga.CARGA_NAVIO, TipoServicoOrdemCarga.CARGA_TREM);
    private static final EnumSet<TipoServicoOrdemCarga> ENTRADAS = EnumSet.of(
            TipoServicoOrdemCarga.RECEBIMENTO, TipoServicoOrdemCarga.UNSTUFF,
            TipoServicoOrdemCarga.DESCARGA_NAVIO, TipoServicoOrdemCarga.DESCARGA_TREM);

    private final PlanoOperacionalCargaRepositorio planoRepositorio;
    private final ComandoPlanoOperacionalCargaRepositorio comandoRepositorio;
    private final LoteCargaRepositorio loteRepositorio;

    public PlanoOperacionalCargaServico(
            PlanoOperacionalCargaRepositorio planoRepositorio,
            ComandoPlanoOperacionalCargaRepositorio comandoRepositorio,
            LoteCargaRepositorio loteRepositorio) {
        this.planoRepositorio = planoRepositorio;
        this.comandoRepositorio = comandoRepositorio;
        this.loteRepositorio = loteRepositorio;
    }

    @Transactional(readOnly = true)
    public List<PlanoResposta> listar() {
        return planoRepositorio.findAllByOrderByCriadoEmDesc().stream().map(this::mapear).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlanoResposta obter(UUID id) {
        return mapear(planoRepositorio.findDetalhadoById(id)
                .orElseThrow(() -> naoEncontrado("Plano operacional não encontrado.")));
    }

    @Transactional
    public PlanoResposta criar(CriarPlanoRequest request) {
        if (planoRepositorio.existsByNumeroIgnoreCase(request.numero())) {
            throw conflito("Já existe plano com esse número.");
        }
        validarContrato(request);
        PlanoOperacionalCarga plano = criarCabecalho(request);
        for (CriarItemPlanoRequest itemRequest : request.itens()) {
            LoteCarga lote = buscarLote(itemRequest.loteId());
            validarItemEspecifico(request.tipo(), itemRequest);
            plano.adicionarItem(criarItem(itemRequest, lote));
        }
        plano.registrarHistorico("CRIADO", request.usuario(), "Plano criado sem movimentar estoque.");
        return mapear(planoRepositorio.save(plano));
    }

    @Transactional
    public PlanoResposta criarNovaVersao(UUID id, NovaVersaoPlanoRequest request) {
        PlanoOperacionalCarga origem = buscarPlano(id);
        if (origem.getStatus() == StatusPlanoOperacional.EM_EXECUCAO
                || origem.getStatus() == StatusPlanoOperacional.PARCIAL) {
            throw conflito("Plano em execução não pode gerar nova versão.");
        }
        if (planoRepositorio.existsByNumeroIgnoreCase(request.novoNumero())) {
            throw conflito("Já existe plano com o novo número.");
        }
        PlanoOperacionalCarga novo = copiarCabecalho(origem, request.novoNumero());
        for (ItemPlanoOperacionalCarga itemOrigem : origem.getItens()) {
            ItemPlanoOperacionalCarga item = new ItemPlanoOperacionalCarga();
            item.setLote(itemOrigem.getLote());
            item.setSequencia(itemOrigem.getSequencia());
            item.setQuantidadePlanejada(itemOrigem.getQuantidadePlanejada());
            item.setVolumePlanejadoM3(itemOrigem.getVolumePlanejadoM3());
            item.setPesoPlanejadoKg(itemOrigem.getPesoPlanejadoKg());
            item.setPosicaoPlanejada(itemOrigem.getPosicaoPlanejada());
            item.setAreaPorao(itemOrigem.getAreaPorao());
            item.setVagaoId(itemOrigem.getVagaoId());
            item.setPosicaoVagao(itemOrigem.getPosicaoVagao());
            item.setCapacidadeReservadaKg(itemOrigem.getCapacidadeReservadaKg());
            novo.adicionarItem(item);
        }
        novo.registrarHistorico("NOVA_VERSAO", request.usuario(), "Versão criada a partir de " + origem.getNumero() + ".");
        return mapear(planoRepositorio.save(novo));
    }

    @Transactional
    public PlanoResposta liberar(UUID id, ComandoPlanoRequest request) {
        PlanoOperacionalCarga plano = buscarPlano(id);
        validarLiberacao(plano);
        executarDominio(() -> plano.liberar(request.usuario()));
        return mapear(planoRepositorio.save(plano));
    }

    @Transactional
    public PlanoResposta atribuirRecursos(UUID id, AtribuirRecursosRequest request) {
        PlanoOperacionalCarga plano = buscarPlano(id);
        executarDominio(() -> plano.atribuirRecursos(request.equipeId(), request.equipamentoId(), request.usuario()));
        return mapear(planoRepositorio.save(plano));
    }

    @Transactional
    public PlanoResposta iniciar(UUID id, ComandoPlanoRequest request) {
        PlanoOperacionalCarga plano = buscarPlano(id);
        executarDominio(() -> plano.iniciar(request.usuario()));
        return mapear(planoRepositorio.save(plano));
    }

    @Transactional
    public PlanoResposta executar(UUID id, ExecutarItemPlanoRequest request) {
        PlanoOperacionalCarga plano = buscarPlano(id);
        String hash = assinatura(request);
        ComandoPlanoOperacionalCarga processado = comandoRepositorio
                .findByPlanoIdAndCommandIdIgnoreCase(id, request.commandId()).orElse(null);
        if (processado != null) {
            if (!processado.getPayloadHash().equals(hash)) {
                throw conflito("O commandId já foi utilizado com payload diferente.");
            }
            return mapear(plano);
        }
        ItemPlanoOperacionalCarga item = plano.getItens().stream()
                .filter(candidato -> candidato.getId().equals(request.itemId())).findFirst()
                .orElseThrow(() -> naoEncontrado("Item do plano não encontrado."));
        LoteCarga lote = buscarLote(item.getLote().getId());
        validarExecucao(plano, item, lote, request);
        aplicarSaldo(plano.getTipo(), lote, request.quantidade(), request.volumeM3(), request.pesoKg());
        executarDominio(() -> item.registrarExecucao(
                request.quantidade(), request.volumeM3(), request.pesoKg(), request.posicaoOrigemReal(),
                request.posicaoDestinoReal(), request.divergencia(), request.codigoAvaria(), request.descricaoAvaria()));
        aplicarCustodia(plano, lote, request.posicaoDestinoReal());
        lote.registrarMovimentacao(criarMovimentacao(plano, request, lote));
        bloquearAvaria(lote, request);
        plano.atualizarStatusExecucao();
        plano.registrarHistorico("EXECUCAO", request.usuario(), "Item " + item.getSequencia() + " executado.");
        ComandoPlanoOperacionalCarga comando = new ComandoPlanoOperacionalCarga();
        comando.setPlano(plano);
        comando.setCommandId(request.commandId());
        comando.setPayloadHash(hash);
        comando.setUsuario(request.usuario());
        comandoRepositorio.save(comando);
        loteRepositorio.save(lote);
        return mapear(planoRepositorio.save(plano));
    }

    @Transactional
    public PlanoResposta concluir(UUID id, ConcluirPlanoRequest request) {
        PlanoOperacionalCarga plano = buscarPlano(id);
        executarDominio(() -> plano.concluir(request.aceitarDivergencia(), request.usuario(), request.observacao()));
        return mapear(planoRepositorio.save(plano));
    }

    @Transactional
    public PlanoResposta cancelar(UUID id, CancelarPlanoRequest request) {
        PlanoOperacionalCarga plano = buscarPlano(id);
        if (plano.getStatus() == StatusPlanoOperacional.EM_EXECUCAO
                || plano.getStatus() == StatusPlanoOperacional.PARCIAL) {
            compensar(plano, request.usuario());
        }
        executarDominio(() -> plano.cancelar(request.motivo(), request.usuario()));
        return mapear(planoRepositorio.save(plano));
    }

    private PlanoOperacionalCarga criarCabecalho(CriarPlanoRequest request) {
        PlanoOperacionalCarga plano = new PlanoOperacionalCarga();
        plano.setNumero(request.numero());
        plano.setTipo(request.tipo());
        plano.setPrioridade(request.prioridade());
        plano.setJanelaInicio(request.janelaInicio());
        plano.setJanelaFim(request.janelaFim());
        plano.setLocal(request.local());
        plano.setOrigemTipo(request.origemTipo());
        plano.setOrigemId(request.origemId());
        plano.setDestinoTipo(request.destinoTipo());
        plano.setDestinoId(request.destinoId());
        plano.setVisitaNavioId(request.visitaNavioId());
        plano.setVisitaFerroviariaId(request.visitaFerroviariaId());
        plano.setLacreOrigem(request.lacreOrigem());
        plano.setLacreDestino(request.lacreDestino());
        plano.setRestricoes(request.restricoes());
        plano.setInstrucaoTrabalho(request.instrucaoTrabalho());
        if (!vazio(request.equipeId()) || !vazio(request.equipamentoId())) {
            plano.atribuirRecursos(request.equipeId(), request.equipamentoId(), request.usuario());
        }
        return plano;
    }

    private PlanoOperacionalCarga copiarCabecalho(PlanoOperacionalCarga origem, String numero) {
        PlanoOperacionalCarga novo = new PlanoOperacionalCarga();
        novo.setNumero(numero);
        novo.setTipo(origem.getTipo());
        novo.setPrioridade(origem.getPrioridade());
        novo.setVersaoPlano(origem.getVersaoPlano() + 1);
        novo.setPlanoOrigemId(origem.getPlanoOrigemId() == null ? origem.getId() : origem.getPlanoOrigemId());
        novo.setJanelaInicio(origem.getJanelaInicio());
        novo.setJanelaFim(origem.getJanelaFim());
        novo.setLocal(origem.getLocal());
        novo.setOrigemTipo(origem.getOrigemTipo());
        novo.setOrigemId(origem.getOrigemId());
        novo.setDestinoTipo(origem.getDestinoTipo());
        novo.setDestinoId(origem.getDestinoId());
        novo.setVisitaNavioId(origem.getVisitaNavioId());
        novo.setVisitaFerroviariaId(origem.getVisitaFerroviariaId());
        novo.setLacreOrigem(origem.getLacreOrigem());
        novo.setLacreDestino(origem.getLacreDestino());
        novo.setRestricoes(origem.getRestricoes());
        novo.setInstrucaoTrabalho(origem.getInstrucaoTrabalho());
        return novo;
    }

    private ItemPlanoOperacionalCarga criarItem(CriarItemPlanoRequest request, LoteCarga lote) {
        ItemPlanoOperacionalCarga item = new ItemPlanoOperacionalCarga();
        item.setLote(lote);
        item.setSequencia(request.sequencia());
        item.setQuantidadePlanejada(request.quantidadePlanejada());
        item.setVolumePlanejadoM3(request.volumePlanejadoM3());
        item.setPesoPlanejadoKg(request.pesoPlanejadoKg());
        item.setPosicaoPlanejada(request.posicaoPlanejada());
        item.setAreaPorao(request.areaPorao());
        item.setVagaoId(request.vagaoId());
        item.setPosicaoVagao(request.posicaoVagao());
        item.setCapacidadeReservadaKg(request.capacidadeReservadaKg());
        return item;
    }

    private void validarContrato(CriarPlanoRequest request) {
        if (!request.janelaFim().isAfter(request.janelaInicio())) {
            throw invalido("A janela operacional é inválida.");
        }
        if ((request.tipo() == TipoServicoOrdemCarga.CARGA_NAVIO
                || request.tipo() == TipoServicoOrdemCarga.DESCARGA_NAVIO) && vazio(request.visitaNavioId())) {
            throw invalido("A operação marítima exige visita de navio.");
        }
        if ((request.tipo() == TipoServicoOrdemCarga.CARGA_TREM
                || request.tipo() == TipoServicoOrdemCarga.DESCARGA_TREM) && vazio(request.visitaFerroviariaId())) {
            throw invalido("A operação ferroviária exige visita ferroviária.");
        }
    }

    private void validarItemEspecifico(TipoServicoOrdemCarga tipo, CriarItemPlanoRequest request) {
        if ((tipo == TipoServicoOrdemCarga.CARGA_NAVIO || tipo == TipoServicoOrdemCarga.DESCARGA_NAVIO)
                && vazio(request.areaPorao())) {
            throw invalido("A operação marítima exige área ou porão por item.");
        }
        if ((tipo == TipoServicoOrdemCarga.CARGA_TREM || tipo == TipoServicoOrdemCarga.DESCARGA_TREM)
                && (vazio(request.vagaoId()) || vazio(request.posicaoVagao()))) {
            throw invalido("A operação ferroviária exige vagão e posição por item.");
        }
    }

    private void validarLiberacao(PlanoOperacionalCarga plano) {
        if (plano.getJanelaFim().isBefore(OffsetDateTime.now())) {
            throw conflito("A janela operacional já foi encerrada.");
        }
        for (ItemPlanoOperacionalCarga item : plano.getItens()) {
            LoteCarga lote = buscarLote(item.getLote().getId());
            if (SAIDAS.contains(plano.getTipo())) {
                validarDisponibilidade(lote, item.getQuantidadePlanejada(), item.getVolumePlanejadoM3(), item.getPesoPlanejadoKg());
            } else if (ENTRADAS.contains(plano.getTipo())) {
                validarCapacidade(lote, item.getQuantidadePlanejada(), item.getVolumePlanejadoM3(), item.getPesoPlanejadoKg());
            }
        }
    }

    private void validarExecucao(PlanoOperacionalCarga plano, ItemPlanoOperacionalCarga item,
            LoteCarga lote, ExecutarItemPlanoRequest request) {
        if (plano.getStatus() != StatusPlanoOperacional.EM_EXECUCAO
                && plano.getStatus() != StatusPlanoOperacional.PARCIAL) {
            throw conflito("O plano deve estar em execução.");
        }
        if (item.getQuantidadeRealizada().add(request.quantidade()).compareTo(item.getQuantidadePlanejada()) > 0
                || item.getVolumeRealizadoM3().add(request.volumeM3()).compareTo(item.getVolumePlanejadoM3()) > 0
                || item.getPesoRealizadoKg().add(request.pesoKg()).compareTo(item.getPesoPlanejadoKg()) > 0) {
            throw conflito("A execução excede o planejado.");
        }
        if (SAIDAS.contains(plano.getTipo())) {
            validarDisponibilidade(lote, request.quantidade(), request.volumeM3(), request.pesoKg());
        } else if (ENTRADAS.contains(plano.getTipo())) {
            validarCapacidade(lote, request.quantidade(), request.volumeM3(), request.pesoKg());
        }
    }

    private void aplicarSaldo(TipoServicoOrdemCarga tipo, LoteCarga lote,
            BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        if (SAIDAS.contains(tipo)) {
            executarDominio(() -> lote.retirarSaldo(quantidade, volume, peso));
        } else if (ENTRADAS.contains(tipo)) {
            lote.adicionarSaldo(quantidade, volume, peso);
        }
    }

    private void validarDisponibilidade(LoteCarga lote, BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        if (quantidade.compareTo(lote.getQuantidadeSaldo()) > 0
                || volume.compareTo(lote.getVolumeSaldoM3()) > 0
                || peso.compareTo(lote.getPesoSaldoKg()) > 0) {
            throw conflito("Saldo insuficiente no cargo lot " + lote.getCodigo() + ".");
        }
    }

    private void validarCapacidade(LoteCarga lote, BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        if (quantidade.compareTo(lote.getQuantidadePrevista().subtract(lote.getQuantidadeSaldo())) > 0
                || volume.compareTo(lote.getVolumePrevistoM3().subtract(lote.getVolumeSaldoM3())) > 0
                || peso.compareTo(lote.getPesoPrevistoKg().subtract(lote.getPesoSaldoKg())) > 0) {
            throw conflito("Capacidade insuficiente no cargo lot " + lote.getCodigo() + ".");
        }
    }

    private void bloquearAvaria(LoteCarga lote, ExecutarItemPlanoRequest request) {
        BigDecimal quantidade = valor(request.quantidadeAvariada());
        BigDecimal volume = valor(request.volumeAvariadoM3());
        BigDecimal peso = valor(request.pesoAvariadoKg());
        if (quantidade.signum() == 0 && volume.signum() == 0 && peso.signum() == 0) {
            return;
        }
        if (vazio(request.codigoAvaria())) {
            throw invalido("Informe o código da avaria para bloquear saldo.");
        }
        validarDisponibilidade(lote, quantidade, volume, peso);
        executarDominio(() -> lote.bloquearSaldo(quantidade, volume, peso));
        lote.setCodigoAvaria(request.codigoAvaria());
        lote.setDescricaoAvaria(request.descricaoAvaria());
    }

    private void aplicarCustodia(PlanoOperacionalCarga plano, LoteCarga lote, String posicaoDestinoReal) {
        TipoUnidadeIntermodal tipo = plano.getDestinoTipo();
        if (tipo == null) {
            return;
        }
        String armazem = lote.getArmazemId();
        String posicao = lote.getPosicaoArmazenagem();
        String veiculo = lote.getVeiculoId();
        String visita = lote.getVisitaNavioId();
        String cliente = lote.getClienteId();
        if (tipo == TipoUnidadeIntermodal.ARMAZEM || tipo == TipoUnidadeIntermodal.PATIO) {
            armazem = plano.getDestinoId();
            posicao = vazio(posicaoDestinoReal) ? plano.getLocal() : posicaoDestinoReal;
            veiculo = null;
            visita = null;
        } else if (tipo == TipoUnidadeIntermodal.CAMINHAO || tipo == TipoUnidadeIntermodal.VAGAO) {
            veiculo = plano.getDestinoId();
        } else if (tipo == TipoUnidadeIntermodal.NAVIO) {
            visita = plano.getVisitaNavioId();
        } else if (tipo == TipoUnidadeIntermodal.CLIENTE) {
            cliente = plano.getDestinoId();
        }
        lote.atualizarLocalizacao(armazem, posicao, veiculo, visita, cliente);
    }

    private MovimentacaoCarga criarMovimentacao(PlanoOperacionalCarga plano,
            ExecutarItemPlanoRequest request, LoteCarga lote) {
        MovimentacaoCarga movimento = new MovimentacaoCarga();
        movimento.setTipo(mapearTipo(plano.getTipo()));
        movimento.setQuantidade(request.quantidade());
        movimento.setVolumeM3(request.volumeM3());
        movimento.setPesoKg(request.pesoKg());
        movimento.setOrigemTipo(plano.getOrigemTipo() == null ? "CARGO_LOT" : plano.getOrigemTipo().name());
        movimento.setOrigemId(vazio(plano.getOrigemId()) ? lote.getId().toString() : plano.getOrigemId());
        movimento.setDestinoTipo(plano.getDestinoTipo() == null ? "CARGO_LOT" : plano.getDestinoTipo().name());
        movimento.setDestinoId(vazio(plano.getDestinoId()) ? lote.getId().toString() : plano.getDestinoId());
        movimento.setArmazemId(lote.getArmazemId());
        movimento.setVeiculoId(lote.getVeiculoId());
        movimento.setVisitaNavioId(plano.getVisitaNavioId());
        movimento.setClienteId(lote.getClienteId());
        movimento.setUsuario(request.usuario());
        movimento.setCorrelationId(request.commandId());
        movimento.setObservacao("Plano " + plano.getNumero() + "; visita ferroviária "
                + plano.getVisitaFerroviariaId() + ".");
        return movimento;
    }

    private TipoMovimentacaoCarga mapearTipo(TipoServicoOrdemCarga tipo) {
        return switch (tipo) {
            case RECEBIMENTO -> TipoMovimentacaoCarga.RECEBIMENTO;
            case ENTREGA -> TipoMovimentacaoCarga.ENTREGA;
            case STUFF -> TipoMovimentacaoCarga.CONSOLIDACAO;
            case UNSTUFF -> TipoMovimentacaoCarga.DESCONSOLIDACAO;
            case CARGA_NAVIO, CARGA_TREM -> TipoMovimentacaoCarga.CARGA_PARCIAL;
            case DESCARGA_NAVIO, DESCARGA_TREM -> TipoMovimentacaoCarga.DESCARGA_PARCIAL;
            case MOVIMENTACAO_INTERNA, TRANSLOAD -> TipoMovimentacaoCarga.TRANSFERENCIA;
            case INSPECAO -> TipoMovimentacaoCarga.ARMAZENAGEM;
            case INVENTARIO -> TipoMovimentacaoCarga.AJUSTE_INVENTARIO;
        };
    }

    private void compensar(PlanoOperacionalCarga plano, String usuario) {
        for (ItemPlanoOperacionalCarga item : plano.getItens()) {
            if (!item.possuiExecucao()) {
                continue;
            }
            LoteCarga lote = buscarLote(item.getLote().getId());
            if (SAIDAS.contains(plano.getTipo())) {
                lote.adicionarSaldo(item.getQuantidadeRealizada(), item.getVolumeRealizadoM3(), item.getPesoRealizadoKg());
            } else if (ENTRADAS.contains(plano.getTipo())) {
                executarDominio(() -> lote.retirarSaldo(
                        item.getQuantidadeRealizada(), item.getVolumeRealizadoM3(), item.getPesoRealizadoKg()));
            }
            MovimentacaoCarga movimento = new MovimentacaoCarga();
            movimento.setTipo(TipoMovimentacaoCarga.TRANSFERENCIA);
            movimento.setQuantidade(item.getQuantidadeRealizada());
            movimento.setVolumeM3(item.getVolumeRealizadoM3());
            movimento.setPesoKg(item.getPesoRealizadoKg());
            movimento.setOrigemTipo("COMPENSACAO");
            movimento.setOrigemId(plano.getId().toString());
            movimento.setDestinoTipo("CARGO_LOT");
            movimento.setDestinoId(lote.getId().toString());
            movimento.setArmazemId(lote.getArmazemId());
            movimento.setUsuario(usuario);
            movimento.setCorrelationId("COMP-" + plano.getId() + "-" + item.getId());
            movimento.setObservacao("Compensação do plano " + plano.getNumero() + ".");
            lote.registrarMovimentacao(movimento);
            loteRepositorio.save(lote);
        }
        plano.registrarHistorico("COMPENSADO", usuario, "Movimentações revertidas antes do cancelamento.");
    }

    private String assinatura(ExecutarItemPlanoRequest request) {
        String payload = request.itemId() + "|" + request.quantidade() + "|" + request.volumeM3() + "|"
                + request.pesoKg() + "|" + request.posicaoOrigemReal() + "|" + request.posicaoDestinoReal()
                + "|" + request.divergencia() + "|" + request.codigoAvaria() + "|" + request.descricaoAvaria()
                + "|" + request.quantidadeAvariada() + "|" + request.volumeAvariadoM3() + "|" + request.pesoAvariadoKg();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    private PlanoOperacionalCarga buscarPlano(UUID id) {
        return planoRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrado("Plano operacional não encontrado."));
    }

    private LoteCarga buscarLote(UUID id) {
        return loteRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrado("Cargo lot não encontrado."));
    }

    private PlanoResposta mapear(PlanoOperacionalCarga plano) {
        return new PlanoResposta(plano.getId(), plano.getNumero(), plano.getTipo(), plano.getStatus(),
                plano.getPrioridade(), plano.getVersaoPlano(), plano.getPlanoOrigemId(), plano.getJanelaInicio(),
                plano.getJanelaFim(), plano.getLocal(), plano.getOrigemTipo(), plano.getOrigemId(),
                plano.getDestinoTipo(), plano.getDestinoId(), plano.getVisitaNavioId(), plano.getVisitaFerroviariaId(),
                plano.getEquipeId(), plano.getEquipamentoId(), plano.getLacreOrigem(), plano.getLacreDestino(),
                plano.getRestricoes(), plano.getInstrucaoTrabalho(), plano.getMotivoCancelamento(),
                plano.getHistoricoOperacional(), plano.getCriadoEm(), plano.getLiberadoEm(), plano.getIniciadoEm(),
                plano.getConcluidoEm(), plano.getItens().stream().map(this::mapearItem).collect(Collectors.toList()));
    }

    private ItemPlanoResposta mapearItem(ItemPlanoOperacionalCarga item) {
        return new ItemPlanoResposta(item.getId(), item.getLote().getId(), item.getLote().getCodigo(), item.getSequencia(),
                item.getQuantidadePlanejada(), item.getQuantidadeRealizada(), item.getVolumePlanejadoM3(),
                item.getVolumeRealizadoM3(), item.getPesoPlanejadoKg(), item.getPesoRealizadoKg(),
                item.getPosicaoPlanejada(), item.getPosicaoOrigemReal(), item.getPosicaoDestinoReal(), item.getAreaPorao(),
                item.getVagaoId(), item.getPosicaoVagao(), item.getCapacidadeReservadaKg(), item.getDivergencia(),
                item.getCodigoAvaria(), item.getDescricaoAvaria());
    }

    private BigDecimal valor(BigDecimal numero) { return numero == null ? BigDecimal.ZERO : numero; }
    private boolean vazio(String valor) { return valor == null || valor.isBlank(); }

    private void executarDominio(Runnable comando) {
        try {
            comando.run();
        } catch (IllegalArgumentException exception) {
            throw invalido(exception.getMessage());
        } catch (IllegalStateException exception) {
            throw conflito(exception.getMessage());
        }
    }

    private ResponseStatusException invalido(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private ResponseStatusException naoEncontrado(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }
}
