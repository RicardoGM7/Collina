package br.ufs.dcomp.collina.repository.associados;

import br.ufs.dcomp.collina.model.associados.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PessoaRepository extends JpaRepository<Pessoa, Integer> {
    Optional<Pessoa> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
}
