package br.ufs.dcomp.collina.dto.estoque;

import br.ufs.dcomp.collina.model.estoque.Categoria;

public record CategoriaResponseDTO(Integer id, String nome, String cor) {

    public CategoriaResponseDTO(Categoria categoria) {
        this(
                categoria.getId(),
                categoria.getNome(),
                categoria.getCor() != null && !categoria.getCor().isBlank()
                        ? categoria.getCor()
                        : Categoria.COR_PADRAO
        );
    }

}
