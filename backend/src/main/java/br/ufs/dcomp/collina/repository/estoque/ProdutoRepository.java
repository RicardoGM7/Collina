package br.ufs.dcomp.collina.repository.estoque;

import br.ufs.dcomp.collina.model.estoque.Produto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Integer> {

    @Override
    @EntityGraph(attributePaths = {"categorias"})
    List<Produto> findAll();

    @Override
    @EntityGraph(attributePaths = {"categorias", "estoque"})
    Optional<Produto> findById(Integer id);

    boolean existsByCategoriasId(Integer categoriaId);
}
