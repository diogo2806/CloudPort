package br.com.cloudport.serviconaviosiderurgico.cliente;

import br.com.cloudport.serviconaviosiderurgico.porta.CadastroNavioPorta;
import br.com.cloudport.serviconaviosiderurgico.porta.NavioCanonico;
import java.math.BigDecimal;
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
@ConditionalOnProperty(
        name = "cloudport.modulo.navio.integracao",
        havingValue = "http",
        matchIfMissing = true)
public class NavioCadastroCliente implements CadastroNavioPorta {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public NavioCadastroCliente(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cloudport.integracao.navio.base-url:http://localhost:8084}") String baseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.baseUrl = removerBarraFinal(baseUrl);
    }

    @Override
    public NavioCanonico buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O identificador do cadastro canônico deve ser informado.");
        }
        NavioCanonicoResposta resposta = restTemplate.getForObject(
                baseUrl + "/navios/{id}",
                NavioCanonicoResposta.class,
                id
        );
        if (resposta == null) {
            throw new IllegalArgumentException("O cadastro canônico não retornou o navio " + id + ".");
        }
        return resposta.paraDominio();
    }

    @Override
    public NavioCanonico buscarPorImo(String codigoImo) {
        String imo = normalizar(codigoImo);
        ResponseEntity<NavioResumoCanonicoResposta[]> resposta = restTemplate.getForEntity(
                baseUrl + "/navios",
                NavioResumoCanonicoResposta[].class
        );
        NavioResumoCanonicoResposta resumo = Arrays.stream(
                        resposta.getBody() == null ? new NavioResumoCanonicoResposta[0] : resposta.getBody())
                .filter(Objects::nonNull)
                .filter(item -> imo.equals(normalizar(item.getCodigoImo())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Não existe navio no cadastro canônico com o IMO " + imo + "."));
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

    public static class NavioResumoCanonicoResposta {
        private Long identificador;
        private String codigoImo;

        public Long getIdentificador() { return identificador; }
        public void setIdentificador(Long identificador) { this.identificador = identificador; }
        public String getCodigoImo() { return codigoImo; }
        public void setCodigoImo(String codigoImo) { this.codigoImo = codigoImo; }
    }

    public static class NavioCanonicoResposta {
        private Long identificador;
        private String nome;
        private String codigoImo;
        private String paisBandeira;
        private String empresaArmadora;
        private Integer capacidadeTeu;
        private BigDecimal loaMetros;
        private BigDecimal caladoMaximoMetros;
        private String callSign;

        public NavioCanonico paraDominio() {
            return new NavioCanonico(
                    identificador,
                    nome,
                    codigoImo,
                    paisBandeira,
                    empresaArmadora,
                    capacidadeTeu,
                    loaMetros,
                    caladoMaximoMetros,
                    callSign
            );
        }

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
