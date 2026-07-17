package br.com.cloudport.servicogate.model;

import br.com.cloudport.servicogate.model.enums.DirecaoMovimentacaoPessoa;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "movimentacao_pessoa_acesso")
public class MovimentacaoPessoaAcesso extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_acesso_id", nullable = false)
    private PessoaAcesso pessoa;

    @Enumerated(EnumType.STRING)
    @Column(name = "direcao", nullable = false, length = 20)
    private DirecaoMovimentacaoPessoa direcao;

    @Column(name = "ponto_acesso", nullable = false, length = 120)
    private String pontoAcesso;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    @Column(name = "usuario_responsavel", nullable = false, length = 120)
    private String usuarioResponsavel;

    @Column(name = "origem_acao", length = 80)
    private String origemAcao;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "permanencia_minutos")
    private Long permanenciaMinutos;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PessoaAcesso getPessoa() {
        return pessoa;
    }

    public void setPessoa(PessoaAcesso pessoa) {
        this.pessoa = pessoa;
    }

    public DirecaoMovimentacaoPessoa getDirecao() {
        return direcao;
    }

    public void setDirecao(DirecaoMovimentacaoPessoa direcao) {
        this.direcao = direcao;
    }

    public String getPontoAcesso() {
        return pontoAcesso;
    }

    public void setPontoAcesso(String pontoAcesso) {
        this.pontoAcesso = pontoAcesso;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }

    public void setRegistradoEm(LocalDateTime registradoEm) {
        this.registradoEm = registradoEm;
    }

    public String getUsuarioResponsavel() {
        return usuarioResponsavel;
    }

    public void setUsuarioResponsavel(String usuarioResponsavel) {
        this.usuarioResponsavel = usuarioResponsavel;
    }

    public String getOrigemAcao() {
        return origemAcao;
    }

    public void setOrigemAcao(String origemAcao) {
        this.origemAcao = origemAcao;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Long getPermanenciaMinutos() {
        return permanenciaMinutos;
    }

    public void setPermanenciaMinutos(Long permanenciaMinutos) {
        this.permanenciaMinutos = permanenciaMinutos;
    }
}
