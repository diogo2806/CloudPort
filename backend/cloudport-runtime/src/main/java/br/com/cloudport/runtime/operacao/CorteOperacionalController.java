package br.com.cloudport.runtime.operacao;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operacao/corte")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','PLANEJADOR')")
public class CorteOperacionalController {

    private static final List<String> SCHEMAS = List.of(
            "cloudport_autenticacao",
            "cloudport_carga_geral",
            "cloudport_gate",
            "cloudport_rail",
            "cloudport_visibilidade",
            "cloudport_yard",
            "cloudport_navio",
            "cloudport_siderurgico");

    private final Instant iniciadoEm = Instant.now();
    private final String instanciaId;
    private final boolean escritaHabilitada;
    private final boolean jobsHabilitados;
    private final boolean consumidoresHabilitados;
    private final String integracaoAutenticacao;
    private final String integracaoNavio;
    private final String integracaoYard;
    private final String revisao;

    public CorteOperacionalController(
            @Value("${cloudport.runtime.instance-id:local}") String instanciaId,
            @Value("${cloudport.runtime.cutover-writes-enabled:false}") boolean escritaHabilitada,
            @Value("${cloudport.runtime.jobs-enabled:false}") boolean jobsHabilitados,
            @Value("${cloudport.runtime.consumers-enabled:false}") boolean consumidoresHabilitados,
            @Value("${cloudport.modulo.autenticacao.integracao:local}") String integracaoAutenticacao,
            @Value("${cloudport.modulo.navio.integracao:local}") String integracaoNavio,
            @Value("${cloudport.modulo.yard.integracao:local}") String integracaoYard,
            @Value("${cloudport.runtime.revisao:desconhecida}") String revisao) {
        this.instanciaId = instanciaId;
        this.escritaHabilitada = escritaHabilitada;
        this.jobsHabilitados = jobsHabilitados;
        this.consumidoresHabilitados = consumidoresHabilitados;
        this.integracaoAutenticacao = integracaoAutenticacao;
        this.integracaoNavio = integracaoNavio;
        this.integracaoYard = integracaoYard;
        this.revisao = revisao;
    }

    @GetMapping
    public EstadoCorteOperacional consultar() {
        Map<String, String> integracoes = new LinkedHashMap<>();
        integracoes.put("autenticacao", integracaoAutenticacao);
        integracoes.put("navio", integracaoNavio);
        integracoes.put("yard", integracaoYard);
        boolean adaptadoresLocais = integracoes.values().stream()
                .allMatch(valor -> "local".equalsIgnoreCase(valor));
        String papel = escritaHabilitada && jobsHabilitados && consumidoresHabilitados
                ? "CANONICO_ATIVO"
                : escritaHabilitada
                        ? "CANONICO_CONTROLADO"
                        : "OBSERVACAO_SOMENTE_LEITURA";
        return new EstadoCorteOperacional(
                "cloudport-runtime",
                instanciaId,
                revisao,
                papel,
                iniciadoEm,
                escritaHabilitada,
                jobsHabilitados,
                consumidoresHabilitados,
                adaptadoresLocais,
                Map.copyOf(integracoes),
                SCHEMAS);
    }

    public record EstadoCorteOperacional(
            String runtime,
            String instanciaId,
            String revisao,
            String papel,
            Instant iniciadoEm,
            boolean escritaHabilitada,
            boolean jobsHabilitados,
            boolean consumidoresHabilitados,
            boolean adaptadoresLocais,
            Map<String, String> integracoes,
            List<String> schemas) {
    }
}
