package br.com.cloudport.visibilidade.repository;

import br.com.cloudport.visibilidade.entity.StatusNavio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusNavioRepository extends JpaRepository<StatusNavio, Long> {

    Optional<StatusNavio> findByNavioId(String navioId);

    List<StatusNavio> findByStatusOperacional(String statusOperacional);

    boolean existsByNavioId(String navioId);
}