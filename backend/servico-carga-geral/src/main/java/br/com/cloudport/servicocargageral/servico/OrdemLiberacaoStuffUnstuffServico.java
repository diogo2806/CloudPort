package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.ConsumoOrdemLiberacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.OrdemLiberacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dto.OrdemLiberacaoStuffUnstuffDTOs.OrigemOperacionalRequest;
import br.com.cloudport.servicocargageral.repositorio.ConsumoOrdemLiberacaoStuffUnstuffRepositorio;
import br.com.cloudport.servicocargageral.repositorio.OrdemLiberacaoStuffUnstuffRepositorio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrdemLiberacaoStuffUnstuffServico {

    private final OrdemLiberacaoStuffUnstuffRepositorio repositorio;
    private final ConsumoOrdemLiberacaoStuffUnstuffRepositorio consumoRepositorio;

    public OrdemLiberacaoStuffUnstuffServico(
            OrdemLiberacaoStuffUnstuffRepositorio repositorio,
            ConsumoOrdemLiberacaoStuffUnstuffRepositorio consumoRepositorio) {
        this.repositorio = repositorio;
        this.consumoRepositorio = consumoRepositorio;
    }

    @Transactional
    public void reservar(UUID operacaoId, OrigemOperacionalRequest origem, BigDecimal quantidadePlanejada) {
        if (origem.vigenteAte().isBefore(origem.vigenteDe())) {
            throw conflito("A vigência final da origem operacional deve ser posterior à inicial.");
        }
        OrdemLiberacaoStuffUnstuff ordem = new OrdemLiberacaoStuffUnstuff();
        ordem.setOperacaoId(operacaoId);
        ordem.setTipoOrigem(origem.tipo().name());
        ordem.setIdentificadorOrigem(origem.identificador().trim());
        ordem.setVersaoOrigem(origem.versao());
        ordem.setQuantidadeAutorizada(origem.quantidadeAutorizada());
        ordem.setVigenteDe(origem.vigenteDe());
        ordem.setVigenteAte(origem.vigenteAte());
        ordem.setHold(origem.hold());
        ordem.setSnapshotOrigem(origem.snapshot());
        executar(() -> ordem.reservar(quantidadePlanejada, OffsetDateTime.now()));
        repositorio.saveAndFlush(ordem);
    }

    @Transactional
    public void validarParaInicio(UUID operacaoId) {
        OrdemLiberacaoStuffUnstuff ordem = buscarComBloqueio(operacaoId);
        executar(() -> ordem.validarParaInicio(OffsetDateTime.now()));
    }

    @Transactional
    public void consumir(UUID operacaoId, UUID commandId, BigDecimal quantidade) {
        OrdemLiberacaoStuffUnstuff ordem = buscarComBloqueio(operacaoId);
        if (consumoRepositorio.existsByOperacaoIdAndCommandId(operacaoId, commandId)) {
            return;
        }
        executar(() -> ordem.consumir(quantidade));
        ConsumoOrdemLiberacaoStuffUnstuff consumo = new ConsumoOrdemLiberacaoStuffUnstuff();
        consumo.setOperacaoId(operacaoId);
        consumo.setCommandId(commandId);
        consumo.setQuantidade(quantidade);
        repositorio.save(ordem);
        consumoRepositorio.save(consumo);
    }

    @Transactional
    public void concluir(UUID operacaoId) {
        OrdemLiberacaoStuffUnstuff ordem = buscarComBloqueio(operacaoId);
        executar(ordem::concluir);
        repositorio.save(ordem);
    }

    @Transactional
    public void compensarCancelamento(UUID operacaoId) {
        OrdemLiberacaoStuffUnstuff ordem = buscarComBloqueio(operacaoId);
        ordem.compensarCancelamento();
        repositorio.save(ordem);
    }

    private OrdemLiberacaoStuffUnstuff buscarComBloqueio(UUID operacaoId) {
        return repositorio.findComBloqueioByOperacaoId(operacaoId)
                .orElseThrow(() -> conflito("A operação não possui origem operacional persistida."));
    }

    private void executar(Runnable acao) {
        try {
            acao.run();
        } catch (IllegalStateException exception) {
            throw conflito(exception.getMessage());
        }
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }
}
