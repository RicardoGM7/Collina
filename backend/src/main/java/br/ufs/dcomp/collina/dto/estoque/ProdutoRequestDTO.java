package br.ufs.dcomp.collina.dto.estoque;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Set;

public record ProdutoRequestDTO(
        @NotBlank(message = "O nome do produto é obrigatório.")
        String nome,

        String descricao,

        @NotNull(message = "O preço do produto é obrigatório.")
        @PositiveOrZero(message = "O preço do produto não pode ser negativo.")
        @Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo 2 casas decimais.")
        BigDecimal preco,

        @NotEmpty(message = "O produto deve estar vinculado a pelo menos uma categoria.")
        Set<Integer> categoriaIds,

        @PositiveOrZero(message = "O estoque mínimo não pode ser negativo.")
        Integer estoqueMinimo,

        @PositiveOrZero(message = "A quantidade inicial não pode ser negativa.")
        Integer quantidadeInicial
) {
    public ProdutoRequestDTO(
            String nome,
            String descricao,
            BigDecimal preco,
            Set<Integer> categoriaIds,
            Integer estoqueMinimo) {
        this(nome, descricao, preco, categoriaIds, estoqueMinimo, 0);
    }
}
