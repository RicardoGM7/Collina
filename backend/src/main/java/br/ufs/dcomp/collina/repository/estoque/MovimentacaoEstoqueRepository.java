package br.ufs.dcomp.collina.repository.estoque;

import br.ufs.dcomp.collina.model.estoque.MovimentacaoEstoque;
import br.ufs.dcomp.collina.model.estoque.MovimentacaoEstoqueEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Integer> {

    // Lista o histórico de movimentações de um produto ordenado da mais recente para a mais antiga
    List<MovimentacaoEstoque> findByEstoqueProdutoIdOrderByDataDesc(Integer produtoId);

    // Lista movimentações por tipo (ex: VENDA, COMPRA_FORNECEDOR, PERDA_DANO)
    List<MovimentacaoEstoque> findByTipo(MovimentacaoEstoqueEnum tipo);
}
