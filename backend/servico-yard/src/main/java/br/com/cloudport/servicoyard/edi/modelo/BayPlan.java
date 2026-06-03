package br.com.cloudport.servicoyard.edi.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "bay_plan")
public class BayPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_navio", nullable = false, length = 50)
    private String codigoNavio;

    @Column(name = "nome_navio", length = 100)
    private String nomeNavio;

    @Column(name = "codigo_viagem", nullable = false, length = 30)
    private String codigoViagem;

    @Column(name = "porto_carga", length = 10)
    private String portoCarga;

    @Column(name = "porto_descarga", length = 10)
    private String portoDescarga;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusBayPlan status;

    @Column(name = "origem_mensagem", length = 20)
    private String origemMensagem;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Version
    @Column(name = "versao")
    private Long versao;

    @OneToMany(mappedBy = "bayPlan", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    private List<BayPlanContainer> containers = new ArrayList<>();

    @PrePersist
    @PreUpdate
    void touch() {
        atualizadoEm = LocalDateTime.now();
        if (criadoEm == null) criadoEm = atualizadoEm;
    }

    public Long getId() { return id; }
    public String getCodigoNavio() { return codigoNavio; }
    public void setCodigoNavio(String codigoNavio) { this.codigoNavio = codigoNavio; }
    public String getNomeNavio() { return nomeNavio; }
    public void setNomeNavio(String nomeNavio) { this.nomeNavio = nomeNavio; }
    public String getCodigoViagem() { return codigoViagem; }
    public void setCodigoViagem(String codigoViagem) { this.codigoViagem = codigoViagem; }
    public String getPortoCarga() { return portoCarga; }
    public void setPortoCarga(String portoCarga) { this.portoCarga = portoCarga; }
    public String getPortoDescarga() { return portoDescarga; }
    public void setPortoDescarga(String portoDescarga) { this.portoDescarga = portoDescarga; }
    public StatusBayPlan getStatus() { return status; }
    public void setStatus(StatusBayPlan status) { this.status = status; }
    public String getOrigemMensagem() { return origemMensagem; }
    public void setOrigemMensagem(String origemMensagem) { this.origemMensagem = origemMensagem; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public Long getVersao() { return versao; }
    public List<BayPlanContainer> getContainers() { return containers; }
    public void setContainers(List<BayPlanContainer> containers) { this.containers = containers; }

    public void adicionarContainer(BayPlanContainer c) {
        c.setBayPlan(this);
        containers.add(c);
    }
}
