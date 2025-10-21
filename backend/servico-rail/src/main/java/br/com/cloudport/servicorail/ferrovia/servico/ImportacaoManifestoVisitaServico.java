package br.com.cloudport.servicorail.ferrovia.servico;

import br.com.cloudport.servicorail.comum.sanitizacao.SanitizadorEntrada;
import br.com.cloudport.servicorail.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicorail.ferrovia.dto.OperacaoConteinerVisitaRequisicaoDto;
import br.com.cloudport.servicorail.ferrovia.dto.VagaoVisitaRequisicaoDto;
import br.com.cloudport.servicorail.ferrovia.dto.VisitaTremRequisicaoDto;
import br.com.cloudport.servicorail.ferrovia.dto.VisitaTremRespostaDto;
import br.com.cloudport.servicorail.ferrovia.importacao.ArquivoManifestoVisitaParser;
import br.com.cloudport.servicorail.ferrovia.importacao.ResultadoManifestoVisita;
import br.com.cloudport.servicorail.ferrovia.importacao.ResultadoManifestoVisita.VagaoManifestoImportado;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ImportacaoManifestoVisitaServico {

    private final List<ArquivoManifestoVisitaParser> parsers;
    private final VisitaTremServico visitaTremServico;
    private final SanitizadorEntrada sanitizadorEntrada;

    public ImportacaoManifestoVisitaServico(List<ArquivoManifestoVisitaParser> parsers,
                                            VisitaTremServico visitaTremServico,
                                            SanitizadorEntrada sanitizadorEntrada) {
        this.parsers = parsers;
        this.visitaTremServico = visitaTremServico;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    public VisitaTremRespostaDto importarManifesto(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nenhum arquivo foi enviado para importação.");
        }
        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não foi possível ler o arquivo enviado.");
        }
        String nomeArquivo = StringUtils.hasText(arquivo.getOriginalFilename())
                ? StringUtils.cleanPath(Objects.requireNonNull(arquivo.getOriginalFilename()))
                : "arquivo-manifesto";

        ArquivoManifestoVisitaParser parser = parsers.stream()
                .filter(item -> item.suporta(nomeArquivo, conteudo))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Formato de arquivo não suportado para importação."));

        ResultadoManifestoVisita resultado = parser.parse(nomeArquivo, conteudo);
        ajustarCamposPadrao(resultado);
        VisitaTremRequisicaoDto dto = converterResultadoParaDto(resultado);
        return visitaTremServico.salvarOuAtualizarPorIdentificador(dto, true);
    }

    private void ajustarCamposPadrao(ResultadoManifestoVisita resultado) {
        if (resultado.getStatusVisita() == null) {
            resultado.setStatusVisita(StatusVisitaTrem.PLANEJADO);
        }
        if (resultado.getHoraPartidaPrevista() == null && resultado.getHoraChegadaPrevista() != null) {
            resultado.setHoraPartidaPrevista(resultado.getHoraChegadaPrevista().plusHours(4));
        }
    }

    private VisitaTremRequisicaoDto converterResultadoParaDto(ResultadoManifestoVisita resultado) {
        VisitaTremRequisicaoDto dto = new VisitaTremRequisicaoDto();
        dto.setIdentificadorTrem(resultado.getIdentificadorTrem());
        dto.setOperadoraFerroviaria(resultado.getOperadoraFerroviaria());
        dto.setHoraChegadaPrevista(resultado.getHoraChegadaPrevista());
        dto.setHoraPartidaPrevista(resultado.getHoraPartidaPrevista());
        dto.setStatusVisita(Optional.ofNullable(resultado.getStatusVisita()).orElse(StatusVisitaTrem.PLANEJADO));
        dto.setListaDescarga(converterConteineres(resultado.getIdentificacoesDescarga()));
        dto.setListaCarga(converterConteineres(resultado.getIdentificacoesCarga()));
        dto.setListaVagoes(converterVagoes(resultado.getVagoes()));
        return dto;
    }

    private List<OperacaoConteinerVisitaRequisicaoDto> converterConteineres(List<String> identificacoes) {
        if (identificacoes == null || identificacoes.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> codigosNormalizados = new LinkedHashSet<>();
        for (String identificacao : identificacoes) {
            String normalizado = normalizarIdentificacao(identificacao);
            if (StringUtils.hasText(normalizado)) {
                codigosNormalizados.add(normalizado);
            }
        }
        return codigosNormalizados.stream()
                .map(codigo -> {
                    OperacaoConteinerVisitaRequisicaoDto dto = new OperacaoConteinerVisitaRequisicaoDto();
                    dto.setCodigoConteiner(codigo);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<VagaoVisitaRequisicaoDto> converterVagoes(List<VagaoManifestoImportado> vagoes) {
        if (vagoes == null || vagoes.isEmpty()) {
            return new ArrayList<>();
        }
        List<VagaoVisitaRequisicaoDto> lista = new ArrayList<>();
        int posicaoSequencial = 0;
        for (VagaoManifestoImportado vagao : vagoes) {
            if (vagao == null) {
                continue;
            }
            if (!StringUtils.hasText(vagao.getIdentificadorVagao())) {
                continue;
            }
            VagaoVisitaRequisicaoDto dto = new VagaoVisitaRequisicaoDto();
            Integer posicaoInformada = Optional.ofNullable(vagao.getPosicaoNoTrem()).orElse(null);
            if (posicaoInformada == null || posicaoInformada <= 0) {
                posicaoSequencial++;
                dto.setPosicaoNoTrem(posicaoSequencial);
            } else {
                dto.setPosicaoNoTrem(posicaoInformada);
                posicaoSequencial = Math.max(posicaoSequencial, posicaoInformada);
            }
            dto.setIdentificadorVagao(vagao.getIdentificadorVagao());
            dto.setTipoVagao(vagao.getTipoVagao());
            lista.add(dto);
        }
        return lista;
    }

    private String normalizarIdentificacao(String valor) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        try {
            limpo = ValidacaoEntradaUtil.limparTexto(limpo);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O arquivo de manifesto contém identificações de contêiner com caracteres inválidos.");
        }
        if (!StringUtils.hasText(limpo)) {
            return null;
        }
        String normalizado = limpo.trim().toUpperCase(Locale.ROOT);
        if (normalizado.length() > 20) {
            return normalizado.substring(0, 20);
        }
        return normalizado;
    }
}
