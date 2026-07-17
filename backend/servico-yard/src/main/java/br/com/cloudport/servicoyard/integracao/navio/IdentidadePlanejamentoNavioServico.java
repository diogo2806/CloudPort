package br.com.cloudport.servicoyard.integracao.navio;

import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IdentidadePlanejamentoNavioServico {

    private final ConsultaPlanejamentoNavioPorta consultaPlanejamentoNavioPorta;

    public IdentidadePlanejamentoNavioServico(
            ConsultaPlanejamentoNavioPorta consultaPlanejamentoNavioPorta) {
        this.consultaPlanejamentoNavioPorta = consultaPlanejamentoNavioPorta;
    }

    public NavioPlanejamento buscarNavioCanonico(Long navioCadastroId) {
        if (navioCadastroId == null) {
            throw new IllegalArgumentException("O identificador do navio canônico deve ser informado.");
        }
        NavioPlanejamento navio = consultaPlanejamentoNavioPorta.buscarNavioPorId(navioCadastroId);
        if (navio == null || navio.identificador() == null) {
            throw new IllegalArgumentException("Navio canônico não encontrado: " + navioCadastroId + ".");
        }
        return navio;
    }

    public ContextoPlanejamentoNavio resolverVisita(
            Long visitaNavioId,
            Long navioCadastroIdEsperado,
            String codigoNavioEsperado,
            String codigoViagemEsperado) {
        if (visitaNavioId == null) {
            throw new IllegalArgumentException("O identificador da visita canônica deve ser informado.");
        }

        VisitaPlanejamento visita = consultaPlanejamentoNavioPorta.buscarVisitaPorId(visitaNavioId);
        if (visita == null || visita.identificador() == null || visita.navioCadastroId() == null) {
            throw new IllegalArgumentException(
                    "A visita " + visitaNavioId + " não está vinculada a um navio canônico.");
        }
        if (navioCadastroIdEsperado != null
                && !Objects.equals(navioCadastroIdEsperado, visita.navioCadastroId())) {
            throw new IllegalArgumentException(
                    "A visita " + visitaNavioId + " pertence ao navio canônico "
                            + visita.navioCadastroId() + ", não ao navio " + navioCadastroIdEsperado + ".");
        }

        NavioPlanejamento navio = buscarNavioCanonico(visita.navioCadastroId());
        validarCodigoNavio(codigoNavioEsperado, navio);
        String codigoViagem = resolverCodigoViagem(codigoViagemEsperado, visita);
        return new ContextoPlanejamentoNavio(navio, visita, codigoViagem);
    }

    public ContextoPlanejamentoNavio validarFontePersistida(
            Long visitaNavioId,
            Long navioCadastroId,
            String codigoNavio,
            String codigoViagem,
            Long versaoNavioCanonico,
            Long versaoVisita) {
        ContextoPlanejamentoNavio contexto = resolverVisita(
                visitaNavioId,
                navioCadastroId,
                codigoNavio,
                codigoViagem);
        if (versaoNavioCanonico == null || versaoVisita == null) {
            throw new IllegalStateException(
                    "O plano não registra as versões das fontes canônicas e deve ser migrado antes da operação.");
        }
        if (!Objects.equals(versaoNavioCanonico, contexto.navio().versao())) {
            throw new IllegalStateException(
                    "O cadastro canônico do navio foi alterado após a criação do plano. Revise o planejamento.");
        }
        if (!Objects.equals(versaoVisita, contexto.visita().versao())) {
            throw new IllegalStateException(
                    "A visita operacional foi alterada após a criação do plano. Revise o planejamento.");
        }
        return contexto;
    }

    private void validarCodigoNavio(String codigoNavioEsperado, NavioPlanejamento navio) {
        if (!StringUtils.hasText(codigoNavioEsperado)) {
            return;
        }
        String codigo = normalizar(codigoNavioEsperado);
        boolean corresponde = codigo.equals(normalizar(navio.codigoImo()))
                || codigo.equals(normalizar(navio.nome()))
                || codigo.equals(normalizar(navio.callSign()))
                || codigo.equals(String.valueOf(navio.identificador()));
        if (!corresponde) {
            throw new IllegalArgumentException(
                    "O navio informado no plano não corresponde ao cadastro canônico da visita.");
        }
    }

    private String resolverCodigoViagem(String codigoViagemEsperado, VisitaPlanejamento visita) {
        String viagemEntrada = normalizar(visita.viagemEntrada());
        String viagemSaida = normalizar(visita.viagemSaida());
        if (!StringUtils.hasText(codigoViagemEsperado)) {
            if (StringUtils.hasText(viagemSaida)) {
                return visita.viagemSaida().trim();
            }
            if (StringUtils.hasText(viagemEntrada)) {
                return visita.viagemEntrada().trim();
            }
            throw new IllegalArgumentException(
                    "A visita " + visita.identificador() + " não possui viagem operacional definida.");
        }

        String codigo = normalizar(codigoViagemEsperado);
        if (codigo.equals(viagemSaida)) {
            return visita.viagemSaida().trim();
        }
        if (codigo.equals(viagemEntrada)) {
            return visita.viagemEntrada().trim();
        }
        throw new IllegalArgumentException(
                "A viagem informada não corresponde à viagem de entrada ou saída da visita canônica.");
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : "";
    }
}
