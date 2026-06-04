package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.ItemCargaSiderurgica;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusItemCarga;
import br.com.cloudport.serviconaviosiderurgico.dto.ItemCargaSiderurgicaDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.ItemCargaSiderurgicaRepositorio;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemCargaSiderurgicaServico {

    private final ItemCargaSiderurgicaRepositorio repositorio;
    private final OperacaoSiderurgicaServico operacaoServico;

    public ItemCargaSiderurgicaServico(ItemCargaSiderurgicaRepositorio repositorio, OperacaoSiderurgicaServico operacaoServico) {
        this.repositorio = repositorio;
        this.operacaoServico = operacaoServico;
    }

    @Transactional(readOnly = true)
    public List<ItemCargaSiderurgicaDTO> listar(Long operacaoId) {
        return repositorio.findByOperacaoIdOrderBySequenciaOperacionalAscIdAsc(operacaoId)
                .stream().map(ItemCargaSiderurgicaDTO::de).collect(Collectors.toList());
    }

    @Transactional
    public ItemCargaSiderurgicaDTO criar(Long operacaoId, ItemCargaSiderurgicaDTO dto) {
        String lote = dto.codigoLote().trim().toUpperCase(Locale.ROOT);
        if (repositorio.existsByOperacaoIdAndCodigoLoteIgnoreCase(operacaoId, lote)) {
            throw new IllegalArgumentException("Ja existe item com este lote na operacao.");
        }
        ItemCargaSiderurgica item = new ItemCargaSiderurgica();
        item.setOperacao(operacaoServico.buscarEntidade(operacaoId));
        item.setCodigoLote(lote);
        item.setTipoCarga(dto.tipoCarga());
        item.setProduto(dto.produto().trim());
        item.setQuantidade(dto.quantidade());
        item.setPesoUnitarioToneladas(dto.pesoUnitarioToneladas());
        item.setPesoTotalToneladas(dto.pesoTotalToneladas());
        item.setPorao(dto.porao());
        item.setPosicaoBordo(dto.posicaoBordo());
        item.setOrigemPatio(dto.origemPatio());
        item.setDestinoPatio(dto.destinoPatio());
        item.setSequenciaOperacional(dto.sequenciaOperacional());
        item.setStatus(dto.status() == null ? StatusItemCarga.PLANEJADO : dto.status());
        return ItemCargaSiderurgicaDTO.de(repositorio.save(item));
    }
}
