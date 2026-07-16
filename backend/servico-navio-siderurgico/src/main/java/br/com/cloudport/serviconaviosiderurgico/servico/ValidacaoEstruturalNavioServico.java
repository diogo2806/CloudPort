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
                    List.of(ocorrencia("ESTIVA", "PLANO_AUSENTE", erro.getMessage(), null)),
                    verificacoesNaoConfiguradas(configuracao)
            );
        }

        List<OcorrenciaEstruturalDTO> erros = new ArrayList<>();
        List<OcorrenciaEstruturalDTO> alertas = new ArrayList<>();
        List<String> naoConfiguradas = verificacoesNaoConfiguradas(configuracao);
        List<PosicaoEstivaNavioDTO> posicoes = plano.posicoes() == null ? List.of() : plano.posicoes();
        Map<Long, ItemOperacaoNavio> itens = carregarItens(visitaId);

        validarPesoPorPorao(navio, configuracao, posicoes, erros, alertas);
        validarPesoPorCamada(configuracao, posicoes, erros);
        validarAltura(configuracao, posicoes, erros);
        validarPoroesInterditados(configuracao, posicoes, erros);
        validarLashing(configuracao, posicoes, erros);
        validarEstabilidadeTransversal(configuracao, posicoes, erros, alertas);
        validarSegregacao(configuracao, posicoes, itens, erros);

        String status = erros.isEmpty() ? (alertas.isEmpty() ? "VALIDO" : "VALIDO_COM_ALERTAS") : "INVALIDO";
        return new ValidacaoEstruturalNavioDTO(
                visitaId,
                plano.id(),
                LocalDateTime.now(),
                status,
                List.copyOf(erros),
                List.copyOf(alertas),
                naoConfiguradas
        );
    }

    private void validarPesoPorPorao(
            NavioSiderurgico navio,
            ConfiguracaoRestricoesEstruturaisDTO configuracao,
            List<PosicaoEstivaNavioDTO> posicoes,
            List<OcorrenciaEstruturalDTO> erros,
            List<OcorrenciaEstruturalDTO> alertas
    ) {
        BigDecimal limite = configuracao == null ? null : configuracao.limitePesoPorPoraoToneladas();
        boolean estimado = false;
        if (limite == null && navio.getDwtToneladas() != null && navio.getQuantidadePoroes() != null && navio.getQuantidadePoroes() > 0) {
            limite = navio.getDwtToneladas().divide(BigDecimal.valueOf(navio.getQuantidadePoroes()), 3, RoundingMode.HALF_UP);
            estimado = true;
        }
        if (limite == null) {
            return;
        }
        Map<Integer, BigDecimal> pesoPorPorao = new HashMap<>();
        posicoes.forEach(posicao -> pesoPorPorao.merge(posicao.porao(), valor(posicao.pesoToneladas()), BigDecimal::add));
        for (Map.Entry<Integer, BigDecimal> entrada : pesoPorPorao.entrySet()) {
            if (entrada.getValue().compareTo(limite) > 0) {
                erros.add(new OcorrenciaEstruturalDTO(
                        "ESTRUTURA",
                        "PESO_PORAO_EXCEDIDO",
                        "O porao " + entrada.getKey() + " possui " + entrada.getValue() + " t para o limite de " + limite + " t.",
                        null,
                        entrada.getKey(),
                        null,
                        null
                ));
            } else if (entrada.getValue().compareTo(limite.multiply(BigDecimal.valueOf(0.90))) >= 0) {
                alertas.add(new OcorrenciaEstruturalDTO(
                        "ESTRUTURA",
                        estimado ? "PESO_PORAO_PROXIMO_LIMITE_ESTIMADO" : "PESO_PORAO_PROXIMO_LIMITE",
                        "O porao " + entrada.getKey() + " atingiu pelo menos 90% do limite considerado.",
                        null,
                        entrada.getKey(),
                        null,
                        null
                ));
            }
        }
    }

    private void validarPesoPorCamada(
            ConfiguracaoRestricoesEstruturaisDTO configuracao,
            List<PosicaoEstivaNavioDTO> posicoes,
            List<OcorrenciaEstruturalDTO> erros
    ) {
        if (configuracao == null || configuracao.limitePesoPorCamadaToneladas() == null) {
            return;
        }
        Map<String, BigDecimal> pesoPorCamada = new HashMap<>();
        posicoes.forEach(posicao -> pesoPorCamada.merge(
                posicao.porao() + "|" + posicao.camada(),
                valor(posicao.pesoToneladas()),
                BigDecimal::add
        ));
        pesoPorCamada.forEach((chave, peso) -> {
            if (peso.compareTo(configuracao.limitePesoPorCamadaToneladas()) > 0) {
                String[] partes = chave.split("\\|");
                erros.add(new OcorrenciaEstruturalDTO(
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

    private void validarAltura(
            ConfiguracaoRestricoesEstruturaisDTO configuracao,
            List<PosicaoEstivaNavioDTO> posicoes,
            List<OcorrenciaEstruturalDTO> erros
    ) {
        if (configuracao == null || configuracao.alturaMaximaCamadas() == null) {
            return;
        }
        posicoes.stream()
                .filter(posicao -> posicao.camada() > configuracao.alturaMaximaCamadas())
                .forEach(posicao -> erros.add(ocorrenciaPosicao(
                        "ESTRUTURA",
                        "ALTURA_EXCEDIDA",
                        "A posicao excede a altura maxima de " + configuracao.alturaMaximaCamadas() + " camadas.",
                        posicao
                )));
    }

    private void validarPoroesInterditados(
            ConfiguracaoRestricoesEstruturaisDTO configuracao,
            List<PosicaoEstivaNavioDTO> posicoes,
            List<OcorrenciaEstruturalDTO> erros
    ) {
        Set<Integer> interditados = configuracao == null || configuracao.poroesInterditados() == null
                ? Set.of()
                : configuracao.poroesInterditados();
        posicoes.stream()
                .filter(posicao -> interditados.contains(posicao.porao()))
                .forEach(posicao -> erros.add(ocorrenciaPosicao(
                        "ESTRUTURA",
                        "PORAO_INTERDITADO",
                        "Existe carga planejada em porao interditado.",
                        posicao
                )));
    }

    private void validarLashing(
            ConfiguracaoRestricoesEstruturaisDTO configuracao,
            List<PosicaoEstivaNavioDTO> posicoes,
            List<OcorrenciaEstruturalDTO> erros
    ) {
        if (configuracao == null || configuracao.exigirLashingAPartirCamada() == null) {
            return;
        }
        Set<String> comLashing = normalizarPosicoes(configuracao.posicoesComLashing());
        posicoes.stream()
                .filter(posicao -> posicao.camada() >= configuracao.exigirLashingAPartirCamada())
                .filter(posicao -> !comLashing.contains(chavePosicao(posicao)))
                .forEach(posicao -> erros.add(ocorrenciaPosicao(
                        "LASHING",
                        "LASHING_AUSENTE",
                        "A posicao exige lashing registrado a partir da camada " + configuracao.exigirLashingAPartirCamada() + ".",
                        posicao
                )));
    }

    private void validarEstabilidadeTransversal(
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
        BigDecimal totalLateral = bombordo.add(boreste);
        if (totalLateral.signum() == 0) {
            alertas.add(ocorrencia("ESTABILIDADE", "SEM_PESO_LATERAL", "Nao existem pesos em bombordo ou boreste para validar o equilibrio transversal.", null));
            return;
        }
        BigDecimal desequilibrio = bombordo.subtract(boreste).abs()
                .multiply(BigDecimal.valueOf(100))
                .divide(totalLateral, 2, RoundingMode.HALF_UP);
        if (desequilibrio.compareTo(configuracao.desequilibrioBombordoBoresteMaximoPercentual()) > 0) {
            erros.add(ocorrencia(
                    "ESTABILIDADE",
                    "DESEQUILIBRIO_TRANSVERSAL",
                    "O desequilibrio entre bombordo e boreste e de " + desequilibrio + "% e excede o limite configurado.",
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
        for (int indiceA = 0; indiceA < posicoes.size(); indiceA++) {
            PosicaoEstivaNavioDTO posicaoA = posicoes.get(indiceA);
            ItemOperacaoNavio itemA = itens.get(posicaoA.itemOperacaoId());
            if (itemA == null) {
                continue;
            }
            for (int indiceB = indiceA + 1; indiceB < posicoes.size(); indiceB++) {
                PosicaoEstivaNavioDTO posicaoB = posicoes.get(indiceB);
                ItemOperacaoNavio itemB = itens.get(posicaoB.itemOperacaoId());
                if (itemB == null) {
                    continue;
                }
                regras.stream()
                        .filter(regra -> combina(regra, itemA.getTipoCarga(), itemB.getTipoCarga()))
                        .filter(regra -> violaSegregacao(regra, posicaoA, posicaoB))
                        .forEach(regra -> erros.add(new OcorrenciaEstruturalDTO(
                                "SEGREGACAO",
                                "SEGREGACAO_INCOMPATIVEL",
                                "As cargas " + itemA.getCodigoLote() + " e " + itemB.getCodigoLote() + " violam a regra de segregacao.",
                                itemA.getId(),
                                posicaoA.porao(),
                                posicaoA.camada(),
                                posicaoA.coluna()
                        )));
            }
        }
    }

    private boolean combina(RegraSegregacaoDTO regra, TipoCargaSiderurgica tipoA, TipoCargaSiderurgica tipoB) {
        return regra != null && (
                Objects.equals(regra.tipoA(), tipoA) && Objects.equals(regra.tipoB(), tipoB)
                        || Objects.equals(regra.tipoA(), tipoB) && Objects.equals(regra.tipoB(), tipoA)
        );
    }

    private boolean violaSegregacao(RegraSegregacaoDTO regra, PosicaoEstivaNavioDTO a, PosicaoEstivaNavioDTO b) {
        if (regra.mesmoPoraoProibido() && Objects.equals(a.porao(), b.porao())) {
            return true;
        }
        return Objects.equals(a.porao(), b.porao())
                && Math.abs(a.coluna() - b.coluna()) < regra.distanciaMinimaColunas();
    }

    private Map<Long, ItemOperacaoNavio> carregarItens(Long visitaId) {
        Map<Long, ItemOperacaoNavio> itens = new HashMap<>();
        itemRepositorio.findByVisitaNavioId(visitaId).forEach(item -> itens.put(item.getId(), item));
        return itens;
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

    private Set<String> normalizarPosicoes(Set<String> posicoes) {
        Set<String> normalizadas = new HashSet<>();
        if (posicoes != null) {
            posicoes.stream().filter(Objects::nonNull).map(String::trim).map(String::toUpperCase).forEach(normalizadas::add);
        }
        return normalizadas;
    }

    private String chavePosicao(PosicaoEstivaNavioDTO posicao) {
        return (posicao.porao() + "-" + posicao.camada() + "-" + posicao.coluna() + "-" + posicao.bordo()).toUpperCase();
    }

    private BigDecimal valor(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private OcorrenciaEstruturalDTO ocorrencia(String categoria, String codigo, String mensagem, Long itemId) {
        return new OcorrenciaEstruturalDTO(categoria, codigo, mensagem, itemId, null, null, null);
    }

    private OcorrenciaEstruturalDTO ocorrenciaPosicao(
            String categoria,
            String codigo,
            String mensagem,
            PosicaoEstivaNavioDTO posicao
    ) {
        return new OcorrenciaEstruturalDTO(
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
