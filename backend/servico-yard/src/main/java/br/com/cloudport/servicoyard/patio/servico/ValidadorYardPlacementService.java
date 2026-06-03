package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.comum.constantes.YardConstants;
import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
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

    private final ConteinerPatioRepositorio conteinerPatioRepositorio;
    private final BercoPortuarioRepositorio bercoRepositorio;

    public ValidadorYardPlacementService(ConteinerPatioRepositorio conteinerPatioRepositorio,
                                         BercoPortuarioRepositorio bercoRepositorio) {
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

        if (camadaSolicitada > YardConstants.EMPILHAMENTO_MAXIMO) {
            throw new IllegalArgumentException(
                    String.format(
                            "Altura de empilhamento máxima é %d níveis (solicitado: nível %d)",
                            YardConstants.EMPILHAMENTO_MAXIMO, camadaSolicitada
                    )
            );
        }

        Optional<ConteinerPatio> conteinerOpt =
                conteinerPatioRepositorio.findByCodigoIgnoreCase(requisicaoDto.getCodigo());

        if (conteinerOpt.isEmpty()) {
            return;
        }

        ConteinerPatio conteiner = conteinerOpt.get();
        if (conteiner.getPesoToneladas() == null) {
            return;
        }

        if (conteiner.getPesoToneladas().compareTo(YardConstants.PESO_LIMITE_PILHA_DUPLA) > 0) {
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
                        c.getTipoCarga() == TipoCargaConteiner.PERIGOSO &&
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
        if (pesoToneladas.compareTo(YardConstants.PESO_LIMITE_PILHA_INTERMEDIARIA) >= 0) {
            return 1;
        } else if (pesoToneladas.compareTo(YardConstants.PESO_LIMITE_PILHA_DUPLA) >= 0) {
            return 2;
        } else {
            return YardConstants.EMPILHAMENTO_MAXIMO;
        }
    }
}
