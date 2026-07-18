package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.comum.erro.TradutorConflitoCadastroCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.CategoriaReferenciaCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.NaturezaCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusConhecimentoCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusLoteCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoMovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.ConhecimentoCarga;
import br.com.cloudport.servicocargageral.dominio.ItemConhecimentoCarga;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dominio.MovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.ReferenciaCarga;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.ConhecimentoDetalhe;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.ConhecimentoResumo;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.CriarConhecimentoRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.CriarItemRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.CriarLoteRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.CriarReferenciaRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.DashboardResposta;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.ItemResposta;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.LoteDetalhe;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.LoteResumo;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.MovimentacaoResposta;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.ReferenciaResposta;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.RegistrarAvariaRequest;
import br.com.cloudport.servicocargageral.dto.CargaGeralDTOs.RegistrarMovimentacaoRequest;
import br.com.cloudport.servicocargageral.repositorio.ConhecimentoCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.ItemConhecimentoCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.MovimentacaoCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.ReferenciaCargaRepositorio;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CargaGeralServico {

    private final ConhecimentoCargaRepositorio conhecimentoRepositorio;
    private final ItemConhecimentoCargaRepositorio itemRepositorio;
    private final LoteCargaRepositorio loteRepositorio;
    private final MovimentacaoCargaRepositorio movimentacaoRepositorio;
    private final ReferenciaCargaRepositorio referenciaRepositorio;

    public CargaGeralServico(
            ConhecimentoCargaRepositorio conhecimentoRepositorio,
            ItemConhecimentoCargaRepositorio itemRepositorio,
            LoteCargaRepositorio loteRepositorio,
            MovimentacaoCargaRepositorio movimentacaoRepositorio,
            ReferenciaCargaRepositorio referenciaRepositorio) {
        this.conhecimentoRepositorio = conhecimentoRepositorio;
        this.itemRepositorio = itemRepositorio;
        this.loteRepositorio = loteRepositorio;
        this.movimentacaoRepositorio = movimentacaoRepositorio;
        this.referenciaRepositorio = referenciaRepositorio;
    }

    @Transactional(readOnly = true)
    public List<ConhecimentoResumo> listarConhecimentos() {
        return conhecimentoRepositorio.findAll().stream()
                .sorted(Comparator.comparing(ConhecimentoCarga::getAtualizadoEm).reversed())
                .map(this::mapearConhecimentoResumo)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConhecimentoDetalhe criarConhecimento(CriarConhecimentoRequest request) {
        if (conhecimentoRepositorio.existsByNumeroIgnoreCase(request.numero())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um Bill of Lading com esse número.");
        }
        ConhecimentoCarga conhecimento = new ConhecimentoCarga();
        conhecimento.setNumero(request.numero());
        conhecimento.setTipoOperacao(request.tipoOperacao());
        conhecimento.setEmbarcador(request.embarcador());
        conhecimento.setConsignatario(request.consignatario());
        conhecimento.setClienteId(request.clienteId());
        conhecimento.setOperadorId(request.operadorId());
        conhecimento.setVisitaNavioId(request.visitaNavioId());
        conhecimento.setVisitaVeiculoId(request.visitaVeiculoId());
        conhecimento.setArmazemId(request.armazemId());
        conhecimento.setPortoOrigem(request.portoOrigem());
        conhecimento.setPortoDestino(request.portoDestino());
        conhecimento.setObservacoes(request.observacoes());
        try {
            return mapearConhecimentoDetalhe(conhecimentoRepositorio.saveAndFlush(conhecimento));
        } catch (DataIntegrityViolationException exception) {
            throw TradutorConflitoCadastroCarga.traduzir(exception);
        }
    }

    @Transactional(readOnly = true)
    public ConhecimentoDetalhe obterConhecimento(UUID id) {
        return mapearConhecimentoDetalhe(buscarConhecimento(id));
    }

    @Transactional
    public ItemResposta adicionarItem(UUID conhecimentoId, CriarItemRequest request) {
        ConhecimentoCarga conhecimento = buscarConhecimento(conhecimentoId);
        boolean sequenciaDuplicada = conhecimento.getItens().stream()
                .anyMatch(item -> item.getSequencia() == request.sequencia());
        if (sequenciaDuplicada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A sequência do item já existe no conhecimento.");
        }
        validarMercadoriaPerigosa(request);
        validarTemperatura(request.temperaturaMinima(), request.temperaturaMaxima());

        ItemConhecimentoCarga item = new ItemConhecimentoCarga();
        item.setSequencia(request.sequencia());
        item.setDescricao(request.descricao());
        item.setCommodityCodigo(normalizarCodigo(request.commodityCodigo()));
        item.setTipoProdutoCodigo(normalizarCodigo(request.tipoProdutoCodigo()));
        item.setTipoEmbalagemCodigo(normalizarCodigo(request.tipoEmbalagemCodigo()));
        item.setQuantidadeManifestada(request.quantidadeManifestada());
        item.setVolumeM3(request.volumeM3());
        item.setPesoKg(request.pesoKg());
        item.setCodigoArmazenagem(normalizarCodigo(request.codigoArmazenagem()));
        item.setCodigoManuseio(normalizarCodigo(request.codigoManuseio()));
        item.setMercadoriaPerigosa(request.mercadoriaPerigosa());
        item.setNumeroUn(normalizarCodigo(request.numeroUn()));
        item.setClasseImdg(normalizarCodigo(request.classeImdg()));
        item.setTemperaturaMinima(request.temperaturaMinima());
        item.setTemperaturaMaxima(request.temperaturaMaxima());
        conhecimento.adicionarItem(item);
        try {
            conhecimentoRepositorio.saveAndFlush(conhecimento);
            return mapearItem(item);
        } catch (DataIntegrityViolationException exception) {
            throw TradutorConflitoCadastroCarga.traduzir(exception);
        }
    }

    @Transactional
    public LoteResumo adicionarLote(UUID itemId, CriarLoteRequest request) {
        if (loteRepositorio.existsByCodigoIgnoreCase(request.codigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um cargo lot com esse código.");
        }
        ItemConhecimentoCarga item = itemRepositorio.findDetalhadoById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item do conhecimento não encontrado."));
        LoteCarga lote = new LoteCarga();
        lote.setCodigo(request.codigo());
        lote.setNatureza(request.natureza());
        lote.setQuantidadePrevista(request.quantidadePrevista());
        lote.setVolumePrevistoM3(request.volumePrevistoM3());
        lote.setPesoPrevistoKg(request.pesoPrevistoKg());
        lote.setUnidadeMedida(request.unidadeMedida());
        lote.setMarcasEmbalagem(request.marcasEmbalagem());
        lote.setArmazemId(request.armazemId());
        lote.setPosicaoArmazenagem(request.posicaoArmazenagem());
        lote.setVeiculoId(request.veiculoId());
        lote.setVisitaNavioId(request.visitaNavioId());
        lote.setClienteId(request.clienteId());
        if (request.lotePaiId() != null) {
            lote.setLotePai(loteRepositorio.findById(request.lotePaiId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote pai não encontrado.")));
        }
        item.adicionarLote(lote);
        try {
            itemRepositorio.saveAndFlush(item);
            return mapearLote(lote);
        } catch (DataIntegrityViolationException exception) {
            throw TradutorConflitoCadastroCarga.traduzir(exception);
        }
    }

    @Transactional(readOnly = true)
    public List<LoteResumo> listarLotes(StatusLoteCarga status, NaturezaCarga natureza) {
        List<LoteCarga> lotes;
        if (status != null && natureza != null) {
            lotes = loteRepositorio.findByStatusAndNaturezaOrderByAtualizadoEmDesc(status, natureza);
        } else if (status != null) {
            lotes = loteRepositorio.findByStatusOrderByAtualizadoEmDesc(status);
        } else if (natureza != null) {
            lotes = loteRepositorio.findByNaturezaOrderByAtualizadoEmDesc(natureza);
        } else {
            lotes = loteRepositorio.findAllByOrderByAtualizadoEmDesc();
        }
        return lotes.stream().map(this::mapearLote).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoteDetalhe obterLote(UUID id) {
        LoteCarga lote = loteRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo lot não encontrado."));
        return mapearLoteDetalhe(lote);
    }

    @Transactional
    public LoteDetalhe registrarMovimentacao(UUID loteId, RegistrarMovimentacaoRequest request) {
        validarMovimentacao(request);
        LoteCarga lote = buscarLoteComBloqueio(loteId);
        TipoMovimentacaoCarga tipo = request.tipo();

        if (tipo == TipoMovimentacaoCarga.RECEBIMENTO
                || tipo == TipoMovimentacaoCarga.DESCARGA_PARCIAL
                || tipo == TipoMovimentacaoCarga.AJUSTE_INVENTARIO) {
            lote.adicionarSaldo(request.quantidade(), request.volumeM3(), request.pesoKg());
        } else if (tipo == TipoMovimentacaoCarga.CARGA_PARCIAL || tipo == TipoMovimentacaoCarga.ENTREGA) {
            retirarSaldo(lote, request);
        } else if (tipo == TipoMovimentacaoCarga.CONSOLIDACAO || tipo == TipoMovimentacaoCarga.DESCONSOLIDACAO) {
            transferirEntreLotes(lote, request);
        }

        lote.atualizarLocalizacao(
                request.armazemId(),
                request.destinoTipo() != null && request.destinoTipo().equalsIgnoreCase("POSICAO") ? request.destinoId() : null,
                request.veiculoId(),
                request.visitaNavioId(),
                request.clienteId());
        lote.getItem().getConhecimento().iniciarOperacao();

        MovimentacaoCarga movimentacao = criarMovimentacao(request);
        lote.registrarMovimentacao(movimentacao);
        loteRepositorio.save(lote);
        return mapearLoteDetalhe(lote);
    }

    @Transactional
    public LoteDetalhe registrarAvaria(UUID loteId, RegistrarAvariaRequest request) {
        LoteCarga lote = buscarLoteComBloqueio(loteId);
        lote.setCodigoAvaria(normalizarCodigo(request.codigoAvaria()));
        lote.setDescricaoAvaria(request.descricaoAvaria().trim());
        lote.setStatus(StatusLoteCarga.AVARIADO);
        return mapearLoteDetalhe(loteRepositorio.save(lote));
    }

    @Transactional(readOnly = true)
    public List<ReferenciaResposta> listarReferencias(CategoriaReferenciaCarga categoria) {
        List<ReferenciaCarga> referencias = categoria == null
                ? referenciaRepositorio.findAllByOrderByCategoriaAscCodigoAsc()
                : referenciaRepositorio.findByCategoriaAndAtivoTrueOrderByCodigo(categoria);
        return referencias.stream().map(this::mapearReferencia).collect(Collectors.toList());
    }

    @Transactional
    public ReferenciaResposta criarReferencia(CriarReferenciaRequest request) {
        if (referenciaRepositorio.existsByCategoriaAndCodigoIgnoreCase(request.categoria(), request.codigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A referência já existe nessa categoria.");
        }
        ReferenciaCarga referencia = new ReferenciaCarga();
        referencia.setCategoria(request.categoria());
        referencia.setCodigo(request.codigo());
        referencia.setDescricao(request.descricao().trim());
        referencia.setAtributosJson(request.atributosJson());
        referencia.setAtivo(request.ativo());
        try {
            return mapearReferencia(referenciaRepositorio.saveAndFlush(referencia));
        } catch (DataIntegrityViolationException exception) {
            throw TradutorConflitoCadastroCarga.traduzir(exception);
        }
    }

    @Transactional
    public ReferenciaResposta atualizarReferenciaAtiva(UUID id, boolean ativo) {
        ReferenciaCarga referencia = referenciaRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Referência não encontrada."));
        referencia.setAtivo(ativo);
        return mapearReferencia(referenciaRepositorio.save(referencia));
    }

    @Transactional(readOnly = true)
    public DashboardResposta obterDashboard() {
        List<ConhecimentoCarga> conhecimentos = conhecimentoRepositorio.findAll();
        List<LoteCarga> lotes = loteRepositorio.findAllByOrderByAtualizadoEmDesc();
        List<MovimentacaoResposta> movimentacoes = movimentacaoRepositorio.findTop100ByOrderByOcorridoEmDesc().stream()
                .limit(20)
                .map(this::mapearMovimentacao)
                .collect(Collectors.toList());

        long conhecimentosAbertos = conhecimentos.stream()
                .filter(item -> item.getStatus() != StatusConhecimentoCarga.CONCLUIDO
                        && item.getStatus() != StatusConhecimentoCarga.CANCELADO)
                .count();
        long lotesNoTerminal = lotes.stream().filter(item -> item.getQuantidadeSaldo().signum() > 0).count();
        long lotesBreakBulk = lotes.stream().filter(item -> item.getNatureza() == NaturezaCarga.BREAK_BULK).count();
        long lotesAvariados = lotes.stream().filter(item -> item.getStatus() == StatusLoteCarga.AVARIADO).count();

        return new DashboardResposta(
                conhecimentosAbertos,
                lotesNoTerminal,
                lotesBreakBulk,
                lotesAvariados,
                somar(lotes, Saldo.QUANTIDADE),
                somar(lotes, Saldo.VOLUME),
                somar(lotes, Saldo.PESO),
                movimentacoes);
    }

    private void transferirEntreLotes(LoteCarga origem, RegistrarMovimentacaoRequest request) {
        if (request.loteRelacionadoId() == null || request.loteRelacionadoId().equals(origem.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um lote relacionado diferente para consolidar ou desconsolidar.");
        }
        LoteCarga destino = buscarLoteComBloqueio(request.loteRelacionadoId());
        retirarSaldo(origem, request);
        destino.adicionarSaldo(request.quantidade(), request.volumeM3(), request.pesoKg());
        if (request.tipo() == TipoMovimentacaoCarga.DESCONSOLIDACAO && destino.getLotePai() == null) {
            destino.setLotePai(origem);
        }
        loteRepositorio.save(destino);
    }

    private void retirarSaldo(LoteCarga lote, RegistrarMovimentacaoRequest request) {
        try {
            lote.retirarSaldo(request.quantidade(), request.volumeM3(), request.pesoKg());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    private MovimentacaoCarga criarMovimentacao(RegistrarMovimentacaoRequest request) {
        MovimentacaoCarga movimentacao = new MovimentacaoCarga();
        movimentacao.setTipo(request.tipo());
        movimentacao.setQuantidade(request.quantidade());
        movimentacao.setVolumeM3(request.volumeM3());
        movimentacao.setPesoKg(request.pesoKg());
        movimentacao.setLoteRelacionadoId(request.loteRelacionadoId());
        movimentacao.setOrigemTipo(request.origemTipo());
        movimentacao.setOrigemId(request.origemId());
        movimentacao.setDestinoTipo(request.destinoTipo());
        movimentacao.setDestinoId(request.destinoId());
        movimentacao.setVeiculoId(request.veiculoId());
        movimentacao.setVisitaNavioId(request.visitaNavioId());
        movimentacao.setArmazemId(request.armazemId());
        movimentacao.setClienteId(request.clienteId());
        movimentacao.setUsuario(request.usuario().trim());
        movimentacao.setCorrelationId(request.correlationId());
        movimentacao.setObservacao(request.observacao());
        movimentacao.setOcorridoEm(request.ocorridoEm());
        return movimentacao;
    }

    private void validarMovimentacao(RegistrarMovimentacaoRequest request) {
        boolean semQuantidade = request.quantidade().signum() == 0
                && request.volumeM3().signum() == 0
                && request.pesoKg().signum() == 0;
        boolean apenasLocalizacao = request.tipo() == TipoMovimentacaoCarga.ARMAZENAGEM
                || request.tipo() == TipoMovimentacaoCarga.TRANSFERENCIA;
        if (semQuantidade && !apenasLocalizacao) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe quantidade, volume ou peso para a movimentação.");
        }
    }

    private void validarMercadoriaPerigosa(CriarItemRequest request) {
        if (request.mercadoriaPerigosa()
                && (vazio(request.numeroUn()) || vazio(request.classeImdg()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mercadoria perigosa exige número UN e classe IMDG.");
        }
    }

    private void validarTemperatura(BigDecimal minima, BigDecimal maxima) {
        if (minima != null && maxima != null && minima.compareTo(maxima) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A temperatura mínima não pode ser maior que a máxima.");
        }
    }

    private ConhecimentoCarga buscarConhecimento(UUID id) {
        return conhecimentoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill of Lading não encontrado."));
    }

    private LoteCarga buscarLoteComBloqueio(UUID id) {
        return loteRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo lot não encontrado."));
    }

    private ConhecimentoResumo mapearConhecimentoResumo(ConhecimentoCarga conhecimento) {
        return new ConhecimentoResumo(
                conhecimento.getId(),
                conhecimento.getNumero(),
                conhecimento.getTipoOperacao(),
                conhecimento.getStatus(),
                conhecimento.getEmbarcador(),
                conhecimento.getConsignatario(),
                conhecimento.getClienteId(),
                conhecimento.getVisitaNavioId(),
                conhecimento.getVisitaVeiculoId(),
                conhecimento.getArmazemId(),
                conhecimento.getItens().size(),
                conhecimento.getAtualizadoEm());
    }

    private ConhecimentoDetalhe mapearConhecimentoDetalhe(ConhecimentoCarga conhecimento) {
        return new ConhecimentoDetalhe(
                conhecimento.getId(),
                conhecimento.getNumero(),
                conhecimento.getTipoOperacao(),
                conhecimento.getStatus(),
                conhecimento.getEmbarcador(),
                conhecimento.getConsignatario(),
                conhecimento.getClienteId(),
                conhecimento.getOperadorId(),
                conhecimento.getVisitaNavioId(),
                conhecimento.getVisitaVeiculoId(),
                conhecimento.getArmazemId(),
                conhecimento.getPortoOrigem(),
                conhecimento.getPortoDestino(),
                conhecimento.getObservacoes(),
                conhecimento.getCriadoEm(),
                conhecimento.getAtualizadoEm(),
                conhecimento.getItens().stream().map(this::mapearItem).collect(Collectors.toList()));
    }

    private ItemResposta mapearItem(ItemConhecimentoCarga item) {
        return new ItemResposta(
                item.getId(),
                item.getSequencia(),
                item.getDescricao(),
                item.getCommodityCodigo(),
                item.getTipoProdutoCodigo(),
                item.getTipoEmbalagemCodigo(),
                item.getQuantidadeManifestada(),
                item.getVolumeM3(),
                item.getPesoKg(),
                item.getCodigoArmazenagem(),
                item.getCodigoManuseio(),
                item.isMercadoriaPerigosa(),
                item.getNumeroUn(),
                item.getClasseImdg(),
                item.getTemperaturaMinima(),
                item.getTemperaturaMaxima(),
                item.getLotes().stream().map(this::mapearLote).collect(Collectors.toList()));
    }

    private LoteResumo mapearLote(LoteCarga lote) {
        return new LoteResumo(
                lote.getId(),
                lote.getCodigo(),
                lote.getItem().getConhecimento().getNumero(),
                lote.getItem().getSequencia(),
                lote.getItem().getDescricao(),
                lote.getNatureza(),
                lote.getStatus(),
                lote.getQuantidadePrevista(),
                lote.getQuantidadeSaldo(),
                lote.getVolumeSaldoM3(),
                lote.getPesoSaldoKg(),
                lote.getUnidadeMedida(),
                lote.getArmazemId(),
                lote.getPosicaoArmazenagem(),
                lote.getVeiculoId(),
                lote.getVisitaNavioId(),
                lote.getClienteId(),
                lote.getCodigoAvaria(),
                lote.getLotePai() == null ? null : lote.getLotePai().getId(),
                lote.getAtualizadoEm());
    }

    private LoteDetalhe mapearLoteDetalhe(LoteCarga lote) {
        return new LoteDetalhe(
                mapearLote(lote),
                lote.getMarcasEmbalagem(),
                lote.getDescricaoAvaria(),
                lote.getMovimentacoes().stream().map(this::mapearMovimentacao).collect(Collectors.toList()));
    }

    private MovimentacaoResposta mapearMovimentacao(MovimentacaoCarga movimentacao) {
        return new MovimentacaoResposta(
                movimentacao.getId(),
                movimentacao.getLote().getId(),
                movimentacao.getLote().getCodigo(),
                movimentacao.getTipo(),
                movimentacao.getQuantidade(),
                movimentacao.getVolumeM3(),
                movimentacao.getPesoKg(),
                movimentacao.getLoteRelacionadoId(),
                movimentacao.getOrigemTipo(),
                movimentacao.getOrigemId(),
                movimentacao.getDestinoTipo(),
                movimentacao.getDestinoId(),
                movimentacao.getVeiculoId(),
                movimentacao.getVisitaNavioId(),
                movimentacao.getArmazemId(),
                movimentacao.getClienteId(),
                movimentacao.getUsuario(),
                movimentacao.getCorrelationId(),
                movimentacao.getObservacao(),
                movimentacao.getOcorridoEm());
    }

    private ReferenciaResposta mapearReferencia(ReferenciaCarga referencia) {
        return new ReferenciaResposta(
                referencia.getId(),
                referencia.getCategoria(),
                referencia.getCodigo(),
                referencia.getDescricao(),
                referencia.getAtributosJson(),
                referencia.isAtivo(),
                referencia.getAtualizadoEm());
    }

    private BigDecimal somar(List<LoteCarga> lotes, Saldo saldo) {
        return lotes.stream().map(lote -> saldo.valor(lote)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizarCodigo(String valor) {
        return vazio(valor) ? null : valor.trim().toUpperCase();
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    private enum Saldo {
        QUANTIDADE {
            @Override BigDecimal valor(LoteCarga lote) { return lote.getQuantidadeSaldo(); }
        },
        VOLUME {
            @Override BigDecimal valor(LoteCarga lote) { return lote.getVolumeSaldoM3(); }
        },
        PESO {
            @Override BigDecimal valor(LoteCarga lote) { return lote.getPesoSaldoKg(); }
        };

        abstract BigDecimal valor(LoteCarga lote);
    }
}
