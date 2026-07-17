package br.com.cloudport.servicogate.integration.navio;

import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.porta.navio.EmbarqueDiretoNavioPorta;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.navio.integracao",
        havingValue = "http",
        matchIfMissing = true)
public class EmbarqueDiretoNavioHttpAdapter implements EmbarqueDiretoNavioPorta {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public EmbarqueDiretoNavioHttpAdapter(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cloudport.integracao.navio.base-url:http://localhost:8084}") String baseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    @Override
    public Resultado embarcar(Comando comando) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        propagarToken(headers);
        Requisicao requisicao = new Requisicao(comando.getCodigoConteiner(), comando.getEmbarcadoEm());
        ResponseEntity<Resposta> response = restTemplate.exchange(
                baseUrl + "/plano-estiva/atribuicoes/{atribuicaoId}/embarcar-direto-gate",
                HttpMethod.PATCH,
                new HttpEntity<>(requisicao, headers),
                Resposta.class,
                comando.getAtribuicaoEstivaId());
        Resposta resposta = response.getBody();
        if (resposta == null) {
            throw new BusinessException("O serviço de Navio não retornou a confirmação do embarque direto");
        }
        return new Resultado(
                resposta.getAtribuicaoEstivaId(),
                resposta.getPlanoEstivaId(),
                resposta.getCodigoConteiner(),
                resposta.getBaia(),
                resposta.getFileira(),
                resposta.getCamada(),
                resposta.getEmbarcadoEm());
    }

    private void propagarToken(HttpHeaders headers) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
            headers.setBearerAuth(jwt.getToken().getTokenValue());
        }
    }

    private String removerBarraFinal(String valor) {
        if (!StringUtils.hasText(valor)) {
            return "http://localhost:8084";
        }
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }

    public static class Requisicao {
        private String codigoConteiner;
        private LocalDateTime embarcadoEm;

        public Requisicao() {
        }

        public Requisicao(String codigoConteiner, LocalDateTime embarcadoEm) {
            this.codigoConteiner = codigoConteiner;
            this.embarcadoEm = embarcadoEm;
        }

        public String getCodigoConteiner() { return codigoConteiner; }
        public void setCodigoConteiner(String codigoConteiner) { this.codigoConteiner = codigoConteiner; }
        public LocalDateTime getEmbarcadoEm() { return embarcadoEm; }
        public void setEmbarcadoEm(LocalDateTime embarcadoEm) { this.embarcadoEm = embarcadoEm; }
    }

    public static class Resposta {
        private Long atribuicaoEstivaId;
        private Long planoEstivaId;
        private String codigoConteiner;
        private int baia;
        private int fileira;
        private int camada;
        private LocalDateTime embarcadoEm;

        public Long getAtribuicaoEstivaId() { return atribuicaoEstivaId; }
        public void setAtribuicaoEstivaId(Long atribuicaoEstivaId) { this.atribuicaoEstivaId = atribuicaoEstivaId; }
        public Long getPlanoEstivaId() { return planoEstivaId; }
        public void setPlanoEstivaId(Long planoEstivaId) { this.planoEstivaId = planoEstivaId; }
        public String getCodigoConteiner() { return codigoConteiner; }
        public void setCodigoConteiner(String codigoConteiner) { this.codigoConteiner = codigoConteiner; }
        public int getBaia() { return baia; }
        public void setBaia(int baia) { this.baia = baia; }
        public int getFileira() { return fileira; }
        public void setFileira(int fileira) { this.fileira = fileira; }
        public int getCamada() { return camada; }
        public void setCamada(int camada) { this.camada = camada; }
        public LocalDateTime getEmbarcadoEm() { return embarcadoEm; }
        public void setEmbarcadoEm(LocalDateTime embarcadoEm) { this.embarcadoEm = embarcadoEm; }
    }
}
