package br.ufs.dcomp.collina.repository.estoque;

import br.ufs.dcomp.collina.model.estoque.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    boolean existsByNome(String nome);

    boolean existsByNomeIgnoreCase(String nome);
}
