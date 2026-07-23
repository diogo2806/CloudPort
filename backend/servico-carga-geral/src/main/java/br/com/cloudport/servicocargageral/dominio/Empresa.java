package br.com.cloudport.servicocargageral.dominio;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "empresa", uniqueConstraints = {
    @UniqueConstraint(name = "uk_empresa_codigo", columnNames = "codigo"),
    @UniqueConstraint(name = "uk_empresa_documento", columnNames = "documento_normalizado")
})
public class Empresa {

    public enum PapelEmpresa {
        CLIENTE, EMBARCADOR, CONSIGNATARIO, IMPORTADOR, EXPORTADOR,
        DONO_CARGA, OPERADOR, AGENTE, TRANSPORTADORA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(name = "razao_social", nullable = false, length = 180)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 180)
    private String nomeFantasia;

    @Column(nullable = false, length = 40)
    private String documento;

    @Column(name = "documento_normalizado", nullable = false, length = 40)
    private String documentoNormalizado;

    @Column(name = "inscricao_estadual", length = 40)
    private String inscricaoEstadual;

    @Column(length = 500)
    private String endereco;

    @Column(length = 180)
    private String contato;

    @Column(length = 180)
    private String email;

    @Column(length = 40)
    private String telefone;

    @Column(nullable = false, length = 80)
    private String pais = "BRASIL";

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(length = 1000)
    private String observacoes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "empresa_papel", joinColumns = @JoinColumn(name = "empresa_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false, length = 30)
    private Set<PapelEmpresa> papeis = new LinkedHashSet<>();

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
        normalizar();
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
        normalizar();
    }

    private void normalizar() {
        codigo = texto(codigo).toUpperCase();
        razaoSocial = texto(razaoSocial);
        nomeFantasia = texto(nomeFantasia);
        documento = texto(documento);
        documentoNormalizado = documento.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        pais = texto(pais).toUpperCase();
    }

    private String texto(String valor) { return valor == null ? "" : valor.trim(); }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }
    public String getNomeFantasia() { return nomeFantasia; }
    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getDocumentoNormalizado() { return documentoNormalizado; }
    public String getInscricaoEstadual() { return inscricaoEstadual; }
    public void setInscricaoEstadual(String inscricaoEstadual) { this.inscricaoEstadual = inscricaoEstadual; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public String getContato() { return contato; }
    public void setContato(String contato) { this.contato = contato; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public Set<PapelEmpresa> getPapeis() { return papeis; }
    public void setPapeis(Set<PapelEmpresa> papeis) { this.papeis = papeis == null ? new LinkedHashSet<>() : new LinkedHashSet<>(papeis); }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}