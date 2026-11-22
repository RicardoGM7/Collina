package br.ufs.dcomp.collina.repository.estoque;

import br.ufs.dcomp.collina.model.estoque.MovimentacaoEstoque;
import br.ufs.dcomp.collina.model.estoque.MovimentacaoEstoqueEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Integer> {

    @Override
    @EntityGraph(attributePaths = {"estoque", "estoque.produto"})
    List<MovimentacaoEstoque> findAll();

    @EntityGraph(attributePaths = {"estoque", "estoque.produto"})
    List<MovimentacaoEstoque> findAllByOrderByDataDesc();

    @Override
    @EntityGraph(attributePaths = {"estoque", "estoque.produto"})
    Optional<MovimentacaoEstoque> findById(Integer id);

    // Lista o histórico de movimentações de um produto ordenado da mais recente para a mais antiga
    @EntityGraph(attributePaths = {"estoque", "estoque.produto"})
    List<MovimentacaoEstoque> findByEstoqueProdutoIdOrderByDataDesc(Integer produtoId);

    // Lista movimentações por tipo (ex: VENDA, COMPRA_FORNECEDOR, PERDA_DANO)
    @EntityGraph(attributePaths = {"estoque", "estoque.produto"})
    List<MovimentacaoEstoque> findByTipoOrderByDataDesc(MovimentacaoEstoqueEnum tipo);

    boolean existsByEstoqueId(Integer estoqueId);
}
