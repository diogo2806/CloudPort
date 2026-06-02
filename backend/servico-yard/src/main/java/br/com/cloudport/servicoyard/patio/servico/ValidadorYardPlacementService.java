package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.container.repositorio.ConteinerRepositorio;
import br.com.cloudport.servicoyard.patio.dto.ConteinerPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.recursos.entidade.BercoPortuario;
import br.com.cloudport.servicoyard.recursos.repositorio.BercoPortuarioRepositorio;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ValidadorYardPlacementService {

    private final ConteinerRepositorio conteinerRepositorio;
    private final ConteinerPatioRepositorio conteinerPatioRepositorio;
    private final BercoPortuarioRepositorio bercoRepositorio;

    private static final Integer ALTURA_MAXIMA_EMPILHAMENTO_PADRAO = 4;
    private static final BigDecimal PESO_LIMITE_EMPILHAMENTO = new BigDecimal("20"); // toneladas

    public ValidadorYardPlacementService(ConteinerRepositorio conteinerRepositorio,
                                         ConteinerPatioRepositorio conteinerPatioRepositorio,
                                         BercoPortuarioRepositorio bercoRepositorio) {
        this.conteinerRepositorio = conteinerRepositorio;
        this.conteinerPatioRepositorio = conteinerPatioRepositorio;
        this.bercoRepositorio = bercoRepositorio;
    }

    public void validarAlocacao(ConteinerPatioRequisicaoDto requisicaoDto) {
        validarCompatibilidadeCarga(requisicaoDto);
        validarAlturaPilha(requisicaoDto);
        validarIsolamentoCargaPerigosa(requisicaoDto);
    }

    private void validarCompatibilidadeCarga(ConteinerPatioRequisicaoDto requisicaoDto) {
        String tipoCargaStr = requisicaoDto.getTipoCarga();
        if (tipoCargaStr == null) {
            return;
        }

        TipoCargaConteiner tipoCarga = converterTipoCarga(tipoCargaStr);
        if (tipoCarga == null) {
            return;
        }

        String destino = requisicaoDto.getDestino();
        Optional<BercoPortuario> berco = bercoRepositorio.findByCodigoIgnoreCase(destino);

        if (berco.isEmpty()) {
            return;
        }

        BercoPortuario bercoPortuario = berco.get();
        switch (tipoCarga) {
            case REFRIGERADO:
                if (!bercoPortuario.isCompatReefer()) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Berço '%s' não suporta contêineres refrigerados (REEFER)",
                                    destino
                            )
                    );
                }
                if (!bercoPortuario.isEnergiaGenerica()) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Berço '%s' não dispõe de infraestrutura de energia para contêineres REEFER",
                                    destino
                            )
                    );
                }
                break;
            case PERIGOSO:
                if (!bercoPortuario.isCompatPerigosa()) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Berço '%s' não autoriza cargas perigosas (IMO)",
                                    destino
                            )
                    );
                }
                break;
            case SECO:
                if (!bercoPortuario.isCompatContainer()) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Berço '%s' não suporta contêineres secos",
                                    destino
                            )
                    );
                }
                break;
            case GRANELEIRO:
                if (!bercoPortuario.isCompatGranel()) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Berço '%s' não suporta cargas a granel",
                                    destino
                            )
                    );
                }
                break;
            default:
                if (!bercoPortuario.isCompatContainer()) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Berço '%s' não compatível com tipo de carga '%s'",
                                    destino, tipoCarga
                            )
                    );
                }
        }
    }

    private void validarAlturaPilha(ConteinerPatioRequisicaoDto requisicaoDto) {
        Integer camadaSolicitada = extrairNivelDaCamada(requisicaoDto.getCamadaOperacional());
        if (camadaSolicitada == null || camadaSolicitada <= 0) {
            return;
        }

        if (camadaSolicitada > ALTURA_MAXIMA_EMPILHAMENTO_PADRAO) {
            throw new IllegalArgumentException(
                    String.format(
                            "Altura de empilhamento máxima é %d níveis (solicitado: nível %d)",
                            ALTURA_MAXIMA_EMPILHAMENTO_PADRAO, camadaSolicitada
                    )
            );
        }

        Optional<br.com.cloudport.servicoyard.container.entidade.Conteiner> conteinerOpt =
                conteinerRepositorio.findByIdentificacaoIgnoreCase(requisicaoDto.getCodigo());

        if (conteinerOpt.isEmpty()) {
            return;
        }

        br.com.cloudport.servicoyard.container.entidade.Conteiner conteiner = conteinerOpt.get();
        if (conteiner.getPesoToneladas() == null) {
            return;
        }

        if (conteiner.getPesoToneladas().compareTo(PESO_LIMITE_EMPILHAMENTO) > 0) {
            Integer alturaMaximaParaPeso = calcularAlturaMaximaParaPeso(conteiner.getPesoToneladas());
            if (camadaSolicitada > alturaMaximaParaPeso) {
                throw new IllegalArgumentException(
                        String.format(
                                "Contêiner com peso %s toneladas pode ser empilhado apenas até nível %d (solicitado: nível %d)",
                                conteiner.getPesoToneladas(), alturaMaximaParaPeso, camadaSolicitada
                        )
                );
            }
        }
    }

    private void validarIsolamentoCargaPerigosa(ConteinerPatioRequisicaoDto requisicaoDto) {
        String tipoCargaStr = requisicaoDto.getTipoCarga();
        if (tipoCargaStr == null) {
            return;
        }

        TipoCargaConteiner tipoCarga = converterTipoCarga(tipoCargaStr);
        if (tipoCarga != TipoCargaConteiner.PERIGOSO) {
            return;
        }

        Integer linha = requisicaoDto.getLinha();
        Integer coluna = requisicaoDto.getColuna();

        boolean existeContainerProximo = conteinerPatioRepositorio.findAll().stream()
                .anyMatch(c -> c.getPosicao() != null &&
                        c.getCarga() != null &&
                        "PERIGOSO".equals(c.getCarga().getCodigo()) &&
                        Math.abs(c.getPosicao().getLinha() - linha) <= 1 &&
                        Math.abs(c.getPosicao().getColuna() - coluna) <= 1 &&
                        !(c.getPosicao().getLinha().equals(linha) &&
                                c.getPosicao().getColuna().equals(coluna))
                );

        if (existeContainerProximo) {
            throw new IllegalArgumentException(
                    String.format(
                            "Cargas perigosas (IMO) devem estar isoladas. Há outra carga perigosa próxima à posição (%d,%d). " +
                                    "Distância mínima: 1 bloco",
                            linha, coluna
                    )
            );
        }
    }

    private TipoCargaConteiner converterTipoCarga(String tipoCargaStr) {
        if (tipoCargaStr == null) {
            return null;
        }
        try {
            return TipoCargaConteiner.valueOf(tipoCargaStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Integer extrairNivelDaCamada(String camadaOperacional) {
        if (camadaOperacional == null || camadaOperacional.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(camadaOperacional);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer calcularAlturaMaximaParaPeso(BigDecimal pesoToneladas) {
        if (pesoToneladas.compareTo(new BigDecimal("25")) >= 0) {
            return 1;
        } else if (pesoToneladas.compareTo(new BigDecimal("20")) >= 0) {
            return 2;
        } else {
            return ALTURA_MAXIMA_EMPILHAMENTO_PADRAO;
        }
    }
}
