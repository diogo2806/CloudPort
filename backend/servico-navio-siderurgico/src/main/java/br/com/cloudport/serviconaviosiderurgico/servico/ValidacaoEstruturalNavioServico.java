package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.BordoEstivaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.NavioSiderurgico;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoCargaSiderurgica;
import br.com.cloudport.serviconaviosiderurgico.dto.ConfiguracaoRestricoesEstruturaisDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ConfiguracaoRestricoesEstruturaisDTO.RegraSegregacaoDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PlanoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.PosicaoEstivaNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ValidacaoEstruturalNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ValidacaoEstruturalNavioDTO.OcorrenciaEstruturalDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValidacaoEstruturalNavioServico {

    private final VisitaNavioServico visitaServico;
    private final PlanoEstivaNavioServico planoServico;
    private final ItemOperacaoNavioRepositorio itemRepositorio;

    public ValidacaoEstruturalNavioServico(
            VisitaNavioServico visitaServico,
            PlanoEstivaNavioServico planoServico,
            ItemOperacaoNavioRepositorio itemRepositorio
    ) {
        this.visitaServico = visitaServico;
        this.planoServico = planoServico;
        this.itemRepositorio = itemRepositorio;
    }

    @Transactional(readOnly = true)
    public ValidacaoEstruturalNavioDTO validar(Long visitaId, ConfiguracaoRestricoesEstruturaisDTO configuracao) {
        NavioSiderurgico navio = visitaServico.buscarEntidade(visitaId).getNavio();
        PlanoEstivaNavioDTO plano;
        try {
            plano = planoServico.obter(visitaId);
        } catch (IllegalArgumentException erro) {
            return new ValidacaoEstruturalNavioDTO(
                    visitaId,
                    null,
                    LocalDateTime.now(),
                    "SEM_PLANO",
                    List.of(),
                    List.of(ocorrencia("ESTIVA", "PLANO_AUSENTE", erro.getMessage(), null, null, null, null)),
                    verificacoesNaoConfiguradas(configuracao)
            );
        }

        List<PosicaoEstivaNavioDTO> posicoes = plano.posicoes() == null ? List.of() : plano.posicoes();
        Map<Long, ItemOperacaoNavio> itens = new HashMap<>();
        itemRepositorio.findByVisitaNavioId(visitaId).forEach(item -> itens.put(item.getId(), item));
        List<OcorrenciaEstruturalDTO> erros = new ArrayList<>();
        List<OcorrenciaEstruturalDTO> alertas = new ArrayList<>();

        validarPesoPorao(navio, configuracao, posicoes, erros, alertas);
        validarPesoCamada(configuracao, posicoes, erros);
        validarAlturaInterdicaoELashing(configuracao, posicoes, erros);
        validarEquilibrio(configuracao, posicoes, erros, alertas);
        validarSegregacao(configuracao, posicoes, itens, erros);

        String status = erros.isEmpty() ? (alertas.isEmpty() ? "VALIDO" : "VALIDO_COM_ALERTAS") : "INVALIDO";
        return new ValidacaoEstruturalNavioDTO(
                visitaId,
                plano.id(),
                LocalDateTime.now(),
                status,
                List.copyOf(erros),
                List.copyOf(alertas),
                verificacoesNaoConfiguradas(configuracao)
        );
    }

    private void validarPesoPorao(
            NavioSiderurgico navio,
            ConfiguracaoRestricoesEstruturaisDTO configuracao,
            List<PosicaoEstivaNavioDTO> posicoes,
            List<OcorrenciaEstruturalDTO> erros,
            List<OcorrenciaEstruturalDTO> alertas
    ) {
        BigDecimal limite = configuracao == null ? null : configuracao.limitePesoPorPoraoToneladas();
        boolean estimado = false;
        if (limite == null && navio.getDwtToneladas() != null && navio.getQuantidadePoroes() != null
                && navio.getQuantidadePoroes() > 0) {
            limite = navio.getDwtToneladas().divide(
                    BigDecimal.valueOf(navio.getQuantidadePoroes()),
                    3,
                    RoundingMode.HALF_UP
            );
            estimado = true;
        }
        if (limite == null) {
            return;
        }
        Map<Integer, BigDecimal> totais = new HashMap<>();
        posicoes.forEach(posicao -> totais.merge(posicao.porao(), valor(posicao.pesoToneladas()), BigDecimal::add));
        for (Map.Entry<Integer, BigDecimal> total : totais.entrySet()) {
            if (total.getValue().compareTo(limite) > 0) {
                erros.add(ocorrencia(
                        "ESTRUTURA",
                        "PESO_PORAO_EXCEDIDO",
                        "O porao " + total.getKey() + " possui " + total.getValue() + " t para o limite de " + limite + " t.",
                        null,
                        total.getKey(),
                        null,
                        null
                ));
            } else if (total.getValue().compareTo(limite.multiply(BigDecimal.valueOf(0.9))) >= 0) {
                alertas.add(ocorrencia(
                        "ESTRUTURA",
                        estimado ? "PESO_PORAO_PROXIMO_LIMITE_ESTIMADO" : "PESO_PORAO_PROXIMO_LIMITE",
                        "O porao " + total.getKey() + " atingiu pelo menos 90% do limite considerado.",
                        null,
                        total.getKey(),
                        null,
                        null
                ));
            }
        }
    }

    private void validarPesoCamada(
            ConfiguracaoRestricoesEstruturaisDTO configuracao,
            List<PosicaoEstivaNavioDTO> posicoes,
            List<OcorrenciaEstruturalDTO> erros
    ) {
        if (configuracao == null || configuracao.limitePesoPorCamadaToneladas() == null) {
            return;
        }
        Map<String, BigDecimal> totais = new HashMap<>();
        posicoes.forEach(posicao -> totais.merge(
                posicao.porao() + "|" + posicao.camada(),
                valor(posicao.pesoToneladas()),
                BigDecimal::add
        ));
        totais.forEach((chave, peso) -> {
            if (peso.compareTo(configuracao.limitePesoPorCamadaToneladas()) > 0) {
                String[] partes = chave.split("\\|");
                erros.add(ocorrencia(
                        "ESTRUTURA",
                        "PESO_CAMADA_EXCEDIDO",
                        "A camada " + partes[1] + " do porao " + partes[0] + " excede o limite configurado.",
                        null,
                        Integer.valueOf(partes[0]),
                        Integer.valueOf(partes[1]),
                        null
                ));
            }
        });
    }

    private void validarAlturaInterdicaoELashing(
            ConfiguracaoRestricoesEstruturaisDTO configuracao,
            List<PosicaoEstivaNavioDTO> posicoes,
            List<OcorrenciaEstruturalDTO> erros
    ) {
        if (configuracao == null) {
            return;
        }
        Set<Integer> interditados = configuracao.poroesInterditados() == null
                ? Set.of()
                : configuracao.poroesInterditados();
        Set<String> comLashing = new HashSet<>();
        if (configuracao.posicoesComLashing() != null) {
            configuracao.posicoesComLashing().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .forEach(comLashing::add);
        }
        for (PosicaoEstivaNavioDTO posicao : posicoes) {
            if (configuracao.alturaMaximaCamadas() != null && posicao.camada() > configuracao.alturaMaximaCamadas()) {
                erros.add(ocorrenciaPosicao("ESTRUTURA", "ALTURA_EXCEDIDA", "A posicao excede a altura maxima configurada.", posicao));
            }
            if (interditados.contains(posicao.porao())) {
                erros.add(ocorrenciaPosicao("ESTRUTURA", "PORAO_INTERDITADO", "Existe carga planejada em porao interditado.", posicao));
            }
            if (configuracao.exigirLashingAPartirCamada() != null
                    && posicao.camada() >= configuracao.exigirLashingAPartirCamada()
                    && !comLashing.contains(chavePosicao(posicao))) {
                erros.add(ocorrenciaPosicao("LASHING", "LASHING_AUSENTE", "A posicao exige lashing registrado.", posicao));
            }
        }
    }

    private void validarEquilibrio(
            ConfiguracaoRestricoesEstruturaisDTO configuracao,
            List<PosicaoEstivaNavioDTO> posicoes,
            List<OcorrenciaEstruturalDTO> erros,
            List<OcorrenciaEstruturalDTO> alertas
    ) {
        if (configuracao == null || configuracao.desequilibrioBombordoBoresteMaximoPercentual() == null) {
            return;
        }
        BigDecimal bombordo = peso(posicoes, BordoEstivaNavio.BB);
        BigDecimal boreste = peso(posicoes, BordoEstivaNavio.BE);
        BigDecimal total = bombordo.add(boreste);
        if (total.signum() == 0) {
            alertas.add(ocorrencia(
                    "ESTABILIDADE",
                    "SEM_PESO_LATERAL",
                    "Nao existem pesos em bombordo ou boreste para validar o equilibrio transversal.",
                    null,
                    null,
                    null,
                    null
            ));
            return;
        }
        BigDecimal desequilibrio = bombordo.subtract(boreste).abs()
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
        if (desequilibrio.compareTo(configuracao.desequilibrioBombordoBoresteMaximoPercentual()) > 0) {
            erros.add(ocorrencia(
                    "ESTABILIDADE",
                    "DESEQUILIBRIO_TRANSVERSAL",
                    "O desequilibrio entre bombordo e boreste e de " + desequilibrio + "%.",
                    null,
                    null,
                    null,
                    null
            ));
        }
    }

    private void validarSegregacao(
            ConfiguracaoRestricoesEstruturaisDTO configuracao,
            List<PosicaoEstivaNavioDTO> posicoes,
            Map<Long, ItemOperacaoNavio> itens,
            List<OcorrenciaEstruturalDTO> erros
    ) {
        List<RegraSegregacaoDTO> regras = configuracao == null || configuracao.regrasSegregacao() == null
                ? List.of()
                : configuracao.regrasSegregacao();
        for (int a = 0; a < posicoes.size(); a++) {
            PosicaoEstivaNavioDTO posicaoA = posicoes.get(a);
            ItemOperacaoNavio itemA = itens.get(posicaoA.itemOperacaoId());
            if (itemA == null) {
                continue;
            }
            for (int b = a + 1; b < posicoes.size(); b++) {
                PosicaoEstivaNavioDTO posicaoB = posicoes.get(b);
                ItemOperacaoNavio itemB = itens.get(posicaoB.itemOperacaoId());
                if (itemB == null) {
                    continue;
                }
                for (RegraSegregacaoDTO regra : regras) {
                    if (combina(regra, itemA.getTipoCarga(), itemB.getTipoCarga())
                            && viola(regra, posicaoA, posicaoB)) {
                        erros.add(ocorrenciaPosicao(
                                "SEGREGACAO",
                                "SEGREGACAO_INCOMPATIVEL",
                                "As cargas " + itemA.getCodigoLote() + " e " + itemB.getCodigoLote() + " violam a regra de segregacao.",
                                posicaoA
                        ));
                    }
                }
            }
        }
    }

    private boolean combina(RegraSegregacaoDTO regra, TipoCargaSiderurgica a, TipoCargaSiderurgica b) {
        return regra != null && ((regra.tipoA() == a && regra.tipoB() == b)
                || (regra.tipoA() == b && regra.tipoB() == a));
    }

    private boolean viola(RegraSegregacaoDTO regra, PosicaoEstivaNavioDTO a, PosicaoEstivaNavioDTO b) {
        if (!Objects.equals(a.porao(), b.porao())) {
            return false;
        }
        return regra.mesmoPoraoProibido()
                || Math.abs(a.coluna() - b.coluna()) < regra.distanciaMinimaColunas();
    }

    private List<String> verificacoesNaoConfiguradas(ConfiguracaoRestricoesEstruturaisDTO configuracao) {
        List<String> faltantes = new ArrayList<>();
        if (configuracao == null || configuracao.limitePesoPorCamadaToneladas() == null) faltantes.add("LIMITE_PESO_POR_CAMADA");
        if (configuracao == null || configuracao.desequilibrioBombordoBoresteMaximoPercentual() == null) faltantes.add("ESTABILIDADE_TRANSVERSAL");
        if (configuracao == null || configuracao.alturaMaximaCamadas() == null) faltantes.add("ALTURA_MAXIMA");
        if (configuracao == null || configuracao.exigirLashingAPartirCamada() == null) faltantes.add("LASHING");
        if (configuracao == null || configuracao.regrasSegregacao() == null || configuracao.regrasSegregacao().isEmpty()) faltantes.add("SEGREGACAO");
        return List.copyOf(faltantes);
    }

    private BigDecimal peso(List<PosicaoEstivaNavioDTO> posicoes, BordoEstivaNavio bordo) {
        return posicoes.stream()
                .filter(posicao -> posicao.bordo() == bordo)
                .map(PosicaoEstivaNavioDTO::pesoToneladas)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String chavePosicao(PosicaoEstivaNavioDTO posicao) {
        return (posicao.porao() + "-" + posicao.camada() + "-" + posicao.coluna() + "-" + posicao.bordo()).toUpperCase();
    }

    private BigDecimal valor(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private OcorrenciaEstruturalDTO ocorrencia(
            String categoria,
            String codigo,
            String mensagem,
            Long itemId,
            Integer porao,
            Integer camada,
            Integer coluna
    ) {
        return new OcorrenciaEstruturalDTO(categoria, codigo, mensagem, itemId, porao, camada, coluna);
    }

    private OcorrenciaEstruturalDTO ocorrenciaPosicao(
            String categoria,
            String codigo,
            String mensagem,
            PosicaoEstivaNavioDTO posicao
    ) {
        return ocorrencia(
                categoria,
                codigo,
                mensagem,
                posicao.itemOperacaoId(),
                posicao.porao(),
                posicao.camada(),
                posicao.coluna()
        );
    }
}
