package br.ufs.dcomp.collina.repository.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.Funcionario;
import br.ufs.dcomp.collina.model.funcionarios.StatusFuncionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Integer> {
    Optional<Funcionario> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
    List<Funcionario> findByStatus(StatusFuncionario status);
}
