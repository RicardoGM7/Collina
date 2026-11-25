package br.ufs.dcomp.collina.repository.financeiro;

import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.model.financeiro.CancelamentoDeMensalidade;


public interface CancelamentoDeMensalidadeRepository extends JpaRepository<CancelamentoDeMensalidade, Long> {

    Optional<CancelamentoDeMensalidade> findByMensalidadeCancelada(Mensalidade mensalidadeCancelada);
}
