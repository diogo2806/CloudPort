package br.com.cloudport.servicoyard.patio.otimizacao;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.OrdemTrabalhoPatioServico;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PredictiveReshuffflingServico {

    private static final int MAX_TENTATIVAS_DESTINO = 5;

    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final PosicaoPatioRepositorio posicaoRepositorio;
    private final OrdemTrabalhoPatioServico ordemServico;
    private final MapaOcupacaoServico mapaOcupacao;

    public PredictiveReshuffflingServico(ConteinerPatioRepositorio conteinerRepositorio,
                                           PosicaoPatioRepositorio posicaoRepositorio,
                                           OrdemTrabalhoPatioServico ordemServico,
                                           MapaOcupacaoServico mapaOcupacao) {
        this.conteinerRepositorio = conteinerRepositorio;
        this.posicaoRepositorio = posicaoRepositorio;
        this.ordemServico = ordemServico;
        this.mapaOcupacao = mapaOcupacao;
    }

    @Transactional(readOnly = true)
    public PlanoReshuffflingDto analisarNecessidadeReshuffling() {
        MapaOcupacaoServico.NivelOcupacaoEnum nivelOcupacao = mapaOcupacao.obterNivelOcupacao();

        if (nivelOcupacao != MapaOcupacaoServico.NivelOcupacaoEnum.BAIXA) {
            return new PlanoReshuffflingDto(
                    new ArrayList<>(),
                    false,
                    "Pátio não está em baixa ocupação. Reshuffling não recomendado."
            );
        }

        List<ConteinerPatio> conteineres = conteinerRepositorio.findAll();
        List<ConteinerParaReshuffflingDto> reshuffles = identificarCandidatos(conteineres);

        if (reshuffles.isEmpty()) {
            return new PlanoReshuffflingDto(
                    reshuffles,
                    false,
                    "Nenhum candidato para reshuffling identificado."
            );
        }

        return new PlanoReshuffflingDto(
                reshuffles,
                true,
                String.format("Identificados %d contêineres bloqueadores para pre-shuffling", reshuffles.size())
        );
    }

    @Transactional
    public void executarReshuffflingNoturno() {
        PlanoReshuffflingDto plano = analisarNecessidadeReshuffling();

        if (!plano.isRecomendado()) {
            return;
        }

        for (ConteinerParaReshuffflingDto candidato : plano.getConteinersParaReshuffling()) {
            executarReshuffflingConteiner(candidato);
        }
    }

    @Transactional(readOnly = true)
    public List<ConteinerParaReshuffflingDto> identificarCandidatos(List<ConteinerPatio> conteineres) {
        List<ConteinerParaReshuffflingDto> candidatos = new ArrayList<>();
        List<PosicaoPatio> posicoes = posicaoRepositorio.findAllByOrderByLinhaAscColunaAscCamadaOperacionalAsc();
        Set<String> bloqueadoresIncluidos = new HashSet<>();

        for (ConteinerPatio conteinerBase : conteineres) {
            if (!isPriorityPara(conteinerBase)) {
                continue;
            }

            ConteinerPatio bloqueador = verificarConteinerEmCima(conteinerBase, conteineres);
            if (bloqueador == null
                    || !temETAMaisLongo(conteinerBase, bloqueador)
                    || !bloqueadoresIncluidos.add(bloqueador.getCodigo().toUpperCase(Locale.ROOT))) {
                continue;
            }

            NovaPosicaoReshuffflingDto novaPosicao = calcularNovaPositicao(
                    bloqueador, conteineres, posicoes, Set.of());
            if (novaPosicao == null) {
                continue;
            }

            candidatos.add(new ConteinerParaReshuffflingDto(
                    bloqueador.getCodigo(),
                    bloqueador.getPosicao().getLinha(),
                    bloqueador.getPosicao().getColuna(),
                    bloqueador.getPosicao().getCamadaOperacional(),
                    "BLOQUEANDO_" + conteinerBase.getCodigo(),
                    novaPosicao,
                    criarChaveIdempotencia(bloqueador)
            ));
        }

        return candidatos.stream()
                .sorted(Comparator.comparing(ConteinerParaReshuffflingDto::getPrioridade).reversed())
                .collect(Collectors.toList());
    }

    @Transactional
    public void executarReshuffflingConteiner(ConteinerParaReshuffflingDto candidato) {
        ConteinerPatio conteiner = conteinerRepositorio.findByCodigoIgnoreCase(candidato.getCodigoConteiner())
                .orElse(null);

        if (conteiner == null || conteiner.getPosicao() == null) {
            return;
        }

        Set<Long> posicoesIgnoradas = new HashSet<>();
        for (int tentativa = 0; tentativa < MAX_TENTATIVAS_DESTINO; tentativa++) {
            List<ConteinerPatio> conteineres = conteinerRepositorio.findAll();
            List<PosicaoPatio> posicoes = posicaoRepositorio.findAllByOrderByLinhaAscColunaAscCamadaOperacionalAsc();
            NovaPosicaoReshuffflingDto destino = calcularNovaPositicao(
                    conteiner, conteineres, posicoes, posicoesIgnoradas);
            if (destino == null) {
                return;
            }

            OrdemTrabalhoPatioRequisicaoDto dto = montarOrdem(conteiner, destino, candidato.getChaveIdempotencia());
            try {
                ordemServico.registrarOuReutilizarRemanejamento(dto);
                return;
            } catch (ResponseStatusException ex) {
                if (ex.getStatus() != HttpStatus.CONFLICT && ex.getStatus() != HttpStatus.NOT_FOUND) {
                    throw ex;
                }
                posicoesIgnoradas.add(destino.getPosicaoId());
            }
        }
    }

    private OrdemTrabalhoPatioRequisicaoDto montarOrdem(ConteinerPatio conteiner,
                                                           NovaPosicaoReshuffflingDto destino,
                                                           String chaveIdempotencia) {
        OrdemTrabalhoPatioRequisicaoDto dto = new OrdemTrabalhoPatioRequisicaoDto();
        dto.setCodigoConteiner(conteiner.getCodigo());
        if (conteiner.getTipoCarga() != null) {
            dto.setTipoCarga(conteiner.getTipoCarga().name());
        } else if (conteiner.getCarga() != null) {
            dto.setTipoCarga(conteiner.getCarga().getCodigo());
        }
        dto.setDestino(conteiner.getDestino());
        dto.setLinhaDestino(destino.getNovaLinha());
        dto.setColunaDestino(destino.getNovaColuna());
        dto.setCamadaDestino(destino.getNovaCamada());
        dto.setTipoMovimento(TipoMovimentoPatio.REMANEJAMENTO);
        dto.setStatusConteinerDestino(StatusConteiner.ARMAZENADO);
        dto.setChaveIdempotencia(chaveIdempotencia);
        return dto;
    }

    private boolean isPriorityPara(ConteinerPatio conteiner) {
        if (conteiner.getPosicao() == null) {
            return false;
        }

        return conteiner.getPosicao().getLinha() < 50 && conteiner.getPosicao().getColuna() < 50;
    }

    private ConteinerPatio verificarConteinerEmCima(ConteinerPatio conteinerBase,
                                                       List<ConteinerPatio> conteineres) {
        Integer camadaBase = extrairNivelCamada(conteinerBase.getPosicao().getCamadaOperacional());
        if (camadaBase == null) {
            return null;
        }
        return conteineres.stream()
                .filter(conteiner -> conteiner.getPosicao() != null)
                .filter(conteiner -> !conteiner.getCodigo().equalsIgnoreCase(conteinerBase.getCodigo()))
                .filter(conteiner -> conteiner.getPosicao().getLinha().equals(conteinerBase.getPosicao().getLinha()))
                .filter(conteiner -> conteiner.getPosicao().getColuna().equals(conteinerBase.getPosicao().getColuna()))
                .filter(conteiner -> {
                    Integer camada = extrairNivelCamada(conteiner.getPosicao().getCamadaOperacional());
                    return camada != null && camada > camadaBase;
                })
                .min(Comparator.comparing(conteiner ->
                        extrairNivelCamada(conteiner.getPosicao().getCamadaOperacional())))
                .orElse(null);
    }

    private boolean temETAMaisLongo(ConteinerPatio conteinerAbaixo, ConteinerPatio conteinerEmCima) {
        if (conteinerAbaixo.getAtualizadoEm() == null || conteinerEmCima.getAtualizadoEm() == null) {
            return false;
        }
        LocalDateTime agora = LocalDateTime.now();
        long idadeConteinerAbaixo = ChronoUnit.HOURS.between(conteinerAbaixo.getAtualizadoEm(), agora);
        long idadeConteinerEmCima = Math.max(
                ChronoUnit.HOURS.between(conteinerEmCima.getAtualizadoEm(), agora), 1L);
        return idadeConteinerAbaixo > (idadeConteinerEmCima * 2);
    }

    private NovaPosicaoReshuffflingDto calcularNovaPositicao(ConteinerPatio conteiner,
                                                               List<ConteinerPatio> conteineres,
                                                               List<PosicaoPatio> posicoes,
                                                               Set<Long> posicoesIgnoradas) {
        if (conteiner.getPosicao() == null) {
            return null;
        }
        LocalDateTime agora = LocalDateTime.now();
        return posicoes.stream()
                .filter(posicao -> posicao.getId() != null && !posicoesIgnoradas.contains(posicao.getId()))
                .filter(posicao -> !posicao.getId().equals(conteiner.getPosicao().getId()))
                .filter(posicao -> !mesmaPilha(posicao, conteiner.getPosicao()))
                .filter(posicao -> posicaoElegivel(posicao, conteiner, conteineres, agora))
                .sorted(Comparator
                        .comparingInt((PosicaoPatio posicao) -> distancia(conteiner.getPosicao(), posicao))
                        .thenComparingInt(posicao -> nivelOuMaximo(posicao.getCamadaOperacional())))
                .map(posicao -> new NovaPosicaoReshuffflingDto(
                        posicao.getId(),
                        posicao.getLinha(),
                        posicao.getColuna(),
                        posicao.getCamadaOperacional()))
                .findFirst()
                .orElse(null);
    }

    private boolean posicaoElegivel(PosicaoPatio posicao,
                                      ConteinerPatio conteiner,
                                      List<ConteinerPatio> conteineres,
                                      LocalDateTime agora) {
        if (posicao.isBloqueada() || posicao.isInterditada() || !posicao.isAreaPermitida()) {
            return false;
        }
        if (posicao.possuiReservaAtiva(agora)) {
            return false;
        }
        if (conteineres.stream()
                .filter(atual -> atual.getPosicao() != null)
                .anyMatch(atual -> posicao.getId().equals(atual.getPosicao().getId()))) {
            return false;
        }
        Integer nivel = extrairNivelCamada(posicao.getCamadaOperacional());
        if (nivel == null || nivel <= 0) {
            return false;
        }
        if (posicao.getCamadaMaxima() != null && nivel > posicao.getCamadaMaxima()) {
            return false;
        }
        if (posicao.getCapacidadePilha() != null && nivel > posicao.getCapacidadePilha()) {
            return false;
        }
        if (conteiner.getPesoToneladas() != null
                && posicao.getPesoMaximoToneladas() != null
                && conteiner.getPesoToneladas().compareTo(posicao.getPesoMaximoToneladas()) > 0) {
            return false;
        }
        if (conteiner.getTipoCarga() != null && StringUtils.hasText(posicao.getTiposCargaPermitidos())) {
            String tipoCarga = conteiner.getTipoCarga().name();
            boolean permitido = List.of(posicao.getTiposCargaPermitidos().split("[,;|]"))
                    .stream()
                    .map(String::trim)
                    .anyMatch(tipo -> tipo.equalsIgnoreCase(tipoCarga));
            if (!permitido) {
                return false;
            }
        }
        return possuiApoioReal(posicao, conteineres, nivel);
    }

    private boolean possuiApoioReal(PosicaoPatio posicao,
                                     List<ConteinerPatio> conteineres,
                                     int nivel) {
        if (nivel == 1) {
            return true;
        }
        for (int nivelInferior = 1; nivelInferior < nivel; nivelInferior++) {
            int nivelEsperado = nivelInferior;
            boolean ocupado = conteineres.stream()
                    .filter(conteiner -> conteiner.getPosicao() != null)
                    .filter(conteiner -> conteiner.getPosicao().getLinha().equals(posicao.getLinha()))
                    .filter(conteiner -> conteiner.getPosicao().getColuna().equals(posicao.getColuna()))
                    .map(conteiner -> extrairNivelCamada(conteiner.getPosicao().getCamadaOperacional()))
                    .anyMatch(nivelAtual -> nivelAtual != null && nivelAtual == nivelEsperado);
            if (!ocupado) {
                return false;
            }
        }
        return true;
    }

    private boolean mesmaPilha(PosicaoPatio primeira, PosicaoPatio segunda) {
        return primeira.getLinha().equals(segunda.getLinha())
                && primeira.getColuna().equals(segunda.getColuna());
    }

    private int distancia(PosicaoPatio origem, PosicaoPatio destino) {
        return Math.abs(origem.getLinha() - destino.getLinha())
                + Math.abs(origem.getColuna() - destino.getColuna());
    }

    private String criarChaveIdempotencia(ConteinerPatio conteiner) {
        return "RESHUFFLING:"
                + conteiner.getCodigo().toUpperCase(Locale.ROOT)
                + ":POSICAO:"
                + conteiner.getPosicao().getId();
    }

    private int nivelOuMaximo(String camada) {
        Integer nivel = extrairNivelCamada(camada);
        return nivel != null ? nivel : Integer.MAX_VALUE;
    }

    private Integer extrairNivelCamada(String camada) {
        if (!StringUtils.hasText(camada)) {
            return null;
        }
        String digitos = camada.replaceAll("\\D+", "");
        if (!StringUtils.hasText(digitos)) {
            return null;
        }
        try {
            return Integer.valueOf(digitos);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static class PlanoReshuffflingDto {
        private List<ConteinerParaReshuffflingDto> conteinersParaReshuffling;
        private boolean recomendado;
        private String mensagem;

        public PlanoReshuffflingDto(List<ConteinerParaReshuffflingDto> conteinersParaReshuffling,
                                     boolean recomendado,
                                     String mensagem) {
            this.conteinersParaReshuffling = conteinersParaReshuffling;
            this.recomendado = recomendado;
            this.mensagem = mensagem;
        }

        public List<ConteinerParaReshuffflingDto> getConteinersParaReshuffling() { return conteinersParaReshuffling; }
        public boolean isRecomendado() { return recomendado; }
        public String getMensagem() { return mensagem; }
    }

    public static class ConteinerParaReshuffflingDto {
        private String codigoConteiner;
        private int linhaAtual;
        private int colunaAtual;
        private String camadaAtual;
        private String prioridade;
        private NovaPosicaoReshuffflingDto novaPosicao;
        private String chaveIdempotencia;

        public ConteinerParaReshuffflingDto(String codigoConteiner,
                                             int linhaAtual,
                                             int colunaAtual,
                                             String camadaAtual,
                                             String prioridade,
                                             NovaPosicaoReshuffflingDto novaPosicao,
                                             String chaveIdempotencia) {
            this.codigoConteiner = codigoConteiner;
            this.linhaAtual = linhaAtual;
            this.colunaAtual = colunaAtual;
            this.camadaAtual = camadaAtual;
            this.prioridade = prioridade;
            this.novaPosicao = novaPosicao;
            this.chaveIdempotencia = chaveIdempotencia;
        }

        public String getCodigoConteiner() { return codigoConteiner; }
        public int getLinhaAtual() { return linhaAtual; }
        public int getColunaAtual() { return colunaAtual; }
        public String getCamadaAtual() { return camadaAtual; }
        public String getPrioridade() { return prioridade; }
        public NovaPosicaoReshuffflingDto getNovaPosicao() { return novaPosicao; }
        public int getNovaLinha() { return novaPosicao.getNovaLinha(); }
        public int getNovaColuna() { return novaPosicao.getNovaColuna(); }
        public String getNovaCamada() { return novaPosicao.getNovaCamada(); }
        public String getChaveIdempotencia() { return chaveIdempotencia; }
    }

    public static class NovaPosicaoReshuffflingDto {
        private Long posicaoId;
        private int novaLinha;
        private int novaColuna;
        private String novaCamada;

        public NovaPosicaoReshuffflingDto(Long posicaoId,
                                           int novaLinha,
                                           int novaColuna,
                                           String novaCamada) {
            this.posicaoId = posicaoId;
            this.novaLinha = novaLinha;
            this.novaColuna = novaColuna;
            this.novaCamada = novaCamada;
        }

        public Long getPosicaoId() { return posicaoId; }
        public int getNovaLinha() { return novaLinha; }
        public int getNovaColuna() { return novaColuna; }
        public String getNovaCamada() { return novaCamada; }
    }
}
