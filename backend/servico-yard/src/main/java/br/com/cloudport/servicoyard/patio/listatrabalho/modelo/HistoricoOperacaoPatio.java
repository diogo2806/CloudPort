package br.com.cloudport.servicoyard.patio.listatrabalho.modelo;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "historico_operacao_patio")
public class HistoricoOperacaoPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_queue_id")
    private Long workQueueId;

    @Column(name = "ordem_trabalho_patio_id")
    private Long ordemTrabalhoPatioId;

    @Column(name = "acao", nullable = false, length = 50)
    private String acao;

    @Column(name = "usuario", nullable = false, length = 120)
    private String usuario;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "detalhes", length = 2000)
    private String detalhes;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public Long getWorkQueueId() { return workQueueId; }
    public void setWorkQueueId(Long workQueueId) { this.workQueueId = workQueueId; }
    public Long getOrdemTrabalhoPatioId() { return ordemTrabalhoPatioId; }
    public void setOrdemTrabalhoPatioId(Long ordemTrabalhoPatioId) { this.ordemTrabalhoPatioId = ordemTrabalhoPatioId; }
    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
