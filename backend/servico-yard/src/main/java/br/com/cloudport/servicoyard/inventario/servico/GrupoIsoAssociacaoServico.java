package br.com.cloudport.servicoyard.inventario.servico;

import br.com.cloudport.servicoyard.inventario.modelo.GrupoIsoEquipamento;
import br.com.cloudport.servicoyard.inventario.modelo.TipoEquipamentoInventario;
import br.com.cloudport.servicoyard.inventario.repositorio.GrupoIsoEquipamentoRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.TipoEquipamentoInventarioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GrupoIsoAssociacaoServico {

    private final GrupoIsoEquipamentoRepositorio grupoRepositorio;
    private final TipoEquipamentoInventarioRepositorio tipoRepositorio;
    private final JdbcTemplate jdbcTemplate;

    public GrupoIsoAssociacaoServico(GrupoIsoEquipamentoRepositorio grupoRepositorio,
            TipoEquipamentoInventarioRepositorio tipoRepositorio, JdbcTemplate jdbcTemplate) {
        this.grupoRepositorio = grupoRepositorio;
        this.tipoRepositorio = tipoRepositorio;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void associar(Long tipoId, Long grupoIsoId) {
        TipoEquipamentoInventario tipo = tipoRepositorio.findById(tipoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de equipamento não encontrado"));
        GrupoIsoEquipamento grupo = grupoRepositorio.findById(grupoIsoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo ISO não encontrado"));
        if (!grupo.isAtivo()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Grupo ISO inativo não pode ser associado a um tipo");
        }
        if (grupo.getCategoria() != tipo.getCategoria()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A categoria do Grupo ISO é incompatível com o tipo de equipamento");
        }
        if (grupo.isRefrigerado() && !tipo.isRefrigerado()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Grupo ISO refrigerado exige tipo de equipamento refrigerado");
        }
        jdbcTemplate.update("update tipo_equipamento_inventario set grupo_iso_id = ? where id = ?", grupoIsoId, tipoId);
    }

    @Transactional
    public void desassociar(Long tipoId) {
        if (!tipoRepositorio.existsById(tipoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de equipamento não encontrado");
        }
        jdbcTemplate.update("update tipo_equipamento_inventario set grupo_iso_id = null where id = ?", tipoId);
    }
}
