package br.com.cloudport.servicoyard.patio.custodia.servico;

import br.com.cloudport.servicoyard.patio.custodia.dto.CustodiaExchangeAreaComandoDto;
import br.com.cloudport.servicoyard.patio.custodia.dto.CustodiaExchangeAreaRespostaDto;
import br.com.cloudport.servicoyard.patio.custodia.modelo.CustodiaExchangeArea;
import br.com.cloudport.servicoyard.patio.custodia.modelo.StatusCustodiaExchangeArea;
import br.com.cloudport.servicoyard.patio.custodia.repositorio.CustodiaExchangeAreaRepositorio;
import br.com.cloudport.servicoyard.patio.servico.PublicadorEventoMovimentoPatio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustodiaExchangeAreaServico {

    private static final Set<StatusCustodiaExchangeArea> STATUS_ATIVOS = Set.of(
            StatusCustodiaExchangeArea.ENTREGUE,
            StatusCustodiaExchangeArea.DIVERGENTE);

    private final CustodiaExchangeAreaRepositorio repositorio;
    private final PublicadorEventoMovimentoPatio publicadorEvento;

    public CustodiaExchangeAreaServico(CustodiaExchangeAreaRepositorio repositorio,
                                        PublicadorEventoMovimentoPatio publicadorEvento) {
        this.repositorio = repositorio;
        this.publicadorEvento = publicadorEvento;
    }

    @Transactional(readOnly = true)
    public List<CustodiaExchangeAreaRespostaDto> listar(StatusCustodiaExchangeArea status) {
        List<CustodiaExchangeArea> custodias = status == null
                ? repositorio.findAllByOrderByAtualizadoEmDesc()
                : repositorio.findAllByStatusOrderByAtualizadoEmDesc(status);
        return custodias.stream().map(CustodiaExchangeAreaRespostaDto::deEntidade).toList();
    }

    @Transactional
    public CustodiaExchangeAreaRespostaDto entregarNaExchangeArea(CustodiaExchangeAreaComandoDto comando) {
        CustodiaExchangeArea existentePorChave = repositorio
                .findByChaveIdempotenciaEntrega(comando.getChaveIdempotencia())
                .orElse(null);
        if (existentePorChave != null) {
            validarMesmaEntrega(existentePorChave, comando);
            return CustodiaExchangeAreaRespostaDto.deEntidade(existentePorChave);
        }

        repositorio.findFirstByCodigoUnidadeIgnoreCaseAndStatusInOrderByCriadoEmDesc(
                        comando.getCodigoUnidade(), STATUS_ATIVOS)
                .ifPresent(custodia -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            custodia.isBloqueada()
                                    ? "A unidade possui divergência de custódia bloqueante na exchange area."
                                    : "A unidade já possui uma entrega de custódia aguardando recebimento.");
                });

        CustodiaExchangeArea custodia = new CustodiaExchangeArea();
        custodia.setCodigoUnidade(normalizar(comando.getCodigoUnidade()));
        custodia.setArea(normalizar(comando.getArea()));
        custodia.setPosicao(normalizar(comando.getPosicao()));
        custodia.setEquipamentoEntrega(normalizar(comando.getEquipamento()));
        custodia.setOperadorEntrega(limpar(comando.getOperador()));
        custodia.setCondicaoEntrega(normalizar(comando.getCondicao()));
        custodia.setLacresEntrega(normalizarLacres(comando.getLacres()));
        custodia.setEntregueEm(LocalDateTime.now());
        custodia.setChaveIdempotenciaEntrega(limpar(comando.getChaveIdempotencia()));
        custodia.setStatus(StatusCustodiaExchangeArea.ENTREGUE);
        custodia.setBloqueada(false);

        try {
            CustodiaExchangeArea salva = repositorio.saveAndFlush(custodia);
            CustodiaExchangeAreaRespostaDto resposta = CustodiaExchangeAreaRespostaDto.deEntidade(salva);
            publicadorEvento.entregarNaExchangeArea(resposta);
            return resposta;
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A entrega de custódia já foi registrada ou existe handoff ativo para a unidade.", ex);
        }
    }

    @Transactional
    public CustodiaExchangeAreaRespostaDto receberDaExchangeArea(Long custodiaId,
                                                                  CustodiaExchangeAreaComandoDto comando) {
        CustodiaExchangeArea existentePorChave = repositorio
                .findByChaveIdempotenciaRecebimento(comando.getChaveIdempotencia())
                .orElse(null);
        if (existentePorChave != null) {
            if (!Objects.equals(existentePorChave.getId(), custodiaId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A chave idempotente de recebimento já foi usada em outra custódia.");
            }
            validarMesmoRecebimento(existentePorChave, comando);
            return CustodiaExchangeAreaRespostaDto.deEntidade(existentePorChave);
        }

        CustodiaExchangeArea custodia = repositorio.findByIdForUpdate(custodiaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Custódia de exchange area não encontrada."));
        if (custodia.getStatus() != StatusCustodiaExchangeArea.ENTREGUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    custodia.getStatus() == StatusCustodiaExchangeArea.DIVERGENTE
                            ? "A custódia está bloqueada por divergência e não pode ser recebida novamente."
                            : "A custódia já foi recebida e não pode mudar novamente.");
        }

        custodia.setEquipamentoRecebimento(normalizar(comando.getEquipamento()));
        custodia.setOperadorRecebimento(limpar(comando.getOperador()));
        custodia.setCondicaoRecebimento(normalizar(comando.getCondicao()));
        custodia.setLacresRecebimento(normalizarLacres(comando.getLacres()));
        custodia.setRecebidoEm(LocalDateTime.now());
        custodia.setChaveIdempotenciaRecebimento(limpar(comando.getChaveIdempotencia()));

        List<String> divergencias = identificarDivergencias(custodia, comando);
        if (divergencias.isEmpty()) {
            custodia.setStatus(StatusCustodiaExchangeArea.RECEBIDA);
            custodia.setBloqueada(false);
            custodia.setMotivoDivergencia(null);
        } else {
            custodia.setStatus(StatusCustodiaExchangeArea.DIVERGENTE);
            custodia.setBloqueada(true);
            custodia.setMotivoDivergencia(String.join("; ", divergencias));
        }

        try {
            CustodiaExchangeArea salva = repositorio.saveAndFlush(custodia);
            CustodiaExchangeAreaRespostaDto resposta = CustodiaExchangeAreaRespostaDto.deEntidade(salva);
            publicadorEvento.receberDaExchangeArea(resposta);
            return resposta;
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O recebimento de custódia já foi registrado.", ex);
        }
    }

    private List<String> identificarDivergencias(CustodiaExchangeArea custodia,
                                                   CustodiaExchangeAreaComandoDto comando) {
        List<String> divergencias = new ArrayList<>();
        comparar(divergencias, "unidade", custodia.getCodigoUnidade(), normalizar(comando.getCodigoUnidade()));
        comparar(divergencias, "área", custodia.getArea(), normalizar(comando.getArea()));
        comparar(divergencias, "posição", custodia.getPosicao(), normalizar(comando.getPosicao()));
        comparar(divergencias, "condição", custodia.getCondicaoEntrega(), normalizar(comando.getCondicao()));
        comparar(divergencias, "lacres", custodia.getLacresEntrega(), normalizarLacres(comando.getLacres()));
        return divergencias;
    }

    private void comparar(List<String> divergencias, String campo, String esperado, String recebido) {
        if (!Objects.equals(esperado, recebido)) {
            divergencias.add(campo + " divergente: esperado " + esperado + ", recebido " + recebido);
        }
    }

    private void validarMesmaEntrega(CustodiaExchangeArea custodia, CustodiaExchangeAreaComandoDto comando) {
        boolean igual = Objects.equals(custodia.getCodigoUnidade(), normalizar(comando.getCodigoUnidade()))
                && Objects.equals(custodia.getArea(), normalizar(comando.getArea()))
                && Objects.equals(custodia.getPosicao(), normalizar(comando.getPosicao()))
                && Objects.equals(custodia.getEquipamentoEntrega(), normalizar(comando.getEquipamento()))
                && Objects.equals(custodia.getOperadorEntrega(), limpar(comando.getOperador()))
                && Objects.equals(custodia.getCondicaoEntrega(), normalizar(comando.getCondicao()))
                && Objects.equals(custodia.getLacresEntrega(), normalizarLacres(comando.getLacres()));
        if (!igual) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A chave idempotente da entrega foi reutilizada com conteúdo diferente.");
        }
    }

    private void validarMesmoRecebimento(CustodiaExchangeArea custodia, CustodiaExchangeAreaComandoDto comando) {
        boolean igual = Objects.equals(custodia.getCodigoUnidade(), normalizar(comando.getCodigoUnidade()))
                && Objects.equals(custodia.getArea(), normalizar(comando.getArea()))
                && Objects.equals(custodia.getPosicao(), normalizar(comando.getPosicao()))
                && Objects.equals(custodia.getEquipamentoRecebimento(), normalizar(comando.getEquipamento()))
                && Objects.equals(custodia.getOperadorRecebimento(), limpar(comando.getOperador()))
                && Objects.equals(custodia.getCondicaoRecebimento(), normalizar(comando.getCondicao()))
                && Objects.equals(custodia.getLacresRecebimento(), normalizarLacres(comando.getLacres()));
        if (!igual) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A chave idempotente do recebimento foi reutilizada com conteúdo diferente.");
        }
    }

    private String normalizarLacres(String lacres) {
        return List.of(limpar(lacres).split("[;,\\s]+"))
                .stream()
                .filter(StringUtils::hasText)
                .map(this::normalizar)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(","));
    }

    private String normalizar(String valor) {
        return limpar(valor).toUpperCase(Locale.ROOT);
    }

    private String limpar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : "";
    }
}
