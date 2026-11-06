package br.ufs.dcomp.collina.repository.estoque;


import br.ufs.dcomp.collina.model.estoque.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Integer> {
}
