package br.ufs.dcomp.collina.dto.estoque;

import br.ufs.dcomp.collina.model.estoque.Produto;

import java.util.Set;
import java.util.stream.Collectors;

public record ProdutoResponseDTO(
        Integer id,
        String nome,
        String descricao,
        Double preco,
        Set<CategoriaResponseDTO> categorias) {

        public ProdutoResponseDTO(Produto produto){
           this(
                   produto.getId(),
                   produto.getNome(),
                   produto.getDescricao(),
                   produto.getPreco(),
                   produto.getCategorias().stream().map(CategoriaResponseDTO::new).collect(Collectors.toSet())
           );
        }


}
