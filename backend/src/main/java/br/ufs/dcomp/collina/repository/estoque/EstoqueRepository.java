package br.ufs.dcomp.collina.repository.estoque;

import br.ufs.dcomp.collina.model.estoque.Estoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EstoqueRepository extends JpaRepository<Estoque, Integer> {

    // Busca o estoque pelo ID do produto associado
    Optional<Estoque> findByProdutoId(Integer produtoId);

    // RF-E13: Alerta de estoque mínimo (itens com saldo menor ou igual ao mínimo)
    @Query("SELECT e FROM Estoque e WHERE e.quantidade <= e.estoqueMinimo")
    List<Estoque> findItensComEstoqueBaixo();
}
