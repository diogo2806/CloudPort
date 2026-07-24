package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.comum.constantes.YardConstants;
import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.patio.dto.ViolacaoEstivagemPatioDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.SeveridadeAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoRegraEstivagemPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DetectorAvisoEstivagemPatioServico {

    private final ConteinerPatioRepositorio conteinerRepositorio;

    public DetectorAvisoEstivagemPatioServico(ConteinerPatioRepositorio conteinerRepositorio) {
        this.conteinerRepositorio = conteinerRepositorio;
    }

    public List<ViolacaoEstivagemPatioDto> detectar(ConteinerPatio unidade) {
        return detectar(unidade, conteinerRepositorio.findAllByOrderByPosicaoLinhaAscPosicaoColunaAsc());
    }

    public List<ViolacaoEstivagemPatioDto> detectar(ConteinerPatio unidade,
                                                     List<ConteinerPatio> inventario) {
        if (unidade == null || unidade.getPosicao() == null) {
            return List.of();
        }
        PosicaoPatio posicao = unidade.getPosicao();
        List<ConteinerPatio> fotografia = inventario == null ? List.of() : inventario;
        List<ConteinerPatio> pilha = fotografia.stream()
                .filter(item -> item.getPosicao() != null)
                .filter(item -> posicao.getLinha().equals(item.getPosicao().getLinha()))
                .filter(item -> posicao.getColuna().equals(item.getPosicao().getColuna()))
                .collect(Collectors.toList());

        List<ViolacaoEstivagemPatioDto> violacoes = new ArrayList<>();
        detectarPeso(unidade, posicao, violacoes);
        detectarAltura(posicao, violacoes);
        detectarTipoEReefer(unidade, posicao, violacoes);
        detectarPerigoso(unidade, posicao, fotografia, violacoes);
        detectarCapacidade(posicao, pilha, violacoes);
        detectarReserva(unidade, posicao, violacoes);
        detectarApoio(posicao, pilha, violacoes);
        detectarRegraPilha(unidade, posicao, pilha, violacoes);
        return List.copyOf(violacoes);
    }

    private void detectarPeso(ConteinerPatio unidade,
                               PosicaoPatio posicao,
                               List<ViolacaoEstivagemPatioDto> violacoes) {
        BigDecimal peso = unidade.getPesoToneladas();
        BigDecimal limite = posicao.getPesoMaximoToneladas();
        if (peso != null && limite != null && peso.compareTo(limite) > 0) {
            violacoes.add(violacao(
                    TipoRegraEstivagemPatio.PESO,
                    SeveridadeAvisoEstivagemPatio.CRITICA,
                    "Peso observado: " + peso + " t",
                    "Peso máximo da posição: " + limite + " t",
                    "Mover a unidade para uma posição compatível com o peso ou revisar a capacidade estrutural.",
                    true));
        }
    }

    private void detectarAltura(PosicaoPatio posicao,
                                 List<ViolacaoEstivagemPatioDto> violacoes) {
        Integer camada = camada(posicao.getCamadaOperacional());
        int limite = posicao.getCamadaMaxima() != null
                ? posicao.getCamadaMaxima()
                : YardConstants.EMPILHAMENTO_MAXIMO;
        if (camada != null && camada > limite) {
            violacoes.add(violacao(
                    TipoRegraEstivagemPatio.ALTURA,
                    SeveridadeAvisoEstivagemPatio.CRITICA,
                    "Unidade localizada na camada " + camada,
                    "Camada máxima permitida: " + limite,
                    "Reposicionar a unidade abaixo do limite de empilhamento configurado.",
                    true));
        }
    }

    private void detectarTipoEReefer(ConteinerPatio unidade,
                                      PosicaoPatio posicao,
                                      List<ViolacaoEstivagemPatioDto> violacoes) {
        TipoCargaConteiner tipo = unidade.getTipoCarga();
        Set<String> permitidos = tiposPermitidos(posicao.getTiposCargaPermitidos());
        if (tipo == null || permitidos.isEmpty() || permitidos.contains("*")
                || permitidos.contains("TODOS") || permitidos.contains(tipo.name())) {
            return;
        }
        if (tipo == TipoCargaConteiner.REFRIGERADO) {
            violacoes.add(violacao(
                    TipoRegraEstivagemPatio.REEFER,
                    SeveridadeAvisoEstivagemPatio.CRITICA,
                    "Contêiner reefer em posição sem compatibilidade configurada",
                    "Tipos permitidos: " + String.join(", ", permitidos),
                    "Mover para posição reefer com alimentação e monitoramento disponíveis.",
                    true));
            return;
        }
        violacoes.add(violacao(
                TipoRegraEstivagemPatio.TIPO,
                SeveridadeAvisoEstivagemPatio.CRITICA,
                "Tipo de carga observado: " + tipo.name(),
                "Tipos permitidos: " + String.join(", ", permitidos),
                "Mover a unidade para uma posição compatível com o tipo de carga.",
                true));
    }

    private void detectarPerigoso(ConteinerPatio unidade,
                                   PosicaoPatio posicao,
                                   List<ConteinerPatio> inventario,
                                   List<ViolacaoEstivagemPatioDto> violacoes) {
        if (unidade.getTipoCarga() != TipoCargaConteiner.PERIGOSO) {
            return;
        }
        boolean perigosoProximo = inventario.stream()
                .filter(item -> item != unidade)
                .filter(item -> item.getTipoCarga() == TipoCargaConteiner.PERIGOSO)
                .filter(item -> item.getPosicao() != null)
                .anyMatch(item -> Math.abs(item.getPosicao().getLinha() - posicao.getLinha()) <= 1
                        && Math.abs(item.getPosicao().getColuna() - posicao.getColuna()) <= 1);
        if (perigosoProximo) {
            violacoes.add(violacao(
                    TipoRegraEstivagemPatio.PERIGOSO,
                    SeveridadeAvisoEstivagemPatio.CRITICA,
                    "Existe outra carga perigosa em posição adjacente",
                    "Cargas perigosas devem respeitar o isolamento operacional configurado",
                    "Segregar imediatamente a unidade e revalidar a vizinhança.",
                    true));
        }
    }

    private void detectarCapacidade(PosicaoPatio posicao,
                                     List<ConteinerPatio> pilha,
                                     List<ViolacaoEstivagemPatioDto> violacoes) {
        Integer capacidade = posicao.getCapacidadePilha();
        if (capacidade != null && pilha.size() > capacidade) {
            violacoes.add(violacao(
                    TipoRegraEstivagemPatio.CAPACIDADE,
                    SeveridadeAvisoEstivagemPatio.CRITICA,
                    "Quantidade observada na pilha: " + pilha.size(),
                    "Capacidade configurada: " + capacidade,
                    "Remover o excedente da pilha antes de novos movimentos.",
                    true));
        }
    }

    private void detectarReserva(ConteinerPatio unidade,
                                  PosicaoPatio posicao,
                                  List<ViolacaoEstivagemPatioDto> violacoes) {
        if (!posicao.possuiReservaAtiva(LocalDateTime.now())) {
            return;
        }
        String reservadoPara = posicao.getReservaCodigoConteiner();
        if (!StringUtils.hasText(reservadoPara)
                || reservadoPara.equalsIgnoreCase(unidade.getCodigo())) {
            return;
        }
        violacoes.add(violacao(
                TipoRegraEstivagemPatio.RESERVA,
                SeveridadeAvisoEstivagemPatio.ALTA,
                "Posição ocupada por " + unidade.getCodigo(),
                "Posição reservada para " + reservadoPara,
                "Replanejar a ocupação ou liberar formalmente a reserva antes do movimento.",
                false));
    }

    private void detectarApoio(PosicaoPatio posicao,
                                List<ConteinerPatio> pilha,
                                List<ViolacaoEstivagemPatioDto> violacoes) {
        Integer camadaAtual = camada(posicao.getCamadaOperacional());
        if (camadaAtual == null || camadaAtual <= 1) {
            return;
        }
        boolean possuiApoio = pilha.stream()
                .map(ConteinerPatio::getPosicao)
                .map(PosicaoPatio::getCamadaOperacional)
                .map(this::camada)
                .anyMatch(valor -> valor != null && valor == camadaAtual - 1);
        if (!possuiApoio) {
            violacoes.add(violacao(
                    TipoRegraEstivagemPatio.APOIO,
                    SeveridadeAvisoEstivagemPatio.CRITICA,
                    "Unidade sem apoio físico imediatamente abaixo da camada " + camadaAtual,
                    "Deve existir unidade de apoio na camada " + (camadaAtual - 1),
                    "Bloquear a pilha e corrigir a sequência física de apoio.",
                    true));
        }
    }

    private void detectarRegraPilha(ConteinerPatio unidade,
                                     PosicaoPatio posicao,
                                     List<ConteinerPatio> pilha,
                                     List<ViolacaoEstivagemPatioDto> violacoes) {
        if (posicao.isBloqueada() || posicao.isInterditada() || !posicao.isAreaPermitida()) {
            violacoes.add(violacao(
                    TipoRegraEstivagemPatio.REGRA_PILHA,
                    SeveridadeAvisoEstivagemPatio.CRITICA,
                    "Unidade ocupa posição bloqueada, interditada ou fora da área permitida",
                    "A posição deve estar liberada e operacional",
                    "Interditar novos movimentos e retirar a unidade da posição.",
                    true));
            return;
        }
        if (unidade.getPesoToneladas() == null) {
            return;
        }
        Integer camadaAtual = camada(posicao.getCamadaOperacional());
        boolean pesoInvertido = pilha.stream()
                .filter(item -> item != unidade)
                .filter(item -> item.getPesoToneladas() != null)
                .filter(item -> camada(item.getPosicao().getCamadaOperacional()) != null)
                .filter(item -> camadaAtual != null
                        && camada(item.getPosicao().getCamadaOperacional()) < camadaAtual)
                .anyMatch(item -> item.getPesoToneladas().compareTo(unidade.getPesoToneladas()) < 0);
        if (pesoInvertido) {
            violacoes.add(violacao(
                    TipoRegraEstivagemPatio.REGRA_PILHA,
                    SeveridadeAvisoEstivagemPatio.CRITICA,
                    "Unidade mais pesada posicionada acima de unidade mais leve",
                    "A distribuição de peso deve ser decrescente da base para o topo",
                    "Reordenar a pilha preservando as unidades mais pesadas nas camadas inferiores.",
                    true));
        }
    }

    private ViolacaoEstivagemPatioDto violacao(TipoRegraEstivagemPatio regra,
                                                SeveridadeAvisoEstivagemPatio severidade,
                                                String observado,
                                                String esperado,
                                                String acao,
                                                boolean bloqueia) {
        return new ViolacaoEstivagemPatioDto(regra, severidade, observado, esperado, acao, bloqueia);
    }

    private Set<String> tiposPermitidos(String valor) {
        if (!StringUtils.hasText(valor)) {
            return Set.of();
        }
        return Arrays.stream(valor.split("[,;|\\s]+"))
                .filter(StringUtils::hasText)
                .map(item -> item.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Integer camada(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        String digitos = valor.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digitos)) {
            return null;
        }
        try {
            return Integer.valueOf(digitos);
        } catch (NumberFormatException excecao) {
            return null;
        }
    }
}
