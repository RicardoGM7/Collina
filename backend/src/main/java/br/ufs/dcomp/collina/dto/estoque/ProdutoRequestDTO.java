package br.ufs.dcomp.collina.dto.estoque;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Set;

public record ProdutoRequestDTO(
        @NotBlank(message = "O nome do produto é obrigatório.")
        String nome,

        String descricao,

        @NotNull(message = "O preço do produto é obrigatório.")
        @PositiveOrZero(message = "O preço do produto não pode ser negativo.")
        Double preco,

        Set<Integer> categoriaIds,

        @PositiveOrZero(message = "O estoque mínimo não pode ser negativo.")
        Integer estoqueMinimo
) {}
