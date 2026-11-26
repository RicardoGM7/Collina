package br.ufs.dcomp.collina.repository.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.Presenca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PresencaRepository extends JpaRepository<Presenca, Integer> {
    Optional<Presenca> findByAlocacaoId(Integer alocacaoId);

    @Query("SELECT p FROM Presenca p JOIN p.alocacao a WHERE a.funcionario.id = :funcionarioId")
    List<Presenca> findByFuncionarioId(@Param("funcionarioId") Integer funcionarioId);

    @Query("SELECT p FROM Presenca p JOIN p.alocacao a WHERE a.data BETWEEN :dataInicio AND :dataFim")
    List<Presenca> findByPeriodo(@Param("dataInicio") LocalDate dataInicio, @Param("dataFim") LocalDate dataFim);
}
