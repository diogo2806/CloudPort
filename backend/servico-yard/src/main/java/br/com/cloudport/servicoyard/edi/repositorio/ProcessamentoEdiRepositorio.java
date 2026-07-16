package br.com.cloudport.servicoyard.edi.repositorio;

import br.com.cloudport.servicoyard.edi.modelo.ProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.StatusProcessamentoEdi;
import br.com.cloudport.servicoyard.edi.modelo.TipoMensagemEdi;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessamentoEdiRepositorio extends JpaRepository<ProcessamentoEdi, Long> {

    Page<ProcessamentoEdi> findByTipoMensagemAndStatus(
            TipoMensagemEdi tipoMensagem,
            StatusProcessamentoEdi status,
            Pageable pageable
    );

    Page<ProcessamentoEdi> findByTipoMensagem(TipoMensagemEdi tipoMensagem, Pageable pageable);

    Page<ProcessamentoEdi> findByStatus(StatusProcessamentoEdi status, Pageable pageable);

    Optional<ProcessamentoEdi> findByChaveIdempotencia(String chaveIdempotencia);

    Optional<ProcessamentoEdi> findByIdentificadorInterchangeAndIdentificadorMensagemAndReferenciaMensagem(
            String identificadorInterchange,
            String identificadorMensagem,
            String referenciaMensagem
    );

    Optional<ProcessamentoEdi> findTopByReprocessamentoDeIdOrderByTentativaDesc(Long reprocessamentoDeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select processamento
              from ProcessamentoEdi processamento
             where processamento.status in :status
               and (processamento.proximaTentativaEm is null
                    or processamento.proximaTentativaEm <= :agora)
             order by processamento.criadoEm asc
            """)
    List<ProcessamentoEdi> buscarPendentesParaProcessamento(
            @Param("status") Collection<StatusProcessamentoEdi> status,
            @Param("agora") LocalDateTime agora,
            Pageable pageable
    );

    @Modifying
    @Query(value = """
            INSERT INTO edi_processamento (
                tipo_mensagem,
                status,
                conteudo_original,
                codigo_navio,
                codigo_viagem,
                referencia_mensagem,
                correlation_id,
                identificador_interchange,
                identificador_mensagem,
                chave_idempotencia,
                hash_conteudo,
                tentativa,
                proxima_tentativa_em,
                criado_em,
                atualizado_em
            ) VALUES (
                :tipoMensagem,
                'RECEBIDO',
                :conteudoOriginal,
                :codigoNavio,
                :codigoViagem,
                :referenciaMensagem,
                :correlationId,
                :identificadorInterchange,
                :identificadorMensagem,
                :chaveIdempotencia,
                :hashConteudo,
                0,
                :agora,
                :agora,
                :agora
            )
            ON CONFLICT (chave_idempotencia) DO NOTHING
            """, nativeQuery = true)
    int inserirSeAusente(
            @Param("tipoMensagem") String tipoMensagem,
            @Param("conteudoOriginal") String conteudoOriginal,
            @Param("codigoNavio") String codigoNavio,
            @Param("codigoViagem") String codigoViagem,
            @Param("referenciaMensagem") String referenciaMensagem,
            @Param("correlationId") String correlationId,
            @Param("identificadorInterchange") String identificadorInterchange,
            @Param("identificadorMensagem") String identificadorMensagem,
            @Param("chaveIdempotencia") String chaveIdempotencia,
            @Param("hashConteudo") String hashConteudo,
            @Param("agora") LocalDateTime agora
    );
}
