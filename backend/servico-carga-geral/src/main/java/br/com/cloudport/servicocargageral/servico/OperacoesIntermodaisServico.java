package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.AlocacaoCargoLot;
import br.com.cloudport.servicocargageral.dominio.AvariaCarga;
import br.com.cloudport.servicocargageral.dominio.AvariaCarga.Evidencia;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoMovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.InventarioFisicoCargoLot;
import br.com.cloudport.servicocargageral.dominio.InventarioFisicoCargoLot.Contagem;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dominio.MovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.OperacaoTransload;
import br.com.cloudport.servicocargageral.dominio.OperacaoTransload.ItemTransload;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.EstagioGateCarga;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.ModalTransporteCargo;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusAvariaCarga;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusDivergenciaInventarioCargo;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusReservaGateCarga;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.TipoMovimentoGateCarga;
import br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.TipoOperacaoTransporteCargo;
import br.com.cloudport.servicocargageral.dominio.PlanoTransporteCargoLot;
import br.com.cloudport.servicocargageral.dominio.ReservaGateCarga;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.AdicionarEvidenciaAvariaRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.AbrirAvariaRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.AbrirInventarioFisicoRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.AlocacaoResposta;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.AvariaResposta;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ComandoMotivadoRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.CompensarGateCargaRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ConfirmarGateCargaRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ContagemResposta;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.CriarAlocacaoRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.EvidenciaAvariaResposta;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ExecutarTransloadRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ExecutarTransporteRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.InventarioFisicoResposta;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ItemTransloadRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ItemTransloadResposta;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.PlanejarTransporteRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.PlanoTransporteResposta;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.RegistrarContagemRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ReservaGateCargaResposta;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ReservarGateCargaRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.ResolverDivergenciaRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.TransicionarAvariaRequest;
import br.com.cloudport.servicocargageral.dto.OperacoesIntermodaisDTOs.TransloadResposta;
import br.com.cloudport.servicocargageral.integracao.inventario.InventarioConteinerCliente;
import br.com.cloudport.servicocargageral.integracao.yard.CapacidadeCargoLotCliente;
import br.com.cloudport.servicocargageral.integracao.yard.CapacidadeCargoLotCliente.ReservaCapacidadeResposta;
import br.com.cloudport.servicocargageral.repositorio.AlocacaoCargoLotRepositorio;
import br.com.cloudport.servicocargageral.repositorio.AvariaCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.InventarioFisicoCargoLotRepositorio;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.OperacaoTransloadRepositorio;
import br.com.cloudport.servicocargageral.repositorio.PlanoTransporteCargoLotRepositorio;
import br.com.cloudport.servicocargageral.repositorio.ReservaGateCargaRepositorio;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperacoesIntermodaisServico {

    private final LoteCargaRepositorio loteRepositorio;
    private final OperacaoTransloadRepositorio transloadRepositorio;
    private final ReservaGateCargaRepositorio reservaGateRepositorio;
    private final AlocacaoCargoLotRepositorio alocacaoRepositorio;
    private final PlanoTransporteCargoLotRepositorio planoTransporteRepositorio;
    private final AvariaCargaRepositorio avariaRepositorio;
    private final InventarioFisicoCargoLotRepositorio inventarioRepositorio;
    private final InventarioConteinerCliente inventarioConteinerCliente;
    private final CapacidadeCargoLotCliente capacidadeCargoLotCliente;

    public OperacoesIntermodaisServico(
            LoteCargaRepositorio loteRepositorio,
            OperacaoTransloadRepositorio transloadRepositorio,
            ReservaGateCargaRepositorio reservaGateRepositorio,
            AlocacaoCargoLotRepositorio alocacaoRepositorio,
            PlanoTransporteCargoLotRepositorio planoTransporteRepositorio,
            AvariaCargaRepositorio avariaRepositorio,
            InventarioFisicoCargoLotRepositorio inventarioRepositorio,
            InventarioConteinerCliente inventarioConteinerCliente,
            CapacidadeCargoLotCliente capacidadeCargoLotCliente) {
        this.loteRepositorio = loteRepositorio;
        this.transloadRepositorio = transloadRepositorio;
        this.reservaGateRepositorio = reservaGateRepositorio;
        this.alocacaoRepositorio = alocacaoRepositorio;
        this.planoTransporteRepositorio = planoTransporteRepositorio;
        this.avariaRepositorio = avariaRepositorio;
        this.inventarioRepositorio = inventarioRepositorio;
        this.inventarioConteinerCliente = inventarioConteinerCliente;
        this.capacidadeCargoLotCliente = capacidadeCargoLotCliente;
    }

    @Transactional
    public TransloadResposta executarTransload(ExecutarTransloadRequest request) {
        OperacaoTransload existente = transloadRepositorio.findByCommandId(request.commandId()).orElse(null);
        if (existente != null) return mapearTransload(existente);
        if (request.unidadeOrigem().equalsIgnoreCase(request.unidadeDestino())) {
            throw conflito("Unidades de origem e destino do transload devem ser diferentes.");
        }

        UUID reservaOrigemId = UUID.randomUUID();
        UUID reservaDestinoId = UUID.randomUUID();
        inventarioConteinerCliente.reservar(request.unidadeOrigem(), reservaOrigemId, request.usuario());
        inventarioConteinerCliente.reservar(request.unidadeDestino(), reservaDestinoId, request.usuario());

        Map<UUID, LoteCarga> lotes = bloquearLotesTransload(request.itens());
        for (ItemTransloadRequest item : request.itens()) {
            if (item.loteOrigemId().equals(item.loteDestinoId())) {
                throw conflito("Transload exige lotes distintos para origem e destino.");
            }
            LoteCarga origem = lotes.get(item.loteOrigemId());
            LoteCarga destino = lotes.get(item.loteDestinoId());
            validarCapacidadeDestino(destino, item.quantidade(), item.volumeM3(), item.pesoKg());
            executarEstado(() -> origem.retirarSaldo(item.quantidade(), item.volumeM3(), item.pesoKg()));
            destino.adicionarSaldo(item.quantidade(), item.volumeM3(), item.pesoKg());
            registrarMovimentacao(
                    origem,
                    TipoMovimentacaoCarga.CARGA_PARCIAL,
                    item.quantidade(),
                    item.volumeM3(),
                    item.pesoKg(),
                    "CONTEINER",
                    request.unidadeOrigem(),
                    "CONTEINER",
                    request.unidadeDestino(),
                    request.usuario(),
                    request.correlationId(),
                    "Saída no transload " + request.commandId());
            registrarMovimentacao(
                    destino,
                    TipoMovimentacaoCarga.DESCARGA_PARCIAL,
                    item.quantidade(),
                    item.volumeM3(),
                    item.pesoKg(),
                    "CONTEINER",
                    request.unidadeOrigem(),
                    "CONTEINER",
                    request.unidadeDestino(),
                    request.usuario(),
                    request.correlationId(),
                    "Entrada no transload " + request.commandId());
        }
        loteRepositorio.saveAll(lotes.values());

        OperacaoTransload operacao = new OperacaoTransload();
        operacao.setCommandId(request.commandId());
        operacao.setUnidadeOrigem(request.unidadeOrigem());
        operacao.setUnidadeDestino(request.unidadeDestino());
        operacao.setReservaOrigemId(reservaOrigemId);
        operacao.setReservaDestinoId(reservaDestinoId);
        operacao.setLacreOrigem(request.lacreOrigem());
        operacao.setLacreDestino(request.lacreDestino());
        operacao.setDivergencia(request.divergencia());
        operacao.setCodigoAvaria(request.codigoAvaria());
        operacao.setDescricaoAvaria(request.descricaoAvaria());
        operacao.setUsuario(request.usuario());
        operacao.setCorrelationId(request.correlationId());
        request.itens().forEach(item -> operacao.adicionarItem(
                item.loteOrigemId(), item.loteDestinoId(), item.quantidade(), item.volumeM3(), item.pesoKg()));
        OperacaoTransload salva = transloadRepositorio.save(operacao);
        inventarioConteinerCliente.liberar(reservaOrigemId, request.usuario(), "Transload concluído", "CONCLUIDA");
        inventarioConteinerCliente.liberar(reservaDestinoId, request.usuario(), "Transload concluído", "CONCLUIDA");
        return mapearTransload(salva);
    }

    @Transactional
    public ReservaGateCargaResposta reservarGate(ReservarGateCargaRequest request) {
        ReservaGateCarga existente = reservaGateRepositorio.findByCommandIdReserva(request.commandId()).orElse(null);
        if (existente != null) return mapearReservaGate(existente);
        LoteCarga lote = buscarLote(request.loteId());
        String numeroBl = lote.getItem().getConhecimento().getNumero();
        if (!numeroBl.equalsIgnoreCase(request.blNumero())) {
            throw conflito("Cargo lot não pertence ao Bill of Lading informado.");
        }
        EstagioGateCarga estagioEsperado = request.tipoMovimento() == TipoMovimentoGateCarga.RETIRADA
                ? EstagioGateCarga.SAIDA
                : EstagioGateCarga.ENTRADA;
        if (request.estagioConfirmacao() != estagioEsperado) {
            throw conflito("Estágio físico incompatível com o tipo de movimento do Gate.");
        }
        validarReservaGate(lote, request);
        ReservaGateCarga reserva = new ReservaGateCarga();
        reserva.setCommandIdReserva(request.commandId());
        reserva.setAgendamentoCodigo(request.agendamentoCodigo());
        reserva.setBlNumero(request.blNumero());
        reserva.setDeliveryOrder(request.deliveryOrder());
        reserva.setLoteId(request.loteId());
        reserva.setTipoMovimento(request.tipoMovimento());
        reserva.setEstagioConfirmacao(request.estagioConfirmacao());
        reserva.setQuantidade(request.quantidade());
        reserva.setVolumeM3(request.volumeM3());
        reserva.setPesoKg(request.pesoKg());
        reserva.setUsuarioReserva(request.usuario());
        return mapearReservaGate(reservaGateRepositorio.save(reserva));
    }

    @Transactional
    public ReservaGateCargaResposta confirmarGate(UUID reservaId, ConfirmarGateCargaRequest request) {
        ReservaGateCarga reserva = buscarReservaGate(reservaId);
        if (reserva.getStatus() == StatusReservaGateCarga.CONFIRMADA
                && request.commandId().equals(reserva.getCommandIdConfirmacao())) {
            return mapearReservaGate(reserva);
        }
        LoteCarga lote = buscarLote(reserva.getLoteId());
        if (reserva.getTipoMovimento() == TipoMovimentoGateCarga.RETIRADA) {
            executarEstado(() -> lote.retirarSaldo(reserva.getQuantidade(), reserva.getVolumeM3(), reserva.getPesoKg()));
            registrarMovimentacao(lote, TipoMovimentacaoCarga.ENTREGA,
                    reserva.getQuantidade(), reserva.getVolumeM3(), reserva.getPesoKg(),
                    "CARGO_LOT", lote.getCodigo(), "GATE", reserva.getAgendamentoCodigo(),
                    request.usuario(), request.commandId().toString(), "Retirada parcial confirmada pelo Gate");
        } else {
            validarCapacidadeDestino(lote, reserva.getQuantidade(), reserva.getVolumeM3(), reserva.getPesoKg());
            lote.adicionarSaldo(reserva.getQuantidade(), reserva.getVolumeM3(), reserva.getPesoKg());
            registrarMovimentacao(lote, TipoMovimentacaoCarga.RECEBIMENTO,
                    reserva.getQuantidade(), reserva.getVolumeM3(), reserva.getPesoKg(),
                    "GATE", reserva.getAgendamentoCodigo(), "CARGO_LOT", lote.getCodigo(),
                    request.usuario(), request.commandId().toString(), "Entrega parcial confirmada pelo Gate");
        }
        executarEstado(() -> reserva.confirmar(request.commandId(), request.usuario(), request.estagio()));
        loteRepositorio.save(lote);
        return mapearReservaGate(reservaGateRepositorio.save(reserva));
    }

    @Transactional
    public ReservaGateCargaResposta compensarGate(UUID reservaId, CompensarGateCargaRequest request) {
        ReservaGateCarga reserva = buscarReservaGate(reservaId);
        if (reserva.getStatus() == StatusReservaGateCarga.COMPENSADA
                && request.commandId().equals(reserva.getCommandIdCompensacao())) {
            return mapearReservaGate(reserva);
        }
        if (reserva.getStatus() == StatusReservaGateCarga.CONFIRMADA) {
            LoteCarga lote = buscarLote(reserva.getLoteId());
            if (reserva.getTipoMovimento() == TipoMovimentoGateCarga.RETIRADA) {
                lote.adicionarSaldo(reserva.getQuantidade(), reserva.getVolumeM3(), reserva.getPesoKg());
            } else {
                executarEstado(() -> lote.retirarSaldo(
                        reserva.getQuantidade(), reserva.getVolumeM3(), reserva.getPesoKg()));
            }
            registrarMovimentacao(lote, TipoMovimentacaoCarga.AJUSTE_INVENTARIO,
                    reserva.getQuantidade(), reserva.getVolumeM3(), reserva.getPesoKg(),
                    "GATE", reserva.getAgendamentoCodigo(), "COMPENSACAO", reserva.getId().toString(),
                    request.usuario(), request.commandId().toString(), request.motivo());
            loteRepositorio.save(lote);
        }
        executarEstado(() -> reserva.compensar(request.commandId(), request.motivo()));
        return mapearReservaGate(reservaGateRepositorio.save(reserva));
    }

    @Transactional(readOnly = true)
    public List<ReservaGateCargaResposta> listarReservasGate(String agendamentoCodigo) {
        return reservaGateRepositorio.findByAgendamentoCodigoOrderByReservadoEmDesc(agendamentoCodigo).stream()
                .map(this::mapearReservaGate)
                .toList();
    }

    @Transactional
    public AlocacaoResposta criarAlocacao(CriarAlocacaoRequest request) {
        AlocacaoCargoLot existente = alocacaoRepositorio.findByCommandId(request.commandId()).orElse(null);
        if (existente != null) return mapearAlocacao(existente);
        LoteCarga lote = buscarLote(request.loteId());
        validarSaldoDisponivel(lote, request.quantidade(), request.volumeM3(), request.pesoKg());
        ReservaCapacidadeResposta capacidade = capacidadeCargoLotCliente.reservar(
                request.destino(), request.commandId(), request.loteId(), request.quantidade(),
                request.volumeM3(), request.pesoKg(), request.usuario());
        AlocacaoCargoLot alocacao = new AlocacaoCargoLot();
        alocacao.setCommandId(request.commandId());
        alocacao.setLoteId(request.loteId());
        alocacao.setReservaCapacidadeId(capacidade.id());
        alocacao.setOrigem(request.origem() == null ? lote.getPosicaoArmazenagem() : request.origem());
        alocacao.setDestino(request.destino());
        alocacao.setRecurso(request.recurso());
        alocacao.setPrioridade(request.prioridade());
        alocacao.setRestricoes(request.restricoes());
        alocacao.setQuantidade(request.quantidade());
        alocacao.setVolumeM3(request.volumeM3());
        alocacao.setPesoKg(request.pesoKg());
        alocacao.setUsuario(request.usuario());
        return mapearAlocacao(alocacaoRepositorio.save(alocacao));
    }

    @Transactional
    public AlocacaoResposta confirmarAlocacao(UUID id, ComandoMotivadoRequest request) {
        AlocacaoCargoLot alocacao = buscarAlocacao(id);
        if (alocacao.getStatus() == br.com.cloudport.servicocargageral.dominio.OperacoesIntermodaisTipos.StatusAlocacaoCargoLot.CONFIRMADA) {
            return mapearAlocacao(alocacao);
        }
        LoteCarga lote = buscarLote(alocacao.getLoteId());
        capacidadeCargoLotCliente.confirmar(alocacao.getReservaCapacidadeId(), request.usuario(), request.motivo());
        lote.atualizarLocalizacao(null, alocacao.getDestino(), null, null, null);
        registrarMovimentacao(lote, TipoMovimentacaoCarga.TRANSFERENCIA,
                alocacao.getQuantidade(), alocacao.getVolumeM3(), alocacao.getPesoKg(),
                "POSICAO", alocacao.getOrigem(), "POSICAO", alocacao.getDestino(),
                request.usuario(), alocacao.getCommandId().toString(), request.motivo());
        executarEstado(alocacao::confirmar);
        loteRepositorio.save(lote);
        return mapearAlocacao(alocacaoRepositorio.save(alocacao));
    }

    @Transactional
    public AlocacaoResposta cancelarAlocacao(UUID id, ComandoMotivadoRequest request) {
        AlocacaoCargoLot alocacao = buscarAlocacao(id);
        capacidadeCargoLotCliente.cancelar(alocacao.getReservaCapacidadeId(), request.usuario(), request.motivo());
        executarEstado(() -> alocacao.cancelar(request.motivo()));
        return mapearAlocacao(alocacaoRepositorio.save(alocacao));
    }

    @Transactional(readOnly = true)
    public List<AlocacaoResposta> listarAlocacoes(UUID loteId) {
        return alocacaoRepositorio.findByLoteIdOrderByCriadoEmDesc(loteId).stream()
                .map(this::mapearAlocacao)
                .toList();
    }

    @Transactional
    public PlanoTransporteResposta planejarTransporte(PlanejarTransporteRequest request) {
        PlanoTransporteCargoLot existente = planoTransporteRepositorio
                .findByCommandIdPlanejamento(request.commandId()).orElse(null);
        if (existente != null) return mapearPlanoTransporte(existente);
        LoteCarga lote = buscarLote(request.loteId());
        if (!lote.getItem().getConhecimento().getNumero().equalsIgnoreCase(request.blNumero())) {
            throw conflito("Cargo lot não pertence ao Bill of Lading informado.");
        }
        if (request.capacidadePesoKg() != null
                && request.pesoPlanejadoKg().compareTo(request.capacidadePesoKg()) > 0) {
            throw conflito("Peso planejado excede a capacidade do compartimento ou vagão.");
        }
        if (request.tipoOperacao() == TipoOperacaoTransporteCargo.CARGA) {
            validarSaldoDisponivel(lote, request.quantidadePlanejada(), request.volumePlanejadoM3(), request.pesoPlanejadoKg());
        } else {
            validarCapacidadeDestino(lote, request.quantidadePlanejada(), request.volumePlanejadoM3(), request.pesoPlanejadoKg());
        }
        PlanoTransporteCargoLot plano = new PlanoTransporteCargoLot();
        plano.setCommandIdPlanejamento(request.commandId());
        plano.setModal(request.modal());
        plano.setTipoOperacao(request.tipoOperacao());
        plano.setVisitaId(request.visitaId());
        plano.setBlNumero(request.blNumero());
        plano.setLoteId(request.loteId());
        plano.setCompartimento(request.compartimento());
        plano.setPosicao(request.posicao());
        plano.setSequencia(request.sequencia());
        plano.setEquipamento(request.equipamento());
        plano.setCustodia(request.custodia());
        plano.setRestricoes(request.restricoes());
        plano.setCapacidadePesoKg(request.capacidadePesoKg());
        plano.setQuantidadePlanejada(request.quantidadePlanejada());
        plano.setVolumePlanejadoM3(request.volumePlanejadoM3());
        plano.setPesoPlanejadoKg(request.pesoPlanejadoKg());
        plano.setUsuarioPlanejamento(request.usuario());
        return mapearPlanoTransporte(planoTransporteRepositorio.save(plano));
    }

    @Transactional
    public PlanoTransporteResposta executarTransporte(UUID planoId, ExecutarTransporteRequest request) {
        PlanoTransporteCargoLot plano = buscarPlanoTransporte(planoId);
        if (request.commandId().equals(plano.getCommandIdExecucao())) return mapearPlanoTransporte(plano);
        BigDecimal restanteQuantidade = plano.getQuantidadePlanejada().subtract(plano.getQuantidadeRealizada());
        BigDecimal restanteVolume = plano.getVolumePlanejadoM3().subtract(plano.getVolumeRealizadoM3());
        BigDecimal restantePeso = plano.getPesoPlanejadoKg().subtract(plano.getPesoRealizadoKg());
        if (request.quantidade().compareTo(restanteQuantidade) != 0
                || request.volumeM3().compareTo(restanteVolume) != 0
                || request.pesoKg().compareTo(restantePeso) != 0) {
            throw conflito("A confirmação física deve conciliar integralmente o saldo restante do plano.");
        }
        LoteCarga lote = buscarLote(plano.getLoteId());
        if (plano.getTipoOperacao() == TipoOperacaoTransporteCargo.CARGA) {
            executarEstado(() -> lote.retirarSaldo(request.quantidade(), request.volumeM3(), request.pesoKg()));
            registrarMovimentacao(lote, TipoMovimentacaoCarga.CARGA_PARCIAL,
                    request.quantidade(), request.volumeM3(), request.pesoKg(),
                    "CARGO_LOT", lote.getCodigo(), plano.getModal().name(), plano.getVisitaId(),
                    request.usuario(), request.commandId().toString(), "Carga modal no " + plano.getCompartimento());
        } else {
            validarCapacidadeDestino(lote, request.quantidade(), request.volumeM3(), request.pesoKg());
            lote.adicionarSaldo(request.quantidade(), request.volumeM3(), request.pesoKg());
            registrarMovimentacao(lote, TipoMovimentacaoCarga.DESCARGA_PARCIAL,
                    request.quantidade(), request.volumeM3(), request.pesoKg(),
                    plano.getModal().name(), plano.getVisitaId(), "CARGO_LOT", lote.getCodigo(),
                    request.usuario(), request.commandId().toString(), "Descarga modal do " + plano.getCompartimento());
        }
        if (plano.getModal() == ModalTransporteCargo.NAVIO) {
            lote.atualizarLocalizacao(null, plano.getPosicao(), null, plano.getVisitaId(), null);
        } else {
            lote.atualizarLocalizacao(null, plano.getPosicao(), plano.getCompartimento(), null, null);
        }
        executarEstado(() -> plano.executar(
                request.commandId(), request.quantidade(), request.volumeM3(), request.pesoKg(), request.usuario()));
        loteRepositorio.save(lote);
        return mapearPlanoTransporte(planoTransporteRepositorio.save(plano));
    }

    @Transactional
    public PlanoTransporteResposta cancelarTransporte(UUID planoId, ComandoMotivadoRequest request) {
        PlanoTransporteCargoLot plano = buscarPlanoTransporte(planoId);
        executarEstado(() -> plano.cancelar(request.motivo()));
        return mapearPlanoTransporte(planoTransporteRepositorio.save(plano));
    }

    @Transactional(readOnly = true)
    public List<PlanoTransporteResposta> listarPlanosTransporte(ModalTransporteCargo modal, String visitaId) {
        return planoTransporteRepositorio.findByModalAndVisitaIdOrderBySequenciaAsc(modal, visitaId).stream()
                .map(this::mapearPlanoTransporte)
                .toList();
    }

    @Transactional
    public AvariaResposta abrirAvaria(AbrirAvariaRequest request) {
        AvariaCarga existente = avariaRepositorio.findByCommandId(request.commandId()).orElse(null);
        if (existente != null) return mapearAvaria(existente);
        LoteCarga lote = buscarLote(request.loteId());
        executarEstado(() -> lote.bloquearSaldo(
                request.quantidadeAfetada(), request.volumeAfetadoM3(), request.pesoAfetadoKg()));
        lote.setCodigoAvaria(request.codigo());
        lote.setDescricaoAvaria(request.descricao());
        AvariaCarga avaria = new AvariaCarga();
        avaria.setCommandId(request.commandId());
        avaria.setLoteId(request.loteId());
        avaria.setCodigo(request.codigo());
        avaria.setDescricao(request.descricao());
        avaria.setQuantidadeAfetada(request.quantidadeAfetada());
        avaria.setVolumeAfetadoM3(request.volumeAfetadoM3());
        avaria.setPesoAfetadoKg(request.pesoAfetadoKg());
        avaria.setResponsavel(request.responsavel());
        loteRepositorio.save(lote);
        return mapearAvaria(avariaRepositorio.save(avaria));
    }

    @Transactional
    public AvariaResposta adicionarEvidencia(UUID avariaId, AdicionarEvidenciaAvariaRequest request) {
        AvariaCarga avaria = buscarAvaria(avariaId);
        avaria.adicionarEvidencia(request.tipo(), request.uri(), request.checksum(), request.responsavel());
        return mapearAvaria(avariaRepositorio.save(avaria));
    }

    @Transactional
    public AvariaResposta transicionarAvaria(UUID avariaId, TransicionarAvariaRequest request) {
        AvariaCarga avaria = buscarAvaria(avariaId);
        LoteCarga lote = buscarLote(avaria.getLoteId());
        String acao = request.acao().trim().toUpperCase();
        switch (acao) {
            case "INSPECIONAR" -> executarEstado(() -> avaria.iniciarInspecao(request.usuario(), request.observacao()));
            case "REPARAR" -> executarEstado(() -> avaria.iniciarReparo(request.usuario(), request.observacao()));
            case "CONCLUIR_REPARO" -> {
                executarEstado(() -> avaria.concluirReparo(request.usuario(), request.observacao()));
                executarEstado(() -> lote.liberarSaldoBloqueado(
                        avaria.getQuantidadeAfetada(), avaria.getVolumeAfetadoM3(), avaria.getPesoAfetadoKg()));
            }
            case "BAIXAR" -> {
                executarEstado(() -> avaria.baixar(request.usuario(), request.observacao()));
                executarEstado(() -> lote.baixarSaldoBloqueado(
                        avaria.getQuantidadeAfetada(), avaria.getVolumeAfetadoM3(), avaria.getPesoAfetadoKg()));
                registrarMovimentacao(lote, TipoMovimentacaoCarga.AJUSTE_INVENTARIO,
                        avaria.getQuantidadeAfetada(), avaria.getVolumeAfetadoM3(), avaria.getPesoAfetadoKg(),
                        "SALDO_BLOQUEADO", avaria.getId().toString(), "BAIXA_AVARIA", avaria.getCodigo(),
                        request.usuario(), avaria.getCommandId().toString(), request.observacao());
            }
            default -> throw conflito("Ação de avaria inválida.");
        }
        loteRepositorio.save(lote);
        return mapearAvaria(avariaRepositorio.save(avaria));
    }

    @Transactional(readOnly = true)
    public List<AvariaResposta> listarAvarias(UUID loteId) {
        return avariaRepositorio.findByLoteIdOrderByCriadoEmDesc(loteId).stream()
                .map(this::mapearAvaria)
                .toList();
    }

    @Transactional
    public InventarioFisicoResposta abrirInventario(AbrirInventarioFisicoRequest request) {
        InventarioFisicoCargoLot existente = inventarioRepositorio
                .findByCommandIdAbertura(request.commandId()).orElse(null);
        if (existente != null) return mapearInventario(existente);
        InventarioFisicoCargoLot inventario = new InventarioFisicoCargoLot();
        inventario.setCommandIdAbertura(request.commandId());
        inventario.setPosicao(request.posicao());
        inventario.setAbertoPor(request.usuario());
        return mapearInventario(inventarioRepositorio.save(inventario));
    }

    @Transactional
    public InventarioFisicoResposta registrarContagem(UUID inventarioId, RegistrarContagemRequest request) {
        InventarioFisicoCargoLot inventario = buscarInventario(inventarioId);
        LoteCarga lote = buscarLote(request.loteId());
        if (!lote.getCodigo().equalsIgnoreCase(request.identificacao())) {
            throw conflito("Código de barras ou QR não corresponde ao cargo lot informado.");
        }
        inventario.registrarContagem(
                request.commandId(),
                request.loteId(),
                request.identificacao(),
                lote.getQuantidadeSaldo(),
                lote.getVolumeSaldoM3(),
                lote.getPesoSaldoKg(),
                request.quantidadeContada(),
                request.volumeContadoM3(),
                request.pesoContadoKg(),
                request.usuario(),
                request.observacao());
        return mapearInventario(inventarioRepositorio.save(inventario));
    }

    @Transactional
    public InventarioFisicoResposta resolverDivergencia(UUID inventarioId, ResolverDivergenciaRequest request) {
        InventarioFisicoCargoLot inventario = buscarInventario(inventarioId);
        Contagem contagem = inventario.getContagens().stream()
                .filter(item -> item.getCommandId().equals(request.commandIdContagem()))
                .findFirst()
                .orElseThrow(() -> naoEncontrada("Contagem do inventário não encontrada."));
        if (contagem.getStatusDivergencia() != StatusDivergenciaInventarioCargo.PENDENTE) {
            return mapearInventario(inventario);
        }
        if (request.ajustarSaldo()) {
            LoteCarga lote = buscarLote(contagem.getLoteId());
            aplicarAjusteInventario(lote, contagem, request.usuario(), request.motivo());
            loteRepositorio.save(lote);
        }
        executarEstado(() -> contagem.resolver(request.ajustarSaldo(), request.usuario(), request.motivo()));
        return mapearInventario(inventarioRepositorio.save(inventario));
    }

    @Transactional
    public InventarioFisicoResposta concluirInventario(UUID inventarioId, ComandoMotivadoRequest request) {
        InventarioFisicoCargoLot inventario = buscarInventario(inventarioId);
        executarEstado(() -> inventario.concluir(request.usuario(), request.motivo()));
        return mapearInventario(inventarioRepositorio.save(inventario));
    }

    @Transactional(readOnly = true)
    public List<InventarioFisicoResposta> listarInventarios() {
        return inventarioRepositorio.findAllByOrderByAbertoEmDesc().stream()
                .map(this::mapearInventario)
                .toList();
    }

    private Map<UUID, LoteCarga> bloquearLotesTransload(List<ItemTransloadRequest> itens) {
        List<UUID> ids = itens.stream()
                .flatMap(item -> List.of(item.loteOrigemId(), item.loteDestinoId()).stream())
                .distinct()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        Map<UUID, LoteCarga> lotes = new HashMap<>();
        ids.forEach(id -> lotes.put(id, buscarLote(id)));
        return lotes;
    }

    private void validarReservaGate(LoteCarga lote, ReservarGateCargaRequest request) {
        BigDecimal quantidadeReservada = reservaGateRepositorio.somarQuantidadeReservada(
                lote.getId(), request.tipoMovimento(), StatusReservaGateCarga.RESERVADA);
        BigDecimal volumeReservado = reservaGateRepositorio.somarVolumeReservado(
                lote.getId(), request.tipoMovimento(), StatusReservaGateCarga.RESERVADA);
        BigDecimal pesoReservado = reservaGateRepositorio.somarPesoReservado(
                lote.getId(), request.tipoMovimento(), StatusReservaGateCarga.RESERVADA);
        if (request.tipoMovimento() == TipoMovimentoGateCarga.RETIRADA) {
            if (quantidadeReservada.add(request.quantidade()).compareTo(lote.getQuantidadeDisponivel()) > 0
                    || volumeReservado.add(request.volumeM3()).compareTo(lote.getVolumeDisponivelM3()) > 0
                    || pesoReservado.add(request.pesoKg()).compareTo(lote.getPesoDisponivelKg()) > 0) {
                throw conflito("Quantidade disponível insuficiente para reserva do Gate.");
            }
        } else {
            BigDecimal capacidadeQuantidade = lote.getQuantidadePrevista().subtract(lote.getQuantidadeSaldo());
            BigDecimal capacidadeVolume = lote.getVolumePrevistoM3().subtract(lote.getVolumeSaldoM3());
            BigDecimal capacidadePeso = lote.getPesoPrevistoKg().subtract(lote.getPesoSaldoKg());
            if (quantidadeReservada.add(request.quantidade()).compareTo(capacidadeQuantidade) > 0
                    || volumeReservado.add(request.volumeM3()).compareTo(capacidadeVolume) > 0
                    || pesoReservado.add(request.pesoKg()).compareTo(capacidadePeso) > 0) {
                throw conflito("Capacidade insuficiente para reserva de entrega no Gate.");
            }
        }
    }

    private void validarSaldoDisponivel(LoteCarga lote, BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        if (quantidade.compareTo(lote.getQuantidadeDisponivel()) > 0
                || volume.compareTo(lote.getVolumeDisponivelM3()) > 0
                || peso.compareTo(lote.getPesoDisponivelKg()) > 0) {
            throw conflito("Saldo disponível insuficiente no cargo lot " + lote.getCodigo() + ".");
        }
    }

    private void validarCapacidadeDestino(LoteCarga lote, BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        if (lote.getQuantidadeSaldo().add(quantidade).compareTo(lote.getQuantidadePrevista()) > 0
                || lote.getVolumeSaldoM3().add(volume).compareTo(lote.getVolumePrevistoM3()) > 0
                || lote.getPesoSaldoKg().add(peso).compareTo(lote.getPesoPrevistoKg()) > 0) {
            throw conflito("Capacidade prevista excedida no cargo lot " + lote.getCodigo() + ".");
        }
    }

    private void aplicarAjusteInventario(LoteCarga lote, Contagem contagem, String usuario, String motivo) {
        BigDecimal deltaQuantidade = contagem.getQuantidadeContada().subtract(contagem.getQuantidadeLogica());
        BigDecimal deltaVolume = contagem.getVolumeContadoM3().subtract(contagem.getVolumeLogicoM3());
        BigDecimal deltaPeso = contagem.getPesoContadoKg().subtract(contagem.getPesoLogicoKg());
        BigDecimal adicionarQuantidade = deltaQuantidade.max(BigDecimal.ZERO);
        BigDecimal adicionarVolume = deltaVolume.max(BigDecimal.ZERO);
        BigDecimal adicionarPeso = deltaPeso.max(BigDecimal.ZERO);
        BigDecimal retirarQuantidade = deltaQuantidade.min(BigDecimal.ZERO).abs();
        BigDecimal retirarVolume = deltaVolume.min(BigDecimal.ZERO).abs();
        BigDecimal retirarPeso = deltaPeso.min(BigDecimal.ZERO).abs();
        if (adicionarQuantidade.signum() > 0 || adicionarVolume.signum() > 0 || adicionarPeso.signum() > 0) {
            lote.adicionarSaldo(adicionarQuantidade, adicionarVolume, adicionarPeso);
            registrarMovimentacao(lote, TipoMovimentacaoCarga.AJUSTE_INVENTARIO,
                    adicionarQuantidade, adicionarVolume, adicionarPeso,
                    "INVENTARIO_FISICO", contagem.getCommandId().toString(), "CARGO_LOT", lote.getCodigo(),
                    usuario, contagem.getCommandId().toString(), motivo);
        }
        if (retirarQuantidade.signum() > 0 || retirarVolume.signum() > 0 || retirarPeso.signum() > 0) {
            executarEstado(() -> lote.retirarSaldo(retirarQuantidade, retirarVolume, retirarPeso));
            registrarMovimentacao(lote, TipoMovimentacaoCarga.AJUSTE_INVENTARIO,
                    retirarQuantidade, retirarVolume, retirarPeso,
                    "CARGO_LOT", lote.getCodigo(), "INVENTARIO_FISICO", contagem.getCommandId().toString(),
                    usuario, contagem.getCommandId().toString(), motivo);
        }
    }

    private void registrarMovimentacao(
            LoteCarga lote,
            TipoMovimentacaoCarga tipo,
            BigDecimal quantidade,
            BigDecimal volume,
            BigDecimal peso,
            String origemTipo,
            String origemId,
            String destinoTipo,
            String destinoId,
            String usuario,
            String correlationId,
            String observacao) {
        MovimentacaoCarga movimentacao = new MovimentacaoCarga();
        movimentacao.setTipo(tipo);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setVolumeM3(volume);
        movimentacao.setPesoKg(peso);
        movimentacao.setOrigemTipo(origemTipo);
        movimentacao.setOrigemId(origemId);
        movimentacao.setDestinoTipo(destinoTipo);
        movimentacao.setDestinoId(destinoId);
        movimentacao.setUsuario(usuario);
        movimentacao.setCorrelationId(correlationId);
        movimentacao.setObservacao(observacao);
        lote.registrarMovimentacao(movimentacao);
    }

    private LoteCarga buscarLote(UUID id) {
        return loteRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Cargo lot não encontrado."));
    }

    private ReservaGateCarga buscarReservaGate(UUID id) {
        return reservaGateRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Reserva de carga do Gate não encontrada."));
    }

    private AlocacaoCargoLot buscarAlocacao(UUID id) {
        return alocacaoRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Allocation de cargo lot não encontrada."));
    }

    private PlanoTransporteCargoLot buscarPlanoTransporte(UUID id) {
        return planoTransporteRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Plano modal de cargo lot não encontrado."));
    }

    private AvariaCarga buscarAvaria(UUID id) {
        return avariaRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Avaria de carga não encontrada."));
    }

    private InventarioFisicoCargoLot buscarInventario(UUID id) {
        return inventarioRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Inventário físico não encontrado."));
    }

    private TransloadResposta mapearTransload(OperacaoTransload operacao) {
        List<ItemTransloadResposta> itens = operacao.getItens().stream()
                .map(item -> new ItemTransloadResposta(
                        item.getLoteOrigemId(), item.getLoteDestinoId(),
                        item.getQuantidade(), item.getVolumeM3(), item.getPesoKg()))
                .toList();
        return new TransloadResposta(
                operacao.getId(), operacao.getCommandId(), operacao.getUnidadeOrigem(), operacao.getUnidadeDestino(),
                operacao.getLacreOrigem(), operacao.getLacreDestino(), operacao.getDivergencia(),
                operacao.getCodigoAvaria(), operacao.getStatus(), operacao.getUsuario(), operacao.getExecutadoEm(), itens);
    }

    private ReservaGateCargaResposta mapearReservaGate(ReservaGateCarga reserva) {
        return new ReservaGateCargaResposta(
                reserva.getId(), reserva.getAgendamentoCodigo(), reserva.getBlNumero(), reserva.getDeliveryOrder(),
                reserva.getLoteId(), reserva.getTipoMovimento(), reserva.getEstagioConfirmacao(), reserva.getStatus(),
                reserva.getQuantidade(), reserva.getVolumeM3(), reserva.getPesoKg(), reserva.getReservadoEm(),
                reserva.getConfirmadoEm(), reserva.getCompensadoEm());
    }

    private AlocacaoResposta mapearAlocacao(AlocacaoCargoLot alocacao) {
        return new AlocacaoResposta(
                alocacao.getId(), alocacao.getLoteId(), alocacao.getReservaCapacidadeId(), alocacao.getOrigem(),
                alocacao.getDestino(), alocacao.getRecurso(), alocacao.getPrioridade(), alocacao.getRestricoes(),
                alocacao.getQuantidade(), alocacao.getVolumeM3(), alocacao.getPesoKg(), alocacao.getStatus(),
                alocacao.getCriadoEm(), alocacao.getConfirmadoEm());
    }

    private PlanoTransporteResposta mapearPlanoTransporte(PlanoTransporteCargoLot plano) {
        return new PlanoTransporteResposta(
                plano.getId(), plano.getModal(), plano.getTipoOperacao(), plano.getStatus(), plano.getVisitaId(),
                plano.getBlNumero(), plano.getLoteId(), plano.getCompartimento(), plano.getPosicao(), plano.getSequencia(),
                plano.getEquipamento(), plano.getCustodia(), plano.getRestricoes(), plano.getQuantidadePlanejada(),
                plano.getVolumePlanejadoM3(), plano.getPesoPlanejadoKg(), plano.getQuantidadeRealizada(),
                plano.getVolumeRealizadoM3(), plano.getPesoRealizadoKg(), plano.getPlanejadoEm(), plano.getExecutadoEm());
    }

    private AvariaResposta mapearAvaria(AvariaCarga avaria) {
        List<EvidenciaAvariaResposta> evidencias = avaria.getEvidencias().stream()
                .map(evidencia -> new EvidenciaAvariaResposta(
                        evidencia.getTipo(), evidencia.getUri(), evidencia.getChecksum(),
                        evidencia.getResponsavel(), evidencia.getRegistradoEm()))
                .toList();
        return new AvariaResposta(
                avaria.getId(), avaria.getLoteId(), avaria.getCodigo(), avaria.getDescricao(),
                avaria.getQuantidadeAfetada(), avaria.getVolumeAfetadoM3(), avaria.getPesoAfetadoKg(),
                avaria.getStatus(), avaria.getResponsavel(), avaria.getInspecionadoPor(), avaria.getReparadoPor(),
                avaria.getObservacoes(), avaria.getCriadoEm(), avaria.getAtualizadoEm(), evidencias);
    }

    private InventarioFisicoResposta mapearInventario(InventarioFisicoCargoLot inventario) {
        List<ContagemResposta> contagens = inventario.getContagens().stream()
                .map(contagem -> new ContagemResposta(
                        contagem.getCommandId(), contagem.getLoteId(), contagem.getIdentificacao(),
                        contagem.getQuantidadeLogica(), contagem.getVolumeLogicoM3(), contagem.getPesoLogicoKg(),
                        contagem.getQuantidadeContada(), contagem.getVolumeContadoM3(), contagem.getPesoContadoKg(),
                        contagem.getStatusDivergencia(), contagem.getUsuario(), contagem.getObservacao(),
                        contagem.getContadoEm(), contagem.getResolvidoPor(), contagem.getMotivoResolucao()))
                .toList();
        return new InventarioFisicoResposta(
                inventario.getId(), inventario.getPosicao(), inventario.getStatus(), inventario.getAbertoPor(),
                inventario.getAbertoEm(), inventario.getConcluidoPor(), inventario.getConcluidoEm(),
                inventario.getMotivo(), contagens);
    }

    private void executarEstado(Runnable acao) {
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
