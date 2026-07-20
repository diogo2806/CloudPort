package br.com.cloudport.servicoyard.operacao2d;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "workspace_grafico_2d", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_workspace_grafico_2d_versao",
                columnNames = {"nome", "escopo", "proprietario", "versao"})
})
public class WorkspaceGrafico2D {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 160)
    private String nome;

    @Column(name = "escopo", nullable = false, length = 20)
    private String escopo;

    @Column(name = "papel", length = 80)
    private String papel;

    @Column(name = "proprietario", nullable = false, length = 120)
    private String proprietario;

    @Column(name = "versao", nullable = false)
    private Long versao;

    @Column(name = "conteudo_json", nullable = false, columnDefinition = "text")
    private String conteudoJson;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prepararInclusao() {
        criadoEm = criadoEm == null ? LocalDateTime.now() : criadoEm;
        versao = versao == null ? 1L : versao;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEscopo() { return escopo; }
    public void setEscopo(String escopo) { this.escopo = escopo; }
    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }
    public String getProprietario() { return proprietario; }
    public void setProprietario(String proprietario) { this.proprietario = proprietario; }
    public Long getVersao() { return versao; }
    public void setVersao(Long versao) { this.versao = versao; }
    public String getConteudoJson() { return conteudoJson; }
    public void setConteudoJson(String conteudoJson) { this.conteudoJson = conteudoJson; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
