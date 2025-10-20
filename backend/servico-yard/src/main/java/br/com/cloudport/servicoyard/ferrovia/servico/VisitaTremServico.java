package br.com.cloudport.servicoyard.ferrovia.servico;

import br.com.cloudport.servicoyard.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicoyard.container.validacao.SanitizadorEntrada;
import br.com.cloudport.servicoyard.ferrovia.dto.VisitaTremRequisicaoDto;
import br.com.cloudport.servicoyard.ferrovia.dto.VisitaTremRespostaDto;
import br.com.cloudport.servicoyard.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicoyard.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicoyard.ferrovia.repositorio.VisitaTremRepositorio;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VisitaTremServico {

    private static final int DIAS_MAXIMO_CONSULTA = 30;

    private final VisitaTremRepositorio visitaTremRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public VisitaTremServico(VisitaTremRepositorio visitaTremRepositorio,
                              SanitizadorEntrada sanitizadorEntrada) {
        this.visitaTremRepositorio = visitaTremRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional
    public VisitaTremRespostaDto registrarVisita(VisitaTremRequisicaoDto dto) {
        VisitaTrem visita = new VisitaTrem();
        aplicarDados(visita, dto);
        VisitaTrem salvo = visitaTremRepositorio.save(visita);
        return VisitaTremRespostaDto.deEntidade(salvo);
    }

    @Transactional
    public VisitaTremRespostaDto atualizarVisita(Long id, VisitaTremRequisicaoDto dto) {
        VisitaTrem existente = visitaTremRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita de trem não encontrada."));
        aplicarDados(existente, dto);
        VisitaTrem atualizado = visitaTremRepositorio.save(existente);
        return VisitaTremRespostaDto.deEntidade(atualizado);
    }

    @Transactional(readOnly = true)
    public VisitaTremRespostaDto consultarVisita(Long id) {
        VisitaTrem visita = visitaTremRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita de trem não encontrada."));
        return VisitaTremRespostaDto.deEntidade(visita);
    }

    @Transactional(readOnly = true)
    public List<VisitaTremRespostaDto> listarVisitasProximosDias(int dias) {
        if (dias < 1 || dias > DIAS_MAXIMO_CONSULTA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O intervalo de consulta deve estar entre 1 e %d dias.", DIAS_MAXIMO_CONSULTA));
        }
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicio = agora.minusDays(1);
        LocalDateTime limite = agora.plusDays(dias);
        return visitaTremRepositorio.buscarVisitasPlanejadasOuAtivas(inicio, agora, limite, StatusVisitaTrem.PARTIU)
                .stream()
                .distinct()
                .map(VisitaTremRespostaDto::deEntidade)
                .collect(Collectors.toList());
    }

    private void aplicarDados(VisitaTrem visita, VisitaTremRequisicaoDto dto) {
        String identificadorLimpo = sanitizarObrigatorio(dto.getIdentificadorTrem(), "identificador do trem", 40)
                .toUpperCase(Locale.ROOT);
        String operadoraLimpa = sanitizarObrigatorio(dto.getOperadoraFerroviaria(), "operadora ferroviária", 80);

        LocalDateTime horaChegada = Objects.requireNonNull(dto.getHoraChegadaPrevista(),
                "A hora prevista de chegada deve ser informada.");
        LocalDateTime horaPartida = Objects.requireNonNull(dto.getHoraPartidaPrevista(),
                "A hora prevista de partida deve ser informada.");

        if (horaPartida.isBefore(horaChegada)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A partida prevista não pode ser anterior à chegada prevista.");
        }

        horaChegada = horaChegada.truncatedTo(ChronoUnit.MINUTES);
        horaPartida = horaPartida.truncatedTo(ChronoUnit.MINUTES);

        StatusVisitaTrem status = Objects.requireNonNull(dto.getStatusVisita(),
                "O status da visita deve ser informado.");

        visita.setIdentificadorTrem(identificadorLimpo);
        visita.setOperadoraFerroviaria(operadoraLimpa);
        visita.setHoraChegadaPrevista(horaChegada);
        visita.setHoraPartidaPrevista(horaPartida);
        visita.setStatusVisita(status);
    }

    private String sanitizarObrigatorio(String valor, String campo, int tamanhoMaximo) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        limpo = ValidacaoEntradaUtil.limparTexto(limpo);
        if (!StringUtils.hasText(limpo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O campo %s é obrigatório.", campo));
        }
        String normalizado = limpo.trim();
        if (normalizado.length() > tamanhoMaximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O campo %s deve ter no máximo %d caracteres.", campo, tamanhoMaximo));
        }
        return normalizado;
    }
}
