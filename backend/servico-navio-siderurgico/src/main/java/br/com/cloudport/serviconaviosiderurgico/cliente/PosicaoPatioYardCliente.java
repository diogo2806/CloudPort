package br.com.cloudport.serviconaviosiderurgico.cliente;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class PosicaoPatioYardCliente {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PosicaoPatioYardCliente(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cloudport.integracao.yard.base-url:http://localhost:8081}") String baseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    public List<PosicaoPatioYardDTO> listarPosicoes() {
        ResponseEntity<PosicaoPatioYardDTO[]> resposta = restTemplate.getForEntity(
                baseUrl + "/yard/patio/reservas/posicoes",
                PosicaoPatioYardDTO[].class
        );
        PosicaoPatioYardDTO[] corpo = resposta.getBody();
        if (corpo == null) {
            return List.of();
        }
        return Arrays.stream(corpo).filter(Objects::nonNull).toList();
    }

    private String removerBarraFinal(String valor) {
        if (!StringUtils.hasText(valor)) {
            return "http://localhost:8081";
        }
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }

    public static class PosicaoPatioYardDTO {
        private Long id;
        private Integer linha;
        private Integer coluna;
        private String camadaOperacional;
        private boolean ocupada;
        private String codigoConteiner;
        private String statusConteiner;
        private String bloco;
        private boolean bloqueada;
        private boolean interditada;
        private boolean areaPermitida = true;
        private List<String> tiposCargaPermitidos = List.of();
        private BigDecimal pesoMaximoToneladas;
        private BigDecimal alturaMaximaMetros;
        private Integer camadaMaxima;
        private Integer capacidadePilha;
        private long ocupacaoPilha;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getLinha() { return linha; }
        public void setLinha(Integer linha) { this.linha = linha; }
        public Integer getColuna() { return coluna; }
        public void setColuna(Integer coluna) { this.coluna = coluna; }
        public String getCamadaOperacional() { return camadaOperacional; }
        public void setCamadaOperacional(String camadaOperacional) { this.camadaOperacional = camadaOperacional; }
        public boolean isOcupada() { return ocupada; }
        public void setOcupada(boolean ocupada) { this.ocupada = ocupada; }
        public String getCodigoConteiner() { return codigoConteiner; }
        public void setCodigoConteiner(String codigoConteiner) { this.codigoConteiner = codigoConteiner; }
        public String getStatusConteiner() { return statusConteiner; }
        public void setStatusConteiner(String statusConteiner) { this.statusConteiner = statusConteiner; }
        public String getBloco() { return bloco; }
        public void setBloco(String bloco) { this.bloco = bloco; }
        public boolean isBloqueada() { return bloqueada; }
        public void setBloqueada(boolean bloqueada) { this.bloqueada = bloqueada; }
        public boolean isInterditada() { return interditada; }
        public void setInterditada(boolean interditada) { this.interditada = interditada; }
        public boolean isAreaPermitida() { return areaPermitida; }
        public void setAreaPermitida(boolean areaPermitida) { this.areaPermitida = areaPermitida; }
        public List<String> getTiposCargaPermitidos() { return tiposCargaPermitidos == null ? List.of() : tiposCargaPermitidos; }
        public void setTiposCargaPermitidos(List<String> tiposCargaPermitidos) { this.tiposCargaPermitidos = tiposCargaPermitidos; }
        public BigDecimal getPesoMaximoToneladas() { return pesoMaximoToneladas; }
        public void setPesoMaximoToneladas(BigDecimal pesoMaximoToneladas) { this.pesoMaximoToneladas = pesoMaximoToneladas; }
        public BigDecimal getAlturaMaximaMetros() { return alturaMaximaMetros; }
        public void setAlturaMaximaMetros(BigDecimal alturaMaximaMetros) { this.alturaMaximaMetros = alturaMaximaMetros; }
        public Integer getCamadaMaxima() { return camadaMaxima; }
        public void setCamadaMaxima(Integer camadaMaxima) { this.camadaMaxima = camadaMaxima; }
        public Integer getCapacidadePilha() { return capacidadePilha; }
        public void setCapacidadePilha(Integer capacidadePilha) { this.capacidadePilha = capacidadePilha; }
        public long getOcupacaoPilha() { return ocupacaoPilha; }
        public void setOcupacaoPilha(long ocupacaoPilha) { this.ocupacaoPilha = ocupacaoPilha; }

        public String identificador() {
            return id == null ? null : String.valueOf(id);
        }
    }
}
