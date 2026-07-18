package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.ContainerOtimizacaoDto;
import br.com.cloudport.servicoyard.patio.dto.GrupoRecebimentoPatioDto;
import br.com.cloudport.servicoyard.patio.dto.PlanoRecebimentoPatioDto;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AgrupadorRecebimentoPatioServico {

    private static final int DURACAO_JANELA_HORAS = 4;
    private static final BigDecimal LIMITE_PESO_LEVE = new BigDecimal("10");
    private static final BigDecimal LIMITE_PESO_MEDIO = new BigDecimal("20");
    private static final BigDecimal LIMITE_PESO_PESADO = new BigDecimal("30");

    public PlanoRecebimentoPatioDto planejar(List<ContainerOtimizacaoDto> conteineres) {
        List<ContainerOtimizacaoDto> entrada = conteineres == null ? List.of() : conteineres;
        validarEntrada(entrada);

        Map<ChaveAgrupamento, List<ContainerOtimizacaoDto>> agrupados = new LinkedHashMap<>();
        for (ContainerOtimizacaoDto conteiner : entrada) {
            ChaveAgrupamento chave = criarChave(conteiner);
            agrupados.computeIfAbsent(chave, valor -> new ArrayList<>()).add(conteiner);
        }

        List<GrupoRecebimentoPatioDto> grupos = new ArrayList<>();
        for (Map.Entry<ChaveAgrupamento, List<ContainerOtimizacaoDto>> entry : agrupados.entrySet()) {
            grupos.add(criarGrupo(entry.getKey(), entry.getValue()));
        }

        grupos.sort(Comparator
                .comparing(GrupoRecebimentoPatioDto::getPrioridade)
                .thenComparing(GrupoRecebimentoPatioDto::getInicioJanelaRecebimento,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(GrupoRecebimentoPatioDto::getChaveAgrupamento));

        return criarPlano(entrada, grupos);
    }

    private void validarEntrada(List<ContainerOtimizacaoDto> conteineres) {
        Set<Long> ids = new HashSet<>();
        Set<String> codigos = new HashSet<>();

        for (ContainerOtimizacaoDto conteiner : conteineres) {
            if (conteiner == null) {
                throw new IllegalArgumentException("A lista de recebimento contém um contêiner nulo.");
            }
            if (conteiner.getId() == null) {
                throw new IllegalArgumentException("Todo contêiner deve possuir identificador.");
            }
            if (!ids.add(conteiner.getId())) {
                throw new IllegalArgumentException("Identificador de contêiner duplicado: " + conteiner.getId());
            }

            String codigo = normalizar(conteiner.getCodigo());
            if (codigo.isBlank()) {
                throw new IllegalArgumentException("Todo contêiner deve possuir código.");
            }
            if (!codigos.add(codigo)) {
                throw new IllegalArgumentException("Código de contêiner duplicado: " + conteiner.getCodigo());
            }
        }
    }

    private ChaveAgrupamento criarChave(ContainerOtimizacaoDto conteiner) {
        boolean refrigerado = ehRefrigerado(conteiner);
        boolean perigoso = ehPerigoso(conteiner);
        LocalDateTime inicioJanela = calcularInicioJanela(dataReferenciaRecebimento(conteiner));

        return new ChaveAgrupamento(
                textoOuPadrao(conteiner.getCategoria(), "NAO_INFORMADA"),
                textoOuPadrao(conteiner.getArmador(), "SEM_ARMADOR"),
                textoOuPadrao(conteiner.getVisitaSaida(), "SEM_VISITA"),
                textoOuPadrao(conteiner.getDestino(), "SEM_DESTINO"),
                comprimentoNormalizado(conteiner.getComprimentoPes()),
                resolverTipoEquipamento(conteiner, refrigerado),
                resolverEstadoCarga(conteiner),
                refrigerado,
                perigoso,
                perigoso ? textoOuPadrao(conteiner.getClasseImo(), "SEM_CLASSE_IMO") : "NAO_APLICAVEL",
                calcularFaixaPeso(conteiner.getPesoToneladas()),
                inicioJanela
        );
    }

    private GrupoRecebimentoPatioDto criarGrupo(ChaveAgrupamento chave,
                                                  List<ContainerOtimizacaoDto> conteineres) {
        List<ContainerOtimizacaoDto> ordenados = conteineres.stream()
                .sorted(Comparator
                        .comparing(this::dataReferenciaRecebimento,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ContainerOtimizacaoDto::getEtaPartida,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ContainerOtimizacaoDto::getCodigo,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        GrupoRecebimentoPatioDto grupo = new GrupoRecebimentoPatioDto();
        grupo.setChaveAgrupamento(chave.comoTexto());
        grupo.setNome(criarNomeGrupo(chave));
        grupo.setPrioridade(calcularPrioridade(chave));
        grupo.setCategoria(chave.categoria());
        grupo.setArmador(chave.armador());
        grupo.setVisitaSaida(chave.visitaSaida());
        grupo.setDestino(chave.destino());
        grupo.setComprimentoPes(chave.comprimentoPes() == 0 ? null : chave.comprimentoPes());
        grupo.setTipoEquipamento(chave.tipoEquipamento());
        grupo.setEstadoCarga(chave.estadoCarga());
        grupo.setRefrigerado(chave.refrigerado());
        grupo.setPerigoso(chave.perigoso());
        grupo.setClasseImo(chave.classeImo());
        grupo.setFaixaPeso(chave.faixaPeso());
        grupo.setInicioJanelaRecebimento(chave.inicioJanela());
        grupo.setFimJanelaRecebimento(chave.inicioJanela() == null
                ? null
                : chave.inicioJanela().plusHours(DURACAO_JANELA_HORAS));
        grupo.setQuantidadeConteineres(ordenados.size());
        grupo.setTeus(ordenados.stream().mapToInt(this::calcularTeus).sum());
        grupo.setPesoTotalToneladas(somarPesos(ordenados));
        grupo.setAlertas(criarAlertasGrupo(chave, ordenados));
        grupo.setConteineres(ordenados);
        return grupo;
    }

    private PlanoRecebimentoPatioDto criarPlano(List<ContainerOtimizacaoDto> conteineres,
                                                  List<GrupoRecebimentoPatioDto> grupos) {
        PlanoRecebimentoPatioDto plano = new PlanoRecebimentoPatioDto();
        plano.setTotalConteineres(conteineres.size());
        plano.setTotalGrupos(grupos.size());
        plano.setTotalTeus(conteineres.stream().mapToInt(this::calcularTeus).sum());
        plano.setPesoTotalToneladas(somarPesos(conteineres));
        plano.setPrimeiraChegada(conteineres.stream()
                .map(this::dataReferenciaRecebimento)
                .filter(data -> data != null)
                .min(LocalDateTime::compareTo)
                .orElse(null));
        plano.setUltimaChegada(conteineres.stream()
                .map(this::dataReferenciaRecebimento)
                .filter(data -> data != null)
                .max(LocalDateTime::compareTo)
                .orElse(null));
        plano.setAlertas(criarAlertasPlano(conteineres));
        plano.setGrupos(grupos);
        return plano;
    }

    private List<String> criarAlertasGrupo(ChaveAgrupamento chave,
                                            List<ContainerOtimizacaoDto> conteineres) {
        List<String> alertas = new ArrayList<>();
        if (chave.comprimentoPes() == 0) {
            alertas.add("Comprimento não informado; a necessidade de TEU foi estimada como 1 por contêiner.");
        }
        if (chave.inicioJanela() == null) {
            alertas.add("Sem ETA de chegada; o grupo não possui janela operacional de recebimento.");
        }
        if (chave.perigoso() && "SEM_CLASSE_IMO".equals(chave.classeImo())) {
            alertas.add("Carga perigosa sem classe IMO; não liberar posição definitiva antes da classificação.");
        }
        if (categoriaRequerVisita(chave.categoria()) && "SEM_VISITA".equals(chave.visitaSaida())) {
            alertas.add("Categoria de exportação ou transbordo sem visita de saída informada.");
        }
        if (conteineres.stream().anyMatch(conteiner -> conteiner.getPesoToneladas() == null)) {
            alertas.add("Há contêiner sem peso; validar limite da pilha antes do recebimento.");
        }
        return alertas;
    }

    private List<String> criarAlertasPlano(List<ContainerOtimizacaoDto> conteineres) {
        int semJanela = 0;
        int semComprimento = 0;
        int perigososSemClasse = 0;
        int semVisitaSaida = 0;

        for (ContainerOtimizacaoDto conteiner : conteineres) {
            if (dataReferenciaRecebimento(conteiner) == null) {
                semJanela++;
            }
            if (comprimentoNormalizado(conteiner.getComprimentoPes()) == 0) {
                semComprimento++;
            }
            if (ehPerigoso(conteiner) && normalizar(conteiner.getClasseImo()).isBlank()) {
                perigososSemClasse++;
            }
            String categoria = textoOuPadrao(conteiner.getCategoria(), "NAO_INFORMADA");
            if (categoriaRequerVisita(categoria) && normalizar(conteiner.getVisitaSaida()).isBlank()) {
                semVisitaSaida++;
            }
        }

        List<String> alertas = new ArrayList<>();
        adicionarAlertaContagem(alertas, semJanela, "contêiner(es) sem ETA de chegada");
        adicionarAlertaContagem(alertas, semComprimento, "contêiner(es) sem comprimento ISO válido");
        adicionarAlertaContagem(alertas, perigososSemClasse, "contêiner(es) perigoso(s) sem classe IMO");
        adicionarAlertaContagem(alertas, semVisitaSaida, "contêiner(es) de exportação/transbordo sem visita de saída");
        return alertas;
    }

    private void adicionarAlertaContagem(List<String> alertas, int quantidade, String descricao) {
        if (quantidade > 0) {
            alertas.add(quantidade + " " + descricao + ".");
        }
    }

    private String criarNomeGrupo(ChaveAgrupamento chave) {
        StringBuilder nome = new StringBuilder();
        nome.append(chave.categoria())
                .append(" · ")
                .append(chave.armador())
                .append(" · ")
                .append(chave.visitaSaida())
                .append(" · ")
                .append(chave.destino());

        if (chave.comprimentoPes() > 0) {
            nome.append(" · ").append(chave.comprimentoPes()).append("'");
        }
        nome.append(" · ").append(chave.tipoEquipamento());
        if (chave.perigoso()) {
            nome.append(" · IMO ").append(chave.classeImo());
        }
        return nome.toString();
    }

    private int calcularPrioridade(ChaveAgrupamento chave) {
        int prioridade;
        if (chave.perigoso()) {
            prioridade = 10;
        } else if (chave.refrigerado()) {
            prioridade = 20;
        } else if ("VAZIO".equals(chave.estadoCarga())) {
            prioridade = 50;
        } else {
            prioridade = 40;
        }
        return chave.inicioJanela() == null ? prioridade + 5 : prioridade;
    }

    private LocalDateTime dataReferenciaRecebimento(ContainerOtimizacaoDto conteiner) {
        return conteiner.getEtaChegada() != null ? conteiner.getEtaChegada() : conteiner.getEtaPartida();
    }

    private LocalDateTime calcularInicioJanela(LocalDateTime referencia) {
        if (referencia == null) {
            return null;
        }
        int horaInicial = (referencia.getHour() / DURACAO_JANELA_HORAS) * DURACAO_JANELA_HORAS;
        return referencia.withHour(horaInicial).withMinute(0).withSecond(0).withNano(0);
    }

    private boolean ehRefrigerado(ContainerOtimizacaoDto conteiner) {
        if (Boolean.TRUE.equals(conteiner.getRefrigerado())) {
            return true;
        }
        String tipo = normalizar(conteiner.getTipoEquipamento() + " " + conteiner.getTipoCarga());
        return tipo.contains("REEFER") || tipo.contains("REFRIGER");
    }

    private boolean ehPerigoso(ContainerOtimizacaoDto conteiner) {
        if (Boolean.TRUE.equals(conteiner.getPerigoso())) {
            return true;
        }
        if (!normalizar(conteiner.getClasseImo()).isBlank() || !normalizar(conteiner.getNumeroOnu()).isBlank()) {
            return true;
        }
        String restricoes = normalizar(conteiner.getRestricoes());
        return restricoes.contains("IMO") || restricoes.contains("HAZ") || restricoes.contains("PERIGOS");
    }

    private String resolverTipoEquipamento(ContainerOtimizacaoDto conteiner, boolean refrigerado) {
        if (!normalizar(conteiner.getTipoEquipamento()).isBlank()) {
            return normalizar(conteiner.getTipoEquipamento());
        }
        if (refrigerado) {
            return "REEFER";
        }
        if (!normalizar(conteiner.getTipoCarga()).isBlank()) {
            return normalizar(conteiner.getTipoCarga());
        }
        return "DRY";
    }

    private String resolverEstadoCarga(ContainerOtimizacaoDto conteiner) {
        if (!normalizar(conteiner.getEstadoCarga()).isBlank()) {
            return normalizar(conteiner.getEstadoCarga());
        }
        String tipoCarga = normalizar(conteiner.getTipoCarga());
        return tipoCarga.contains("VAZIO") || tipoCarga.contains("EMPTY") ? "VAZIO" : "CHEIO";
    }

    private String calcularFaixaPeso(BigDecimal pesoToneladas) {
        if (pesoToneladas == null) {
            return "PESO_NAO_INFORMADO";
        }
        if (pesoToneladas.compareTo(LIMITE_PESO_LEVE) < 0) {
            return "ATE_10T";
        }
        if (pesoToneladas.compareTo(LIMITE_PESO_MEDIO) < 0) {
            return "10_A_20T";
        }
        if (pesoToneladas.compareTo(LIMITE_PESO_PESADO) < 0) {
            return "20_A_30T";
        }
        return "ACIMA_30T";
    }

    private BigDecimal somarPesos(List<ContainerOtimizacaoDto> conteineres) {
        return conteineres.stream()
                .map(ContainerOtimizacaoDto::getPesoToneladas)
                .filter(peso -> peso != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int calcularTeus(ContainerOtimizacaoDto conteiner) {
        Integer comprimento = conteiner.getComprimentoPes();
        if (comprimento == null) {
            return 1;
        }
        return comprimento >= 40 ? 2 : 1;
    }

    private int comprimentoNormalizado(Integer comprimentoPes) {
        if (comprimentoPes == null) {
            return 0;
        }
        return comprimentoPes == 20 || comprimentoPes == 40 || comprimentoPes == 45 ? comprimentoPes : 0;
    }

    private boolean categoriaRequerVisita(String categoria) {
        return categoria.contains("EXPORT") || categoria.contains("TRANSBORD") || categoria.contains("TRANSSHIP");
    }

    private String textoOuPadrao(String valor, String padrao) {
        String normalizado = normalizar(valor);
        return normalizado.isBlank() ? padrao : normalizado;
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String normalizado = semAcentos.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");
        return removerSublinhadosDasBordas(normalizado);
    }

    private String removerSublinhadosDasBordas(String valor) {
        int inicio = 0;
        int fim = valor.length();

        while (inicio < fim && valor.charAt(inicio) == '_') {
            inicio++;
        }
        while (fim > inicio && valor.charAt(fim - 1) == '_') {
            fim--;
        }
        return valor.substring(inicio, fim);
    }

    private record ChaveAgrupamento(String categoria,
                                     String armador,
                                     String visitaSaida,
                                     String destino,
                                     int comprimentoPes,
                                     String tipoEquipamento,
                                     String estadoCarga,
                                     boolean refrigerado,
                                     boolean perigoso,
                                     String classeImo,
                                     String faixaPeso,
                                     LocalDateTime inicioJanela) {

        private String comoTexto() {
            return String.join("|",
                    categoria,
                    armador,
                    visitaSaida,
                    destino,
                    String.valueOf(comprimentoPes),
                    tipoEquipamento,
                    estadoCarga,
                    refrigerado ? "REEFER" : "NAO_REEFER",
                    perigoso ? "PERIGOSO" : "NAO_PERIGOSO",
                    classeImo,
                    faixaPeso,
                    inicioJanela == null ? "SEM_JANELA" : inicioJanela.toString());
        }
    }
}
