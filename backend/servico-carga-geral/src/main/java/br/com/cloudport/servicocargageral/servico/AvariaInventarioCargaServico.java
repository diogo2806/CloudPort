package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.AvariaOperacionalCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoMovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.ContagemInventarioCarga;
import br.com.cloudport.servicocargageral.dominio.InventarioFisicoCarga;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dominio.MovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.OperacaoIntermodalTipos.ResultadoAvaria;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.AbrirInventarioRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.AvariaResposta;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.ConciliarInventarioRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.ContagemResposta;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.EncerrarAvariaRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.InspecionarAvariaRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.InventarioResposta;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.RegistrarAvariaRequest;
import br.com.cloudport.servicocargageral.dto.OperacaoIntermodalDTOs.RegistrarContagemRequest;
import br.com.cloudport.servicocargageral.repositorio.AvariaOperacionalCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.InventarioFisicoCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvariaInventarioCargaServico {

    private final AvariaOperacionalCargaRepositorio avariaRepositorio;
    private final InventarioFisicoCargaRepositorio inventarioRepositorio;
    private final LoteCargaRepositorio loteRepositorio;

    public AvariaInventarioCargaServico(
            AvariaOperacionalCargaRepositorio avariaRepositorio,
            InventarioFisicoCargaRepositorio inventarioRepositorio,
            LoteCargaRepositorio loteRepositorio) {
        this.avariaRepositorio = avariaRepositorio;
        this.inventarioRepositorio = inventarioRepositorio;
        this.loteRepositorio = loteRepositorio;
    }

    @Transactional(readOnly = true)
    public List<AvariaResposta> listarAvarias(UUID loteId) {
        return avariaRepositorio.findByLoteIdOrderByCriadoEmDesc(loteId).stream()
                .map(this::mapearAvaria).collect(Collectors.toList());
    }

    @Transactional
    public AvariaResposta registrarAvaria(RegistrarAvariaRequest request) {
        LoteCarga lote = buscarLote(request.loteId());
        validarDisponibilidade(lote, request.quantidadeAfetada(), request.volumeAfetadoM3(), request.pesoAfetadoKg());
        executar(() -> lote.bloquearSaldo(request.quantidadeAfetada(), request.volumeAfetadoM3(), request.pesoAfetadoKg()));
        lote.setCodigoAvaria(request.codigo());
        lote.setDescricaoAvaria(request.descricao());
        AvariaOperacionalCarga avaria = new AvariaOperacionalCarga();
        avaria.setLote(lote);
        avaria.setCodigo(request.codigo());
        avaria.setDescricao(request.descricao());
        avaria.setQuantidadeAfetada(request.quantidadeAfetada());
        avaria.setVolumeAfetadoM3(request.volumeAfetadoM3());
        avaria.setPesoAfetadoKg(request.pesoAfetadoKg());
        avaria.setResponsavel(request.responsavel());
        avaria.setEvidenciasJson(request.evidenciasJson());
        avaria.registrarHistorico("REGISTRADA", request.responsavel(), request.descricao());
        executar(() -> avaria.segregar(request.responsavel()));
        loteRepositorio.save(lote);
        return mapearAvaria(avariaRepositorio.save(avaria));
    }

    @Transactional
    public AvariaResposta inspecionar(UUID id, InspecionarAvariaRequest request) {
        AvariaOperacionalCarga avaria = buscarAvaria(id);
        executar(() -> avaria.iniciarInspecao(request.relatorio(), request.usuario()));
        return mapearAvaria(avariaRepositorio.save(avaria));
    }

    @Transactional
    public AvariaResposta encerrar(UUID id, EncerrarAvariaRequest request) {
        AvariaOperacionalCarga avaria = buscarAvaria(id);
        LoteCarga lote = buscarLote(avaria.getLote().getId());
        if (request.resultado() == ResultadoAvaria.REINTEGRAR) {
            executar(() -> lote.liberarSaldoBloqueado(avaria.getQuantidadeAfetada(),
                    avaria.getVolumeAfetadoM3(), avaria.getPesoAfetadoKg()));
        } else if (request.resultado() == ResultadoAvaria.BAIXAR) {
            executar(() -> lote.baixarSaldoBloqueado(avaria.getQuantidadeAfetada(),
                    avaria.getVolumeAfetadoM3(), avaria.getPesoAfetadoKg()));
        }
        executar(() -> avaria.encerrar(request.resultado(), request.observacao(), request.usuario()));
        loteRepositorio.save(lote);
        return mapearAvaria(avariaRepositorio.save(avaria));
    }

    @Transactional(readOnly = true)
    public List<InventarioResposta> listarInventarios() {
        return inventarioRepositorio.findAllByOrderByCriadoEmDesc().stream()
                .map(this::mapearInventario).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventarioResposta obterInventario(UUID id) {
        return mapearInventario(inventarioRepositorio.findDetalhadoById(id)
                .orElseThrow(() -> naoEncontrado("Inventário não encontrado.")));
    }

    @Transactional
    public InventarioResposta abrirInventario(AbrirInventarioRequest request) {
        if (inventarioRepositorio.existsByCodigoIgnoreCase(request.codigo())) {
            throw conflito("Já existe inventário com esse código.");
        }
        InventarioFisicoCarga inventario = new InventarioFisicoCarga();
        inventario.setCodigo(request.codigo());
        inventario.setArmazemId(request.armazemId());
        inventario.setPosicaoReferencia(request.posicaoReferencia());
        inventario.setMotivo(request.motivo());
        inventario.setAbertoPor(request.usuario());
        return mapearInventario(inventarioRepositorio.save(inventario));
    }

    @Transactional
    public InventarioResposta registrarContagem(UUID id, RegistrarContagemRequest request) {
        InventarioFisicoCarga inventario = buscarInventario(id);
        LoteCarga lote = identificarLote(request.codigoIdentificacao());
        if (!inventario.getArmazemId().equalsIgnoreCase(lote.getArmazemId())) {
            throw conflito("O cargo lot não pertence ao armazém do inventário.");
        }
        ContagemInventarioCarga contagem = new ContagemInventarioCarga();
        contagem.setLote(lote);
        contagem.setCodigoIdentificacao(request.codigoIdentificacao());
        contagem.setPosicao(request.posicao());
        contagem.setNumeroContagem(request.numeroContagem());
        contagem.setQuantidade(request.quantidade());
        contagem.setVolumeM3(request.volumeM3());
        contagem.setPesoKg(request.pesoKg());
        contagem.setUsuario(request.usuario());
        contagem.setCorrelationId(request.correlationId());
        executar(() -> inventario.adicionarContagem(contagem));
        return mapearInventario(inventarioRepositorio.save(inventario));
    }

    @Transactional
    public InventarioResposta enviarParaAprovacao(UUID id, String usuario) {
        InventarioFisicoCarga inventario = buscarInventario(id);
        executar(() -> inventario.enviarParaAprovacao(usuario));
        return mapearInventario(inventarioRepositorio.save(inventario));
    }

    @Transactional
    public InventarioResposta conciliar(UUID id, ConciliarInventarioRequest request) {
        InventarioFisicoCarga inventario = buscarInventario(id);
        Map<UUID, ContagemInventarioCarga> ultimaContagem = inventario.getContagens().stream()
                .sorted(Comparator.comparing(ContagemInventarioCarga::getContadoEm))
                .collect(Collectors.toMap(item -> item.getLote().getId(), item -> item,
                        (anterior, posterior) -> posterior, LinkedHashMap::new));
        for (ContagemInventarioCarga contagem : ultimaContagem.values()) {
            LoteCarga lote = buscarLote(contagem.getLote().getId());
            BigDecimal quantidadeAnterior = lote.getQuantidadeSaldo();
            BigDecimal volumeAnterior = lote.getVolumeSaldoM3();
            BigDecimal pesoAnterior = lote.getPesoSaldoKg();
            ajustarSaldo(lote, contagem.getQuantidade(), contagem.getVolumeM3(), contagem.getPesoKg());
            lote.atualizarLocalizacao(inventario.getArmazemId(), contagem.getPosicao(),
                    lote.getVeiculoId(), lote.getVisitaNavioId(), lote.getClienteId());
            lote.registrarMovimentacao(criarAjuste(inventario, contagem, quantidadeAnterior, volumeAnterior,
                    pesoAnterior, request));
            loteRepositorio.save(lote);
        }
        executar(() -> inventario.concluir(request.aprovador(), request.justificativa()));
        return mapearInventario(inventarioRepositorio.save(inventario));
    }

    private void ajustarSaldo(LoteCarga lote, BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        if (quantidade.compareTo(lote.getQuantidadeSaldo()) > 0) {
            lote.adicionarSaldo(quantidade.subtract(lote.getQuantidadeSaldo()),
                    maxZero(volume.subtract(lote.getVolumeSaldoM3())), maxZero(peso.subtract(lote.getPesoSaldoKg())));
        } else if (quantidade.compareTo(lote.getQuantidadeSaldo()) < 0) {
            executar(() -> lote.retirarSaldo(lote.getQuantidadeSaldo().subtract(quantidade),
                    maxZero(lote.getVolumeSaldoM3().subtract(volume)), maxZero(lote.getPesoSaldoKg().subtract(peso))));
        } else {
            BigDecimal diferencaVolume = volume.subtract(lote.getVolumeSaldoM3());
            BigDecimal diferencaPeso = peso.subtract(lote.getPesoSaldoKg());
            if (diferencaVolume.signum() > 0 || diferencaPeso.signum() > 0) {
                lote.adicionarSaldo(BigDecimal.ZERO, maxZero(diferencaVolume), maxZero(diferencaPeso));
            } else if (diferencaVolume.signum() < 0 || diferencaPeso.signum() < 0) {
                executar(() -> lote.retirarSaldo(BigDecimal.ZERO, maxZero(diferencaVolume.negate()),
                        maxZero(diferencaPeso.negate())));
            }
        }
    }

    private MovimentacaoCarga criarAjuste(InventarioFisicoCarga inventario, ContagemInventarioCarga contagem,
            BigDecimal quantidadeAnterior, BigDecimal volumeAnterior, BigDecimal pesoAnterior,
            ConciliarInventarioRequest request) {
        MovimentacaoCarga movimento = new MovimentacaoCarga();
        movimento.setTipo(TipoMovimentacaoCarga.AJUSTE_INVENTARIO);
        movimento.setQuantidade(contagem.getQuantidade().subtract(quantidadeAnterior).abs());
        movimento.setVolumeM3(contagem.getVolumeM3().subtract(volumeAnterior).abs());
        movimento.setPesoKg(contagem.getPesoKg().subtract(pesoAnterior).abs());
        movimento.setOrigemTipo("SALDO_SISTEMA");
        movimento.setOrigemId(contagem.getLote().getId().toString());
        movimento.setDestinoTipo("CONTAGEM_FISICA");
        movimento.setDestinoId(inventario.getId().toString());
        movimento.setArmazemId(inventario.getArmazemId());
        movimento.setUsuario(request.aprovador());
        movimento.setCorrelationId(contagem.getCorrelationId());
        movimento.setObservacao(request.justificativa());
        return movimento;
    }

    private LoteCarga identificarLote(String codigo) {
        String valor = codigo == null ? "" : codigo.trim();
        int separador = valor.indexOf(':');
        if (separador >= 0 && separador < valor.length() - 1) {
            valor = valor.substring(separador + 1).trim();
        }
        try {
            UUID id = UUID.fromString(valor);
            return loteRepositorio.findComBloqueioById(id)
                    .orElseThrow(() -> naoEncontrado("Cargo lot não encontrado."));
        } catch (IllegalArgumentException exception) {
            return loteRepositorio.findByCodigoIgnoreCase(valor)
                    .orElseThrow(() -> naoEncontrado("Cargo lot não encontrado."));
        }
    }

    private AvariaOperacionalCarga buscarAvaria(UUID id) {
        return avariaRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrado("Avaria não encontrada."));
    }

    private InventarioFisicoCarga buscarInventario(UUID id) {
        return inventarioRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrado("Inventário não encontrado."));
    }

    private LoteCarga buscarLote(UUID id) {
        return loteRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrado("Cargo lot não encontrado."));
    }

    private void validarDisponibilidade(LoteCarga lote, BigDecimal quantidade, BigDecimal volume, BigDecimal peso) {
        if (quantidade.compareTo(lote.getQuantidadeSaldo()) > 0
                || volume.compareTo(lote.getVolumeSaldoM3()) > 0
                || peso.compareTo(lote.getPesoSaldoKg()) > 0) {
            throw conflito("A avaria excede o saldo disponível.");
        }
    }

    private AvariaResposta mapearAvaria(AvariaOperacionalCarga avaria) {
        return new AvariaResposta(avaria.getId(), avaria.getLote().getId(), avaria.getLote().getCodigo(),
                avaria.getCodigo(), avaria.getDescricao(), avaria.getQuantidadeAfetada(), avaria.getVolumeAfetadoM3(),
                avaria.getPesoAfetadoKg(), avaria.getResponsavel(), avaria.getEvidenciasJson(), avaria.getStatus(),
                avaria.getRelatorioInspecao(), avaria.getResultadoTratamento(), avaria.getHistoricoOperacional(),
                avaria.getCriadoEm(), avaria.getEncerradoEm());
    }

    private InventarioResposta mapearInventario(InventarioFisicoCarga inventario) {
        return new InventarioResposta(inventario.getId(), inventario.getCodigo(), inventario.getArmazemId(),
                inventario.getPosicaoReferencia(), inventario.getMotivo(), inventario.getStatus(), inventario.getAbertoPor(),
                inventario.getAprovadoPor(), inventario.getJustificativaAjuste(), inventario.getHistoricoOperacional(),
                inventario.getCriadoEm(), inventario.getConcluidoEm(), inventario.getContagens().stream()
                        .map(this::mapearContagem).collect(Collectors.toList()));
    }

    private ContagemResposta mapearContagem(ContagemInventarioCarga contagem) {
        return new ContagemResposta(contagem.getId(), contagem.getLote().getId(), contagem.getLote().getCodigo(),
                contagem.getCodigoIdentificacao(), contagem.getPosicao(), contagem.getNumeroContagem(),
                contagem.getQuantidade(), contagem.getVolumeM3(), contagem.getPesoKg(), contagem.getUsuario(),
                contagem.getCorrelationId(), contagem.getContadoEm());
    }

    private BigDecimal maxZero(BigDecimal valor) { return valor.signum() < 0 ? BigDecimal.ZERO : valor; }

    private void executar(Runnable comando) {
        try {
            comando.run();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (IllegalStateException exception) {
            throw conflito(exception.getMessage());
        }
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private ResponseStatusException naoEncontrado(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }
}
