package br.com.cloudport.servicoyard.inventario.dto;

import br.com.cloudport.servicoyard.inventario.modelo.ContagemInventarioFisico;
import br.com.cloudport.servicoyard.inventario.modelo.TipoEquipamentoInventario;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import br.com.cloudport.servicoyard.inventario.modelo.VinculoEquipamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class InventarioCanonicoDTO {

    private InventarioCanonicoDTO() {
    }

    public record CriarTipoEquipamentoRequest(
            String codigo,
            String descricao,
            TipoEquipamentoInventario.CategoriaEquipamento categoria,
            String codigoIso,
            Integer comprimentoMm,
            Integer larguraMm,
            Integer alturaMm,
            BigDecimal taraKg,
            BigDecimal capacidadeKg,
            boolean refrigerado,
            String grupoEquivalencia) {
    }

    public record TipoEquipamentoResposta(
            Long id,
            String codigo,
            String descricao,
            TipoEquipamentoInventario.CategoriaEquipamento categoria,
            String codigoIso,
            Integer comprimentoMm,
            Integer larguraMm,
            Integer alturaMm,
            BigDecimal taraKg,
            BigDecimal capacidadeKg,
            boolean refrigerado,
            String grupoEquivalencia,
            boolean ativo) {
    }

    public record CriarPrefixoRequest(
            String prefixo,
            String proprietario,
            TipoEquipamentoInventario.CategoriaEquipamento categoria) {
    }

    public record PrefixoResposta(
            Long id,
            String prefixo,
            String proprietario,
            TipoEquipamentoInventario.CategoriaEquipamento categoria,
            boolean ativo) {
    }

    public record CriarUnidadeRequest(
            String identificacao,
            String tipoEquipamentoCodigo,
            UnidadeInventario.EstadoUnidade estado,
            UnidadeInventario.CondicaoEquipamento condicao,
            String proprietario,
            String operador,
            String posicaoAtual,
            String posicaoPlanejada,
            BigDecimal pesoBrutoKg,
            String observacoes,
            String usuario,
            String origemAcao) {
    }

    public record AtualizarEstadoRequest(
            UnidadeInventario.EstadoUnidade estado,
            String motivo,
            String usuario,
            String origemAcao) {
    }

    public record AtualizarPropriedadeRequest(
            String proprietario,
            String operador,
            String usuario,
            String origemAcao) {
    }

    public record AtualizarPosicaoRequest(
            String posicaoAtual,
            String posicaoPlanejada,
            String usuario,
            String origemAcao) {
    }

    public record LacreRequest(
            String numero,
            String tipo,
            String status,
            String responsavel) {
    }

    public record DocumentoRequest(
            String tipo,
            String numero,
            String uri,
            String checksum,
            UnidadeInventario.StatusDocumento status,
            LocalDate validoAte) {
    }

    public record AvariaRequest(
            String componente,
            String tipo,
            String severidade,
            UnidadeInventario.StatusAvaria status,
            String descricao,
            LocalDateTime reparadaEm,
            String responsavel) {
    }

    public record RestricaoRequest(
            UnidadeInventario.TipoRestricao tipo,
            String codigo,
            String descricao,
            String autoridade,
            Boolean ativa,
            LocalDateTime validoDe,
            LocalDateTime validoAte) {
    }

    public record ManutencaoRequest(
            String ordemServico,
            String tipoServico,
            String fornecedor,
            UnidadeInventario.StatusManutencao status,
            LocalDateTime concluidaEm,
            String observacoes) {
    }

    public record ReeferRequest(
            BigDecimal setpointC,
            BigDecimal temperaturaSupplyC,
            BigDecimal temperaturaReturnC,
            BigDecimal umidadePercentual,
            BigDecimal ventilacaoM3h,
            boolean ligado,
            String alarme,
            LocalDateTime lidoEm,
            String responsavel) {
    }

    public record MontagemRequest(
            Long unidadePrincipalId,
            Long unidadeRelacionadaId,
            VinculoEquipamento.PapelEquipamento papel,
            String responsavel,
            String observacoes) {
    }

    public record DesmontagemRequest(
            String responsavel,
            String motivo) {
    }

    public record ItemContagemRequest(
            String identificacao,
            String posicaoLida,
            boolean encontrada,
            String observacoes) {
    }

    public record ContagemLoteRequest(
            String lote,
            List<ItemContagemRequest> itens,
            String responsavel) {
    }

    public record ResolverDivergenciaRequest(
            String responsavel,
            String observacoes) {
    }

    public record ResumoInventario(
            long totalUnidades,
            long totalConteiners,
            long totalChassis,
            long totalCarretas,
            long totalAcessorios,
            long totalNoPatio,
            long totalComHold,
            long totalAvariadas,
            long totalEmManutencao,
            long totalReefer,
            long totalMontadas,
            long totalDivergenciasAbertas,
            LocalDateTime atualizadoEm) {
    }

    public record UnidadeResumo(
            Long id,
            String identificacao,
            String prefixo,
            TipoEquipamentoInventario.CategoriaEquipamento categoria,
            String tipoEquipamentoCodigo,
            String tipoEquipamentoDescricao,
            String codigoIso,
            UnidadeInventario.EstadoUnidade estado,
            UnidadeInventario.CondicaoEquipamento condicao,
            UnidadeInventario.StatusManutencao statusManutencao,
            String proprietario,
            String operador,
            String posicaoAtual,
            String posicaoPlanejada,
            BigDecimal pesoBrutoKg,
            boolean refrigerado,
            long holdsAtivos,
            long permissionsAtivas,
            long avariasAbertas,
            long documentos,
            long equipamentosVinculados,
            LocalDateTime atualizadoEm) {
    }

    public record DashboardInventarioResposta(
            ResumoInventario resumo,
            List<UnidadeResumo> unidades) {
    }

    public record LacreResposta(
            String numero,
            String tipo,
            String status,
            LocalDateTime anexadoEm,
            LocalDateTime removidoEm,
            String responsavel) {
    }

    public record DocumentoResposta(
            String tipo,
            String numero,
            String uri,
            String checksum,
            UnidadeInventario.StatusDocumento status,
            LocalDate validoAte,
            LocalDateTime registradoEm) {
    }

    public record AvariaResposta(
            String componente,
            String tipo,
            String severidade,
            UnidadeInventario.StatusAvaria status,
            String descricao,
            LocalDateTime detectadaEm,
            LocalDateTime reparadaEm,
            String responsavel) {
    }

    public record RestricaoResposta(
            UnidadeInventario.TipoRestricao tipo,
            String codigo,
            String descricao,
            String autoridade,
            boolean ativa,
            LocalDateTime validoDe,
            LocalDateTime validoAte,
            LocalDateTime registradoEm) {
    }

    public record ManutencaoResposta(
            String ordemServico,
            String tipoServico,
            String fornecedor,
            UnidadeInventario.StatusManutencao status,
            LocalDateTime abertaEm,
            LocalDateTime concluidaEm,
            String observacoes) {
    }

    public record HistoricoAtributoResposta(
            String atributo,
            String valorAnterior,
            String valorAtual,
            String origem,
            String responsavel,
            LocalDateTime alteradoEm) {
    }

    public record ReeferResposta(
            BigDecimal setpointC,
            BigDecimal temperaturaSupplyC,
            BigDecimal temperaturaReturnC,
            BigDecimal umidadePercentual,
            BigDecimal ventilacaoM3h,
            boolean ligado,
            String alarme,
            LocalDateTime lidoEm,
            String responsavel) {
    }

    public record VinculoResposta(
            Long id,
            Long unidadePrincipalId,
            String unidadePrincipal,
            Long unidadeRelacionadaId,
            String unidadeRelacionada,
            VinculoEquipamento.PapelEquipamento papel,
            boolean ativo,
            LocalDateTime montadoEm,
            LocalDateTime desmontadoEm,
            String responsavelMontagem,
            String responsavelDesmontagem,
            String observacoes) {
    }

    public record UnidadeDetalheResposta(
            UnidadeResumo unidade,
            TipoEquipamentoResposta tipoEquipamento,
            List<TipoEquipamentoResposta> tiposEquivalentes,
            List<LacreResposta> lacres,
            List<DocumentoResposta> documentos,
            List<AvariaResposta> avarias,
            List<RestricaoResposta> holdsPermissions,
            List<ManutencaoResposta> manutencoes,
            List<ReeferResposta> reefer,
            List<VinculoResposta> equipamentosMontados,
            List<HistoricoAtributoResposta> historicoAtributos,
            String observacoes,
            LocalDateTime criadoEm) {
    }

    public record DivergenciaResposta(
            Long id,
            String lote,
            Long unidadeId,
            String identificacao,
            String posicaoEsperada,
            String posicaoLida,
            ContagemInventarioFisico.StatusContagem status,
            ContagemInventarioFisico.TipoDivergencia tipoDivergencia,
            String observacoes,
            String responsavel,
            LocalDateTime registradoEm,
            LocalDateTime resolvidoEm,
            String resolvidoPor) {
    }

    public record ContagemLoteResposta(
            String lote,
            int totalItens,
            long conferentes,
            long divergentes,
            List<DivergenciaResposta> resultados) {
    }
}
