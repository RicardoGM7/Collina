package br.ufs.dcomp.collina.repository.espacos;

import br.ufs.dcomp.collina.model.espacos.Reserva;
import br.ufs.dcomp.collina.model.espacos.StatusReservaEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends Repository<Reserva, Integer> {

    // saveAndFlush: a violação da restrição única aparece aqui (e não só no commit)
    Reserva saveAndFlush(Reserva reserva);

    Reserva save(Reserva reserva);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Reserva> findWithLockById(Integer id);

    List<Reserva> findByTitularId(Integer titularId);

    // Agenda do dia de um espaço (blocos ocupados)
    List<Reserva> findByEspacoIdAndDataUsoAndStatus(Integer espacoId, LocalDate dataUso, StatusReservaEnum status);

    List<Reserva> findByEspacoIdAndDataUsoAndStatusAndBlocoIdIn(
            Integer espacoId, LocalDate dataUso, StatusReservaEnum status, Collection<Integer> blocoIds);

    // Cota mensal: reservas confirmadas do titular com data de uso em [inicio, fim)
    long countByTitularIdAndStatusAndDataUsoGreaterThanEqualAndDataUsoLessThan(
            Integer titularId, StatusReservaEnum status, LocalDate inicio, LocalDate fim);

    boolean existsByEspacoId(Integer espacoId);

    boolean existsByBlocoId(Integer blocoId);
}
