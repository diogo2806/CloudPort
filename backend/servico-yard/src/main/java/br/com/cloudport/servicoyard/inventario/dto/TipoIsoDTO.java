package br.com.cloudport.servicoyard.inventario.dto;

import br.com.cloudport.servicoyard.inventario.modelo.TipoEquipamentoInventario;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class TipoIsoDTO {
    private TipoIsoDTO() { }

    public record ManutencaoRequest(
            String codigo,
            String isoId,
            String descricao,
            TipoEquipamentoInventario.CategoriaEquipamento categoria,
            Long grupoIsoId,
            String arquetipoIso,
            boolean indicadorArquetipo,
            Integer comprimentoMm,
            Integer larguraMm,
            Integer alturaMm,
            BigDecimal taraKg,
            BigDecimal capacidadeKg,
            boolean refrigerado,
            String grupoEquivalencia,
            boolean provisorioEdi,
            String usuario) { }

    public record SituacaoRequest(boolean ativo, String usuario) { }

    public record Resposta(
            Long id,
            String codigo,
            String isoId,
            String descricao,
            TipoEquipamentoInventario.CategoriaEquipamento categoria,
            Long grupoIsoId,
            String grupoIsoCodigo,
            String arquetipoIso,
            boolean indicadorArquetipo,
            Integer comprimentoMm,
            Integer larguraMm,
            Integer alturaMm,
            BigDecimal taraKg,
            BigDecimal capacidadeKg,
            boolean refrigerado,
            String grupoEquivalencia,
            boolean provisorioEdi,
            boolean ativo,
            long dependencias,
            String motivoBloqueio,
            String criadoPor,
            String atualizadoPor,
            LocalDateTime criadoEm,
            LocalDateTime atualizadoEm) { }
}