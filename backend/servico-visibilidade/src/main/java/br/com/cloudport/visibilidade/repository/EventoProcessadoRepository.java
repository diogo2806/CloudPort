package br.com.cloudport.visibilidade.repository;

import br.com.cloudport.visibilidade.entity.EventoProcessado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, String> {

    @Modifying
    @Query(value = "INSERT INTO visibilidade_evento_processado "
            + "(identidade_evento, tipo_evento, hash_payload, processado_em) "
            + "VALUES (:identidadeEvento, :tipoEvento, :hashPayload, CURRENT_TIMESTAMP) "
            + "ON CONFLICT (identidade_evento) DO NOTHING",
            nativeQuery = true)
    int inserirSeAusente(@Param("identidadeEvento") String identidadeEvento,
                         @Param("tipoEvento") String tipoEvento,
                         @Param("hashPayload") String hashPayload);
}
