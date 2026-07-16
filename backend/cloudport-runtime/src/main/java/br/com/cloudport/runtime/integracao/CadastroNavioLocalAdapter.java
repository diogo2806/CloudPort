package br.com.cloudport.runtime.integracao;

import br.com.cloudport.serviconavio.navio.dto.NavioDetalheDTO;
import br.com.cloudport.serviconavio.navio.dto.NavioResumoDTO;
import br.com.cloudport.serviconavio.navio.servico.NavioServico;
import br.com.cloudport.serviconaviosiderurgico.porta.CadastroNavioPorta;
import br.com.cloudport.serviconaviosiderurgico.porta.NavioCanonico;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.navio.integracao",
        havingValue = "local")
public class CadastroNavioLocalAdapter implements CadastroNavioPorta {

    private final NavioServico navioServico;

    public CadastroNavioLocalAdapter(NavioServico navioServico) {
        this.navioServico = navioServico;
    }

    @Override
    public NavioCanonico buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O identificador do cadastro canônico deve ser informado.");
        }
        return mapear(navioServico.buscarDetalhe(id));
    }

    @Override
    public NavioCanonico buscarPorImo(String codigoImo) {
        String imo = normalizar(codigoImo);
        NavioResumoDTO resumo = navioServico.listarResumo().stream()
                .filter(item -> imo.equals(normalizar(item.getCodigoImo())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Não existe navio no cadastro canônico com o IMO " + imo + "."));
        return buscarPorId(resumo.getIdentificador());
    }

    private NavioCanonico mapear(NavioDetalheDTO dto) {
        return new NavioCanonico(
                dto.getIdentificador(),
                dto.getNome(),
                dto.getCodigoImo(),
                dto.getPaisBandeira(),
                dto.getEmpresaArmadora(),
                dto.getCapacidadeTeu(),
                dto.getLoaMetros(),
                dto.getCaladoMaximoMetros(),
                dto.getCallSign());
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : "";
    }
}
