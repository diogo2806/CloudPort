package br.com.cloudport.servicorail.ferrovia.inspecao.dto;

import br.com.cloudport.servicorail.ferrovia.inspecao.modelo.DefeitoInspecaoVagao;
import br.com.cloudport.servicorail.ferrovia.inspecao.modelo.DefeitoInspecaoVagao.SeveridadeDefeitoVagao;
import br.com.cloudport.servicorail.ferrovia.inspecao.modelo.InspecaoVagao;
import br.com.cloudport.servicorail.ferrovia.inspecao.modelo.InspecaoVagao.StatusInspecaoVagao;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class InspecaoVagaoDto {

    private InspecaoVagaoDto() {
    }

    public static class Registro {

        @NotBlank
        @Size(max = 35)
        private String identificadorVagao;

        @NotNull
        private Boolean rodasAprovadas;

        @NotNull
        private Boolean freiosAprovados;

        @NotNull
        private Boolean engatesAprovados;

        @NotNull
        private Boolean estruturaAprovada;

        @NotNull
        private Boolean lacresAprovados;

        @NotBlank
        @Size(max = 120)
        private String responsavel;

        @Size(max = 1000)
        private String observacao;

        @Valid
        private List<Defeito> defeitos;

        public String getIdentificadorVagao() {
            return identificadorVagao;
        }

        public void setIdentificadorVagao(String identificadorVagao) {
            this.identificadorVagao = identificadorVagao;
        }

        public Boolean getRodasAprovadas() {
            return rodasAprovadas;
        }

        public void setRodasAprovadas(Boolean rodasAprovadas) {
            this.rodasAprovadas = rodasAprovadas;
        }

        public Boolean getFreiosAprovados() {
            return freiosAprovados;
        }

        public void setFreiosAprovados(Boolean freiosAprovados) {
            this.freiosAprovados = freiosAprovados;
        }

        public Boolean getEngatesAprovados() {
            return engatesAprovados;
        }

        public void setEngatesAprovados(Boolean engatesAprovados) {
            this.engatesAprovados = engatesAprovados;
        }

        public Boolean getEstruturaAprovada() {
            return estruturaAprovada;
        }

        public void setEstruturaAprovada(Boolean estruturaAprovada) {
            this.estruturaAprovada = estruturaAprovada;
        }

        public Boolean getLacresAprovados() {
            return lacresAprovados;
        }

        public void setLacresAprovados(Boolean lacresAprovados) {
            this.lacresAprovados = lacresAprovados;
        }

        public String getResponsavel() {
            return responsavel;
        }

        public void setResponsavel(String responsavel) {
            this.responsavel = responsavel;
        }

        public String getObservacao() {
            return observacao;
        }

        public void setObservacao(String observacao) {
            this.observacao = observacao;
        }

        public List<Defeito> getDefeitos() {
            return defeitos;
        }

        public void setDefeitos(List<Defeito> defeitos) {
            this.defeitos = defeitos;
        }
    }

    public static class Defeito {

        @NotBlank
        @Size(max = 40)
        private String codigo;

        @NotBlank
        @Size(max = 500)
        private String descricao;

        @NotNull
        private SeveridadeDefeitoVagao severidade;

        @Size(max = 500)
        private String evidencia;

        public String getCodigo() {
            return codigo;
        }

        public void setCodigo(String codigo) {
            this.codigo = codigo;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public SeveridadeDefeitoVagao getSeveridade() {
            return severidade;
        }

        public void setSeveridade(SeveridadeDefeitoVagao severidade) {
            this.severidade = severidade;
        }

        public String getEvidencia() {
            return evidencia;
        }

        public void setEvidencia(String evidencia) {
            this.evidencia = evidencia;
        }
    }

    public static class LiberacaoOverride {

        @NotBlank
        @Size(max = 120)
        private String responsavel;

        @NotBlank
        @Size(max = 500)
        private String motivo;

        public String getResponsavel() {
            return responsavel;
        }

        public void setResponsavel(String responsavel) {
            this.responsavel = responsavel;
        }

        public String getMotivo() {
            return motivo;
        }

        public void setMotivo(String motivo) {
            this.motivo = motivo;
        }
    }

    public static class Resposta {

        private final Long id;
        private final Long idVisitaTrem;
        private final String identificadorVagao;
        private final StatusInspecaoVagao status;
        private final Boolean rodasAprovadas;
        private final Boolean freiosAprovados;
        private final Boolean engatesAprovados;
        private final Boolean estruturaAprovada;
        private final Boolean lacresAprovados;
        private final String responsavel;
        private final String observacao;
        private final LocalDateTime inspecionadoEm;
        private final String overridePor;
        private final String overrideMotivo;
        private final LocalDateTime liberadoEm;
        private final List<DefeitoResposta> defeitos;
        private final Long versao;

        private Resposta(InspecaoVagao entidade) {
            this.id = entidade.getId();
            this.idVisitaTrem = entidade.getVisitaTrem() != null ? entidade.getVisitaTrem().getId() : null;
            this.identificadorVagao = entidade.getIdentificadorVagao();
            this.status = entidade.getStatus();
            this.rodasAprovadas = entidade.getRodasAprovadas();
            this.freiosAprovados = entidade.getFreiosAprovados();
            this.engatesAprovados = entidade.getEngatesAprovados();
            this.estruturaAprovada = entidade.getEstruturaAprovada();
            this.lacresAprovados = entidade.getLacresAprovados();
            this.responsavel = entidade.getResponsavel();
            this.observacao = entidade.getObservacao();
            this.inspecionadoEm = entidade.getInspecionadoEm();
            this.overridePor = entidade.getOverridePor();
            this.overrideMotivo = entidade.getOverrideMotivo();
            this.liberadoEm = entidade.getLiberadoEm();
            this.defeitos = entidade.getDefeitos() == null
                    ? Collections.emptyList()
                    : entidade.getDefeitos().stream().map(DefeitoResposta::new).collect(Collectors.toList());
            this.versao = entidade.getVersao();
        }

        public static Resposta deEntidade(InspecaoVagao entidade) {
            return new Resposta(entidade);
        }

        public Long getId() {
            return id;
        }

        public Long getIdVisitaTrem() {
            return idVisitaTrem;
        }

        public String getIdentificadorVagao() {
            return identificadorVagao;
        }

        public StatusInspecaoVagao getStatus() {
            return status;
        }

        public Boolean getRodasAprovadas() {
            return rodasAprovadas;
        }

        public Boolean getFreiosAprovados() {
            return freiosAprovados;
        }

        public Boolean getEngatesAprovados() {
            return engatesAprovados;
        }

        public Boolean getEstruturaAprovada() {
            return estruturaAprovada;
        }

        public Boolean getLacresAprovados() {
            return lacresAprovados;
        }

        public String getResponsavel() {
            return responsavel;
        }

        public String getObservacao() {
            return observacao;
        }

        public LocalDateTime getInspecionadoEm() {
            return inspecionadoEm;
        }

        public String getOverridePor() {
            return overridePor;
        }

        public String getOverrideMotivo() {
            return overrideMotivo;
        }

        public LocalDateTime getLiberadoEm() {
            return liberadoEm;
        }

        public List<DefeitoResposta> getDefeitos() {
            return defeitos;
        }

        public Long getVersao() {
            return versao;
        }
    }

    public static class DefeitoResposta {

        private final String codigo;
        private final String descricao;
        private final SeveridadeDefeitoVagao severidade;
        private final String evidencia;

        private DefeitoResposta(DefeitoInspecaoVagao defeito) {
            this.codigo = defeito.getCodigo();
            this.descricao = defeito.getDescricao();
            this.severidade = defeito.getSeveridade();
            this.evidencia = defeito.getEvidencia();
        }

        public String getCodigo() {
            return codigo;
        }

        public String getDescricao() {
            return descricao;
        }

        public SeveridadeDefeitoVagao getSeveridade() {
            return severidade;
        }

        public String getEvidencia() {
            return evidencia;
        }
    }
}
