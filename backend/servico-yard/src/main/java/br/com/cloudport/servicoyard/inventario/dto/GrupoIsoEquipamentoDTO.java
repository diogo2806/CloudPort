package br.com.cloudport.servicoyard.inventario.dto;

import br.com.cloudport.servicoyard.inventario.modelo.TipoEquipamentoInventario;
import java.time.LocalDateTime;

public final class GrupoIsoEquipamentoDTO {

    private GrupoIsoEquipamentoDTO() {
    }

    public static class SalvarRequest {
        private String codigo;
        private String descricao;
        private TipoEquipamentoInventario.CategoriaEquipamento categoria;
        private boolean refrigerado;

        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        public TipoEquipamentoInventario.CategoriaEquipamento getCategoria() { return categoria; }
        public void setCategoria(TipoEquipamentoInventario.CategoriaEquipamento categoria) { this.categoria = categoria; }
        public boolean isRefrigerado() { return refrigerado; }
        public void setRefrigerado(boolean refrigerado) { this.refrigerado = refrigerado; }
    }

    public static class Resposta {
        private final Long id;
        private final String codigo;
        private final String descricao;
        private final TipoEquipamentoInventario.CategoriaEquipamento categoria;
        private final boolean refrigerado;
        private final boolean ativo;
        private final String criadoPor;
        private final String atualizadoPor;
        private final LocalDateTime criadoEm;
        private final LocalDateTime atualizadoEm;
        private final long tiposAssociados;

        public Resposta(Long id, String codigo, String descricao,
                TipoEquipamentoInventario.CategoriaEquipamento categoria, boolean refrigerado,
                boolean ativo, String criadoPor, String atualizadoPor, LocalDateTime criadoEm,
                LocalDateTime atualizadoEm, long tiposAssociados) {
            this.id = id;
            this.codigo = codigo;
            this.descricao = descricao;
            this.categoria = categoria;
            this.refrigerado = refrigerado;
            this.ativo = ativo;
            this.criadoPor = criadoPor;
            this.atualizadoPor = atualizadoPor;
            this.criadoEm = criadoEm;
            this.atualizadoEm = atualizadoEm;
            this.tiposAssociados = tiposAssociados;
        }

        public Long getId() { return id; }
        public String getCodigo() { return codigo; }
        public String getDescricao() { return descricao; }
        public TipoEquipamentoInventario.CategoriaEquipamento getCategoria() { return categoria; }
        public boolean isRefrigerado() { return refrigerado; }
        public boolean isAtivo() { return ativo; }
        public String getCriadoPor() { return criadoPor; }
        public String getAtualizadoPor() { return atualizadoPor; }
        public LocalDateTime getCriadoEm() { return criadoEm; }
        public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
        public long getTiposAssociados() { return tiposAssociados; }
    }
}
