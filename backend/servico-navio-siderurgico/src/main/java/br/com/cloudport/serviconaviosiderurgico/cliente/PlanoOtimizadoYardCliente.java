package br.com.cloudport.serviconaviosiderurgico.cliente;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.yard.integracao",
        havingValue = "http",
        matchIfMissing = true)
public class PlanoOtimizadoYardCliente {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PlanoOtimizadoYardCliente(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cloudport.integracao.yard.base-url:http://localhost:8081}") String baseUrl
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    public ResultadoAplicacaoPlanoYardDTO aplicar(AplicacaoPlanoYardDTO comando) {
        ResultadoAplicacaoPlanoYardDTO resultado = restTemplate.postForObject(
                baseUrl + "/yard/patio/planos-otimizados/aplicar",
                comando,
                ResultadoAplicacaoPlanoYardDTO.class);
        if (resultado == null) {
            throw new IllegalStateException("O Yard nao confirmou a aplicacao do plano otimizado.");
        }
        return resultado;
    }

    public void compensar(
            String planoId,
            Long visitaNavioId,
            String usuario,
            String motivo,
            List<EstadoAnteriorOrdemYardDTO> estadosAnteriores
    ) {
        CompensacaoPlanoYardDTO comando = new CompensacaoPlanoYardDTO();
        comando.setPlanoId(planoId);
        comando.setVisitaNavioId(visitaNavioId);
        comando.setUsuario(usuario);
        comando.setMotivo(motivo);
        comando.setEstadosAnteriores(estadosAnteriores);
        ResponseEntity<Void> resposta = restTemplate.postForEntity(
                baseUrl + "/yard/patio/planos-otimizados/compensar",
                comando,
                Void.class);
        if (!resposta.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("O Yard nao confirmou a compensacao do plano otimizado.");
        }
    }

    private String removerBarraFinal(String valor) {
        if (!StringUtils.hasText(valor)) {
            return "http://localhost:8081";
        }
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }

    public static class AplicacaoPlanoYardDTO {
        private String planoId;
        private Long visitaNavioId;
        private String usuario;
        private List<ItemPlanoYardDTO> itens = new ArrayList<>();

        public String getPlanoId() { return planoId; }
        public void setPlanoId(String planoId) { this.planoId = planoId; }
        public Long getVisitaNavioId() { return visitaNavioId; }
        public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public List<ItemPlanoYardDTO> getItens() { return itens; }
        public void setItens(List<ItemPlanoYardDTO> itens) { this.itens = itens; }
    }

    public static class ItemPlanoYardDTO {
        private Long ordemTrabalhoPatioId;
        private Long itemOperacaoNavioId;
        private String codigoConteiner;
        private Integer linha;
        private Integer coluna;
        private String camada;
        private String equipamento;
        private Integer prioridadeOperacional;

        public Long getOrdemTrabalhoPatioId() { return ordemTrabalhoPatioId; }
        public void setOrdemTrabalhoPatioId(Long ordemTrabalhoPatioId) { this.ordemTrabalhoPatioId = ordemTrabalhoPatioId; }
        public Long getItemOperacaoNavioId() { return itemOperacaoNavioId; }
        public void setItemOperacaoNavioId(Long itemOperacaoNavioId) { this.itemOperacaoNavioId = itemOperacaoNavioId; }
        public String getCodigoConteiner() { return codigoConteiner; }
        public void setCodigoConteiner(String codigoConteiner) { this.codigoConteiner = codigoConteiner; }
        public Integer getLinha() { return linha; }
        public void setLinha(Integer linha) { this.linha = linha; }
        public Integer getColuna() { return coluna; }
        public void setColuna(Integer coluna) { this.coluna = coluna; }
        public String getCamada() { return camada; }
        public void setCamada(String camada) { this.camada = camada; }
        public String getEquipamento() { return equipamento; }
        public void setEquipamento(String equipamento) { this.equipamento = equipamento; }
        public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
        public void setPrioridadeOperacional(Integer prioridadeOperacional) { this.prioridadeOperacional = prioridadeOperacional; }
    }

    public static class ResultadoAplicacaoPlanoYardDTO {
        private String planoId;
        private Long visitaNavioId;
        private int ordensAtualizadas;
        private List<EstadoAnteriorOrdemYardDTO> estadosAnteriores = new ArrayList<>();

        public String getPlanoId() { return planoId; }
        public void setPlanoId(String planoId) { this.planoId = planoId; }
        public Long getVisitaNavioId() { return visitaNavioId; }
        public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
        public int getOrdensAtualizadas() { return ordensAtualizadas; }
        public void setOrdensAtualizadas(int ordensAtualizadas) { this.ordensAtualizadas = ordensAtualizadas; }
        public List<EstadoAnteriorOrdemYardDTO> getEstadosAnteriores() { return estadosAnteriores; }
        public void setEstadosAnteriores(List<EstadoAnteriorOrdemYardDTO> estadosAnteriores) { this.estadosAnteriores = estadosAnteriores; }
    }

    public static class EstadoAnteriorOrdemYardDTO {
        private Long ordemTrabalhoPatioId;
        private String destino;
        private Integer linhaDestino;
        private Integer colunaDestino;
        private String camadaDestino;
        private Integer prioridadeOperacional;
        private Integer sequenciaNavio;
        private Long workQueueId;

        public Long getOrdemTrabalhoPatioId() { return ordemTrabalhoPatioId; }
        public void setOrdemTrabalhoPatioId(Long ordemTrabalhoPatioId) { this.ordemTrabalhoPatioId = ordemTrabalhoPatioId; }
        public String getDestino() { return destino; }
        public void setDestino(String destino) { this.destino = destino; }
        public Integer getLinhaDestino() { return linhaDestino; }
        public void setLinhaDestino(Integer linhaDestino) { this.linhaDestino = linhaDestino; }
        public Integer getColunaDestino() { return colunaDestino; }
        public void setColunaDestino(Integer colunaDestino) { this.colunaDestino = colunaDestino; }
        public String getCamadaDestino() { return camadaDestino; }
        public void setCamadaDestino(String camadaDestino) { this.camadaDestino = camadaDestino; }
        public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
        public void setPrioridadeOperacional(Integer prioridadeOperacional) { this.prioridadeOperacional = prioridadeOperacional; }
        public Integer getSequenciaNavio() { return sequenciaNavio; }
        public void setSequenciaNavio(Integer sequenciaNavio) { this.sequenciaNavio = sequenciaNavio; }
        public Long getWorkQueueId() { return workQueueId; }
        public void setWorkQueueId(Long workQueueId) { this.workQueueId = workQueueId; }
    }

    public static class CompensacaoPlanoYardDTO {
        private String planoId;
        private Long visitaNavioId;
        private String usuario;
        private String motivo;
        private List<EstadoAnteriorOrdemYardDTO> estadosAnteriores = new ArrayList<>();

        public String getPlanoId() { return planoId; }
        public void setPlanoId(String planoId) { this.planoId = planoId; }
        public Long getVisitaNavioId() { return visitaNavioId; }
        public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
        public List<EstadoAnteriorOrdemYardDTO> getEstadosAnteriores() { return estadosAnteriores; }
        public void setEstadosAnteriores(List<EstadoAnteriorOrdemYardDTO> estadosAnteriores) { this.estadosAnteriores = estadosAnteriores; }
    }
}
