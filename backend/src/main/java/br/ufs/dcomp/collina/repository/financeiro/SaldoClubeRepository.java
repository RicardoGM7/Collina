package br.ufs.dcomp.collina.repository.financeiro;

import br.ufs.dcomp.collina.model.financeiro.SaldoClube;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SaldoClubeRepository extends JpaRepository<SaldoClube, Long> {

    // LEITURA (sem lock): usada em consultas.
    Optional<SaldoClube> findFirstByOrderByIdAsc();

    // ESCRITA (com lock): o saldo do clube é um registro único.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SaldoClube> findTopByOrderByIdAsc();
}