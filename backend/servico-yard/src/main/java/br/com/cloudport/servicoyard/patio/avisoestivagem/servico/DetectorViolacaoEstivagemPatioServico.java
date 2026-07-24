package br.com.cloudport.servicoyard.patio.avisoestivagem.servico;

import br.com.cloudport.servicoyard.comum.constantes.YardConstants;
import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.SeveridadeAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.TipoRegraEstivagemPatio;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DetectorViolacaoEstivagemPatioServico {

    private static final Pattern ALTURA_RESTRICAO = Pattern.compile(
            "(?:ALTURA|HEIGHT)\\s*[:=]\\s*([0-9]+(?:[.,][0-9]+)?)",
            Pattern.CASE_INSENSITIVE);

    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final EquipamentoPatioRepositorio equipamentoRepositorio;

    public DetectorViolacaoEstivagemPatioServico(
            ConteinerPatioRepositorio conteinerRepositorio,
            EquipamentoPatioRepositorio equipamentoRepositorio) {
        this.conteinerRepositorio = conteinerRepositorio;
        this.equipamentoRepositorio = equipamentoRepositorio;
    }

    public Map<String, ViolacaoDetectada> detectar() {
        List<ConteinerPatio> inventario = conteinerRepositorio.findAllByOrderByCodigoAsc().stream()
                .filter(this::ocupaPosicaoAtiva)
                .toList();
        List<EquipamentoPatio> equipamentos = equipamentoRepositorio.findAll();
        Map<ChavePilha, List<ConteinerPatio>> porPilha = agruparPorPilha(inventario);
        Map<String, ViolacaoDetectada> resultado = new LinkedHashMap<>();

        for (ConteinerPatio conteiner : inventario) {
            PosicaoPatio posicao = conteiner.getPosicao();
            List<ConteinerPatio> pilha = porPilha.getOrDefault(
                    new ChavePilha(posicao.getLinha(), posicao.getColuna()), List.of());
            detectarPeso(conteiner, posicao, resultado);
            detectarAltura(conteiner, posicao, resultado);
            detectarTipoCarga(conteiner, posicao, resultado);
            detectarReefer(conteiner, posicao, equipamentos, resultado);
            detectarPerigoso(conteiner, posicao, inventario, resultado);
            detectarCapacidade(conteiner, posicao, pilha, resultado);
            detectarReserva(conteiner, posicao, resultado);
            detectarApoio(conteiner, posicao, pilha, resultado);
            detectarRegraPilha(conteiner, posicao, pilha, resultado);
        }
        return resultado;
    }

    private Map<ChavePilha, List<ConteinerPatio>> agruparPorPilha(List<ConteinerPatio> inventario) {
        Map<ChavePilha, List<ConteinerPatio>> porPilha = new HashMap<>();
        for (ConteinerPatio conteiner : inventario) {
            PosicaoPatio posicao = conteiner.getPosicao();
            ChavePilha chave = new ChavePilha(posicao.getLinha(), posicao.getColuna());
            porPilha.computeIfAbsent(chave, ignorada -> new ArrayList<>()).add(conteiner);
        }
        return porPilha;
    }

    private void detectarPeso(
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            Map<String, ViolacaoDetectada> resultado) {
        if (conteiner.getPesoToneladas() == null || posicao.getPesoMaximoToneladas() == null
                || conteiner.getPesoToneladas().compareTo(posicao.getPesoMaximoToneladas()) <= 0) {
            return;
        }
        adicionar(resultado, conteiner, posicao, TipoRegraEstivagemPatio.PESO,
                SeveridadeAvisoEstivagemPatio.CRITICA,
                "Peso da unidade excede o limite estrutural configurado para a posição.",
                conteiner.getPesoToneladas() + " t",
                "Até " + posicao.getPesoMaximoToneladas() + " t",
                "Realocar a unidade para posição compatível com o peso ou revisar a capacidade estrutural autorizada.");
    }

    private void detectarAltura(
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            Map<String, ViolacaoDetectada> resultado) {
        int camada = nivel(posicao.getCamadaOperacional());
        int limiteCamada = YardConstants.EMPILHAMENTO_MAXIMO;
        if (posicao.getCamadaMaxima() != null) {
            limiteCamada = Math.min(limiteCamada, posicao.getCamadaMaxima());
        }
        if (conteiner.getPesoToneladas() != null) {
            if (conteiner.getPesoToneladas().compareTo(YardConstants.PESO_LIMITE_PILHA_INTERMEDIARIA) >= 0) {
                limiteCamada = Math.min(limiteCamada, 1);
            } else if (conteiner.getPesoToneladas().compareTo(YardConstants.PESO_LIMITE_PILHA_DUPLA) >= 0) {
                limiteCamada = Math.min(limiteCamada, 2);
            }
        }
        BigDecimal altura = extrairAltura(conteiner.getRestricoes());
        boolean camadaInvalida = camada <= 0 || camada > limiteCamada;
        boolean alturaInvalida = altura != null && posicao.getAlturaMaximaMetros() != null
                && altura.compareTo(posicao.getAlturaMaximaMetros()) > 0;
        if (!camadaInvalida && !alturaInvalida) {
            return;
        }
        String observado = "Camada " + posicao.getCamadaOperacional();
        String esperado = "Até camada " + limiteCamada;
        if (altura != null) {
            observado += "; altura " + altura + " m";
        }
        if (posicao.getAlturaMaximaMetros() != null) {
            esperado += "; até " + posicao.getAlturaMaximaMetros() + " m";
        }
        adicionar(resultado, conteiner, posicao, TipoRegraEstivagemPatio.ALTURA,
                SeveridadeAvisoEstivagemPatio.CRITICA,
                "Altura física ou nível de empilhamento excede o limite permitido.",
                observado,
                esperado,
                "Realocar para camada compatível e confirmar dimensões e peso antes da nova estivagem.");
    }

    private void detectarTipoCarga(
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            Map<String, ViolacaoDetectada> resultado) {
        if (!StringUtils.hasText(posicao.getTiposCargaPermitidos())) {
            return;
        }
        String tipo = tipoCarga(conteiner).name();
        boolean permitido = Arrays.stream(posicao.getTiposCargaPermitidos().toUpperCase(Locale.ROOT)
                        .split("[,;|\\s]+"))
                .map(String::trim)
                .anyMatch(tipo::equals);
        if (permitido) {
            return;
        }
        adicionar(resultado, conteiner, posicao, TipoRegraEstivagemPatio.TIPO_CARGA,
                SeveridadeAvisoEstivagemPatio.ALTA,
                "Tipo de carga incompatível com a posição do pátio.",
                tipo,
                posicao.getTiposCargaPermitidos(),
                "Mover a unidade para área autorizada ao tipo de carga ou corrigir a configuração da posição.");
    }

    private void detectarReefer(
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            List<EquipamentoPatio> equipamentos,
            Map<String, ViolacaoDetectada> resultado) {
        if (tipoCarga(conteiner) != TipoCargaConteiner.REFRIGERADO) {
            return;
        }
        boolean cobertura = equipamentos.stream()
                .filter(equipamento -> equipamento.getStatusOperacional() == StatusEquipamento.OPERACIONAL)
                .anyMatch(equipamento -> Objects.equals(equipamento.getLinha(), posicao.getLinha())
                        && Objects.equals(equipamento.getColuna(), posicao.getColuna()));
        if (cobertura) {
            return;
        }
        adicionar(resultado, conteiner, posicao, TipoRegraEstivagemPatio.REEFER,
                SeveridadeAvisoEstivagemPatio.CRITICA,
                "Unidade reefer sem cobertura operacional de equipamento na posição.",
                "Sem equipamento operacional na pilha",
                "Cobertura reefer operacional",
                "Restabelecer a cobertura reefer ou transferir imediatamente a unidade para ponto energizado e monitorado.");
    }

    private void detectarPerigoso(
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            List<ConteinerPatio> inventario,
            Map<String, ViolacaoDetectada> resultado) {
        if (tipoCarga(conteiner) != TipoCargaConteiner.PERIGOSO) {
            return;
        }
        ConteinerPatio vizinho = inventario.stream()
                .filter(outro -> !Objects.equals(outro.getId(), conteiner.getId()))
                .filter(outro -> tipoCarga(outro) == TipoCargaConteiner.PERIGOSO)
                .filter(outro -> Math.abs(outro.getPosicao().getLinha() - posicao.getLinha()) <= 1)
                .filter(outro -> Math.abs(outro.getPosicao().getColuna() - posicao.getColuna()) <= 1)
                .findFirst()
                .orElse(null);
        if (vizinho == null) {
            return;
        }
        adicionar(resultado, conteiner, posicao, TipoRegraEstivagemPatio.PERIGOSO,
                SeveridadeAvisoEstivagemPatio.CRITICA,
                "Carga perigosa não respeita o isolamento mínimo configurado.",
                "Vizinha da unidade " + vizinho.getCodigo(),
                "Sem outra carga perigosa nas posições adjacentes",
                "Isolar a unidade conforme a regra IMO e revalidar toda a vizinhança após a movimentação.");
    }

    private void detectarCapacidade(
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            List<ConteinerPatio> pilha,
            Map<String, ViolacaoDetectada> resultado) {
        if (posicao.getCapacidadePilha() == null || pilha.size() <= posicao.getCapacidadePilha()) {
            return;
        }
        adicionar(resultado, conteiner, posicao, TipoRegraEstivagemPatio.CAPACIDADE,
                SeveridadeAvisoEstivagemPatio.CRITICA,
                "Ocupação da pilha excede a capacidade configurada.",
                pilha.size() + " unidade(s)",
                "Até " + posicao.getCapacidadePilha() + " unidade(s)",
                "Reduzir a ocupação da pilha e revisar a disponibilidade das camadas antes de novo placement.");
    }

    private void detectarReserva(
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            Map<String, ViolacaoDetectada> resultado) {
        if (!posicao.possuiReservaAtiva(LocalDateTime.now())
                || Objects.equals(normalizar(posicao.getReservaCodigoConteiner()), normalizar(conteiner.getCodigo()))) {
            return;
        }
        adicionar(resultado, conteiner, posicao, TipoRegraEstivagemPatio.RESERVA,
                SeveridadeAvisoEstivagemPatio.CRITICA,
                "A posição está ocupada por unidade diferente da reserva ativa.",
                conteiner.getCodigo(),
                posicao.getReservaCodigoConteiner(),
                "Remover a unidade incompatível ou cancelar formalmente a reserva antes de replanejar a posição.");
    }

    private void detectarApoio(
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            List<ConteinerPatio> pilha,
            Map<String, ViolacaoDetectada> resultado) {
        int camada = nivel(posicao.getCamadaOperacional());
        if (camada <= 1) {
            return;
        }
        boolean apoio = pilha.stream()
                .filter(outro -> !Objects.equals(outro.getId(), conteiner.getId()))
                .anyMatch(outro -> nivel(outro.getPosicao().getCamadaOperacional()) == camada - 1);
        if (apoio) {
            return;
        }
        adicionar(resultado, conteiner, posicao, TipoRegraEstivagemPatio.APOIO,
                SeveridadeAvisoEstivagemPatio.CRITICA,
                "Unidade posicionada sem apoio físico na camada imediatamente inferior.",
                "Camada " + camada + " sem ocupação na camada " + (camada - 1),
                "Apoio contínuo abaixo da unidade",
                "Interditar a pilha e corrigir a sequência física de empilhamento antes de liberar novas movimentações.");
    }

    private void detectarRegraPilha(
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            List<ConteinerPatio> pilha,
            Map<String, ViolacaoDetectada> resultado) {
        if (posicao.isBloqueada() || posicao.isInterditada() || !posicao.isAreaPermitida()) {
            adicionar(resultado, conteiner, posicao, TipoRegraEstivagemPatio.REGRA_PILHA,
                    SeveridadeAvisoEstivagemPatio.CRITICA,
                    "Unidade permanece em posição bloqueada, interditada ou fora da área permitida.",
                    estadoPosicao(posicao),
                    "Posição liberada e operacional",
                    "Retirar a unidade da posição restrita ou regularizar formalmente a condição operacional.");
            return;
        }
        if (conteiner.getPesoToneladas() == null) {
            return;
        }
        int camada = nivel(posicao.getCamadaOperacional());
        ConteinerPatio apoioMaisLeve = pilha.stream()
                .filter(outro -> !Objects.equals(outro.getId(), conteiner.getId()))
                .filter(outro -> outro.getPesoToneladas() != null)
                .filter(outro -> nivel(outro.getPosicao().getCamadaOperacional()) < camada)
                .filter(outro -> outro.getPesoToneladas().compareTo(conteiner.getPesoToneladas()) < 0)
                .findFirst()
                .orElse(null);
        if (apoioMaisLeve == null) {
            return;
        }
        adicionar(resultado, conteiner, posicao, TipoRegraEstivagemPatio.REGRA_PILHA,
                SeveridadeAvisoEstivagemPatio.CRITICA,
                "Unidade mais pesada posicionada acima de unidade mais leve na mesma pilha.",
                conteiner.getPesoToneladas() + " t acima de " + apoioMaisLeve.getPesoToneladas() + " t",
                "Peso não crescente da base para o topo",
                "Reordenar a pilha mantendo as unidades mais pesadas nas camadas inferiores.");
    }

    private void adicionar(
            Map<String, ViolacaoDetectada> resultado,
            ConteinerPatio conteiner,
            PosicaoPatio posicao,
            TipoRegraEstivagemPatio regra,
            SeveridadeAvisoEstivagemPatio severidade,
            String descricao,
            String valorObservado,
            String valorEsperado,
            String acaoSugerida) {
        String chave = normalizar(conteiner.getCodigo()) + ":" + posicao.getId() + ":" + regra.name();
        resultado.putIfAbsent(chave, new ViolacaoDetectada(
                chave,
                conteiner.getCodigo(),
                posicao.getId(),
                posicao.getBloco(),
                posicao.getLinha(),
                posicao.getColuna(),
                posicao.getCamadaOperacional(),
                regra,
                severidade,
                descricao,
                valorObservado,
                valorEsperado,
                acaoSugerida));
    }

    private TipoCargaConteiner tipoCarga(ConteinerPatio conteiner) {
        if (conteiner.getTipoCarga() != null) {
            return conteiner.getTipoCarga();
        }
        String descricao = conteiner.getCarga() == null ? "" : normalizar(conteiner.getCarga().getDescricao());
        if (descricao.contains("REEFER") || descricao.contains("REFRIGER")) {
            return TipoCargaConteiner.REFRIGERADO;
        }
        if (descricao.contains("PERIG") || descricao.contains("IMO")) {
            return TipoCargaConteiner.PERIGOSO;
        }
        if (descricao.contains("GRANEL")) {
            return TipoCargaConteiner.GRANELEIRO;
        }
        return TipoCargaConteiner.OUTRO;
    }

    private BigDecimal extrairAltura(String restricoes) {
        if (!StringUtils.hasText(restricoes)) {
            return null;
        }
        Matcher matcher = ALTURA_RESTRICAO.matcher(restricoes);
        if (!matcher.find()) {
            return null;
        }
        try {
            return new BigDecimal(matcher.group(1).replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int nivel(String camadaOperacional) {
        if (!StringUtils.hasText(camadaOperacional)) {
            return -1;
        }
        String digitos = camadaOperacional.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digitos)) {
            return -1;
        }
        try {
            return Integer.parseInt(digitos);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private boolean ocupaPosicaoAtiva(ConteinerPatio conteiner) {
        return conteiner != null
                && conteiner.getPosicao() != null
                && conteiner.getPosicao().getId() != null
                && conteiner.getStatus() != StatusConteiner.LIBERADO
                && conteiner.getStatus() != StatusConteiner.DESPACHADO;
    }

    private String estadoPosicao(PosicaoPatio posicao) {
        if (posicao.isInterditada()) {
            return "INTERDITADA";
        }
        if (posicao.isBloqueada()) {
            return "BLOQUEADA";
        }
        return "FORA_DA_AREA_PERMITIDA";
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : "";
    }

    private record ChavePilha(Integer linha, Integer coluna) {
    }

    public record ViolacaoDetectada(
            String chaveEstavel,
            String codigoUnidade,
            Long posicaoId,
            String bloco,
            Integer linha,
            Integer coluna,
            String camada,
            TipoRegraEstivagemPatio regra,
            SeveridadeAvisoEstivagemPatio severidade,
            String descricao,
            String valorObservado,
            String valorEsperado,
            String acaoSugerida) {
    }
}
