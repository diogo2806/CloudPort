package br.com.cloudport.serviconaviosiderurgico.cliente;

import java.math.BigDecimal;
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
public class NavioCadastroCliente {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public NavioCadastroCliente(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cloudport.integracao.navio.base-url:http://localhost:8084}") String baseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    public NavioCanonicoDTO buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O identificador do cadastro canônico deve ser informado.");
        }
        return restTemplate.getForObject(baseUrl + "/navios/{id}", NavioCanonicoDTO.class, id);
    }

    public NavioCanonicoDTO buscarPorImo(String codigoImo) {
        String imo = normalizar(codigoImo);
        ResponseEntity<NavioResumoCanonicoDTO[]> resposta = restTemplate.getForEntity(
                baseUrl + "/navios",
                NavioResumoCanonicoDTO[].class
        );
        NavioResumoCanonicoDTO resumo = Arrays.stream(resposta.getBody() == null ? new NavioResumoCanonicoDTO[0] : resposta.getBody())
                .filter(Objects::nonNull)
                .filter(item -> imo.equals(normalizar(item.getCodigoImo())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Não existe navio no cadastro canônico com o IMO " + imo + "."));
        return buscarPorId(resumo.getIdentificador());
    }

    private String removerBarraFinal(String valor) {
        if (!StringUtils.hasText(valor)) {
            return "http://localhost:8084";
        }
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : "";
    }

    public static class NavioResumoCanonicoDTO {
        private Long identificador;
        private String codigoImo;

        public Long getIdentificador() { return identificador; }
        public void setIdentificador(Long identificador) { this.identificador = identificador; }
        public String getCodigoImo() { return codigoImo; }
        public void setCodigoImo(String codigoImo) { this.codigoImo = codigoImo; }
    }

    public static class NavioCanonicoDTO {
        private Long identificador;
        private String nome;
        private String codigoImo;
        private String paisBandeira;
        private String empresaArmadora;
        private Integer capacidadeTeu;
        private BigDecimal loaMetros;
        private BigDecimal caladoMaximoMetros;
        private String callSign;

        public Long getIdentificador() { return identificador; }
        public void setIdentificador(Long identificador) { this.identificador = identificador; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getCodigoImo() { return codigoImo; }
        public void setCodigoImo(String codigoImo) { this.codigoImo = codigoImo; }
        public String getPaisBandeira() { return paisBandeira; }
        public void setPaisBandeira(String paisBandeira) { this.paisBandeira = paisBandeira; }
        public String getEmpresaArmadora() { return empresaArmadora; }
        public void setEmpresaArmadora(String empresaArmadora) { this.empresaArmadora = empresaArmadora; }
        public Integer getCapacidadeTeu() { return capacidadeTeu; }
        public void setCapacidadeTeu(Integer capacidadeTeu) { this.capacidadeTeu = capacidadeTeu; }
        public BigDecimal getLoaMetros() { return loaMetros; }
        public void setLoaMetros(BigDecimal loaMetros) { this.loaMetros = loaMetros; }
        public BigDecimal getCaladoMaximoMetros() { return caladoMaximoMetros; }
        public void setCaladoMaximoMetros(BigDecimal caladoMaximoMetros) { this.caladoMaximoMetros = caladoMaximoMetros; }
        public String getCallSign() { return callSign; }
        public void setCallSign(String callSign) { this.callSign = callSign; }
    }
}
