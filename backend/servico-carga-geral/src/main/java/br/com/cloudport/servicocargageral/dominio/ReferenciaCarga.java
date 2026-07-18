package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.CategoriaReferenciaCarga;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "referencia_carga", uniqueConstraints = {
        @UniqueConstraint(name = "uk_referencia_carga_categoria_codigo", columnNames = {"categoria", "codigo"})
})
public class ReferenciaCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategoriaReferenciaCarga categoria;

    @Column(nullable = false, length = 80)
    private String codigo;

    @Column(nullable = false, length = 240)
    private String descricao;

    @Column(name = "atributos_json", columnDefinition = "text")
    private String atributosJson;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        codigo = normalizarCodigo(codigo);
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
        codigo = normalizarCodigo(codigo);
    }

    private String normalizarCodigo(String valor) {
        return valor == null ? null : valor.trim().toUpperCase();
    }

    public UUID getId() { return id; }
    public CategoriaReferenciaCarga getCategoria() { return categoria; }
    public void setCategoria(CategoriaReferenciaCarga categoria) { this.categoria = categoria; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getAtributosJson() { return atributosJson; }
    public void setAtributosJson(String atributosJson) { this.atributosJson = atributosJson; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
