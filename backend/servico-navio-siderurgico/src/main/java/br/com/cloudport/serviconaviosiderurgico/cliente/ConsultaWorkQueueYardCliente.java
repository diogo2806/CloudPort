package br.com.cloudport.serviconaviosiderurgico.cliente;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.yard.integracao",
        havingValue = "http",
        matchIfMissing = true)
public class ConsultaWorkQueueYardCliente {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ConsultaWorkQueueYardCliente(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cloudport.integracao.yard.base-url:http://localhost:8081}") String baseUrl
    ) {
        this.restTemplate = restTemplateBuilder
                .requestFactory(HttpComponentsClientHttpRequestFactory.class)
                .build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    public List<WorkQueueValidacaoYardDTO> listarParaValidacaoPlano(Long visitaNavioId) {
        ResponseEntity<WorkQueueValidacaoYardDTO[]> resposta = restTemplate.getForEntity(
                baseUrl + "/yard/patio/work-queues/validacao-plano?visitaNavioId={visitaNavioId}",
                WorkQueueValidacaoYardDTO[].class,
                visitaNavioId);
        WorkQueueValidacaoYardDTO[] corpo = resposta.getBody();
        if (corpo == null) return List.of();
        return Arrays.stream(corpo).filter(Objects::nonNull).toList();
    }

    private String removerBarraFinal(String valor) {
        if (!StringUtils.hasText(valor)) return "http://localhost:8081";
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }

    public static class WorkQueueValidacaoYardDTO {
        private Long id;
        private Long visitaNavioId;
        private String identificador;
        private String berco;
        private Integer porao;
        private String status;
        private String pow;
        private String poolOperacional;
        private Long equipamentoPatioId;
        private String equipamentoIdentificador;
        private String equipamentoTipo;
        private String equipamentoStatus;
        private Long planoGuindasteId;
        private Long recursoCaisId;
        private int totalOrdens;
        private int totalOrdensDispatchaveis;
        private boolean coberturaValida;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getVisitaNavioId() { return visitaNavioId; }
        public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
        public String getIdentificador() { return identificador; }
        public void setIdentificador(String identificador) { this.identificador = identificador; }
        public String getBerco() { return berco; }
        public void setBerco(String berco) { this.berco = berco; }
        public Integer getPorao() { return porao; }
        public void setPorao(Integer porao) { this.porao = porao; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPow() { return pow; }
        public void setPow(String pow) { this.pow = pow; }
        public String getPoolOperacional() { return poolOperacional; }
        public void setPoolOperacional(String poolOperacional) { this.poolOperacional = poolOperacional; }
        public Long getEquipamentoPatioId() { return equipamentoPatioId; }
        public void setEquipamentoPatioId(Long equipamentoPatioId) { this.equipamentoPatioId = equipamentoPatioId; }
        public String getEquipamentoIdentificador() { return equipamentoIdentificador; }
        public void setEquipamentoIdentificador(String equipamentoIdentificador) { this.equipamentoIdentificador = equipamentoIdentificador; }
        public String getEquipamentoTipo() { return equipamentoTipo; }
        public void setEquipamentoTipo(String equipamentoTipo) { this.equipamentoTipo = equipamentoTipo; }
        public String getEquipamentoStatus() { return equipamentoStatus; }
        public void setEquipamentoStatus(String equipamentoStatus) { this.equipamentoStatus = equipamentoStatus; }
        public Long getPlanoGuindasteId() { return planoGuindasteId; }
        public void setPlanoGuindasteId(Long planoGuindasteId) { this.planoGuindasteId = planoGuindasteId; }
        public Long getRecursoCaisId() { return recursoCaisId; }
        public void setRecursoCaisId(Long recursoCaisId) { this.recursoCaisId = recursoCaisId; }
        public int getTotalOrdens() { return totalOrdens; }
        public void setTotalOrdens(int totalOrdens) { this.totalOrdens = totalOrdens; }
        public int getTotalOrdensDispatchaveis() { return totalOrdensDispatchaveis; }
        public void setTotalOrdensDispatchaveis(int totalOrdensDispatchaveis) { this.totalOrdensDispatchaveis = totalOrdensDispatchaveis; }
        public boolean isCoberturaValida() { return coberturaValida; }
        public void setCoberturaValida(boolean coberturaValida) { this.coberturaValida = coberturaValida; }
    }
}
