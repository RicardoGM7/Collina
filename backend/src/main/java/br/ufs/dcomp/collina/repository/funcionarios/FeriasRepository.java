package br.ufs.dcomp.collina.repository.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.Ferias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FeriasRepository extends JpaRepository<Ferias, Integer> {
    List<Ferias> findByFuncionarioId(Integer funcionarioId);

    @Query("SELECT f FROM Ferias f WHERE f.funcionario.id = :funcionarioId " +
           "AND f.dataInicio <= :dataFim AND f.dataFim >= :dataInicio")
    List<Ferias> findConflitantes(@Param("funcionarioId") Integer funcionarioId,
                                  @Param("dataInicio") LocalDate dataInicio,
                                  @Param("dataFim") LocalDate dataFim);

    @Query("SELECT COUNT(f) > 0 FROM Ferias f WHERE f.funcionario.id = :funcionarioId " +
           "AND :data BETWEEN f.dataInicio AND f.dataFim")
    boolean isFuncionarioEmFerias(@Param("funcionarioId") Integer funcionarioId, @Param("data") LocalDate data);
}
