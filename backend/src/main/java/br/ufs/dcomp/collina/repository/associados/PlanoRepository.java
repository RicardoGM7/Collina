package br.ufs.dcomp.collina.repository.associados;

import br.ufs.dcomp.collina.model.associados.Plano;
import br.ufs.dcomp.collina.model.associados.TipoPlanoEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanoRepository extends JpaRepository<Plano, Integer> {
    Optional<Plano> findByTipo(TipoPlanoEnum tipo);
}
