package br.ufs.dcomp.collina.repository.financeiro;

import br.ufs.dcomp.collina.model.financeiro.StatusSolicitacaoGastoEnum;
import br.ufs.dcomp.collina.model.financeiro.SolicitacaoGasto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitacaoGastoRepository extends JpaRepository<SolicitacaoGasto, Long> {
    // Buscar solicitações de gasto por status
    List<SolicitacaoGasto> findByStatus(StatusSolicitacaoGastoEnum status);

}
