package br.com.cloudport.servicoautenticacao.app.seguranca;

import br.com.cloudport.servicoautenticacao.app.seguranca.dto.DiretrizSegurancaDTO;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SegurancaPoliticaConsultaServico {

    private final PoliticaSegurancaRepositorio politicaSegurancaRepositorio;
    private final SanitizadorConteudoSeguranca sanitizadorConteudoSeguranca;

    public SegurancaPoliticaConsultaServico(
            PoliticaSegurancaRepositorio politicaSegurancaRepositorio,
            SanitizadorConteudoSeguranca sanitizadorConteudoSeguranca
    ) {
        this.politicaSegurancaRepositorio = politicaSegurancaRepositorio;
        this.sanitizadorConteudoSeguranca = sanitizadorConteudoSeguranca;
    }

    public List<DiretrizSegurancaDTO> listarDiretrizes(String versao, String ordenacao) {
        Sort criterioOrdenacao = definirOrdenacao(ordenacao);
        List<PoliticaSeguranca> politicas;
        if (StringUtils.hasText(versao)) {
            politicas = politicaSegurancaRepositorio.findByAtivoTrueAndVersao(versao.trim(), criterioOrdenacao);
        } else {
            politicas = politicaSegurancaRepositorio.findByAtivoTrue(criterioOrdenacao);
        }

        return politicas.stream()
                .map(politica -> DiretrizSegurancaDTO.fromModelo(
                        politica,
                        sanitizadorConteudoSeguranca.sanitizar(politica.getTitulo()),
                        sanitizadorConteudoSeguranca.sanitizar(politica.getDescricao()),
                        sanitizadorConteudoSeguranca.sanitizar(politica.getVersao())
                ))
                .collect(Collectors.toList());
    }

    private Sort definirOrdenacao(String ordenacao) {
        if (!StringUtils.hasText(ordenacao)) {
            return Sort.by(Sort.Order.asc("ordem"));
        }

        String[] partes = ordenacao.toLowerCase().split(",");
        String campo = partes[0].trim();
        Sort.Direction direcao = Sort.Direction.ASC;
        if (partes.length > 1) {
            String direcaoInformada = partes[1].trim();
            if ("desc".equals(direcaoInformada)) {
                direcao = Sort.Direction.DESC;
            } else if (!"asc".equals(direcaoInformada)) {
                throw new ParametroSegurancaInvalidoException("Direção de ordenação inválida informada.");
            }
        }

        switch (campo) {
            case "ordem":
                return Sort.by(new Sort.Order(direcao, "ordem"));
            case "titulo":
                return Sort.by(new Sort.Order(direcao, "titulo"));
            case "versao":
                return Sort.by(new Sort.Order(direcao, "versao"));
            default:
                throw new ParametroSegurancaInvalidoException("Campo de ordenação inválido informado.");
        }
    }
}
