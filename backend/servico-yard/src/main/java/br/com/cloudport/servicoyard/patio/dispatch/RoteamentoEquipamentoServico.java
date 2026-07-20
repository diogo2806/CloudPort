package br.com.cloudport.servicoyard.patio.dispatch;

import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Configuracao;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Rota;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.TelemetriaEquipamentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.TelemetriaEquipamentoPatioRepositorio;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RoteamentoEquipamentoServico {

    private static final double METROS_POR_CELULA = 25.0;

    private final TelemetriaEquipamentoPatioRepositorio telemetriaRepositorio;
    private final NamedParameterJdbcTemplate jdbc;

    public RoteamentoEquipamentoServico(
            TelemetriaEquipamentoPatioRepositorio telemetriaRepositorio,
            NamedParameterJdbcTemplate jdbc) {
        this.telemetriaRepositorio = telemetriaRepositorio;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Rota calcular(EquipamentoPatio equipamento,
                         OrdemTrabalhoPatio ordem,
                         Configuracao configuracao) {
        Optional<TelemetriaEquipamentoPatio> telemetriaOptional =
                telemetriaRepositorio.findByEquipamentoId(equipamento.getId());
        TelemetriaEquipamentoPatio telemetria = telemetriaOptional.orElse(null);
        LocalDateTime recebidoEm = telemetria == null ? null : telemetria.getRecebidoEm();
        boolean atrasada = recebidoEm == null || recebidoEm.isBefore(
                LocalDateTime.now().minusSeconds(configuracao.toleranciaTelemetriaSegundos()));

        double origemX = coordenadaX(telemetria, equipamento);
        double origemY = coordenadaY(telemetria, equipamento);
        double destinoX = ordem.getLinhaDestino() == null ? origemX : ordem.getLinhaDestino();
        double destinoY = ordem.getColunaDestino() == null ? origemY : ordem.getColunaDestino();
        String origem = StringUtils.hasText(telemetria == null ? null : telemetria.getPosicaoMaisProxima())
                ? telemetria.getPosicaoMaisProxima().trim().toUpperCase(Locale.ROOT)
                : posicao(origemX, origemY);
        String destino = StringUtils.hasText(ordem.getDestino())
                ? ordem.getDestino().trim().toUpperCase(Locale.ROOT)
                : posicao(destinoX, destinoY);

        Segmento segmento = buscarSegmento(origem, destino).orElse(null);
        double distancia = segmento == null
                ? Math.hypot(destinoX - origemX, destinoY - origemY) * METROS_POR_CELULA
                : segmento.distanciaMetros();
        double congestionamento = segmento == null ? 0.0 : segmento.congestionamentoPercentual();
        int limiteRegional = segmento != null && segmento.limiteRegionalChe() != null
                ? segmento.limiteRegionalChe()
                : configuracao.limiteRegionalChe();
        int cheAtivosNaOrigem = contarCheAtivosNaOrigem(origem);
        boolean limiteExcedido = cheAtivosNaOrigem >= limiteRegional;
        boolean bloqueada = (segmento != null && segmento.bloqueado()) || limiteExcedido;
        double velocidadeMetrosSegundo = Math.max(0.1, configuracao.velocidadeMediaKmh() / 3.6);
        double fatorCongestionamento = 1.0 + congestionamento / 100.0;
        int etaSegundos = (int) Math.ceil((distancia / velocidadeMetrosSegundo) * fatorCongestionamento)
                + configuracao.tempoColetaSegundos()
                + configuracao.tempoEntregaSegundos();
        String justificativa = "origem=" + origem
                + "; destino=" + destino
                + "; distanciaMetros=" + arredondar(distancia)
                + "; congestionamento=" + arredondar(congestionamento) + "%"
                + "; heading=" + (telemetria == null || telemetria.getHeading() == null
                    ? "NAO_INFORMADO" : arredondar(telemetria.getHeading()))
                + "; telemetria=" + (atrasada ? "ATRASADA" : "ATUAL")
                + "; cheAtivosNaOrigem=" + cheAtivosNaOrigem
                + "; limiteRegional=" + limiteRegional
                + (segmento != null && StringUtils.hasText(segmento.motivoInterdicao())
                    ? "; interdicao=" + segmento.motivoInterdicao() : "");
        return new Rota(origem, destino, distancia, congestionamento, etaSegundos,
                bloqueada, atrasada, recebidoEm, justificativa);
    }

    private Optional<Segmento> buscarSegmento(String origem, String destino) {
        return jdbc.query("""
                SELECT distancia_metros, congestionamento_percentual, bloqueado,
                       motivo_interdicao, limite_regional_che, sentido, atualizado_em
                FROM segmento_rota_dispatch
                WHERE origem = :origem
                  AND destino = :destino
                  AND ativo = TRUE
                  AND vigente_de <= CURRENT_TIMESTAMP
                  AND (vigente_ate IS NULL OR vigente_ate > CURRENT_TIMESTAMP)
                ORDER BY versao DESC
                LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("origem", origem)
                .addValue("destino", destino), this::mapearSegmento).stream().findFirst();
    }

    private int contarCheAtivosNaOrigem(String origem) {
        Integer total = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM decisao_dispatch
                WHERE origem_rota = :origem
                  AND status = 'ATRIBUIDA'
                """, new MapSqlParameterSource("origem", origem), Integer.class);
        return total == null ? 0 : total;
    }

    private Segmento mapearSegmento(ResultSet rs, int rowNum) throws SQLException {
        Integer limite = (Integer) rs.getObject("limite_regional_che");
        Timestamp atualizadoEm = rs.getTimestamp("atualizado_em");
        return new Segmento(
                rs.getDouble("distancia_metros"),
                rs.getDouble("congestionamento_percentual"),
                rs.getBoolean("bloqueado"),
                rs.getString("motivo_interdicao"),
                limite,
                rs.getString("sentido"),
                atualizadoEm == null ? null : atualizadoEm.toLocalDateTime());
    }

    private double coordenadaX(TelemetriaEquipamentoPatio telemetria, EquipamentoPatio equipamento) {
        if (telemetria != null && telemetria.getCoordenadaX() != null) {
            return telemetria.getCoordenadaX();
        }
        return equipamento.getLinha() == null ? 0.0 : equipamento.getLinha();
    }

    private double coordenadaY(TelemetriaEquipamentoPatio telemetria, EquipamentoPatio equipamento) {
        if (telemetria != null && telemetria.getCoordenadaY() != null) {
            return telemetria.getCoordenadaY();
        }
        return equipamento.getColuna() == null ? 0.0 : equipamento.getColuna();
    }

    private String posicao(double x, double y) {
        return "GRID-" + Math.round(x) + "-" + Math.round(y);
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private record Segmento(
            double distanciaMetros,
            double congestionamentoPercentual,
            boolean bloqueado,
            String motivoInterdicao,
            Integer limiteRegionalChe,
            String sentido,
            LocalDateTime atualizadoEm) {
    }
}
