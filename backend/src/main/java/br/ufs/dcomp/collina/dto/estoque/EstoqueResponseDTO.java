package br.ufs.dcomp.collina.dto.estoque;

import br.ufs.dcomp.collina.model.estoque.Estoque;
import br.ufs.dcomp.collina.model.estoque.Produto;

public record EstoqueResponseDTO(Integer id,
                                 Integer quantidade,
                                 Integer estoqueMinimo,
                                 Integer produtoId,
                                 String nomeProduto) {

public EstoqueResponseDTO(Estoque estoque) {
    this(
            estoque.getId(),
            estoque.getQuantidade(),
            estoque.getEstoqueMinimo(),
            estoque.getProduto() != null ? estoque.getProduto().getId() : null,
            estoque.getProduto() != null ? estoque.getProduto().getNome() : null
    );
}
}