package br.com.cloudport.serviconavio.linha.entidade;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Escala (porto) na rotação de um serviço de linha. A rotação é o itinerário
 * ordenado de portos que a linha percorre (UN/LOCODE + nome).
 */
@Entity
@Table(name = "porto_rotacao")
public class PortoRotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Long identificador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_linha_id")
    private ServicoLinha servicoLinha;

    @Column(name = "sequencia", nullable = false)
    private Integer sequencia;

    @Column(name = "porto_unloc", nullable = false, length = 10)
    private String portoUnloc;

    @Column(name = "nome_porto", length = 80)
    private String nomePorto;

    public Long getIdentificador() {
        return identificador;
    }

    public void setIdentificador(Long identificador) {
        this.identificador = identificador;
    }

    public ServicoLinha getServicoLinha() {
        return servicoLinha;
    }

    public void setServicoLinha(ServicoLinha servicoLinha) {
        this.servicoLinha = servicoLinha;
    }

    public Integer getSequencia() {
        return sequencia;
    }

    public void setSequencia(Integer sequencia) {
        this.sequencia = sequencia;
    }

    public String getPortoUnloc() {
        return portoUnloc;
    }

    public void setPortoUnloc(String portoUnloc) {
        this.portoUnloc = portoUnloc;
    }

    public String getNomePorto() {
        return nomePorto;
    }

    public void setNomePorto(String nomePorto) {
        this.nomePorto = nomePorto;
    }
}
