package br.com.cloudport.serviconaviosiderurgico.cliente;

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
                baseUrl + "/yard/patio/posicoes",
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

        public String identificador() {
            return id == null ? null : String.valueOf(id);
        }
    }
}
