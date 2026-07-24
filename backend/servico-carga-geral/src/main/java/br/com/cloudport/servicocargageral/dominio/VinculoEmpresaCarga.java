package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.Empresa.PapelEmpresa;
import java.time.OffsetDateTime;
import java.util.UUID;
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
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(name = "vinculo_empresa_carga", uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_vinculo_empresa_carga_recurso_papel",
        columnNames = {"tipo_recurso", "recurso_id", "papel"})
})
public class VinculoEmpresaCarga {

    public enum TipoRecursoCarga {
        CONHECIMENTO,
        LOTE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_recurso", nullable = false, length = 30)
    private TipoRecursoCarga tipoRecurso;

    @Column(name = "recurso_id", nullable = false)
    private UUID recursoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PapelEmpresa papel;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "empresa_nome_snapshot", nullable = false, length = 180)
    private String empresaNomeSnapshot;

    @Column(name = "vinculado_por", nullable = false, length = 180)
    private String vinculadoPor;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Version
    @Column(nullable = false)
    private long versao;

    protected VinculoEmpresaCarga() {
    }

    public static VinculoEmpresaCarga criar(
            TipoRecursoCarga tipoRecurso,
            UUID recursoId,
            PapelEmpresa papel,
            Empresa empresa,
            String usuario) {
        VinculoEmpresaCarga vinculo = new VinculoEmpresaCarga();
        vinculo.tipoRecurso = tipoRecurso;
        vinculo.recursoId = recursoId;
        vinculo.papel = papel;
        vinculo.alterarEmpresa(empresa, usuario);
        return vinculo;
    }

    public void alterarEmpresa(Empresa empresa, String usuario) {
        if (empresa == null) {
            throw new IllegalArgumentException("A empresa deve ser informada.");
        }
        this.empresa = empresa;
        this.empresaNomeSnapshot = nomeEmpresa(empresa);
        this.vinculadoPor = usuario == null || usuario.isBlank() ? "sistema" : usuario.trim();
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = OffsetDateTime.now();
    }

    private static String nomeEmpresa(Empresa empresa) {
        if (empresa.getNomeFantasia() != null && !empresa.getNomeFantasia().isBlank()) {
            return empresa.getNomeFantasia().trim();
        }
        return empresa.getRazaoSocial();
    }

    public UUID getId() { return id; }
    public TipoRecursoCarga getTipoRecurso() { return tipoRecurso; }
    public UUID getRecursoId() { return recursoId; }
    public PapelEmpresa getPapel() { return papel; }
    public Empresa getEmpresa() { return empresa; }
    public String getEmpresaNomeSnapshot() { return empresaNomeSnapshot; }
    public String getVinculadoPor() { return vinculadoPor; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
    public long getVersao() { return versao; }
}
