package br.com.cloudport.servicorail.ferrovia.servico;

import br.com.cloudport.servicorail.comum.sanitizacao.SanitizadorEntrada;
import br.com.cloudport.servicorail.ferrovia.dto.ReplanejamentoConteinerHistoricoDto;
import br.com.cloudport.servicorail.ferrovia.dto.ReplanejamentoConteinerRequisicaoDto;
import br.com.cloudport.servicorail.ferrovia.dto.VisitaTremRespostaDto;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.OrdemMovimentacao;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.StatusOrdemMovimentacao;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.TipoMovimentacaoOrdem;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.repositorio.OrdemMovimentacaoRepositorio;
import br.com.cloudport.servicorail.ferrovia.modelo.OperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.ReplanejamentoConteinerFerroviario;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusOperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.VagaoVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicorail.ferrovia.repositorio.ReplanejamentoConteinerFerroviarioRepositorio;
import br.com.cloudport.servicorail.ferrovia.repositorio.VisitaTremRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReplanejamentoConteinerFerroviarioServico {

    private static final int CAPACIDADE_PADRAO_VAGAO = 2;

    private final VisitaTremRepositorio visitaTremRepositorio;
    private final OrdemMovimentacaoRepositorio ordemMovimentacaoRepositorio;
    private final ReplanejamentoConteinerFerroviarioRepositorio replanejamentoRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public ReplanejamentoConteinerFerroviarioServico(VisitaTremRepositorio visitaTremRepositorio,
                                                      OrdemMovimentacaoRepositorio ordemMovimentacaoRepositorio,
                                                      ReplanejamentoConteinerFerroviarioRepositorio replanejamentoRepositorio,
                                                      SanitizadorEntrada sanitizadorEntrada) {
        this.visitaTremRepositorio = visitaTremRepositorio;
        this.ordemMovimentacaoRepositorio = ordemMovimentacaoRepositorio;
        this.replanejamentoRepositorio = replanejamentoRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional
    public VisitaTremRespostaDto replanejarConteinerEntreVagoes(Long idVisita,
                                                                 ReplanejamentoConteinerRequisicaoDto dto,
                                                                 String usuario) {
        validarIdentificadorVisita(idVisita);
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Os dados do replanejamento devem ser informados.");
        }

        VisitaTrem visita = visitaTremRepositorio.findOneById(idVisita)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Visita de trem não encontrada."));
        validarVersao(visita, dto.getVersaoComposicao());
        validarStatusVisita(visita);

        String codigoConteiner = normalizarIdentificador(dto.getCodigoConteiner(), "código do contêiner", 20);
        String vagaoOrigemInformado = normalizarIdentificador(dto.getVagaoOrigem(), "vagão de origem", 35);
        String vagaoDestinoInformado = normalizarIdentificador(dto.getVagaoDestino(), "vagão de destino", 35);
        String motivo = sanitizarTextoObrigatorio(dto.getMotivo(), "motivo", 500);
        String usuarioOperacao = sanitizarUsuario(usuario);
        TipoMovimentacaoOrdem tipoMovimentacao = Optional.ofNullable(dto.getTipoMovimentacao())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O tipo da movimentação deve ser informado."));

        if (vagaoOrigemInformado.equals(vagaoDestinoInformado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O vagão de destino deve ser diferente do vagão de origem.");
        }

        VagaoVisita vagaoOrigem = localizarVagao(visita, vagaoOrigemInformado, "origem");
        VagaoVisita vagaoDestino = localizarVagao(visita, vagaoDestinoInformado, "destino");
        validarCompatibilidadeDestino(vagaoDestino);

        List<OperacaoConteinerVisita> manifesto = obterManifesto(visita, tipoMovimentacao);
        int indiceOrigemManifesto = localizarIndiceOperacao(manifesto, codigoConteiner);
        OperacaoConteinerVisita operacao = manifesto.get(indiceOrigemManifesto);

        if (operacao.getStatusOperacao() != StatusOperacaoConteinerVisita.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente operações pendentes podem ser replanejadas.");
        }
        if (!vagaoOrigemInformado.equalsIgnoreCase(operacao.getIdentificadorVagao())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O vagão de origem informado não corresponde ao manifesto atual.");
        }

        validarCapacidadeDestino(visita, operacao, vagaoDestino);

        Long versaoAnterior = versaoAtual(visita);
        int ordemManifestoOrigem = indiceOrigemManifesto + 1;
        manifesto.remove(indiceOrigemManifesto);
        int indiceDestinoManifesto = calcularIndiceDestinoManifesto(
                manifesto,
                vagaoDestinoInformado,
                dto.getOrdemManifestoDestino());
        operacao.setIdentificadorVagao(vagaoDestinoInformado);
        manifesto.add(indiceDestinoManifesto, operacao);
        int ordemManifestoDestino = indiceDestinoManifesto + 1;

        sincronizarOrdemMovimentacao(visita,
                codigoConteiner,
                tipoMovimentacao,
                vagaoDestinoInformado,
                vagaoDestino.getPosicaoNoTrem());

        VisitaTrem visitaAtualizada = visitaTremRepositorio.saveAndFlush(visita);
        Long versaoNova = versaoAtual(visitaAtualizada);

        ReplanejamentoConteinerFerroviario auditoria = new ReplanejamentoConteinerFerroviario(
                visitaAtualizada.getId(),
                codigoConteiner,
                tipoMovimentacao,
                vagaoOrigemInformado,
                vagaoOrigem.getPosicaoNoTrem(),
                vagaoDestinoInformado,
                vagaoDestino.getPosicaoNoTrem(),
                ordemManifestoOrigem,
                ordemManifestoDestino,
                usuarioOperacao,
                motivo,
                versaoAnterior,
                versaoNova);
        replanejamentoRepositorio.save(auditoria);

        return VisitaTremRespostaDto.deEntidade(visitaAtualizada);
    }

    @Transactional(readOnly = true)
    public List<ReplanejamentoConteinerHistoricoDto> listarHistorico(Long idVisita) {
        validarIdentificadorVisita(idVisita);
        if (!visitaTremRepositorio.existsById(idVisita)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita de trem não encontrada.");
        }
        return replanejamentoRepositorio.findByVisitaTremIdOrderByCriadoEmDesc(idVisita)
                .stream()
                .map(ReplanejamentoConteinerHistoricoDto::deEntidade)
                .collect(Collectors.toList());
    }

    private void validarIdentificadorVisita(Long idVisita) {
        if (idVisita == null || idVisita <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador da visita inválido.");
        }
    }

    private void validarVersao(VisitaTrem visita, Long versaoEsperada) {
        if (versaoEsperada == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A versão da composição deve ser informada.");
        }
        if (!Objects.equals(versaoAtual(visita), versaoEsperada)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A composição foi alterada por outro usuário. Recarregue os dados antes de confirmar.");
        }
    }

    private void validarStatusVisita(VisitaTrem visita) {
        if (visita.getStatusVisita() == StatusVisitaTrem.CONCLUIDO
                || visita.getStatusVisita() == StatusVisitaTrem.PARTIU) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível replanejar uma visita concluída ou que já partiu.");
        }
    }

    private VagaoVisita localizarVagao(VisitaTrem visita, String identificador, String papel) {
        return Optional.ofNullable(visita.getListaVagoes())
                .orElseGet(ArrayList::new)
                .stream()
                .filter(vagao -> identificador.equalsIgnoreCase(vagao.getIdentificadorVagao()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format(Locale.ROOT,
                                "O vagão de %s não está cadastrado na composição.", papel)));
    }

    private void validarCompatibilidadeDestino(VagaoVisita vagaoDestino) {
        String tipo = Optional.ofNullable(vagaoDestino.getTipoVagao()).orElse("").trim();
        String tipoNormalizado = tipo.toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(tipo)
                || tipoNormalizado.contains("BLOQUE")
                || tipoNormalizado.contains("INOPERANTE")
                || tipoNormalizado.contains("DANIFICAD")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O vagão de destino não está apto para receber contêineres.");
        }
        if (capacidade(vagaoDestino) <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O vagão de destino não possui capacidade operacional configurada.");
        }
    }

    private void validarCapacidadeDestino(VisitaTrem visita,
                                          OperacaoConteinerVisita operacaoMovida,
                                          VagaoVisita vagaoDestino) {
        long quantidadeDestino = Stream.concat(visita.getListaDescarga().stream(), visita.getListaCarga().stream())
                .filter(operacao -> operacao != operacaoMovida)
                .filter(operacao -> vagaoDestino.getIdentificadorVagao()
                        .equalsIgnoreCase(operacao.getIdentificadorVagao()))
                .count();
        if (quantidadeDestino >= capacidade(vagaoDestino)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format(Locale.ROOT,
                            "O vagão %s atingiu a capacidade de %d contêiner(es).",
                            vagaoDestino.getIdentificadorVagao(),
                            capacidade(vagaoDestino)));
        }
    }

    private int capacidade(VagaoVisita vagao) {
        return Optional.ofNullable(vagao.getCapacidadeConteineres())
                .filter(valor -> valor > 0)
                .orElse(CAPACIDADE_PADRAO_VAGAO);
    }

    private List<OperacaoConteinerVisita> obterManifesto(VisitaTrem visita,
                                                          TipoMovimentacaoOrdem tipoMovimentacao) {
        if (tipoMovimentacao == TipoMovimentacaoOrdem.DESCARGA_TREM) {
            return visita.getListaDescarga();
        }
        if (tipoMovimentacao == TipoMovimentacaoOrdem.CARGA_TREM) {
            return visita.getListaCarga();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "O tipo da movimentação ferroviária é inválido.");
    }

    private int localizarIndiceOperacao(List<OperacaoConteinerVisita> manifesto, String codigoConteiner) {
        for (int indice = 0; indice < manifesto.size(); indice++) {
            OperacaoConteinerVisita operacao = manifesto.get(indice);
            if (codigoConteiner.equalsIgnoreCase(operacao.getCodigoConteiner())) {
                return indice;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "O contêiner informado não está no manifesto selecionado.");
    }

    private int calcularIndiceDestinoManifesto(List<OperacaoConteinerVisita> manifesto,
                                                String vagaoDestino,
                                                Integer ordemSolicitada) {
        if (ordemSolicitada != null) {
            if (ordemSolicitada < 1 || ordemSolicitada > manifesto.size() + 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A ordem de destino no manifesto é inválida.");
            }
            return ordemSolicitada - 1;
        }

        int ultimoIndiceDestino = -1;
        for (int indice = 0; indice < manifesto.size(); indice++) {
            if (vagaoDestino.equalsIgnoreCase(manifesto.get(indice).getIdentificadorVagao())) {
                ultimoIndiceDestino = indice;
            }
        }
        return ultimoIndiceDestino >= 0 ? ultimoIndiceDestino + 1 : manifesto.size();
    }

    private void sincronizarOrdemMovimentacao(VisitaTrem visita,
                                               String codigoConteiner,
                                               TipoMovimentacaoOrdem tipoMovimentacao,
                                               String vagaoDestino,
                                               Integer posicaoDestino) {
        Optional<OrdemMovimentacao> ordemEncontrada = ordemMovimentacaoRepositorio
                .findByVisitaTremIdAndCodigoConteinerIgnoreCaseAndTipoMovimentacao(
                        visita.getId(), codigoConteiner, tipoMovimentacao);

        if (ordemEncontrada.isEmpty()) {
            if (visita.getStatusVisita() != StatusVisitaTrem.PLANEJADO) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A ordem ferroviária vinculada ao manifesto não foi encontrada.");
            }
            return;
        }

        OrdemMovimentacao ordem = ordemEncontrada.get();
        if (ordem.getStatusMovimentacao() != StatusOrdemMovimentacao.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A ordem ferroviária já iniciou e não pode ser replanejada.");
        }
        ordem.setIdentificadorVagao(vagaoDestino);
        ordem.setPosicaoVagaoNoTrem(posicaoDestino);
        ordemMovimentacaoRepositorio.save(ordem);
    }

    private String normalizarIdentificador(String valor, String campo, int tamanhoMaximo) {
        return sanitizarTextoObrigatorio(valor, campo, tamanhoMaximo).toUpperCase(Locale.ROOT);
    }

    private String sanitizarTextoObrigatorio(String valor, String campo, int tamanhoMaximo) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        if (!StringUtils.hasText(limpo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O campo %s é obrigatório.", campo));
        }
        String normalizado = limpo.trim();
        if (normalizado.length() > tamanhoMaximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT,
                            "O campo %s deve ter no máximo %d caracteres.", campo, tamanhoMaximo));
        }
        return normalizado;
    }

    private String sanitizarUsuario(String usuario) {
        String limpo = sanitizadorEntrada.limparTexto(usuario);
        if (!StringUtils.hasText(limpo)) {
            return "sistema";
        }
        String normalizado = limpo.trim();
        return normalizado.length() <= 120 ? normalizado : normalizado.substring(0, 120);
    }

    private Long versaoAtual(VisitaTrem visita) {
        return Optional.ofNullable(visita.getVersao()).orElse(0L);
    }
}
