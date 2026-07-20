package br.com.cloudport.servicoyard.patio.dispatch;

import br.com.cloudport.servicoyard.inventario.modelo.TipoEquipamentoInventario.CategoriaEquipamento;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario.CondicaoEquipamento;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario.EstadoUnidade;
import br.com.cloudport.servicoyard.inventario.repositorio.UnidadeInventarioRepositorio;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Auxiliar;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Configuracao;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchEnums.TipoAuxiliar;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoEquipamento;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SelecaoEquipamentoAuxiliarServico {

    private final UnidadeInventarioRepositorio unidadeRepositorio;
    private final NamedParameterJdbcTemplate jdbc;

    public SelecaoEquipamentoAuxiliarServico(
            UnidadeInventarioRepositorio unidadeRepositorio,
            NamedParameterJdbcTemplate jdbc) {
        this.unidadeRepositorio = unidadeRepositorio;
        this.jdbc = jdbc;
    }

    @Transactional
    public Auxiliar reservarSeNecessario(Long decisaoId,
                                         WorkQueuePatio fila,
                                         OrdemTrabalhoPatio ordem,
                                         EquipamentoPatio equipamento,
                                         Configuracao configuracao,
                                         String operador) {
        if (!configuracao.selecionarAuxiliar()) {
            return null;
        }
        TipoAuxiliar tipo = tipoNecessario(ordem, equipamento.getTipoEquipamento());
        UnidadeInventario unidadePrincipal = unidadeRepositorio
                .findByIdentificacaoIgnoreCase(ordem.getCodigoConteiner()).orElse(null);
        String armador = unidadePrincipal == null ? null : unidadePrincipal.getProprietario();
        UnidadeInventario candidato = unidadeRepositorio.findAllByOrderByIdentificacaoAsc().stream()
                .filter(unidade -> elegivel(unidade, tipo, armador))
                .filter(unidade -> !possuiReservaAtiva(unidade.getId()))
                .min(Comparator
                        .comparing((UnidadeInventario unidade) -> distancia(unidade, ordem))
                        .thenComparing(UnidadeInventario::getIdentificacao))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Nao existe " + tipo + " operacional e livre para a work instruction."));
        UnidadeInventario bloqueada = unidadeRepositorio.findComBloqueioById(candidato.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "O equipamento auxiliar deixou de estar disponivel."));
        if (possuiReservaAtiva(bloqueada.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O equipamento auxiliar foi reservado por outra operacao.");
        }
        String motivo = "Selecionado automaticamente por tipo=" + tipo
                + "; pool=" + valor(fila.getPoolOperacional(), "SEM_POOL")
                + "; armador=" + valor(armador, "NAO_INFORMADO")
                + "; posicao=" + valor(bloqueada.getPosicaoAtual(), "NAO_INFORMADA");
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO reserva_equipamento_auxiliar_dispatch (
                    decisao_dispatch_id, ordem_trabalho_patio_id, unidade_inventario_id,
                    tipo_auxiliar, status, pool_operacional, armador, origem, destino,
                    motivo_selecao, reservado_por, reservado_em
                ) VALUES (
                    :decisaoId, :ordemId, :unidadeId,
                    :tipo, 'RESERVADO', :pool, :armador, :origem, :destino,
                    :motivo, :operador, CURRENT_TIMESTAMP
                )
                """, new MapSqlParameterSource()
                .addValue("decisaoId", decisaoId)
                .addValue("ordemId", ordem.getId())
                .addValue("unidadeId", bloqueada.getId())
                .addValue("tipo", tipo.name())
                .addValue("pool", fila.getPoolOperacional())
                .addValue("armador", armador)
                .addValue("origem", bloqueada.getPosicaoAtual())
                .addValue("destino", ordem.getDestino())
                .addValue("motivo", motivo)
                .addValue("operador", usuario(operador)), keyHolder, new String[]{"id"});
        Long reservaId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        registrarMovimento(reservaId, "COLETA", bloqueada.getPosicaoAtual(), ordem.getDestino(),
                operador, "Coleta planejada pelo scheduler.");
        registrarMovimento(reservaId, "ASSOCIACAO", bloqueada.getPosicaoAtual(), ordem.getDestino(),
                operador, "Associacao ao CHE e a work instruction.");
        jdbc.update("""
                UPDATE reserva_equipamento_auxiliar_dispatch
                SET status = 'ASSOCIADO', associado_em = CURRENT_TIMESTAMP
                WHERE id = :id
                """, new MapSqlParameterSource("id", reservaId));
        return new Auxiliar(reservaId, bloqueada.getId(), bloqueada.getIdentificacao(), tipo.name(),
                "ASSOCIADO", motivo);
    }

    @Transactional
    public void devolverDaOrdem(Long ordemId, String operador) {
        List<Long> reservas = jdbc.queryForList("""
                SELECT id
                FROM reserva_equipamento_auxiliar_dispatch
                WHERE ordem_trabalho_patio_id = :ordemId
                  AND status IN ('RESERVADO', 'ASSOCIADO')
                """, new MapSqlParameterSource("ordemId", ordemId), Long.class);
        for (Long reservaId : reservas) {
            registrarMovimento(reservaId, "DESASSOCIACAO", null, null, operador,
                    "Desassociacao apos encerramento da work instruction.");
            registrarMovimento(reservaId, "DEVOLUCAO", null, null, operador,
                    "Equipamento auxiliar liberado para nova selecao.");
            jdbc.update("""
                    UPDATE reserva_equipamento_auxiliar_dispatch
                    SET status = 'DEVOLVIDO', devolvido_em = CURRENT_TIMESTAMP
                    WHERE id = :id
                    """, new MapSqlParameterSource("id", reservaId));
        }
    }

    @Transactional(readOnly = true)
    public Optional<Auxiliar> buscarPorDecisao(Long decisaoId) {
        return jdbc.query("""
                SELECT r.id, r.unidade_inventario_id, u.identificacao,
                       r.tipo_auxiliar, r.status, r.motivo_selecao
                FROM reserva_equipamento_auxiliar_dispatch r
                JOIN unidade_inventario u ON u.id = r.unidade_inventario_id
                WHERE r.decisao_dispatch_id = :decisaoId
                ORDER BY r.id DESC
                LIMIT 1
                """, new MapSqlParameterSource("decisaoId", decisaoId), (rs, rowNum) -> new Auxiliar(
                rs.getLong("id"),
                rs.getLong("unidade_inventario_id"),
                rs.getString("identificacao"),
                rs.getString("tipo_auxiliar"),
                rs.getString("status"),
                rs.getString("motivo_selecao"))).stream().findFirst();
    }

    private boolean elegivel(UnidadeInventario unidade, TipoAuxiliar tipo, String armador) {
        if (unidade.getCondicao() != CondicaoEquipamento.OPERACIONAL
                || unidade.possuiHoldAtivo(LocalDateTime.now())
                || unidade.getEstado() == EstadoUnidade.INATIVA
                || unidade.getEstado() == EstadoUnidade.APOSENTADA) {
            return false;
        }
        if (StringUtils.hasText(armador) && StringUtils.hasText(unidade.getProprietario())
                && !armador.equalsIgnoreCase(unidade.getProprietario())) {
            return false;
        }
        if (tipo == TipoAuxiliar.CHASSI) {
            return unidade.getCategoria() == CategoriaEquipamento.CHASSI;
        }
        if (unidade.getCategoria() != CategoriaEquipamento.ACESSORIO) {
            return false;
        }
        String referencia = (valor(unidade.getTipoEquipamento().getCodigo(), "") + " "
                + valor(unidade.getTipoEquipamento().getDescricao(), "")).toUpperCase(Locale.ROOT);
        if (tipo == TipoAuxiliar.BOMB_CART) {
            return referencia.contains("BOMB") || referencia.contains("CART");
        }
        if (tipo == TipoAuxiliar.CASSETTE) {
            return referencia.contains("CASSET") || referencia.contains("BOBINA");
        }
        return true;
    }

    private TipoAuxiliar tipoNecessario(OrdemTrabalhoPatio ordem, TipoEquipamento tipoEquipamento) {
        String carga = valor(ordem.getTipoCarga(), "").toUpperCase(Locale.ROOT);
        if (carga.contains("BOBINA") || carga.contains("STEEL") || carga.contains("SIDERURG")) {
            return TipoAuxiliar.CASSETTE;
        }
        if (tipoEquipamento == TipoEquipamento.TRATOR_PORTUARIO) {
            return TipoAuxiliar.CHASSI;
        }
        if (tipoEquipamento == TipoEquipamento.GUINDASTE_SHIP_TO_SHORE) {
            return TipoAuxiliar.BOMB_CART;
        }
        return TipoAuxiliar.ACESSORIO;
    }

    private double distancia(UnidadeInventario unidade, OrdemTrabalhoPatio ordem) {
        if (!StringUtils.hasText(unidade.getPosicaoAtual()) || !StringUtils.hasText(ordem.getDestino())) {
            return Double.MAX_VALUE / 2;
        }
        return unidade.getPosicaoAtual().equalsIgnoreCase(ordem.getDestino()) ? 0.0 : 1.0;
    }

    private boolean possuiReservaAtiva(Long unidadeId) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM reserva_equipamento_auxiliar_dispatch
                    WHERE unidade_inventario_id = :unidadeId
                      AND status IN ('RESERVADO', 'ASSOCIADO')
                )
                """, new MapSqlParameterSource("unidadeId", unidadeId), Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    private void registrarMovimento(Long reservaId, String tipo, String origem, String destino,
                                    String operador, String detalhes) {
        jdbc.update("""
                INSERT INTO movimento_equipamento_auxiliar_dispatch (
                    reserva_id, tipo_movimento, origem, destino, operador, ocorrido_em, detalhes
                ) VALUES (
                    :reservaId, :tipo, :origem, :destino, :operador, CURRENT_TIMESTAMP, :detalhes
                )
                """, new MapSqlParameterSource()
                .addValue("reservaId", reservaId)
                .addValue("tipo", tipo)
                .addValue("origem", origem)
                .addValue("destino", destino)
                .addValue("operador", usuario(operador))
                .addValue("detalhes", detalhes));
    }

    private String usuario(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : "sistema";
    }

    private String valor(String valor, String padrao) {
        return StringUtils.hasText(valor) ? valor.trim() : padrao;
    }
}
