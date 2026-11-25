package br.ufs.dcomp.collina.repository.espacos;

import br.ufs.dcomp.collina.model.espacos.BlocoHorario;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;

public interface BlocoHorarioRepository extends Repository<BlocoHorario, Integer> {

    BlocoHorario save(BlocoHorario bloco);

    List<BlocoHorario> findByEspacoIdOrderByHoraInicioAsc(Integer espacoId);

    // Só devolve os blocos que realmente pertencem ao espaço informado
    List<BlocoHorario> findByEspacoIdAndIdIn(Integer espacoId, Collection<Integer> ids);
}
