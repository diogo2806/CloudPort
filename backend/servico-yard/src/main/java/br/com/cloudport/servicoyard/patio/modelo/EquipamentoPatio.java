package br.com.cloudport.servicoyard.patio.modelo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "equipamento_patio")
public class EquipamentoPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identificador", nullable = false, length = 30, unique = true)
    private String identificador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_equipamento", nullable = false, length = 30)
    private TipoEquipamento tipoEquipamento;

    @Column(name = "linha", nullable = false)
    private Integer linha;

    @Column(name = "coluna", nullable = false)
    private Integer coluna;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_operacional", nullable = false, length = 30)
    private StatusEquipamento statusOperacional;

    public EquipamentoPatio() {
    }

    public EquipamentoPatio(Long id, String identificador, TipoEquipamento tipoEquipamento, Integer linha,
                             Integer coluna, StatusEquipamento statusOperacional) {
        this.id = id;
        this.identificador = identificador;
        this.tipoEquipamento = tipoEquipamento;
        this.linha = linha;
        this.coluna = coluna;
        this.statusOperacional = statusOperacional;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public TipoEquipamento getTipoEquipamento() {
        return tipoEquipamento;
    }

    public void setTipoEquipamento(TipoEquipamento tipoEquipamento) {
        this.tipoEquipamento = tipoEquipamento;
    }

    public Integer getLinha() {
        return linha;
    }

    public void setLinha(Integer linha) {
        this.linha = linha;
    }

    public Integer getColuna() {
        return coluna;
    }

    public void setColuna(Integer coluna) {
        this.coluna = coluna;
    }

    public StatusEquipamento getStatusOperacional() {
        return statusOperacional;
    }

    public void setStatusOperacional(StatusEquipamento statusOperacional) {
        this.statusOperacional = statusOperacional;
    }
}
