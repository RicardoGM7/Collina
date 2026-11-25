package br.ufs.dcomp.collina.repository.espacos;

import br.ufs.dcomp.collina.model.associados.Associado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.Repository;

import java.util.Optional;

// Acesso do módulo de espaços ao Associado, só para travar a linha do titular durante a reserva.
// Protege a cota mensal contra duas solicitações simultâneas do mesmo titular.
// (Fica aqui para não alterar o AssociadoRepository, que é do módulo de associados.)
public interface AssociadoReservaRepository extends Repository<Associado, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Associado> findWithLockById(Integer id);
}
