package br.com.cloudport.serviconavio.escala.servico;

import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import br.com.cloudport.serviconavio.escala.dto.AtualizacaoEscalaDTO;
import br.com.cloudport.serviconavio.escala.dto.CadastroEscalaDTO;
import br.com.cloudport.serviconavio.escala.dto.EscalaDetalheDTO;
import br.com.cloudport.serviconavio.escala.dto.EscalaResumoDTO;
import br.com.cloudport.serviconavio.escala.entidade.Escala;
import br.com.cloudport.serviconavio.escala.entidade.FaseEscala;
import br.com.cloudport.serviconavio.escala.repositorio.EscalaRepositorio;
import br.com.cloudport.serviconavio.navio.entidade.Navio;
import br.com.cloudport.serviconavio.navio.repositorio.NavioRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class EscalaServico {

    private static final int DIAS_MAXIMO_CONSULTA = 60;
    private static final List<FaseEscala> FASES_FORA_CRONOGRAMA =
            List.of(FaseEscala.ENCERRADA, FaseEscala.CANCELADA);

    private final EscalaRepositorio escalaRepositorio;
    private final NavioRepositorio navioRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public EscalaServico(EscalaRepositorio escalaRepositorio,
                         NavioRepositorio navioRepositorio,
                         SanitizadorEntrada sanitizadorEntrada) {
        this.escalaRepositorio = escalaRepositorio;
        this.navioRepositorio = navioRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional(readOnly = true)
    public List<EscalaResumoDTO> listarCronograma(int dias) {
        if (dias < 1 || dias > DIAS_MAXIMO_CONSULTA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O intervalo de consulta deve estar entre 1 e %d dias.", DIAS_MAXIMO_CONSULTA));
        }
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicio = agora.minusDays(1);
        LocalDateTime limite = agora.plusDays(dias);
        return escalaRepositorio.buscarCronograma(inicio, limite, FASES_FORA_CRONOGRAMA).stream()
                .map(this::mapearResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EscalaResumoDTO> listarPorNavio(Long navioId) {
        garantirNavioExiste(navioId);
        return escalaRepositorio.findByNavioIdentificadorOrderByChegadaPrevistaAsc(navioId).stream()
                .map(this::mapearResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EscalaDetalheDTO buscarDetalhe(Long id) {
        return EscalaDetalheDTO.deEntidade(obterEscala(id));
    }

    @Transactional
    public EscalaDetalheDTO registrar(Long navioId, CadastroEscalaDTO dto) {
        Navio navio = obterNavio(navioId);
        LocalDateTime chegada = dto.getChegadaPrevista();
        validarOrdemTempos(chegada, dto.getAtracacaoPrevista(), dto.getPartidaPrevista());

        Escala escala = new Escala();
        escala.setNavio(navio);
        escala.setFase(FaseEscala.PREVISTA);
        escala.setViagemEntrada(sanitizadorEntrada
                .limparTextoObrigatorio(dto.getViagemEntrada(), "viagem de entrada").toUpperCase(Locale.ROOT));
        escala.setViagemSaida(tratarViagemOpcional(dto.getViagemSaida()));
        escala.setChegadaPrevista(chegada);
        escala.setAtracacaoPrevista(dto.getAtracacaoPrevista());
        escala.setPartidaPrevista(dto.getPartidaPrevista());
        escala.setBercoPrevisto(tratarTextoOpcional(dto.getBercoPrevisto()));
        escala.setObservacoes(tratarTextoOpcional(dto.getObservacoes()));

        return EscalaDetalheDTO.deEntidade(escalaRepositorio.save(escala));
    }

    @Transactional
    public EscalaDetalheDTO atualizar(Long id, AtualizacaoEscalaDTO dto) {
        Escala escala = obterEscala(id);
        if (escala.getFase().isTerminal()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível alterar uma escala já encerrada ou cancelada.");
        }

        if (StringUtils.hasText(dto.getViagemEntrada())) {
            escala.setViagemEntrada(sanitizadorEntrada
                    .limparTextoObrigatorio(dto.getViagemEntrada(), "viagem de entrada").toUpperCase(Locale.ROOT));
        }
        if (dto.getViagemSaida() != null) {
            escala.setViagemSaida(tratarViagemOpcional(dto.getViagemSaida()));
        }
        if (dto.getChegadaPrevista() != null) {
            escala.setChegadaPrevista(dto.getChegadaPrevista());
        }
        if (dto.getAtracacaoPrevista() != null) {
            escala.setAtracacaoPrevista(dto.getAtracacaoPrevista());
        }
        if (dto.getPartidaPrevista() != null) {
            escala.setPartidaPrevista(dto.getPartidaPrevista());
        }
        if (dto.getBercoPrevisto() != null) {
            escala.setBercoPrevisto(tratarTextoOpcional(dto.getBercoPrevisto()));
        }
        if (dto.getBercoAtual() != null) {
            escala.setBercoAtual(tratarTextoOpcional(dto.getBercoAtual()));
        }
        if (dto.getObservacoes() != null) {
            escala.setObservacoes(tratarTextoOpcional(dto.getObservacoes()));
        }

        validarOrdemTempos(escala.getChegadaPrevista(), escala.getAtracacaoPrevista(), escala.getPartidaPrevista());

        return EscalaDetalheDTO.deEntidade(escalaRepositorio.save(escala));
    }

    @Transactional
    public EscalaDetalheDTO avancarFase(Long id, FaseEscala destino) {
        Escala escala = obterEscala(id);
        if (destino == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a fase de destino.");
        }
        if (escala.getFase() == destino) {
            return EscalaDetalheDTO.deEntidade(escala);
        }
        if (!escala.getFase().podeTransicionarPara(destino)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format(Locale.ROOT, "Transição de fase inválida: de %s para %s.",
                            escala.getFase(), destino));
        }

        LocalDateTime agora = LocalDateTime.now();
        carimbarTemposEfetivos(escala, destino, agora);
        escala.setFase(destino);
        return EscalaDetalheDTO.deEntidade(escalaRepositorio.save(escala));
    }

    @Transactional
    public void remover(Long id) {
        escalaRepositorio.delete(obterEscala(id));
    }

    private void carimbarTemposEfetivos(Escala escala, FaseEscala destino, LocalDateTime agora) {
        if (destino == FaseEscala.ATRACADO) {
            if (escala.getChegadaEfetiva() == null) {
                escala.setChegadaEfetiva(agora);
            }
            if (escala.getAtracacaoEfetiva() == null) {
                escala.setAtracacaoEfetiva(agora);
            }
        } else if (destino == FaseEscala.OPERANDO) {
            if (escala.getChegadaEfetiva() == null) {
                escala.setChegadaEfetiva(agora);
            }
            if (escala.getAtracacaoEfetiva() == null) {
                escala.setAtracacaoEfetiva(agora);
            }
        } else if (destino == FaseEscala.PARTIU && escala.getPartidaEfetiva() == null) {
            escala.setPartidaEfetiva(agora);
        }
    }

    private void validarOrdemTempos(LocalDateTime chegada,
                                    LocalDateTime atracacao,
                                    LocalDateTime partida) {
        if (chegada == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A chegada prevista é obrigatória.");
        }
        if (atracacao != null && atracacao.isBefore(chegada)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A atracação prevista não pode ser anterior à chegada prevista.");
        }
        if (partida != null && partida.isBefore(chegada)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A partida prevista não pode ser anterior à chegada prevista.");
        }
        if (partida != null && atracacao != null && partida.isBefore(atracacao)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A partida prevista não pode ser anterior à atracação prevista.");
        }
    }

    private void garantirNavioExiste(Long navioId) {
        if (!navioRepositorio.existsById(navioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Navio não encontrado.");
        }
    }

    private Navio obterNavio(Long navioId) {
        return navioRepositorio.findById(navioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Navio não encontrado."));
    }

    private Escala obterEscala(Long id) {
        return escalaRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escala não encontrada."));
    }

    private String tratarViagemOpcional(String valor) {
        String limpo = tratarTextoOpcional(valor);
        return limpo == null ? null : limpo.toUpperCase(Locale.ROOT);
    }

    private String tratarTextoOpcional(String valor) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        return StringUtils.hasText(limpo) ? limpo : null;
    }

    private EscalaResumoDTO mapearResumo(Escala escala) {
        Navio navio = escala.getNavio();
        return new EscalaResumoDTO(
                escala.getId(),
                navio.getIdentificador(),
                navio.getNome(),
                navio.getCodigoImo(),
                escala.getViagemEntrada(),
                escala.getFase(),
                escala.getChegadaPrevista(),
                escala.getBercoPrevisto()
        );
    }
}
