package br.ufs.dcomp.collina.dto.estoque;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record EstoqueMinimoRequestDTO(
        @NotNull(message = "O novo estoque mínimo é obrigatório.")
        @PositiveOrZero(message = "O estoque mínimo não pode ser negativo.")
        Integer novoMinimo
) {}
