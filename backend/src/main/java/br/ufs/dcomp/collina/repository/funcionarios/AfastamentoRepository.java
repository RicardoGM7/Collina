package br.ufs.dcomp.collina.repository.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.Afastamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AfastamentoRepository extends JpaRepository<Afastamento, Integer> {
    List<Afastamento> findByFuncionarioId(Integer funcionarioId);

    @Query("SELECT a FROM Afastamento a WHERE a.funcionario.id = :funcionarioId " +
           "AND a.dataInicio <= :dataFim AND a.dataFim >= :dataInicio")
    List<Afastamento> findConflitantes(@Param("funcionarioId") Integer funcionarioId,
                                       @Param("dataInicio") LocalDate dataInicio,
                                       @Param("dataFim") LocalDate dataFim);

    @Query("SELECT COUNT(a) > 0 FROM Afastamento a WHERE a.funcionario.id = :funcionarioId " +
           "AND :data BETWEEN a.dataInicio AND a.dataFim")
    boolean isFuncionarioAfastado(@Param("funcionarioId") Integer funcionarioId, @Param("data") LocalDate data);
}
