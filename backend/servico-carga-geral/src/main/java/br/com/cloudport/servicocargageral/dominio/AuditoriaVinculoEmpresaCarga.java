package br.com.cloudport.servicocargageral.dominio;

import br.com.cloudport.servicocargageral.dominio.Empresa.PapelEmpresa;
import br.com.cloudport.servicocargageral.dominio.VinculoEmpresaCarga.TipoRecursoCarga;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "auditoria_vinculo_empresa_carga")
public class AuditoriaVinculoEmpresaCarga {

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

    @Column(name = "empresa_anterior_id")
    private UUID empresaAnteriorId;

    @Column(name = "empresa_nova_id")
    private UUID empresaNovaId;

    @Column(nullable = false, length = 30)
    private String acao;

    @Column(nullable = false, length = 180)
    private String usuario;

    @Column(name = "ocorrido_em", nullable = false)
    private OffsetDateTime ocorridoEm;

    protected AuditoriaVinculoEmpresaCarga() {
    }

    public static AuditoriaVinculoEmpresaCarga registrar(
            TipoRecursoCarga tipoRecurso,
            UUID recursoId,
            PapelEmpresa papel,
            UUID empresaAnteriorId,
            UUID empresaNovaId,
            String acao,
            String usuario) {
        AuditoriaVinculoEmpresaCarga auditoria = new AuditoriaVinculoEmpresaCarga();
        auditoria.tipoRecurso = tipoRecurso;
        auditoria.recursoId = recursoId;
        auditoria.papel = papel;
        auditoria.empresaAnteriorId = empresaAnteriorId;
        auditoria.empresaNovaId = empresaNovaId;
        auditoria.acao = acao;
        auditoria.usuario = usuario == null || usuario.isBlank() ? "sistema" : usuario.trim();
        return auditoria;
    }

    @PrePersist
    void prePersist() {
        ocorridoEm = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public TipoRecursoCarga getTipoRecurso() { return tipoRecurso; }
    public UUID getRecursoId() { return recursoId; }
    public PapelEmpresa getPapel() { return papel; }
    public UUID getEmpresaAnteriorId() { return empresaAnteriorId; }
    public UUID getEmpresaNovaId() { return empresaNovaId; }
    public String getAcao() { return acao; }
    public String getUsuario() { return usuario; }
    public OffsetDateTime getOcorridoEm() { return ocorridoEm; }
}
