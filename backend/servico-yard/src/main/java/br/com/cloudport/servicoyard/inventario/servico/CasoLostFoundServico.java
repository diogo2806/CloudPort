package br.com.cloudport.servicoyard.inventario.servico;

import br.com.cloudport.servicoyard.inventario.modelo.CasoLostFound;
import br.com.cloudport.servicoyard.inventario.modelo.StatusCasoLostFound;
import br.com.cloudport.servicoyard.inventario.modelo.TipoCasoLostFound;
import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import br.com.cloudport.servicoyard.inventario.repositorio.CasoLostFoundRepositorio;
import br.com.cloudport.servicoyard.inventario.repositorio.UnidadeInventarioRepositorio;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class CasoLostFoundServico {

    private static final EnumSet<StatusCasoLostFound> STATUS_ATIVOS = EnumSet.of(
            StatusCasoLostFound.ABERTO,
            StatusCasoLostFound.EM_INVESTIGACAO,
            StatusCasoLostFound.ASSOCIADO);

    private final CasoLostFoundRepositorio casoRepositorio;
    private final UnidadeInventarioRepositorio unidadeRepositorio;

    public CasoLostFoundServico(CasoLostFoundRepositorio casoRepositorio,
                                UnidadeInventarioRepositorio unidadeRepositorio) {
        this.casoRepositorio = casoRepositorio;
        this.unidadeRepositorio = unidadeRepositorio;
    }

    public CasoLostFound abrir(String identificacao,
                               TipoCasoLostFound tipoCaso,
                               String evidencia,
                               String abertoPor) {
        String identificacaoNormalizada = obrigatorio(identificacao, "Identificação lida")
                .toUpperCase(Locale.ROOT);
        if (tipoCaso == null) {
            throw new IllegalArgumentException("Tipo do caso deve ser informado");
        }
        casoRepositorio.findFirstByIdentificacaoLidaIgnoreCaseAndStatusIn(identificacaoNormalizada, STATUS_ATIVOS)
                .ifPresent(caso -> {
                    throw new IllegalStateException("Já existe caso ativo para a identificação informada");
                });

        CasoLostFound caso = new CasoLostFound();
        caso.setIdentificacaoLida(identificacaoNormalizada);
        caso.setTipoCaso(tipoCaso);
        caso.setStatus(StatusCasoLostFound.ABERTO);
        caso.setEvidencia(normalizar(evidencia));
        caso.setAbertoPor(obrigatorio(abertoPor, "Usuário de abertura"));
        unidadeRepositorio.findByIdentificacaoIgnoreCase(identificacaoNormalizada).ifPresent(caso::setUnidade);
        return casoRepositorio.save(caso);
    }

    public CasoLostFound investigar(Long id, String responsavel, String evidencia) {
        CasoLostFound caso = obter(id);
        exigirAtivo(caso);
        caso.setResponsavel(obrigatorio(responsavel, "Responsável"));
        caso.setEvidencia(acumular(caso.getEvidencia(), evidencia));
        caso.setStatus(StatusCasoLostFound.EM_INVESTIGACAO);
        caso.setInvestigacaoIniciadaEm(LocalDateTime.now());
        return casoRepositorio.save(caso);
    }

    public CasoLostFound associar(Long id, Long unidadeId, String evidencia) {
        CasoLostFound caso = obter(id);
        exigirAtivo(caso);
        UnidadeInventario unidade = unidadeRepositorio.findById(unidadeId)
                .orElseThrow(() -> new NoSuchElementException("Unidade canônica não encontrada"));
        caso.setUnidade(unidade);
        caso.setEvidencia(acumular(caso.getEvidencia(), evidencia));
        caso.setStatus(StatusCasoLostFound.ASSOCIADO);
        caso.setAssociadaEm(LocalDateTime.now());
        return casoRepositorio.save(caso);
    }

    public CasoLostFound regularizar(Long id, String decisaoFinal) {
        CasoLostFound caso = obter(id);
        if (caso.getStatus() != StatusCasoLostFound.ASSOCIADO || caso.getUnidade() == null) {
            throw new IllegalStateException("O caso deve estar associado a uma unidade canônica");
        }
        UnidadeInventario unidade = caso.getUnidade();
        unidade.setEstado(UnidadeInventario.EstadoUnidade.ATIVA);
        unidade.setCondicao(UnidadeInventario.CondicaoEquipamento.OPERACIONAL);
        unidadeRepositorio.save(unidade);
        caso.setDecisaoFinal(obrigatorio(decisaoFinal, "Decisão final"));
        caso.setStatus(StatusCasoLostFound.REGULARIZADO);
        caso.setRegularizadaEm(LocalDateTime.now());
        return casoRepositorio.save(caso);
    }

    public CasoLostFound baixar(Long id, String decisaoFinal) {
        CasoLostFound caso = obter(id);
        exigirAtivo(caso);
        if (caso.getUnidade() != null) {
            caso.getUnidade().setEstado(UnidadeInventario.EstadoUnidade.INATIVA);
            unidadeRepositorio.save(caso.getUnidade());
        }
        caso.setDecisaoFinal(obrigatorio(decisaoFinal, "Decisão de baixa"));
        caso.setStatus(StatusCasoLostFound.BAIXADO);
        caso.setBaixadaEm(LocalDateTime.now());
        return casoRepositorio.save(caso);
    }

    public CasoLostFound encerrar(Long id, String decisaoFinal) {
        CasoLostFound caso = obter(id);
        if (caso.getStatus() != StatusCasoLostFound.REGULARIZADO
                && caso.getStatus() != StatusCasoLostFound.BAIXADO) {
            throw new IllegalStateException("Somente casos regularizados ou baixados podem ser encerrados");
        }
        caso.setDecisaoFinal(acumular(caso.getDecisaoFinal(), decisaoFinal));
        caso.setStatus(StatusCasoLostFound.ENCERRADO);
        caso.setEncerradaEm(LocalDateTime.now());
        return casoRepositorio.save(caso);
    }

    @Transactional(readOnly = true)
    public CasoLostFound obter(Long id) {
        return casoRepositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Caso Lost & Found/TBD não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<CasoLostFound> listar() {
        return casoRepositorio.findAllByOrderByAbertoEmDesc();
    }

    private void exigirAtivo(CasoLostFound caso) {
        if (!STATUS_ATIVOS.contains(caso.getStatus())) {
            throw new IllegalStateException("Caso não está ativo para esta operação");
        }
    }

    private String acumular(String atual, String adicional) {
        if (!StringUtils.hasText(adicional)) {
            return atual;
        }
        return StringUtils.hasText(atual) ? atual + "\n" + adicional.trim() : adicional.trim();
    }

    private String obrigatorio(String valor, String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new IllegalArgumentException(campo + " deve ser informado");
        }
        return valor.trim();
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
