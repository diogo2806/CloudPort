package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.contracts.api.PaginaResposta;
import br.com.cloudport.serviconaviosiderurgico.dominio.FaseVisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dominio.VisitaNavio;
import br.com.cloudport.serviconaviosiderurgico.dto.VisitaNavioResumoDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.VisitaNavioRepositorio;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import javax.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ConsultaPublicaVisitaNavioServico {

    private static final int TAMANHO_MAXIMO = 200;
    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
            "id", "codigoVisita", "eta", "etb", "etd", "fase", "criadoEm", "atualizadoEm"
    );

    private final VisitaNavioRepositorio visitaRepositorio;

    public ConsultaPublicaVisitaNavioServico(VisitaNavioRepositorio visitaRepositorio) {
        this.visitaRepositorio = visitaRepositorio;
    }

    @Transactional(readOnly = true)
    public PaginaResposta<VisitaNavioResumoDTO> listar(FaseVisitaNavio fase,
                                                        LocalDateTime dataInicio,
                                                        LocalDateTime dataFim,
                                                        Long navioId,
                                                        String codigoVisita,
                                                        String berco,
                                                        String linhaOperadora,
                                                        int pagina,
                                                        int tamanho,
                                                        String ordenarPor,
                                                        Sort.Direction direcao) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanhoSeguro = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO);
        String campoOrdenacao = CAMPOS_ORDENACAO.contains(ordenarPor) ? ordenarPor : "eta";
        Sort.Direction direcaoSegura = direcao == null ? Sort.Direction.DESC : direcao;
        PageRequest pageable = PageRequest.of(paginaSegura, tamanhoSeguro, Sort.by(direcaoSegura, campoOrdenacao));

        Specification<VisitaNavio> specification = Specification.where(null);
        if (fase != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("fase"), fase));
        }
        if (dataInicio != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eta"), dataInicio));
        }
        if (dataFim != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("eta"), dataFim));
        }
        if (navioId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.join("navio", JoinType.INNER).get("id"), navioId));
        }
        if (StringUtils.hasText(codigoVisita)) {
            String valor = "%" + codigoVisita.trim().toUpperCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.like(cb.upper(root.get("codigoVisita")), valor));
        }
        if (StringUtils.hasText(berco)) {
            String valor = "%" + berco.trim().toUpperCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.upper(root.get("bercoPrevisto")), valor),
                    cb.like(cb.upper(root.get("bercoAtual")), valor)
            ));
        }
        if (StringUtils.hasText(linhaOperadora)) {
            String valor = "%" + linhaOperadora.trim().toUpperCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.like(cb.upper(root.get("linhaOperadora")), valor));
        }

        Page<VisitaNavioResumoDTO> resultado = visitaRepositorio.findAll(specification, pageable)
                .map(VisitaNavioResumoDTO::de);
        return new PaginaResposta<>(
                resultado.getContent(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                resultado.isFirst(),
                resultado.isLast()
        );
    }
}
