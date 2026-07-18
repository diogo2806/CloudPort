package br.com.cloudport.servicoyard.inventario.modelo;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "prefixo_equipamento_inventario")
public class PrefixoEquipamentoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prefixo", nullable = false, length = 12, unique = true)
    private String prefixo;

    @Column(name = "proprietario", length = 120)
    private String proprietario;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 30)
    private TipoEquipamentoInventario.CategoriaEquipamento categoria;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        criadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrefixo() {
        return prefixo;
    }

    public void setPrefixo(String prefixo) {
        this.prefixo = prefixo;
    }

    public String getProprietario() {
        return proprietario;
    }

    public void setProprietario(String proprietario) {
        this.proprietario = proprietario;
    }

    public TipoEquipamentoInventario.CategoriaEquipamento getCategoria() {
        return categoria;
    }

    public void setCategoria(TipoEquipamentoInventario.CategoriaEquipamento categoria) {
        this.categoria = categoria;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
