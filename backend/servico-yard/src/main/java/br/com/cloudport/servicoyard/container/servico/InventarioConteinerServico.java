package br.com.cloudport.servicoyard.container.servico;

import br.com.cloudport.servicoyard.container.dto.ConteinerDetalheDTO;
import br.com.cloudport.servicoyard.container.dto.InventarioConteinerRespostaDTO;
import br.com.cloudport.servicoyard.container.dto.InventarioConteinerResumoDTO;
import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventarioConteinerServico {

    private final ConteinerPatioRepositorio conteinerPatioRepositorio;

    public InventarioConteinerServico(ConteinerPatioRepositorio conteinerPatioRepositorio) {
        this.conteinerPatioRepositorio = conteinerPatioRepositorio;
    }

    @Transactional(readOnly = true)
    public InventarioConteinerRespostaDTO consultar(String codigo,
                                                     StatusConteiner status,
                                                     TipoCargaConteiner tipoCarga) {
        String codigoNormalizado = normalizarCodigo(codigo);
        List<ConteinerPatio> conteineres = conteinerPatioRepositorio
                .findAllByOrderByCodigoAsc()
                .stream()
                .filter(conteiner -> filtrarCodigo(conteiner, codigoNormalizado))
                .filter(conteiner -> status == null || conteiner.getStatus() == status)
                .filter(conteiner -> tipoCarga == null || conteiner.getTipoCarga() == tipoCarga)
                .collect(Collectors.toList());

        List<ConteinerDetalheDTO> detalhes = conteineres.stream()
                .map(this::mapearDetalhe)
                .collect(Collectors.toList());

        return new InventarioConteinerRespostaDTO(criarResumo(conteineres), detalhes);
    }

    private InventarioConteinerResumoDTO criarResumo(List<ConteinerPatio> conteineres) {
        long totalOperacionais = conteineres.stream()
                .filter(this::estaEmOperacao)
                .count();
        long totalLiberados = conteineres.stream()
                .filter(conteiner -> conteiner.getStatus() == StatusConteiner.LIBERADO
                        || conteiner.getStatus() == StatusConteiner.DESPACHADO)
                .count();
        long totalRetidos = conteineres.stream()
                .filter(conteiner -> conteiner.getStatus() == StatusConteiner.RETIDO)
                .count();
        long totalDanificados = conteineres.stream()
                .filter(conteiner -> conteiner.getStatus() == StatusConteiner.DANIFICADO)
                .count();
        long totalRefrigerados = conteineres.stream()
                .filter(conteiner -> conteiner.getTipoCarga() == TipoCargaConteiner.REFRIGERADO)
                .count();
        long totalPerigosos = conteineres.stream()
                .filter(conteiner -> conteiner.getTipoCarga() == TipoCargaConteiner.PERIGOSO)
                .count();
        long totalSemPosicao = conteineres.stream()
                .filter(conteiner -> conteiner.getPosicao() == null)
                .count();
        BigDecimal pesoTotal = conteineres.stream()
                .map(ConteinerPatio::getPesoToneladas)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime atualizadoEm = conteineres.stream()
                .map(ConteinerPatio::getAtualizadoEm)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new InventarioConteinerResumoDTO(
                conteineres.size(),
                totalOperacionais,
                totalLiberados,
                totalRetidos,
                totalDanificados,
                totalRefrigerados,
                totalPerigosos,
                totalSemPosicao,
                pesoTotal,
                atualizadoEm);
    }

    private boolean estaEmOperacao(ConteinerPatio conteiner) {
        return conteiner.getStatus() != StatusConteiner.LIBERADO
                && conteiner.getStatus() != StatusConteiner.DESPACHADO;
    }

    private boolean filtrarCodigo(ConteinerPatio conteiner, String codigoNormalizado) {
        return codigoNormalizado == null
                || conteiner.getCodigo().toUpperCase(Locale.ROOT).contains(codigoNormalizado);
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private ConteinerDetalheDTO mapearDetalhe(ConteinerPatio conteiner) {
        return new ConteinerDetalheDTO(
                conteiner.getId(),
                conteiner.getCodigo(),
                formatarPosicao(conteiner.getPosicao()),
                conteiner.getTipoCarga(),
                conteiner.getPesoToneladas(),
                conteiner.getRestricoes(),
                conteiner.getStatus(),
                conteiner.getAtualizadoEm());
    }

    private String formatarPosicao(PosicaoPatio posicao) {
        if (posicao == null) {
            return null;
        }
        return posicao.getLinha() + "-" + posicao.getColuna() + "-" + posicao.getCamadaOperacional();
    }
}
