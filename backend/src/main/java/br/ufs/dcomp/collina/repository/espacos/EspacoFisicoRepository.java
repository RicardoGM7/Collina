package br.ufs.dcomp.collina.repository.espacos;

import br.ufs.dcomp.collina.model.espacos.EspacoFisico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

// Estende Repository (e não JpaRepository) para expor só o que o módulo usa.
public interface EspacoFisicoRepository extends Repository<EspacoFisico, Integer> {

    EspacoFisico save(EspacoFisico espaco);

    Optional<EspacoFisico> findById(Integer id);

    List<EspacoFisico> findAllByOrderByNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Integer id);

    // Trava a linha do espaço até o fim da transação: serializa reservas concorrentes do mesmo espaço
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EspacoFisico> findWithLockById(Integer id);
}
