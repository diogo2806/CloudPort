package br.com.cloudport.serviconaviosiderurgico.cliente;

import java.time.LocalDateTime;
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
public class TelemetriaYardCliente {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public TelemetriaYardCliente(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cloudport.integracao.yard.base-url:http://localhost:8081}") String baseUrl
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    public List<TelemetriaEquipamentoYardDTO> listar() {
        ResponseEntity<TelemetriaEquipamentoYardDTO[]> resposta = restTemplate.getForEntity(
                baseUrl + "/yard/patio/equipamentos/telemetria",
                TelemetriaEquipamentoYardDTO[].class
        );
        return resposta.getBody() == null
                ? List.of()
                : Arrays.stream(resposta.getBody()).filter(Objects::nonNull).toList();
    }

    private String removerBarraFinal(String valor) {
        if (!StringUtils.hasText(valor)) {
            return "http://localhost:8081";
        }
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }

    public static class TelemetriaEquipamentoYardDTO {
        private String equipamento;
        private String tipoEquipamento;
        private String statusOperacional;
        private Double latitude;
        private Double longitude;
        private Double coordenadaX;
        private Double coordenadaY;
        private Double heading;
        private String posicaoMaisProxima;
        private Integer distanciaPosicaoCentimetros;
        private Boolean dentroDaPosicao;
        private String origem;
        private String operadorVmt;
        private String statusVmt;
        private Long workInstructionAtualId;
        private Long sequencia;
        private LocalDateTime capturadoEm;
        private LocalDateTime recebidoEm;

        public String getEquipamento() { return equipamento; }
        public void setEquipamento(String equipamento) { this.equipamento = equipamento; }
        public String getTipoEquipamento() { return tipoEquipamento; }
        public void setTipoEquipamento(String tipoEquipamento) { this.tipoEquipamento = tipoEquipamento; }
        public String getStatusOperacional() { return statusOperacional; }
        public void setStatusOperacional(String statusOperacional) { this.statusOperacional = statusOperacional; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public Double getCoordenadaX() { return coordenadaX; }
        public void setCoordenadaX(Double coordenadaX) { this.coordenadaX = coordenadaX; }
        public Double getCoordenadaY() { return coordenadaY; }
        public void setCoordenadaY(Double coordenadaY) { this.coordenadaY = coordenadaY; }
        public Double getHeading() { return heading; }
        public void setHeading(Double heading) { this.heading = heading; }
        public String getPosicaoMaisProxima() { return posicaoMaisProxima; }
        public void setPosicaoMaisProxima(String posicaoMaisProxima) { this.posicaoMaisProxima = posicaoMaisProxima; }
        public Integer getDistanciaPosicaoCentimetros() { return distanciaPosicaoCentimetros; }
        public void setDistanciaPosicaoCentimetros(Integer distanciaPosicaoCentimetros) { this.distanciaPosicaoCentimetros = distanciaPosicaoCentimetros; }
        public Boolean getDentroDaPosicao() { return dentroDaPosicao; }
        public void setDentroDaPosicao(Boolean dentroDaPosicao) { this.dentroDaPosicao = dentroDaPosicao; }
        public String getOrigem() { return origem; }
        public void setOrigem(String origem) { this.origem = origem; }
        public String getOperadorVmt() { return operadorVmt; }
        public void setOperadorVmt(String operadorVmt) { this.operadorVmt = operadorVmt; }
        public String getStatusVmt() { return statusVmt; }
        public void setStatusVmt(String statusVmt) { this.statusVmt = statusVmt; }
        public Long getWorkInstructionAtualId() { return workInstructionAtualId; }
        public void setWorkInstructionAtualId(Long workInstructionAtualId) { this.workInstructionAtualId = workInstructionAtualId; }
        public Long getSequencia() { return sequencia; }
        public void setSequencia(Long sequencia) { this.sequencia = sequencia; }
        public LocalDateTime getCapturadoEm() { return capturadoEm; }
        public void setCapturadoEm(LocalDateTime capturadoEm) { this.capturadoEm = capturadoEm; }
        public LocalDateTime getRecebidoEm() { return recebidoEm; }
        public void setRecebidoEm(LocalDateTime recebidoEm) { this.recebidoEm = recebidoEm; }
    }
}
