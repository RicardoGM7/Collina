package br.ufs.dcomp.collina.repository.financeiro;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ufs.dcomp.collina.model.financeiro.ExecucaoFechamento;

public interface ExecucaoFechamentoRepository extends JpaRepository<ExecucaoFechamento, Long> {

    Optional<ExecucaoFechamento> findTopByOrderByDataExecucaoDescIdDesc();
}
