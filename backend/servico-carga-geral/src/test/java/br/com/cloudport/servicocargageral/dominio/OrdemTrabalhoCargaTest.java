package br.com.cloudport.servicocargageral.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class OrdemTrabalhoCargaTest {

    @Test
    void deveExecutarCicloDeVidaCompleto() {
        OrdemTrabalhoCarga ordem = novaOrdem();
        ItemOrdemTrabalhoCarga item = new ItemOrdemTrabalhoCarga();
        item.setLote(new LoteCarga());
        item.setQuantidade(BigDecimal.ONE);
        ordem.adicionarItem(item);
        ordem.registrarCriacao("planejador");
        ordem.atribuirRecursos("EQUIPE-1", "CHE-10", "planejador");
        ordem.liberar("planejador");
        ordem.iniciar("operador");
        ordem.registrarServico("Carga posicionada no local de serviço.", "operador");
        ordem.concluir("operador");

        assertEquals(StatusOrdemTrabalhoCarga.CONCLUIDA, ordem.getStatus());
        assertEquals(6, ordem.getEventos().size());
    }

    @Test
    void deveExigirMotivoParaCancelar() {
        OrdemTrabalhoCarga ordem = novaOrdem();
        assertThrows(IllegalArgumentException.class, () -> ordem.cancelar(" ", "operador"));
    }

    @Test
    void naoDeveIniciarOrdemNaoLiberada() {
        OrdemTrabalhoCarga ordem = novaOrdem();
        assertThrows(IllegalStateException.class, () -> ordem.iniciar("operador"));
    }

    private OrdemTrabalhoCarga novaOrdem() {
        OrdemTrabalhoCarga ordem = new OrdemTrabalhoCarga();
        ordem.setNumero("OT-1000");
        ordem.setTipo(TipoServicoOrdemCarga.MOVIMENTACAO_INTERNA);
        ordem.setPrioridade(10);
        ordem.setJanelaInicio(OffsetDateTime.now());
        ordem.setJanelaFim(OffsetDateTime.now().plusHours(2));
        ordem.setLocal("ARMAZEM-A");
        return ordem;
    }
}
