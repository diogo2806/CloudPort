package br.com.cloudport.serviconaviosiderurgico.cliente;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ReservaPosicaoPatioNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class OrdemPatioYardCliente {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OrdemPatioYardCliente(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cloudport.integracao.yard.base-url:http://localhost:8081}") String baseUrl
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    public OrdemPatioYardRespostaDTO criarOuReutilizarOrdem(ItemOperacaoNavio item, ReservaPosicaoPatioNavio reserva) {
        OrdemPatioYardRequisicaoDTO requisicao = OrdemPatioYardRequisicaoDTO.de(item, reserva);
        return restTemplate.postForObject(baseUrl + "/yard/patio/ordens/navio", requisicao, OrdemPatioYardRespostaDTO.class);
    }

    public List<OrdemPatioYardRespostaDTO> listarOrdensDaVisita(Long visitaNavioId) {
        ResponseEntity<OrdemPatioYardRespostaDTO[]> resposta = restTemplate.getForEntity(
                baseUrl + "/yard/patio/ordens/visita-navio/{visitaNavioId}",
                OrdemPatioYardRespostaDTO[].class,
                visitaNavioId
        );
        OrdemPatioYardRespostaDTO[] corpo = resposta.getBody();
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

    public static class OrdemPatioYardRequisicaoDTO {
        private String codigoConteiner;
        private String tipoCarga;
        private String destino;
        private Integer linhaDestino;
        private Integer colunaDestino;
        private String camadaDestino;
        private String tipoMovimento;
        private String statusConteinerDestino;
        private Long visitaNavioId;
        private Long itemOperacaoNavioId;
        private Long planoEstivaNavioId;
        private String tipoOrigem;
        private String tipoDestino;
        private Integer sequenciaNavio;
        private Integer prioridadeOperacional;

        public static OrdemPatioYardRequisicaoDTO de(ItemOperacaoNavio item, ReservaPosicaoPatioNavio reserva) {
            if (reserva == null || reserva.getLinha() == null || reserva.getColuna() == null || !StringUtils.hasText(reserva.getCamada())) {
                throw new IllegalArgumentException("A ordem real do yard exige reserva ativa com linha, coluna e camada.");
            }
            OrdemPatioYardRequisicaoDTO dto = new OrdemPatioYardRequisicaoDTO();
            dto.codigoConteiner = item.getCodigoLote();
            dto.tipoCarga = null;
            dto.destino = destino(item, reserva);
            dto.linhaDestino = reserva.getLinha();
            dto.colunaDestino = reserva.getColuna();
            dto.camadaDestino = reserva.getCamada();
            dto.tipoMovimento = tipoMovimentoPatio(item.getTipoMovimento());
            dto.statusConteinerDestino = statusDestino(item.getTipoMovimento());
            dto.visitaNavioId = item.getVisitaNavio().getId();
            dto.itemOperacaoNavioId = item.getId();
            dto.planoEstivaNavioId = null;
            dto.tipoOrigem = tipoOrigem(item.getTipoMovimento());
            dto.tipoDestino = tipoDestino(item.getTipoMovimento());
            dto.sequenciaNavio = item.getSequenciaOperacional();
            dto.prioridadeOperacional = item.getSequenciaOperacional();
            return dto;
        }

        private static String destino(ItemOperacaoNavio item, ReservaPosicaoPatioNavio reserva) {
            if (StringUtils.hasText(item.getDestinoPatio())) {
                return item.getDestinoPatio();
            }
            if (StringUtils.hasText(reserva.getBloco())) {
                return reserva.getBloco().toUpperCase(Locale.ROOT);
            }
            return reserva.getPosicaoPatioId();
        }

        private static String tipoMovimentoPatio(TipoMovimentoNavio tipoMovimento) {
            if (tipoMovimento == TipoMovimentoNavio.RESTOW) {
                return "REMANEJAMENTO";
            }
            if (tipoMovimento == TipoMovimentoNavio.EMBARQUE) {
                return "TRANSFERENCIA";
            }
            return "ALOCACAO";
        }

        private static String statusDestino(TipoMovimentoNavio tipoMovimento) {
            if (tipoMovimento == TipoMovimentoNavio.EMBARQUE) {
                return "DESPACHADO";
            }
            return "ARMAZENADO";
        }

        private static String tipoOrigem(TipoMovimentoNavio tipoMovimento) {
            return tipoMovimento == TipoMovimentoNavio.DESCARGA ? "NAVIO" : "PATIO";
        }

        private static String tipoDestino(TipoMovimentoNavio tipoMovimento) {
            return tipoMovimento == TipoMovimentoNavio.EMBARQUE ? "NAVIO" : "PATIO";
        }

        public String getCodigoConteiner() { return codigoConteiner; }
        public void setCodigoConteiner(String codigoConteiner) { this.codigoConteiner = codigoConteiner; }
        public String getTipoCarga() { return tipoCarga; }
        public void setTipoCarga(String tipoCarga) { this.tipoCarga = tipoCarga; }
        public String getDestino() { return destino; }
        public void setDestino(String destino) { this.destino = destino; }
        public Integer getLinhaDestino() { return linhaDestino; }
        public void setLinhaDestino(Integer linhaDestino) { this.linhaDestino = linhaDestino; }
        public Integer getColunaDestino() { return colunaDestino; }
        public void setColunaDestino(Integer colunaDestino) { this.colunaDestino = colunaDestino; }
        public String getCamadaDestino() { return camadaDestino; }
        public void setCamadaDestino(String camadaDestino) { this.camadaDestino = camadaDestino; }
        public String getTipoMovimento() { return tipoMovimento; }
        public void setTipoMovimento(String tipoMovimento) { this.tipoMovimento = tipoMovimento; }
        public String getStatusConteinerDestino() { return statusConteinerDestino; }
        public void setStatusConteinerDestino(String statusConteinerDestino) { this.statusConteinerDestino = statusConteinerDestino; }
        public Long getVisitaNavioId() { return visitaNavioId; }
        public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
        public Long getItemOperacaoNavioId() { return itemOperacaoNavioId; }
        public void setItemOperacaoNavioId(Long itemOperacaoNavioId) { this.itemOperacaoNavioId = itemOperacaoNavioId; }
        public Long getPlanoEstivaNavioId() { return planoEstivaNavioId; }
        public void setPlanoEstivaNavioId(Long planoEstivaNavioId) { this.planoEstivaNavioId = planoEstivaNavioId; }
        public String getTipoOrigem() { return tipoOrigem; }
        public void setTipoOrigem(String tipoOrigem) { this.tipoOrigem = tipoOrigem; }
        public String getTipoDestino() { return tipoDestino; }
        public void setTipoDestino(String tipoDestino) { this.tipoDestino = tipoDestino; }
        public Integer getSequenciaNavio() { return sequenciaNavio; }
        public void setSequenciaNavio(Integer sequenciaNavio) { this.sequenciaNavio = sequenciaNavio; }
        public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
        public void setPrioridadeOperacional(Integer prioridadeOperacional) { this.prioridadeOperacional = prioridadeOperacional; }
    }

    public static class OrdemPatioYardRespostaDTO {
        private Long id;
        private String codigoConteiner;
        private String destino;
        private Integer linhaDestino;
        private Integer colunaDestino;
        private String camadaDestino;
        private String tipoMovimento;
        private String statusOrdem;
        private Long visitaNavioId;
        private Long itemOperacaoNavioId;
        private Integer sequenciaNavio;
        private Integer prioridadeOperacional;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCodigoConteiner() { return codigoConteiner; }
        public void setCodigoConteiner(String codigoConteiner) { this.codigoConteiner = codigoConteiner; }
        public String getDestino() { return destino; }
        public void setDestino(String destino) { this.destino = destino; }
        public Integer getLinhaDestino() { return linhaDestino; }
        public void setLinhaDestino(Integer linhaDestino) { this.linhaDestino = linhaDestino; }
        public Integer getColunaDestino() { return colunaDestino; }
        public void setColunaDestino(Integer colunaDestino) { this.colunaDestino = colunaDestino; }
        public String getCamadaDestino() { return camadaDestino; }
        public void setCamadaDestino(String camadaDestino) { this.camadaDestino = camadaDestino; }
        public String getTipoMovimento() { return tipoMovimento; }
        public void setTipoMovimento(String tipoMovimento) { this.tipoMovimento = tipoMovimento; }
        public String getStatusOrdem() { return statusOrdem; }
        public void setStatusOrdem(String statusOrdem) { this.statusOrdem = statusOrdem; }
        public Long getVisitaNavioId() { return visitaNavioId; }
        public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
        public Long getItemOperacaoNavioId() { return itemOperacaoNavioId; }
        public void setItemOperacaoNavioId(Long itemOperacaoNavioId) { this.itemOperacaoNavioId = itemOperacaoNavioId; }
        public Integer getSequenciaNavio() { return sequenciaNavio; }
        public void setSequenciaNavio(Integer sequenciaNavio) { this.sequenciaNavio = sequenciaNavio; }
        public Integer getPrioridadeOperacional() { return prioridadeOperacional; }
        public void setPrioridadeOperacional(Integer prioridadeOperacional) { this.prioridadeOperacional = prioridadeOperacional; }

        public String posicaoDestinoFormatada() {
            if (linhaDestino == null || colunaDestino == null || !StringUtils.hasText(camadaDestino)) {
                return destino;
            }
            return linhaDestino + "-" + colunaDestino + "-" + camadaDestino;
        }
    }
}
