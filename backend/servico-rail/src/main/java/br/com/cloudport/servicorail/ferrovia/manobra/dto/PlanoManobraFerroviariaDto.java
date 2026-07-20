package br.com.cloudport.servicorail.ferrovia.manobra.dto;

import br.com.cloudport.servicorail.ferrovia.manobra.modelo.PlanoManobraFerroviaria;
import br.com.cloudport.servicorail.ferrovia.manobra.modelo.PlanoManobraFerroviaria.StatusPlanoManobra;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

public final class PlanoManobraFerroviariaDto {

    private PlanoManobraFerroviariaDto() {
    }

    public static class Criacao {

        @NotNull
        @Positive
        private Integer sequencia;

        @NotBlank
        @Size(max = 120)
        private String origem;

        @NotBlank
        @Size(max = 120)
        private String destino;

        @NotBlank
        @Size(max = 200)
        private String composicao;

        @NotBlank
        @Size(max = 80)
        private String linha;

        @NotBlank
        @Size(max = 120)
        private String trecho;

        @NotNull
        private LocalDateTime inicioPrevisto;

        @NotNull
        private LocalDateTime fimPrevisto;

        public Integer getSequencia() {
            return sequencia;
        }

        public void setSequencia(Integer sequencia) {
            this.sequencia = sequencia;
        }

        public String getOrigem() {
            return origem;
        }

        public void setOrigem(String origem) {
            this.origem = origem;
        }

        public String getDestino() {
            return destino;
        }

        public void setDestino(String destino) {
            this.destino = destino;
        }

        public String getComposicao() {
            return composicao;
        }

        public void setComposicao(String composicao) {
            this.composicao = composicao;
        }

        public String getLinha() {
            return linha;
        }

        public void setLinha(String linha) {
            this.linha = linha;
        }

        public String getTrecho() {
            return trecho;
        }

        public void setTrecho(String trecho) {
            this.trecho = trecho;
        }

        public LocalDateTime getInicioPrevisto() {
            return inicioPrevisto;
        }

        public void setInicioPrevisto(LocalDateTime inicioPrevisto) {
            this.inicioPrevisto = inicioPrevisto;
        }

        public LocalDateTime getFimPrevisto() {
            return fimPrevisto;
        }

        public void setFimPrevisto(LocalDateTime fimPrevisto) {
            this.fimPrevisto = fimPrevisto;
        }
    }

    public static class AlteracaoStatus {

        @NotNull
        private StatusPlanoManobra status;

        @Size(max = 500)
        private String motivo;

        public StatusPlanoManobra getStatus() {
            return status;
        }

        public void setStatus(StatusPlanoManobra status) {
            this.status = status;
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
        private final Integer sequencia;
        private final String origem;
        private final String destino;
        private final String composicao;
        private final String linha;
        private final String trecho;
        private final LocalDateTime inicioPrevisto;
        private final LocalDateTime fimPrevisto;
        private final StatusPlanoManobra status;
        private final String conflitoDescricao;
        private final String autorizadoPor;
        private final LocalDateTime autorizadoEm;
        private final LocalDateTime iniciadoEm;
        private final LocalDateTime concluidoEm;
        private final String motivoCancelamento;
        private final Long versao;

        private Resposta(PlanoManobraFerroviaria entidade) {
            this.id = entidade.getId();
            this.idVisitaTrem = entidade.getVisitaTrem() != null ? entidade.getVisitaTrem().getId() : null;
            this.sequencia = entidade.getSequencia();
            this.origem = entidade.getOrigem();
            this.destino = entidade.getDestino();
            this.composicao = entidade.getComposicao();
            this.linha = entidade.getLinha();
            this.trecho = entidade.getTrecho();
            this.inicioPrevisto = entidade.getInicioPrevisto();
            this.fimPrevisto = entidade.getFimPrevisto();
            this.status = entidade.getStatus();
            this.conflitoDescricao = entidade.getConflitoDescricao();
            this.autorizadoPor = entidade.getAutorizadoPor();
            this.autorizadoEm = entidade.getAutorizadoEm();
            this.iniciadoEm = entidade.getIniciadoEm();
            this.concluidoEm = entidade.getConcluidoEm();
            this.motivoCancelamento = entidade.getMotivoCancelamento();
            this.versao = entidade.getVersao();
        }

        public static Resposta deEntidade(PlanoManobraFerroviaria entidade) {
            return new Resposta(entidade);
        }

        public Long getId() {
            return id;
        }

        public Long getIdVisitaTrem() {
            return idVisitaTrem;
        }

        public Integer getSequencia() {
            return sequencia;
        }

        public String getOrigem() {
            return origem;
        }

        public String getDestino() {
            return destino;
        }

        public String getComposicao() {
            return composicao;
        }

        public String getLinha() {
            return linha;
        }

        public String getTrecho() {
            return trecho;
        }

        public LocalDateTime getInicioPrevisto() {
            return inicioPrevisto;
        }

        public LocalDateTime getFimPrevisto() {
            return fimPrevisto;
        }

        public StatusPlanoManobra getStatus() {
            return status;
        }

        public String getConflitoDescricao() {
            return conflitoDescricao;
        }

        public String getAutorizadoPor() {
            return autorizadoPor;
        }

        public LocalDateTime getAutorizadoEm() {
            return autorizadoEm;
        }

        public LocalDateTime getIniciadoEm() {
            return iniciadoEm;
        }

        public LocalDateTime getConcluidoEm() {
            return concluidoEm;
        }

        public String getMotivoCancelamento() {
            return motivoCancelamento;
        }

        public Long getVersao() {
            return versao;
        }
    }
}
