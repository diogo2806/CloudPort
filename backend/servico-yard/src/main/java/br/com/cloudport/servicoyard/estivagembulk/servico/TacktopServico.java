package br.com.cloudport.servicoyard.estivagembulk.servico;

import br.com.cloudport.servicoyard.estivagembulk.dto.MaterialLashingDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.TacktopDto;
import br.com.cloudport.servicoyard.estivagembulk.dto.ViolacaoEstivaDto;
import br.com.cloudport.servicoyard.estivagembulk.modelo.MaterialLashingBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PlanoEstivaBulk;
import br.com.cloudport.servicoyard.estivagembulk.modelo.PosicaoBobina;
import br.com.cloudport.servicoyard.estivagembulk.modelo.TipoLashing;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TacktopServico {

    public TacktopDto calcularTacktop(PlanoEstivaBulk plano) {
        return validarSecuring(plano);
    }

    public TacktopDto validarSecuring(PlanoEstivaBulk plano) {
        List<ViolacaoEstivaDto> violacoes = new ArrayList<>();
        List<MaterialLashingDto> materiaisDto = plano.getMateriais().stream()
                .map(this::toDto)
                .toList();

        Map<String, List<PosicaoBobina>> pilhas = plano.getPosicoes().stream()
                .filter(posicao -> posicao.getPosicaoX() != null && posicao.getPosicaoY() != null)
                .collect(Collectors.groupingBy(this::chavePilha));
        List<PosicaoBobina> camadaSuperior = new ArrayList<>();
        for (List<PosicaoBobina> pilha : pilhas.values()) {
            pilha.stream()
                    .max(Comparator.comparingInt(PosicaoBobina::getCamada))
                    .ifPresent(camadaSuperior::add);
        }

        double forcaRequeridaTotal = 0.0;
        double capacidadeDisponivelTotal = 0.0;
        double pesoTotal = 0.0;

        for (PosicaoBobina posicao : plano.getPosicoes()) {
            Long referencia = posicao.getId();
            List<MaterialLashingBulk> materiaisPosicao = plano.getMateriais().stream()
                    .filter(material -> pertence(material, posicao))
                    .toList();

            if (posicao.getTipoLashing() == null || posicao.getTipoLashing() == TipoLashing.SEM_LASHING) {
                violacoes.add(perigo("LASHING_NAO_DEFINIDO",
                        "O tipo de lashing deve ser informado e não pode ser SEM_LASHING", referencia));
            }
            if (posicao.getForcaRequeridaLashingKn() == null || posicao.getForcaRequeridaLashingKn() <= 0.0) {
                violacoes.add(perigo("FORCA_LASHING_NAO_CALCULADA",
                        "A força de projeto do lashing deve ser fornecida por cálculo aprovado", referencia));
            } else {
                forcaRequeridaTotal += posicao.getForcaRequeridaLashingKn();
            }
            if (materiaisPosicao.isEmpty()) {
                violacoes.add(perigo("MATERIAL_LASHING_AUSENTE",
                        "A posição não possui materiais e pontos de amarração registrados", referencia));
                continue;
            }

            Set<String> pontos = new HashSet<>();
            double capacidadePosicao = 0.0;
            for (MaterialLashingBulk material : materiaisPosicao) {
                validarMaterial(material, posicao, pontos, violacoes);
                if (material.getCargaTrabalhoSeguraKn() != null && material.getCargaTrabalhoSeguraKn() > 0.0
                        && material.getQuantidade() > 0) {
                    capacidadePosicao += material.getQuantidade() * material.getCargaTrabalhoSeguraKn();
                }
                if (material.getPesoUnitarioKg() != null && material.getPesoUnitarioKg() > 0.0
                        && material.getQuantidade() > 0) {
                    pesoTotal += material.getQuantidade() * material.getPesoUnitarioKg();
                }
            }
            capacidadeDisponivelTotal += capacidadePosicao;
            if (posicao.getForcaRequeridaLashingKn() != null
                    && capacidadePosicao < posicao.getForcaRequeridaLashingKn()) {
                violacoes.add(perigo("CAPACIDADE_LASHING_INSUFICIENTE",
                        String.format("Capacidade segura %.2f kN menor que a força requerida %.2f kN",
                                capacidadePosicao, posicao.getForcaRequeridaLashingKn()),
                        referencia));
            }
        }

        Double anguloMedio = camadaSuperior.stream()
                .map(PosicaoBobina::getAnguloInclinacao)
                .filter(angulo -> angulo != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .stream()
                .boxed()
                .findFirst()
                .orElse(null);

        TacktopDto dto = new TacktopDto();
        dto.setNumeroBobinasTopLayer(camadaSuperior.size());
        dto.setAnguloInclinacaoGraus(anguloMedio != null ? arredondar(anguloMedio) : null);
        dto.setMateriaisNecessarios(materiaisDto);
        dto.setPesoTotalLashingKg(arredondar(pesoTotal));
        dto.setForcaRequeridaTotalKn(arredondar(forcaRequeridaTotal));
        dto.setCapacidadeDisponivelTotalKn(arredondar(capacidadeDisponivelTotal));
        dto.setViolacoes(violacoes);
        dto.setAprovado(violacoes.stream().noneMatch(this::isPerigo));
        dto.setObservacoes("Resultado calculado somente com forças, materiais certificados e pontos informados; nenhuma quantidade foi estimada.");
        return dto;
    }

    private void validarMaterial(MaterialLashingBulk material, PosicaoBobina posicao,
            Set<String> pontos, List<ViolacaoEstivaDto> violacoes) {
        Long referencia = material.getId() != null ? material.getId() : posicao.getId();
        if (material.getTipo() == null || material.getTipo() == TipoLashing.SEM_LASHING) {
            violacoes.add(perigo("TIPO_MATERIAL_INVALIDO",
                    "O material de securing deve possuir tipo válido", referencia));
        }
        if (material.getQuantidade() <= 0) {
            violacoes.add(perigo("QUANTIDADE_MATERIAL_INVALIDA",
                    "A quantidade do material deve ser maior que zero", referencia));
        }
        if (vazio(material.getPontoAmarracao())) {
            violacoes.add(perigo("PONTO_AMARRACAO_AUSENTE",
                    "Cada material deve estar vinculado a um ponto de amarração identificado", referencia));
        } else if (!pontos.add(material.getPontoAmarracao())) {
            violacoes.add(perigo("PONTO_AMARRACAO_DUPLICADO",
                    "O mesmo ponto de amarração foi contabilizado mais de uma vez", referencia));
        }
        if (material.getCapacidadeNominalKn() == null || material.getCapacidadeNominalKn() <= 0.0
                || material.getCargaTrabalhoSeguraKn() == null || material.getCargaTrabalhoSeguraKn() <= 0.0) {
            violacoes.add(perigo("CAPACIDADE_MATERIAL_AUSENTE",
                    "Capacidade nominal e carga de trabalho segura são obrigatórias", referencia));
        } else if (material.getCargaTrabalhoSeguraKn() > material.getCapacidadeNominalKn()) {
            violacoes.add(perigo("CAPACIDADE_MATERIAL_INVALIDA",
                    "A carga de trabalho segura não pode exceder a capacidade nominal", referencia));
        }
        if (vazio(material.getCertificado())) {
            violacoes.add(perigo("CERTIFICADO_MATERIAL_AUSENTE",
                    "O certificado do material de lashing é obrigatório", referencia));
        }
        if (vazio(material.getReferenciaRegra()) || vazio(material.getVersaoEspecificacao())
                || vazio(material.getResponsavelValidacao())) {
            violacoes.add(perigo("RASTREABILIDADE_MATERIAL_AUSENTE",
                    "Regra, versão da especificação e responsável são obrigatórios", referencia));
        }
        if (!vazio(posicao.getVersaoEspecificacao())
                && !posicao.getVersaoEspecificacao().equals(material.getVersaoEspecificacao())) {
            violacoes.add(perigo("VERSAO_MATERIAL_DIVERGENTE",
                    "O material foi validado com versão diferente da posição", referencia));
        }
    }

    private boolean pertence(MaterialLashingBulk material, PosicaoBobina posicao) {
        if (material.getPosicao() == posicao) {
            return true;
        }
        return material.getPosicao() != null && material.getPosicao().getId() != null
                && material.getPosicao().getId().equals(posicao.getId());
    }

    private MaterialLashingDto toDto(MaterialLashingBulk material) {
        MaterialLashingDto dto = new MaterialLashingDto();
        dto.setId(material.getId());
        dto.setPosicaoId(material.getPosicao() != null ? material.getPosicao().getId() : null);
        dto.setTipo(material.getTipo() != null ? material.getTipo().name() : null);
        dto.setQuantidade(material.getQuantidade());
        dto.setComprimentoM(material.getComprimentoM());
        dto.setPesoUnitarioKg(material.getPesoUnitarioKg());
        dto.setPontoAmarracao(material.getPontoAmarracao());
        dto.setCapacidadeNominalKn(material.getCapacidadeNominalKn());
        dto.setCargaTrabalhoSeguraKn(material.getCargaTrabalhoSeguraKn());
        dto.setCertificado(material.getCertificado());
        dto.setReferenciaRegra(material.getReferenciaRegra());
        dto.setVersaoEspecificacao(material.getVersaoEspecificacao());
        dto.setResponsavelValidacao(material.getResponsavelValidacao());
        dto.setResultadoValidacao(material.getResultadoValidacao() != null
                ? material.getResultadoValidacao().name() : null);
        dto.setDescricao(material.getDescricao());
        return dto;
    }

    private String chavePilha(PosicaoBobina posicao) {
        Long poraoId = posicao.getPorao() != null ? posicao.getPorao().getId() : 0L;
        return poraoId + "_" + Math.round(posicao.getPosicaoX() * 10.0)
                + "_" + Math.round(posicao.getPosicaoY() * 10.0);
    }

    private ViolacaoEstivaDto perigo(String tipo, String descricao, Long referenciaId) {
        return new ViolacaoEstivaDto(tipo, descricao, referenciaId, "PERIGO");
    }

    private boolean isPerigo(ViolacaoEstivaDto violacao) {
        return "PERIGO".equals(violacao.getSeveridade());
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
