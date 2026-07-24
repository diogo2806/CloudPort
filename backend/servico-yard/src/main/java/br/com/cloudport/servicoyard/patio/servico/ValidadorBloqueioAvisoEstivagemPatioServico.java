package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.modelo.AvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.EstadoAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.SeveridadeAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.repositorio.AvisoEstivagemPatioRepositorio;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ValidadorBloqueioAvisoEstivagemPatioServico {

    private static final EnumSet<EstadoAvisoEstivagemPatio> ESTADOS_ATIVOS = EnumSet.of(
            EstadoAvisoEstivagemPatio.ABERTO,
            EstadoAvisoEstivagemPatio.ATRIBUIDO,
            EstadoAvisoEstivagemPatio.EM_CORRECAO,
            EstadoAvisoEstivagemPatio.AGUARDANDO_REVALIDACAO,
            EstadoAvisoEstivagemPatio.REABERTO);
    private static final Pattern POSICAO_PATTERN = Pattern.compile("(\\d++)\\D++(\\d++)(?:\\D++(\\d++))?");
    private static final String ATOR_SISTEMA = "sistema-yard";

    private final AvisoEstivagemPatioServico avisoServico;
    private final AvisoEstivagemPatioRepositorio avisoRepositorio;

    public ValidadorBloqueioAvisoEstivagemPatioServico(
            AvisoEstivagemPatioServico avisoServico,
            AvisoEstivagemPatioRepositorio avisoRepositorio) {
        this.avisoServico = avisoServico;
        this.avisoRepositorio = avisoRepositorio;
    }

    public void validar(String codigoUnidade, String... destinos) {
        Set<String> codigosDestino = codigosPosicao(destinos);
        List<AvisoEstivagemPatio> avisosDaUnidade = avisosAtivosDaUnidade(codigoUnidade);

        boolean unidadePermaneceNaPosicaoViolada = avisosDaUnidade.stream()
                .filter(this::criticoBloqueante)
                .anyMatch(aviso -> codigosDestino.isEmpty()
                        || codigosDestino.contains(aviso.getCodigoPosicao()));
        boolean destinoBloqueado = !codigosDestino.isEmpty()
                && avisoRepositorio.existsByCodigoPosicaoInAndSeveridadeAndBloqueiaOperacaoTrueAndEstadoIn(
                        codigosDestino,
                        SeveridadeAvisoEstivagemPatio.CRITICA,
                        ESTADOS_ATIVOS);

        if (unidadePermaneceNaPosicaoViolada || destinoBloqueado) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Operação bloqueada por aviso crítico de estivagem ativo. "
                            + "A movimentação corretiva deve retirar a unidade da posição violada e usar destino seguro.");
        }
    }

    private List<AvisoEstivagemPatio> avisosAtivosDaUnidade(String codigoUnidade) {
        if (!StringUtils.hasText(codigoUnidade)) {
            return List.of();
        }
        String codigo = codigoUnidade.trim();
        try {
            return avisoServico.reavaliarUnidade(codigo, ATOR_SISTEMA);
        } catch (NoSuchElementException excecao) {
            return avisoRepositorio.findByCodigoUnidadeIgnoreCaseAndEstadoIn(codigo, ESTADOS_ATIVOS);
        }
    }

    private boolean criticoBloqueante(AvisoEstivagemPatio aviso) {
        return aviso.getSeveridade() == SeveridadeAvisoEstivagemPatio.CRITICA
                && aviso.isBloqueiaOperacao()
                && ESTADOS_ATIVOS.contains(aviso.getEstado());
    }

    private Set<String> codigosPosicao(String... destinos) {
        if (destinos == null) {
            return Set.of();
        }
        Set<String> codigos = new HashSet<>();
        for (String destino : destinos) {
            if (!StringUtils.hasText(destino)) {
                continue;
            }
            Matcher matcher = POSICAO_PATTERN.matcher(destino);
            if (!matcher.find()) {
                continue;
            }
            String camada = matcher.group(3) != null ? matcher.group(3) : "1";
            codigos.add(matcher.group(1) + "/" + matcher.group(2) + "/" + camada);
        }
        return codigos;
    }
}
