package br.com.cloudport.servicoyard.inventario.servico;

import br.com.cloudport.servicoyard.inventario.dto.GrupoIsoEquipamentoDTO;
import br.com.cloudport.servicoyard.inventario.modelo.GrupoIsoEquipamento;
import br.com.cloudport.servicoyard.inventario.repositorio.GrupoIsoEquipamentoRepositorio;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrupoIsoEquipamentoServico {

    private final GrupoIsoEquipamentoRepositorio repositorio;
    private final JdbcTemplate jdbcTemplate;

    public GrupoIsoEquipamentoServico(GrupoIsoEquipamentoRepositorio repositorio, JdbcTemplate jdbcTemplate) {
        this.repositorio = repositorio;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<GrupoIsoEquipamentoDTO.Resposta> listar(String termo, String categoria, Boolean ativo) {
        String filtro = normalizarOpcional(termo);
        return repositorio.findAllByOrderByCodigoAsc().stream()
                .filter(grupo -> filtro == null
                        || grupo.getCodigo().toUpperCase(Locale.ROOT).contains(filtro)
                        || grupo.getDescricao().toUpperCase(Locale.ROOT).contains(filtro))
                .filter(grupo -> categoria == null || categoria.isBlank()
                        || grupo.getCategoria().name().equalsIgnoreCase(categoria))
                .filter(grupo -> ativo == null || grupo.isAtivo() == ativo)
                .map(this::resposta)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GrupoIsoEquipamentoDTO.Resposta detalhar(Long id) {
        return resposta(buscar(id));
    }

    @Transactional
    public GrupoIsoEquipamentoDTO.Resposta criar(GrupoIsoEquipamentoDTO.SalvarRequest request, String usuario) {
        validar(request);
        String codigo = normalizarObrigatorio(request.getCodigo(), "O código do Grupo ISO deve ser informado.");
        if (repositorio.existsByCodigoIgnoreCase(codigo)) {
            throw new IllegalArgumentException("Já existe um Grupo ISO com o código informado.");
        }
        GrupoIsoEquipamento grupo = new GrupoIsoEquipamento();
        aplicar(grupo, request, usuario);
        grupo.setCodigo(codigo);
        grupo.setAtivo(true);
        grupo.setCriadoPor(usuario);
        return resposta(repositorio.save(grupo));
    }

    @Transactional
    public GrupoIsoEquipamentoDTO.Resposta atualizar(Long id, GrupoIsoEquipamentoDTO.SalvarRequest request, String usuario) {
        validar(request);
        GrupoIsoEquipamento grupo = buscar(id);
        String codigo = normalizarObrigatorio(request.getCodigo(), "O código do Grupo ISO deve ser informado.");
        repositorio.findByCodigoIgnoreCase(codigo)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> { throw new IllegalArgumentException("Já existe um Grupo ISO com o código informado."); });
        grupo.setCodigo(codigo);
        aplicar(grupo, request, usuario);
        return resposta(repositorio.save(grupo));
    }

    @Transactional
    public GrupoIsoEquipamentoDTO.Resposta alterarSituacao(Long id, boolean ativo, String usuario) {
        GrupoIsoEquipamento grupo = buscar(id);
        grupo.setAtivo(ativo);
        grupo.setAtualizadoPor(usuario);
        return resposta(repositorio.save(grupo));
    }

    @Transactional
    public void excluir(Long id) {
        GrupoIsoEquipamento grupo = buscar(id);
        long associados = contarTiposAssociados(id);
        if (associados > 0) {
            throw new IllegalStateException("Grupo ISO em uso por tipos de equipamento; utilize a inativação.");
        }
        repositorio.delete(grupo);
    }

    private void aplicar(GrupoIsoEquipamento grupo, GrupoIsoEquipamentoDTO.SalvarRequest request, String usuario) {
        grupo.setDescricao(normalizarObrigatorio(request.getDescricao(), "A descrição deve ser informada."));
        grupo.setCategoria(request.getCategoria());
        grupo.setRefrigerado(request.isRefrigerado());
        grupo.setAtualizadoPor(usuario);
    }

    private void validar(GrupoIsoEquipamentoDTO.SalvarRequest request) {
        if (request == null) throw new IllegalArgumentException("Dados do Grupo ISO devem ser informados.");
        if (request.getCategoria() == null) throw new IllegalArgumentException("A categoria deve ser informada.");
    }

    private GrupoIsoEquipamento buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Grupo ISO não encontrado."));
    }

    private GrupoIsoEquipamentoDTO.Resposta resposta(GrupoIsoEquipamento grupo) {
        return new GrupoIsoEquipamentoDTO.Resposta(grupo.getId(), grupo.getCodigo(), grupo.getDescricao(),
                grupo.getCategoria(), grupo.isRefrigerado(), grupo.isAtivo(), grupo.getCriadoPor(),
                grupo.getAtualizadoPor(), grupo.getCriadoEm(), grupo.getAtualizadoEm(), contarTiposAssociados(grupo.getId()));
    }

    private long contarTiposAssociados(Long id) {
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from tipo_equipamento_inventario where grupo_iso_id = ?", Long.class, id);
        return total == null ? 0L : total;
    }

    private static String normalizarObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.trim().isEmpty()) throw new IllegalArgumentException(mensagem);
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizarOpcional(String valor) {
        return valor == null || valor.trim().isEmpty() ? null : valor.trim().toUpperCase(Locale.ROOT);
    }
}
