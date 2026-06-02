package br.com.cloudport.servicoyard.recursos.entidade;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "equipamento_berco")
public class EquipamentoBerco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identificador", nullable = false, unique = true, length = 30)
    private String identificador;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "berco_codigo", nullable = false, length = 30)
    private String bercoCodigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusEquipamentoBerco status;

    @Column(name = "ultima_verificacao", nullable = false)
    private LocalDateTime ultimaVerificacao;

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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getBercoCodigo() {
        return bercoCodigo;
    }

    public void setBercoCodigo(String bercoCodigo) {
        this.bercoCodigo = bercoCodigo;
    }

    public StatusEquipamentoBerco getStatus() {
        return status;
    }

    public void setStatus(StatusEquipamentoBerco status) {
        this.status = status;
    }

    public LocalDateTime getUltimaVerificacao() {
        return ultimaVerificacao;
    }

    public void setUltimaVerificacao(LocalDateTime ultimaVerificacao) {
        this.ultimaVerificacao = ultimaVerificacao;
    }
}
