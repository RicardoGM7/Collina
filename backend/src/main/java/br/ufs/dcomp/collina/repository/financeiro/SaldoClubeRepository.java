package br.ufs.dcomp.collina.repository.financeiro;

import br.ufs.dcomp.collina.model.financeiro.SaldoClube;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaldoClubeRepository extends JpaRepository<SaldoClube, Long> {
    // O saldo do clube é um registro único: busca o primeiro (e único) registro da tabela
    Optional<SaldoClube> findFirstByOrderByIdAsc();
}