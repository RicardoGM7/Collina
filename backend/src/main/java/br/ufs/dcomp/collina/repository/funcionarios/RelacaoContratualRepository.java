package br.ufs.dcomp.collina.repository.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.RelacaoContratual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RelacaoContratualRepository extends JpaRepository<RelacaoContratual, Integer> {
    List<RelacaoContratual> findByFuncionarioId(Integer funcionarioId);

    Optional<RelacaoContratual> findFirstByFuncionarioIdAndDataFimIsNullOrderByDataInicioDesc(Integer funcionarioId);

    @Query("SELECT r FROM RelacaoContratual r WHERE r.funcionario.id = :funcionarioId " +
           "AND r.dataInicio <= :data AND (r.dataFim IS NULL OR r.dataFim >= :data)")
    List<RelacaoContratual> findVigenteByFuncionario(@Param("funcionarioId") Integer funcionarioId, @Param("data") LocalDate data);
}
