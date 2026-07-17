package br.com.cloudport.servicogate.model;

import br.com.cloudport.servicogate.model.enums.SituacaoPessoaAcesso;
import br.com.cloudport.servicogate.model.enums.TipoPessoaAcesso;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "pessoa_acesso")
public class PessoaAcesso extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 140)
    private String nome;

    @Column(name = "documento", nullable = false, length = 30)
    private String documento;

    @Column(name = "documento_normalizado", nullable = false, unique = true, length = 30)
    private String documentoNormalizado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false, length = 30)
    private TipoPessoaAcesso tipoPessoa;

    @Column(name = "empresa", length = 140)
    private String empresa;

    @Column(name = "cracha", length = 50)
    private String cracha;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 20)
    private SituacaoPessoaAcesso situacao;

    @Column(name = "ultimo_acesso_em", nullable = false)
    private LocalDateTime ultimoAcessoEm;

    @Column(name = "ultimo_ponto_acesso", nullable = false, length = 120)
    private String ultimoPontoAcesso;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getDocumentoNormalizado() {
        return documentoNormalizado;
    }

    public void setDocumentoNormalizado(String documentoNormalizado) {
        this.documentoNormalizado = documentoNormalizado;
    }

    public TipoPessoaAcesso getTipoPessoa() {
        return tipoPessoa;
    }

    public void setTipoPessoa(TipoPessoaAcesso tipoPessoa) {
        this.tipoPessoa = tipoPessoa;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getCracha() {
        return cracha;
    }

    public void setCracha(String cracha) {
        this.cracha = cracha;
    }

    public SituacaoPessoaAcesso getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoPessoaAcesso situacao) {
        this.situacao = situacao;
    }

    public LocalDateTime getUltimoAcessoEm() {
        return ultimoAcessoEm;
    }

    public void setUltimoAcessoEm(LocalDateTime ultimoAcessoEm) {
        this.ultimoAcessoEm = ultimoAcessoEm;
    }

    public String getUltimoPontoAcesso() {
        return ultimoPontoAcesso;
    }

    public void setUltimoPontoAcesso(String ultimoPontoAcesso) {
        this.ultimoPontoAcesso = ultimoPontoAcesso;
    }

    public Long getVersao() {
        return versao;
    }

    public void setVersao(Long versao) {
        this.versao = versao;
    }
}
