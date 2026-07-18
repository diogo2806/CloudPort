package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.EventoOrdemTrabalhoCarga;
import br.com.cloudport.servicocargageral.dominio.ItemOrdemTrabalhoCarga;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dominio.OrdemTrabalhoCarga;
import br.com.cloudport.servicocargageral.dto.OrdemTrabalhoCargaDTOs.AtribuirRecursosRequest;
import br.com.cloudport.servicocargageral.dto.OrdemTrabalhoCargaDTOs.CancelarOrdemRequest;
import br.com.cloudport.servicocargageral.dto.OrdemTrabalhoCargaDTOs.CriarItemRequest;
import br.com.cloudport.servicocargageral.dto.OrdemTrabalhoCargaDTOs.CriarOrdemRequest;
import br.com.cloudport.servicocargageral.dto.OrdemTrabalhoCargaDTOs.EventoResposta;
import br.com.cloudport.servicocargageral.dto.OrdemTrabalhoCargaDTOs.ItemResposta;
import br.com.cloudport.servicocargageral.dto.OrdemTrabalhoCargaDTOs.OrdemResposta;
import br.com.cloudport.servicocargageral.dto.OrdemTrabalhoCargaDTOs.RegistrarEventoRequest;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.OrdemTrabalhoCargaRepositorio;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdemTrabalhoCargaServico {
    private final OrdemTrabalhoCargaRepositorio ordemRepositorio;
    private final LoteCargaRepositorio loteRepositorio;

    public OrdemTrabalhoCargaServico(OrdemTrabalhoCargaRepositorio ordemRepositorio, LoteCargaRepositorio loteRepositorio) {
        this.ordemRepositorio = ordemRepositorio;
        this.loteRepositorio = loteRepositorio;
    }

    @Transactional(readOnly = true)
    public List<OrdemResposta> listar() {
        return ordemRepositorio.findAll(Sort.by(Sort.Direction.DESC, "criadoEm")).stream().map(this::resposta).toList();
    }

    @Transactional(readOnly = true)
    public OrdemResposta obter(UUID id) { return resposta(buscar(id)); }

    @Transactional
    public OrdemResposta criar(CriarOrdemRequest request, String usuario) {
        if (!request.janelaFim().isAfter(request.janelaInicio())) throw new IllegalArgumentException("A janela operacional é inválida.");
        if (ordemRepositorio.existsByNumeroIgnoreCase(request.numero().trim())) throw new IllegalStateException("Já existe ordem com o número informado.");
        OrdemTrabalhoCarga ordem = new OrdemTrabalhoCarga();
        ordem.setNumero(request.numero()); ordem.setTipo(request.tipo()); ordem.setPrioridade(request.prioridade());
        ordem.setJanelaInicio(request.janelaInicio()); ordem.setJanelaFim(request.janelaFim()); ordem.setLocal(request.local());
        for (CriarItemRequest itemRequest : request.itens()) {
            LoteCarga lote = loteRepositorio.findById(itemRequest.loteId()).orElseThrow(() -> new IllegalArgumentException("Lote não encontrado: " + itemRequest.loteId()));
            ItemOrdemTrabalhoCarga item = new ItemOrdemTrabalhoCarga(); item.setLote(lote); item.setQuantidade(itemRequest.quantidade()); item.setObservacao(itemRequest.observacao()); ordem.adicionarItem(item);
        }
        ordem.registrarCriacao(usuario);
        return resposta(ordemRepositorio.save(ordem));
    }

    @Transactional public OrdemResposta liberar(UUID id, String usuario) { OrdemTrabalhoCarga ordem = buscar(id); ordem.liberar(usuario); return resposta(ordem); }
    @Transactional public OrdemResposta atribuir(UUID id, AtribuirRecursosRequest request, String usuario) { OrdemTrabalhoCarga ordem = buscar(id); ordem.atribuirRecursos(request.equipeId(), request.equipamentoId(), usuario); return resposta(ordem); }
    @Transactional public OrdemResposta iniciar(UUID id, String usuario) { OrdemTrabalhoCarga ordem = buscar(id); ordem.iniciar(usuario); return resposta(ordem); }
    @Transactional public OrdemResposta registrarEvento(UUID id, RegistrarEventoRequest request, String usuario) { OrdemTrabalhoCarga ordem = buscar(id); ordem.registrarServico(request.descricao(), usuario); return resposta(ordem); }
    @Transactional public OrdemResposta concluir(UUID id, String usuario) { OrdemTrabalhoCarga ordem = buscar(id); ordem.concluir(usuario); return resposta(ordem); }
    @Transactional public OrdemResposta cancelar(UUID id, CancelarOrdemRequest request, String usuario) { OrdemTrabalhoCarga ordem = buscar(id); ordem.cancelar(request.motivo(), usuario); return resposta(ordem); }

    private OrdemTrabalhoCarga buscar(UUID id) { return ordemRepositorio.findById(id).orElseThrow(() -> new IllegalArgumentException("Ordem de trabalho não encontrada.")); }

    private OrdemResposta resposta(OrdemTrabalhoCarga ordem) {
        List<ItemResposta> itens = ordem.getItens().stream().map(this::itemResposta).toList();
        List<EventoResposta> eventos = ordem.getEventos().stream().map(this::eventoResposta).toList();
        return new OrdemResposta(ordem.getId(), ordem.getNumero(), ordem.getTipo(), ordem.getStatus(), ordem.getPrioridade(), ordem.getJanelaInicio(), ordem.getJanelaFim(), ordem.getLocal(), ordem.getEquipeId(), ordem.getEquipamentoId(), ordem.getMotivoCancelamento(), ordem.getVersao(), itens, eventos);
    }

    private ItemResposta itemResposta(ItemOrdemTrabalhoCarga item) { return new ItemResposta(item.getId(), item.getLote().getId(), item.getLote().getCodigo(), item.getQuantidade(), item.getObservacao()); }
    private EventoResposta eventoResposta(EventoOrdemTrabalhoCarga evento) { return new EventoResposta(evento.getId(), evento.getTipo(), evento.getDescricao(), evento.getUsuario(), evento.getOcorridoEm()); }
}
