package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemOperacaoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoMovimentoNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.BloqueioItemNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.ItemOperacaoNavioDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemOperacaoNavioRepositorio;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemOperacaoNavioServico {

    private final ItemOperacaoNavioRepositorio repositorio;
    private final VisitaNavioServico visitaServico;

    public ItemOperacaoNavioServico(ItemOperacaoNavioRepositorio repositorio, VisitaNavioServico visitaServico) {
        this.repositorio = repositorio;
        this.visitaServico = visitaServico;
    }

    @Transactional(readOnly = true)
    public List<ItemOperacaoNavioDTO> listar(Long visitaId, TipoMovimentoNavio tipoMovimento, StatusItemCarga status) {
        visitaServico.buscarEntidade(visitaId);
        return repositorio.findByVisitaNavioIdOrderBySequenciaOperacionalAscIdAsc(visitaId).stream()
                .filter(item -> tipoMovimento == null || item.getTipoMovimento() == tipoMovimento)
                .filter(item -> status == null || item.getStatus() == status)
                .map(ItemOperacaoNavioDTO::de)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ItemOperacaoNavio buscarEntidade(Long visitaId, Long itemId) {
        ItemOperacaoNavio item = repositorio.findById(itemId).orElseThrow(() -> new IllegalArgumentException("Item operacional nao encontrado."));
        if (!Objects.equals(item.getVisitaNavio().getId(), visitaId)) {
            throw new IllegalArgumentException("Item nao pertence a visita informada.");
        }
        return item;
    }

    @Transactional
    public ItemOperacaoNavioDTO criar(Long visitaId, ItemOperacaoNavioDTO dto) {
        VisitaNavio visita = visitaServico.buscarEntidade(visitaId);
        visitaServico.validarVisitaEditavel(visita);
        String lote = normalizarLote(dto.codigoLote());
        if (repositorio.existsByVisitaNavioIdAndTipoMovimentoAndCodigoLoteIgnoreCase(visitaId, dto.tipoMovimento(), lote)) {
            throw new IllegalArgumentException("Ja existe lote com este movimento dentro da visita.");
        }
        ItemOperacaoNavio item = new ItemOperacaoNavio();
        item.setVisitaNavio(visita);
        preencher(item, dto, lote);
        ItemOperacaoNavio salvo = repositorio.save(item);
        visitaServico.registrarEvento(visita, salvo, "ITEM_CRIADO", "Item operacional criado: " + lote + ".", "sistema", null, dto.tipoMovimento().name());
        return ItemOperacaoNavioDTO.de(salvo);
    }

    @Transactional
    public ItemOperacaoNavioDTO atualizar(Long visitaId, Long itemId, ItemOperacaoNavioDTO dto) {
        ItemOperacaoNavio item = buscarEntidade(visitaId, itemId);
        visitaServico.validarVisitaEditavel(item.getVisitaNavio());
        String lote = normalizarLote(dto.codigoLote());
        if (!item.getCodigoLote().equalsIgnoreCase(lote) || item.getTipoMovimento() != dto.tipoMovimento()) {
            if (repositorio.existsByVisitaNavioIdAndTipoMovimentoAndCodigoLoteIgnoreCase(visitaId, dto.tipoMovimento(), lote)) {
                throw new IllegalArgumentException("Ja existe lote com este movimento dentro da visita.");
            }
        }
        String antes = item.getStatus().name();
        preencher(item, dto, lote);
        ItemOperacaoNavio salvo = repositorio.save(item);
        visitaServico.registrarEvento(item.getVisitaNavio(), salvo, "ITEM_ATUALIZADO", "Item operacional atualizado: " + lote + ".", "sistema", antes, salvo.getStatus().name());
        return ItemOperacaoNavioDTO.de(salvo);
    }

    @Transactional
    public ItemOperacaoNavioDTO alterarStatus(Long visitaId, Long itemId, StatusItemCarga status, String usuario, String observacao) {
        ItemOperacaoNavio item = buscarEntidade(visitaId, itemId);
        visitaServico.validarVisitaEditavel(item.getVisitaNavio());
        StatusItemCarga anterior = item.getStatus();
        item.setStatus(status);
        if (status != StatusItemCarga.BLOQUEADO) {
            item.setMotivoBloqueio(null);
        }
        ItemOperacaoNavio salvo = repositorio.save(item);
        String descricao = observacao == null || observacao.isBlank()
                ? "Status do item " + item.getCodigoLote() + " alterado de " + anterior + " para " + status + "."
                : observacao.trim();
        visitaServico.registrarEvento(item.getVisitaNavio(), salvo, "ITEM_STATUS_ALTERADO", descricao, usuario, anterior.name(), status.name());
        return ItemOperacaoNavioDTO.de(salvo);
    }

    @Transactional
    public ItemOperacaoNavioDTO alterarBloqueio(Long visitaId, Long itemId, BloqueioItemNavioDTO dto) {
        ItemOperacaoNavio item = buscarEntidade(visitaId, itemId);
        visitaServico.validarVisitaEditavel(item.getVisitaNavio());
        StatusItemCarga anterior = item.getStatus();
        if (dto.bloqueado()) {
            if (dto.motivo() == null || dto.motivo().isBlank()) {
                throw new IllegalArgumentException("Motivo do bloqueio e obrigatorio.");
            }
            item.setStatus(StatusItemCarga.BLOQUEADO);
            item.setMotivoBloqueio(dto.motivo().trim());
        } else {
            if (item.getStatus() == StatusItemCarga.BLOQUEADO) {
                item.setStatus(StatusItemCarga.LIBERADO);
            }
            item.setMotivoBloqueio(null);
        }
        ItemOperacaoNavio salvo = repositorio.save(item);
        visitaServico.registrarEvento(
                item.getVisitaNavio(),
                salvo,
                dto.bloqueado() ? "ITEM_BLOQUEADO" : "ITEM_DESBLOQUEADO",
                dto.bloqueado() ? item.getMotivoBloqueio() : "Bloqueio removido.",
                dto.usuario(),
                anterior.name(),
                salvo.getStatus().name()
        );
        return ItemOperacaoNavioDTO.de(salvo);
    }

    @Transactional
    public void excluir(Long visitaId, Long itemId) {
        ItemOperacaoNavio item = buscarEntidade(visitaId, itemId);
        visitaServico.validarVisitaEditavel(item.getVisitaNavio());
        if (item.getStatus() == StatusItemCarga.EM_MOVIMENTO || item.getStatus() == StatusItemCarga.OPERADO) {
            throw new IllegalArgumentException("Nao e permitido excluir item em movimento ou ja operado.");
        }
        repositorio.delete(item);
        visitaServico.registrarEvento(item.getVisitaNavio(), item, "ITEM_EXCLUIDO", "Item operacional excluido: " + item.getCodigoLote() + ".", "sistema", item.getCodigoLote(), null);
    }

    private void preencher(ItemOperacaoNavio item, ItemOperacaoNavioDTO dto, String lote) {
        item.setTipoMovimento(dto.tipoMovimento());
        item.setCodigoLote(lote);
        item.setProduto(dto.produto().trim());
        item.setTipoCarga(dto.tipoCarga());
        item.setQuantidade(dto.quantidade());
        item.setPesoUnitarioToneladas(dto.pesoUnitarioToneladas());
        item.setPesoTotalToneladas(dto.pesoTotalToneladas());
        item.setPoraoPlanejado(dto.poraoPlanejado());
        item.setPoraoReal(dto.poraoReal());
        item.setPosicaoPlanejada(limpar(dto.posicaoPlanejada()));
        item.setPosicaoReal(limpar(dto.posicaoReal()));
        item.setOrigemPatio(limpar(dto.origemPatio()));
        item.setDestinoPatio(limpar(dto.destinoPatio()));
        item.setSequenciaOperacional(dto.sequenciaOperacional());
        item.setStatus(dto.status() == null ? StatusItemCarga.PLANEJADO : dto.status());
        item.setMotivoBloqueio(limpar(dto.motivoBloqueio()));
        item.setObservacoes(limpar(dto.observacoes()));
    }

    private String normalizarLote(String codigoLote) {
        if (codigoLote == null || codigoLote.isBlank()) {
            throw new IllegalArgumentException("Codigo do lote e obrigatorio.");
        }
        return codigoLote.trim().toUpperCase(Locale.ROOT);
    }

    private String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
