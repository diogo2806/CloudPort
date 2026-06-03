package br.com.cloudport.servicoyard.patio.otimizacao;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.servico.OrdemTrabalhoPatioServico;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PredictiveReshuffflingServico {

    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final OrdemTrabalhoPatioServico ordemServico;
    private final MapaOcupacaoServico mapaOcupacao;

    public PredictiveReshuffflingServico(ConteinerPatioRepositorio conteinerRepositorio,
                                          OrdemTrabalhoPatioRepositorio ordemRepositorio,
                                          OrdemTrabalhoPatioServico ordemServico,
                                          MapaOcupacaoServico mapaOcupacao) {
        this.conteinerRepositorio = conteinerRepositorio;
        this.ordemRepositorio = ordemRepositorio;
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
                String.format("Identificados %d contêineres para pre-shuffling", reshuffles.size())
        );
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?")
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

        for (ConteinerPatio conteiner : conteineres) {
            if (!isPriorityPara(conteiner)) {
                continue;
            }

            OrdemTrabalhoPatio ordemEmCima = verificarConteinerEmCima(conteiner);
            if (ordemEmCima != null && temETAMaisLongo(conteiner, ordemEmCima)) {
                candidatos.add(new ConteinerParaReshuffflingDto(
                        conteiner.getCodigo(),
                        conteiner.getPosicao().getLinha(),
                        conteiner.getPosicao().getColuna(),
                        conteiner.getPosicao().getCamadaOperacional(),
                        "CONTEINER_EMBAIXO",
                        calcularNovaPositicao(conteiner)
                ));
            }
        }

        return candidatos.stream()
                .sorted(Comparator.comparing(ConteinerParaReshuffflingDto::getPrioridade).reversed())
                .collect(Collectors.toList());
    }

    @Transactional
    public void executarReshuffflingConteiner(ConteinerParaReshuffflingDto candidato) {
        ConteinerPatio conteiner = conteinerRepositorio.findByCodigoIgnoreCase(candidato.getCodigoConteiner())
                .orElse(null);

        if (conteiner == null) {
            return;
        }

        OrdemTrabalhoPatioRequisicaoDto dto = new OrdemTrabalhoPatioRequisicaoDto();
        dto.setCodigoConteiner(candidato.getCodigoConteiner());
        dto.setTipoCarga(conteiner.getCarga() != null ? conteiner.getCarga().getCodigo() : null);
        dto.setDestino(conteiner.getDestino());
        dto.setLinhaDestino(candidato.getNovaLinha());
        dto.setColunaDestino(candidato.getNovaColuna());
        dto.setCamadaDestino(candidato.getNovaCamada());
        dto.setTipoMovimento(TipoMovimentoPatio.REMANEJAMENTO);
        dto.setStatusConteinerDestino(StatusConteiner.ARMAZENADO);
        ordemServico.registrarOrdem(dto);
    }

    private boolean isPriorityPara(ConteinerPatio conteiner) {
        if (conteiner.getPosicao() == null) {
            return false;
        }

        return conteiner.getPosicao().getLinha() < 50 && conteiner.getPosicao().getColuna() < 50;
    }

    private OrdemTrabalhoPatio verificarConteinerEmCima(ConteinerPatio conteinerBase) {
        return ordemRepositorio.findAll().stream()
                .filter(ordem -> ordem.getConteiner() != null)
                .filter(ordem -> ordem.getConteiner().getPosicao().getLinha()
                        .equals(conteinerBase.getPosicao().getLinha()))
                .filter(ordem -> ordem.getConteiner().getPosicao().getColuna()
                        .equals(conteinerBase.getPosicao().getColuna()))
                .filter(ordem -> ordem.getStatusOrdem() == StatusOrdemTrabalhoPatio.PENDENTE)
                .findFirst()
                .orElse(null);
    }

    private boolean temETAMaisLongo(ConteinerPatio conteinerAbaixo, OrdemTrabalhoPatio ordemEmCima) {
        long idadeConteinerAbaixo = java.time.temporal.ChronoUnit.HOURS
                .between(conteinerAbaixo.getAtualizadoEm(), LocalDateTime.now());
        long idadeOrdemEmCima = java.time.temporal.ChronoUnit.HOURS
                .between(ordemEmCima.getCriadoEm(), LocalDateTime.now());

        return idadeConteinerAbaixo > (idadeOrdemEmCima * 2);
    }

    private NovaPosicaoReshuffflingDto calcularNovaPositicao(ConteinerPatio conteiner) {
        List<ConteinerPatio> vizinhos = conteinerRepositorio.findAll().stream()
                .filter(c -> c.getPosicao() != null)
                .filter(c -> Math.abs(c.getPosicao().getLinha() - conteiner.getPosicao().getLinha()) < 10)
                .filter(c -> Math.abs(c.getPosicao().getColuna() - conteiner.getPosicao().getColuna()) < 10)
                .collect(Collectors.toList());

        int novaLinha = conteiner.getPosicao().getLinha() + 5;
        int novaColuna = conteiner.getPosicao().getColuna() + 5;

        return new NovaPosicaoReshuffflingDto(novaLinha, novaColuna, "CAMADA_1");
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

        public ConteinerParaReshuffflingDto(String codigoConteiner, int linhaAtual, int colunaAtual,
                                           String camadaAtual, String prioridade,
                                           NovaPosicaoReshuffflingDto novaPosicao) {
            this.codigoConteiner = codigoConteiner;
            this.linhaAtual = linhaAtual;
            this.colunaAtual = colunaAtual;
            this.camadaAtual = camadaAtual;
            this.prioridade = prioridade;
            this.novaPosicao = novaPosicao;
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
    }

    public static class NovaPosicaoReshuffflingDto {
        private int novaLinha;
        private int novaColuna;
        private String novaCamada;

        public NovaPosicaoReshuffflingDto(int novaLinha, int novaColuna, String novaCamada) {
            this.novaLinha = novaLinha;
            this.novaColuna = novaColuna;
            this.novaCamada = novaCamada;
        }

        public int getNovaLinha() { return novaLinha; }
        public int getNovaColuna() { return novaColuna; }
        public String getNovaCamada() { return novaCamada; }
    }
}
