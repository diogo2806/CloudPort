package br.com.cloudport.servicoyard.integracao.navio;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.navio.integracao", havingValue = "http", matchIfMissing = true)
public class ConsultaPlanejamentoNavioHttpCliente implements ConsultaPlanejamentoNavioPorta {
    private final RestTemplate restTemplate;
    private final String navioBaseUrl;
    private final String visitaBaseUrl;
    public ConsultaPlanejamentoNavioHttpCliente(RestTemplateBuilder builder,
            @Value("${cloudport.integracao.navio.base-url:http://localhost:8084}") String navioBaseUrl,
            @Value("${cloudport.integracao.navio-siderurgico.base-url:http://localhost:8085}") String visitaBaseUrl) {
        this.restTemplate = builder.build();
        this.navioBaseUrl = removerBarraFinal(navioBaseUrl, "http://localhost:8084");
        this.visitaBaseUrl = removerBarraFinal(visitaBaseUrl, "http://localhost:8085");
    }
    @Override public NavioPlanejamento buscarNavioPorId(Long identificador) {
        if (identificador == null) throw new IllegalArgumentException("O identificador do navio canônico deve ser informado.");
        NavioResposta resposta = restTemplate.getForObject(navioBaseUrl + "/navios/{identificador}", NavioResposta.class, identificador);
        if (resposta == null || resposta.getIdentificador() == null) {
            throw new IllegalArgumentException("Navio canônico não encontrado: " + identificador + ".");
        }
        return resposta.paraDominio();
    }
    @Override public NavioPlanejamento buscarNavioPorImo(String codigoImo) {
        String imo = normalizar(codigoImo);
        if (!StringUtils.hasText(imo)) throw new IllegalArgumentException("O código IMO deve ser informado.");
        ResponseEntity<NavioResumoResposta[]> resposta = restTemplate.getForEntity(navioBaseUrl + "/navios", NavioResumoResposta[].class);
        NavioResumoResposta resumo = Arrays.stream(resposta.getBody() == null ? new NavioResumoResposta[0] : resposta.getBody())
                .filter(Objects::nonNull).filter(item -> imo.equals(normalizar(item.getCodigoImo())))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Não existe navio no cadastro canônico com o IMO " + imo + "."));
        return buscarNavioPorId(resumo.getIdentificador());
    }
    @Override public VisitaPlanejamento buscarVisitaPorId(Long identificador) {
        if (identificador == null) throw new IllegalArgumentException("O identificador da visita canônica deve ser informado.");
        VisitaResposta resposta = restTemplate.getForObject(visitaBaseUrl + "/visitas-navio/{identificador}/planejamento",
                VisitaResposta.class, identificador);
        if (resposta == null || resposta.getVisitaNavioId() == null) {
            throw new IllegalArgumentException("Visita canônica não encontrada: " + identificador + ".");
        }
        return resposta.paraDominio();
    }
    private String removerBarraFinal(String valor, String padrao) {
        if (!StringUtils.hasText(valor)) return padrao;
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }
    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : "";
    }
    public static class NavioResumoResposta {
        private Long identificador; private String codigoImo;
        public Long getIdentificador() { return identificador; }
        public void setIdentificador(Long identificador) { this.identificador = identificador; }
        public String getCodigoImo() { return codigoImo; }
        public void setCodigoImo(String codigoImo) { this.codigoImo = codigoImo; }
    }
    public static class NavioResposta {
        private Long identificador; private String nome; private String codigoImo; private String callSign; private Long versao;
        public NavioPlanejamento paraDominio() { return new NavioPlanejamento(identificador, nome, codigoImo, callSign, versao); }
        public Long getIdentificador() { return identificador; }
        public void setIdentificador(Long identificador) { this.identificador = identificador; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getCodigoImo() { return codigoImo; }
        public void setCodigoImo(String codigoImo) { this.codigoImo = codigoImo; }
        public String getCallSign() { return callSign; }
        public void setCallSign(String callSign) { this.callSign = callSign; }
        public Long getVersao() { return versao; }
        public void setVersao(Long versao) { this.versao = versao; }
    }
    public static class VisitaResposta {
        private Long visitaNavioId; private Long navioCadastroId; private String codigoVisita;
        private String viagemEntrada; private String viagemSaida; private String fase; private Long versao;
        public VisitaPlanejamento paraDominio() {
            return new VisitaPlanejamento(visitaNavioId, navioCadastroId, codigoVisita, viagemEntrada, viagemSaida, fase, versao);
        }
        public Long getVisitaNavioId() { return visitaNavioId; }
        public void setVisitaNavioId(Long visitaNavioId) { this.visitaNavioId = visitaNavioId; }
        public Long getNavioCadastroId() { return navioCadastroId; }
        public void setNavioCadastroId(Long navioCadastroId) { this.navioCadastroId = navioCadastroId; }
        public String getCodigoVisita() { return codigoVisita; }
        public void setCodigoVisita(String codigoVisita) { this.codigoVisita = codigoVisita; }
        public String getViagemEntrada() { return viagemEntrada; }
        public void setViagemEntrada(String viagemEntrada) { this.viagemEntrada = viagemEntrada; }
        public String getViagemSaida() { return viagemSaida; }
        public void setViagemSaida(String viagemSaida) { this.viagemSaida = viagemSaida; }
        public String getFase() { return fase; }
        public void setFase(String fase) { this.fase = fase; }
        public Long getVersao() { return versao; }
        public void setVersao(Long versao) { this.versao = versao; }
    }
}
