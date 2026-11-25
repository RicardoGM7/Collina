package br.ufs.dcomp.collina.repository.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.Escala;
import br.ufs.dcomp.collina.model.funcionarios.StatusEscala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EscalaRepository extends JpaRepository<Escala, Integer> {
    Optional<Escala> findByPeriodo(String periodo);
    boolean existsByPeriodo(String periodo);
    List<Escala> findByStatus(StatusEscala status);
}
