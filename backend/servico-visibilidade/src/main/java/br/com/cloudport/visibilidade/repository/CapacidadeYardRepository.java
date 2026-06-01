package br.com.cloudport.visibilidade.repository;

import br.com.cloudport.visibilidade.entity.CapacidadeYard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CapacidadeYardRepository extends JpaRepository<CapacidadeYard, Long> {

    Optional<CapacidadeYard> findByZona(String zona);

    boolean existsByZona(String zona);
}