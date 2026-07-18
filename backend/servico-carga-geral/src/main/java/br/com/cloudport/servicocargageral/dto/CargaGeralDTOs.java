package br.com.cloudport.servicocargageral.dto;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.CategoriaReferenciaCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.NaturezaCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusConhecimentoCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusLoteCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoMovimentacaoCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoOperacaoConhecimento;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public final class CargaGeralDTOs {

    private CargaGeralDTOs() {
    }

    public record CriarConhecimentoRequest(
            @NotBlank @Size(max = 80) String numero,
            @NotNull TipoOperacaoConhecimento tipoOperacao,
            @NotBlank @Size(max = 180) String embarcador,
            @NotBlank @Size(max = 180) String consignatario,
            @Size(max = 80) String clienteId,
            @Size(max = 80) String operadorId,
            @Size(max = 80) String visitaNavioId,
            @Size(max = 80) String visitaVeiculoId,
            @Size(max = 80) String armazemId,
            @Size(max = 120) String portoOrigem,
            @Size(max = 120) String portoDestino,
            @Size(max = 1000) String observacoes) {
    }

    public record CriarItemRequest(
            @Min(1) int sequencia,
            @NotBlank @Size(max = 300) String descricao,
            @NotBlank @Size(max = 80) String commodityCodigo,
            @NotBlank @Size(max = 80) String tipoProdutoCodigo,
            @NotBlank @Size(max = 80) String tipoEmbalagemCodigo,
            @NotNull @DecimalMin("0.001") BigDecimal quantidadeManifestada,
            @NotNull @DecimalMin("0.000") BigDecimal volumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoKg,
            @Size(max = 80) String codigoArmazenagem,
            @Size(max = 80) String codigoManuseio,
            boolean mercadoriaPerigosa,
            @Size(max = 20) String numeroUn,
            @Size(max = 20) String classeImdg,
            BigDecimal temperaturaMinima,
            BigDecimal temperaturaMaxima) {
    }

    public record CriarLoteRequest(
            @NotBlank @Size(max = 100) String codigo,
            @NotNull NaturezaCarga natureza,
            @NotNull @DecimalMin("0.001") BigDecimal quantidadePrevista,
            @NotNull @DecimalMin("0.000") BigDecimal volumePrevistoM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoPrevistoKg,
            @NotBlank @Size(max = 20) String unidadeMedida,
            @Size(max = 300) String marcasEmbalagem,
            @Size(max = 80) String armazemId,
            @Size(max = 120) String posicaoArmazenagem,
            @Size(max = 80) String veiculoId,
            @Size(max = 80) String visitaNavioId,
            @Size(max = 80) String clienteId,
            UUID lotePaiId) {
    }

    public record RegistrarMovimentacaoRequest(
            @NotNull TipoMovimentacaoCarga tipo,
            @NotNull @DecimalMin("0.000") BigDecimal quantidade,
            @NotNull @DecimalMin("0.000") BigDecimal volumeM3,
            @NotNull @DecimalMin("0.000") BigDecimal pesoKg,
            UUID loteRelacionadoId,
            @Size(max = 40) String origemTipo,
            @Size(max = 120) String origemId,
            @Size(max = 40) String destinoTipo,
            @Size(max = 120) String destinoId,
            @Size(max = 80) String veiculoId,
            @Size(max = 80) String visitaNavioId,
            @Size(max = 80) String armazemId,
            @Size(max = 80) String clienteId,
            @NotBlank @Size(max = 120) String usuario,
            @Size(max = 120) String correlationId,
            @Size(max = 1000) String observacao,
            OffsetDateTime ocorridoEm) {
    }

    public record RegistrarAvariaRequest(
            @NotBlank @Size(max = 80) String codigoAvaria,
            @NotBlank @Size(max = 1000) String descricaoAvaria) {
    }

    public record CriarReferenciaRequest(
            @NotNull CategoriaReferenciaCarga categoria,
            @NotBlank @Size(max = 80) String codigo,
            @NotBlank @Size(max = 240) String descricao,
            @Size(max = 4000) String atributosJson,
            boolean ativo) {
    }

    public record ConhecimentoResumo(
            UUID id,
            String numero,
            TipoOperacaoConhecimento tipoOperacao,
            StatusConhecimentoCarga status,
            String embarcador,
            String consignatario,
            String clienteId,
            String visitaNavioId,
            String visitaVeiculoId,
            String armazemId,
            int quantidadeItens,
            OffsetDateTime atualizadoEm) {
    }

    public record ConhecimentoDetalhe(
            UUID id,
            String numero,
            TipoOperacaoConhecimento tipoOperacao,
            StatusConhecimentoCarga status,
            String embarcador,
            String consignatario,
            String clienteId,
            String operadorId,
            String visitaNavioId,
            String visitaVeiculoId,
            String armazemId,
            String portoOrigem,
            String portoDestino,
            String observacoes,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm,
            List<ItemResposta> itens) {
    }

    public record ItemResposta(
            UUID id,
            int sequencia,
            String descricao,
            String commodityCodigo,
            String tipoProdutoCodigo,
            String tipoEmbalagemCodigo,
            BigDecimal quantidadeManifestada,
            BigDecimal volumeM3,
            BigDecimal pesoKg,
            String codigoArmazenagem,
            String codigoManuseio,
            boolean mercadoriaPerigosa,
            String numeroUn,
            String classeImdg,
            BigDecimal temperaturaMinima,
            BigDecimal temperaturaMaxima,
            List<LoteResumo> lotes) {
    }

    public record LoteResumo(
            UUID id,
            String codigo,
            String conhecimentoNumero,
            int itemSequencia,
            String descricaoItem,
            NaturezaCarga natureza,
            StatusLoteCarga status,
            BigDecimal quantidadePrevista,
            BigDecimal quantidadeSaldo,
            BigDecimal volumeSaldoM3,
            BigDecimal pesoSaldoKg,
            String unidadeMedida,
            String armazemId,
            String posicaoArmazenagem,
            String veiculoId,
            String visitaNavioId,
            String clienteId,
            String codigoAvaria,
            UUID lotePaiId,
            OffsetDateTime atualizadoEm) {
    }

    public record LoteDetalhe(
            LoteResumo lote,
            String marcasEmbalagem,
            String descricaoAvaria,
            List<MovimentacaoResposta> movimentacoes) {
    }

    public record MovimentacaoResposta(
            UUID id,
            UUID loteId,
            String loteCodigo,
            TipoMovimentacaoCarga tipo,
            BigDecimal quantidade,
            BigDecimal volumeM3,
            BigDecimal pesoKg,
            UUID loteRelacionadoId,
            String origemTipo,
            String origemId,
            String destinoTipo,
            String destinoId,
            String veiculoId,
            String visitaNavioId,
            String armazemId,
            String clienteId,
            String usuario,
            String correlationId,
            String observacao,
            OffsetDateTime ocorridoEm) {
    }

    public record ReferenciaResposta(
            UUID id,
            CategoriaReferenciaCarga categoria,
            String codigo,
            String descricao,
            String atributosJson,
            boolean ativo,
            OffsetDateTime atualizadoEm) {
    }

    public record DashboardResposta(
            long conhecimentosAbertos,
            long lotesNoTerminal,
            long lotesBreakBulk,
            long lotesAvariados,
            BigDecimal quantidadeEmEstoque,
            BigDecimal volumeEmEstoqueM3,
            BigDecimal pesoEmEstoqueKg,
            List<MovimentacaoResposta> ultimasMovimentacoes) {
    }
}
