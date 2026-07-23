package br.com.cloudport.servicoyard.inventario.servico;

import br.com.cloudport.servicoyard.inventario.dto.TipoIsoDTO;
import br.com.cloudport.servicoyard.inventario.modelo.GrupoIsoEquipamento;
import br.com.cloudport.servicoyard.inventario.modelo.TipoEquipamentoInventario;
import br.com.cloudport.servicoyard.inventario.repositorio.GrupoIsoEquipamentoRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.TipoEquipamentoInventarioRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.UnidadeInventarioRepositorio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TipoIsoServico {
    private final TipoEquipamentoInventarioRepositorio tipos;
    private final GrupoIsoEquipamentoRepositorio grupos;
    private final UnidadeInventarioRepositorio unidades;

    public TipoIsoServico(TipoEquipamentoInventarioRepositorio tipos,
                          GrupoIsoEquipamentoRepositorio grupos,
                          UnidadeInventarioRepositorio unidades) {
        this.tipos = tipos;
        this.grupos = grupos;
        this.unidades = unidades;
    }

    @Transactional(readOnly = true)
    public List<TipoIsoDTO.Resposta> listar(String termo, Long grupoIsoId,
                                             TipoEquipamentoInventario.CategoriaEquipamento categoria,
                                             Boolean refrigerado, Boolean ativo, Boolean arquetipo) {
        String filtro = normalizar(termo);
        return tipos.findAllByOrderByCategoriaAscCodigoAsc().stream()
                .filter(t -> filtro == null || contem(t.getCodigo(), filtro) || contem(t.getCodigoIso(), filtro) || contem(t.getDescricao(), filtro))
                .filter(t -> grupoIsoId == null || t.getGrupoIso() != null && grupoIsoId.equals(t.getGrupoIso().getId()))
                .filter(t -> categoria == null || categoria == t.getCategoria())
                .filter(t -> refrigerado == null || refrigerado == t.isRefrigerado())
                .filter(t -> ativo == null || ativo == t.isAtivo())
                .filter(t -> arquetipo == null || arquetipo == t.isIndicadorArquetipo())
                .map(this::mapear).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TipoIsoDTO.Resposta detalhar(Long id) { return mapear(localizar(id)); }

    @Transactional
    public TipoIsoDTO.Resposta criar(TipoIsoDTO.ManutencaoRequest request) {
        validarRequest(request);
        String codigo = obrigatorio(request.codigo(), "Código interno é obrigatório").toUpperCase(Locale.ROOT);
        String isoId = validarIso(request.isoId());
        if (tipos.existsByCodigoIgnoreCase(codigo)) conflito("Código interno já cadastrado");
        if (isoId != null && tipos.existsByCodigoIsoIgnoreCase(isoId)) conflito("ISO ID já cadastrado");
        TipoEquipamentoInventario tipo = new TipoEquipamentoInventario();
        tipo.setCodigo(codigo);
        tipo.setCodigoIso(isoId);
        preencher(tipo, request);
        tipo.setAtivo(true);
        tipo.setCriadoPor(usuario(request.usuario()));
        tipo.setAtualizadoPor(usuario(request.usuario()));
        return mapear(tipos.save(tipo));
    }

    @Transactional
    public TipoIsoDTO.Resposta atualizar(Long id, TipoIsoDTO.ManutencaoRequest request) {
        validarRequest(request);
        TipoEquipamentoInventario tipo = localizar(id);
        String isoId = validarIso(request.isoId());
        if (!igual(tipo.getCodigoIso(), isoId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ISO ID é imutável após a criação");
        }
        String codigo = obrigatorio(request.codigo(), "Código interno é obrigatório").toUpperCase(Locale.ROOT);
        tipos.findByCodigoIgnoreCase(codigo).filter(outro -> !outro.getId().equals(id)).ifPresent(outro -> conflito("Código interno já cadastrado"));
        tipo.setCodigo(codigo);
        preencher(tipo, request);
        tipo.setAtualizadoPor(usuario(request.usuario()));
        return mapear(tipos.save(tipo));
    }

    @Transactional
    public TipoIsoDTO.Resposta alterarSituacao(Long id, TipoIsoDTO.SituacaoRequest request) {
        TipoEquipamentoInventario tipo = localizar(id);
        if (!request.ativo()) {
            long dependencias = unidades.countByTipoEquipamentoId(id);
            if (dependencias > 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Tipo ISO possui " + dependencias + " unidade(s) dependente(s) e não pode ser inativado");
            }
        }
        tipo.setAtivo(request.ativo());
        tipo.setAtualizadoPor(usuario(request.usuario()));
        return mapear(tipos.save(tipo));
    }

    private void preencher(TipoEquipamentoInventario tipo, TipoIsoDTO.ManutencaoRequest r) {
        validarPositivo(r.comprimentoMm(), "Comprimento"); validarPositivo(r.larguraMm(), "Largura"); validarPositivo(r.alturaMm(), "Altura");
        validarNaoNegativo(r.taraKg(), "Tara"); validarNaoNegativo(r.capacidadeKg(), "Capacidade");
        GrupoIsoEquipamento grupo = r.grupoIsoId() == null ? null : grupos.findById(r.grupoIsoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grupo ISO não encontrado"));
        if (grupo != null && (!grupo.isAtivo() || grupo.getCategoria() != r.categoria() || grupo.isRefrigerado() != r.refrigerado())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Grupo ISO inativo ou incompatível com categoria/reefer");
        }
        tipo.setDescricao(obrigatorio(r.descricao(), "Descrição é obrigatória")); tipo.setCategoria(r.categoria()); tipo.setGrupoIso(grupo);
        tipo.setArquetipoIso(normalizar(r.arquetipoIso())); tipo.setIndicadorArquetipo(r.indicadorArquetipo());
        tipo.setComprimentoMm(r.comprimentoMm()); tipo.setLarguraMm(r.larguraMm()); tipo.setAlturaMm(r.alturaMm());
        tipo.setTaraKg(r.taraKg()); tipo.setCapacidadeKg(r.capacidadeKg()); tipo.setRefrigerado(r.refrigerado());
        tipo.setGrupoEquivalencia(normalizar(r.grupoEquivalencia())); tipo.setProvisorioEdi(r.provisorioEdi());
    }

    private TipoIsoDTO.Resposta mapear(TipoEquipamentoInventario t) {
        long deps = unidades.countByTipoEquipamentoId(t.getId());
        return new TipoIsoDTO.Resposta(t.getId(), t.getCodigo(), t.getCodigoIso(), t.getDescricao(), t.getCategoria(),
                t.getGrupoIso() == null ? null : t.getGrupoIso().getId(), t.getGrupoIso() == null ? null : t.getGrupoIso().getCodigo(),
                t.getArquetipoIso(), t.isIndicadorArquetipo(), t.getComprimentoMm(), t.getLarguraMm(), t.getAlturaMm(),
                t.getTaraKg(), t.getCapacidadeKg(), t.isRefrigerado(), t.getGrupoEquivalencia(), t.isProvisorioEdi(),
                t.isAtivo(), deps, deps > 0 ? "Em uso por inventário; alterações estruturais e inativação são bloqueadas" : null,
                t.getCriadoPor(), t.getAtualizadoPor(), t.getCriadoEm(), t.getAtualizadoEm());
    }

    private TipoEquipamentoInventario localizar(Long id) { return tipos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo ISO não encontrado")); }
    private void validarRequest(TipoIsoDTO.ManutencaoRequest r) { if (r == null || r.categoria() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria é obrigatória"); }
    private String validarIso(String valor) { String iso = normalizar(valor); if (iso != null && !iso.matches("[1-9A-Z][0-9A-Z]{3}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ISO ID deve possuir exatamente 4 caracteres alfanuméricos"); return iso; }
    private void validarPositivo(Integer v, String c) { if (v != null && v <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, c + " deve ser maior que zero"); }
    private void validarNaoNegativo(BigDecimal v, String c) { if (v != null && v.signum() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, c + " não pode ser negativa"); }
    private String obrigatorio(String v, String m) { if (!StringUtils.hasText(v)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, m); return v.trim(); }
    private String normalizar(String v) { return StringUtils.hasText(v) ? v.trim().toUpperCase(Locale.ROOT) : null; }
    private boolean contem(String v, String f) { return v != null && v.toUpperCase(Locale.ROOT).contains(f); }
    private boolean igual(String a, String b) { return a == null ? b == null : a.equalsIgnoreCase(b); }
    private String usuario(String u) { return StringUtils.hasText(u) ? u.trim() : "SISTEMA"; }
    private void conflito(String m) { throw new ResponseStatusException(HttpStatus.CONFLICT, m); }
}