package br.com.cloudport.runtime.integracao;

import br.com.cloudport.serviconavio.navio.dto.NavioDetalheDTO;
import br.com.cloudport.serviconavio.navio.dto.NavioResumoDTO;
import br.com.cloudport.serviconavio.navio.servico.NavioServico;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.servico.VisitaNavioServico;
import br.com.cloudport.servicoyard.integracao.navio.ConsultaPlanejamentoNavioPorta;
import br.com.cloudport.servicoyard.integracao.navio.NavioPlanejamento;
import br.com.cloudport.servicoyard.integracao.navio.VisitaPlanejamento;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.navio.integracao",
        havingValue = "local")
public class ConsultaPlanejamentoNavioLocalAdapter implements ConsultaPlanejamentoNavioPorta {

    private final NavioServico navioServico;
    private final VisitaNavioServico visitaNavioServico;

    public ConsultaPlanejamentoNavioLocalAdapter(
            NavioServico navioServico,
            VisitaNavioServico visitaNavioServico) {
        this.navioServico = navioServico;
        this.visitaNavioServico = visitaNavioServico;
    }

    @Override
    public NavioPlanejamento buscarNavioPorId(Long identificador) {
        NavioDetalheDTO dto = navioServico.buscarDetalhe(identificador);
        return new NavioPlanejamento(
                dto.getIdentificador(),
                dto.getNome(),
                dto.getCodigoImo(),
                dto.getCallSign(),
                dto.getVersao());
    }

    @Override
    public NavioPlanejamento buscarNavioPorImo(String codigoImo) {
        String imo = normalizar(codigoImo);
        NavioResumoDTO resumo = navioServico.listarResumo().stream()
                .filter(item -> imo.equals(normalizar(item.getCodigoImo())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Não existe navio no cadastro canônico com o IMO " + imo + "."));
        return buscarNavioPorId(resumo.getIdentificador());
    }

    @Override
    public VisitaPlanejamento buscarVisitaPorId(Long identificador) {
        VisitaNavio visita = visitaNavioServico.buscarEntidade(identificador);
        if (visita.getNavio().getNavioCadastroId() == null) {
            throw new IllegalStateException(
                    "A visita operacional não está vinculada ao cadastro canônico de navios.");
        }
        return new VisitaPlanejamento(
                visita.getId(),
                visita.getNavio().getNavioCadastroId(),
                visita.getCodigoVisita(),
                visita.getViagemEntrada(),
                visita.getViagemSaida(),
                visita.getFase() == null ? null : visita.getFase().name(),
                visita.getVersao());
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : "";
    }
}
