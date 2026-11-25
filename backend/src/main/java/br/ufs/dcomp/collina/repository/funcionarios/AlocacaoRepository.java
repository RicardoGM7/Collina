package br.ufs.dcomp.collina.repository.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.Alocacao;
import br.ufs.dcomp.collina.model.funcionarios.StatusAlocacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AlocacaoRepository extends JpaRepository<Alocacao, Integer> {
    List<Alocacao> findByEscalaId(Integer escalaId);
    List<Alocacao> findByFuncionarioId(Integer funcionarioId);
    List<Alocacao> findByFuncionarioIdAndData(Integer funcionarioId, LocalDate data);
    List<Alocacao> findByData(LocalDate data);

    @Query("SELECT a FROM Alocacao a WHERE a.funcionario.id = :funcionarioId " +
           "AND a.data BETWEEN :inicio AND :fim AND a.status != 'CANCELADA'")
    List<Alocacao> findAtivasByFuncionarioIdAndPeriodo(@Param("funcionarioId") Integer funcionarioId,
                                                      @Param("inicio") LocalDate inicio,
                                                      @Param("fim") LocalDate fim);

    @Query("SELECT a FROM Alocacao a WHERE a.funcionario.id = :funcionarioId " +
           "AND a.data = :data AND a.status != 'CANCELADA'")
    List<Alocacao> findAtivasByFuncionarioIdAndData(@Param("funcionarioId") Integer funcionarioId,
                                                   @Param("data") LocalDate data);

    boolean existsByFuncionarioIdAndDataAndTurnoIdAndStatusNot(Integer funcionarioId, LocalDate data, Integer turnoId, StatusAlocacao status);
}
