package br.ufs.dcomp.collina.repository.auth;

import br.ufs.dcomp.collina.model.auth.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PessoaRepository extends JpaRepository<Pessoa, Integer> {
    Optional<Pessoa> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
}
