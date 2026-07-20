package br.com.cloudport.servicoyard.patio.dispatch;

import java.time.LocalDateTime;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

public final class SegmentoRotaDispatchDto {

    private SegmentoRotaDispatchDto() {
    }

    public static class Requisicao {
        @NotBlank
        @Size(max = 120)
        private String origem;
        @NotBlank
        @Size(max = 120)
        private String destino;
        @NotNull
        @PositiveOrZero
        private Double distanciaMetros;
        @Size(max = 40)
        private String sentido;
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        private Double congestionamentoPercentual;
        private Boolean bloqueado;
        @Size(max = 500)
        private String motivoInterdicao;
        @Positive
        private Integer limiteRegionalChe;
        private LocalDateTime vigenteDe;
        private LocalDateTime vigenteAte;

        public String getOrigem() { return origem; }
        public void setOrigem(String origem) { this.origem = origem; }
        public String getDestino() { return destino; }
        public void setDestino(String destino) { this.destino = destino; }
        public Double getDistanciaMetros() { return distanciaMetros; }
        public void setDistanciaMetros(Double distanciaMetros) { this.distanciaMetros = distanciaMetros; }
        public String getSentido() { return sentido; }
        public void setSentido(String sentido) { this.sentido = sentido; }
        public Double getCongestionamentoPercentual() { return congestionamentoPercentual; }
        public void setCongestionamentoPercentual(Double congestionamentoPercentual) { this.congestionamentoPercentual = congestionamentoPercentual; }
        public Boolean getBloqueado() { return bloqueado; }
        public void setBloqueado(Boolean bloqueado) { this.bloqueado = bloqueado; }
        public String getMotivoInterdicao() { return motivoInterdicao; }
        public void setMotivoInterdicao(String motivoInterdicao) { this.motivoInterdicao = motivoInterdicao; }
        public Integer getLimiteRegionalChe() { return limiteRegionalChe; }
        public void setLimiteRegionalChe(Integer limiteRegionalChe) { this.limiteRegionalChe = limiteRegionalChe; }
        public LocalDateTime getVigenteDe() { return vigenteDe; }
        public void setVigenteDe(LocalDateTime vigenteDe) { this.vigenteDe = vigenteDe; }
        public LocalDateTime getVigenteAte() { return vigenteAte; }
        public void setVigenteAte(LocalDateTime vigenteAte) { this.vigenteAte = vigenteAte; }
    }

    public record Resposta(
            Long id,
            String origem,
            String destino,
            double distanciaMetros,
            String sentido,
            double congestionamentoPercentual,
            boolean bloqueado,
            String motivoInterdicao,
            Integer limiteRegionalChe,
            boolean ativo,
            long versao,
            LocalDateTime vigenteDe,
            LocalDateTime vigenteAte,
            String atualizadoPor,
            LocalDateTime atualizadoEm) {
    }
}
