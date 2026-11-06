package br.ufs.dcomp.collina.dto.estoque;

import br.ufs.dcomp.collina.model.estoque.MovimentacaoEstoqueEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MovimentacaoEstoqueRequestDTO(
        @NotNull(message = "O ID do produto é obrigatório.")
        Integer produtoID,

        @NotNull(message = "O tipo de movimentação é obrigatório.")
        MovimentacaoEstoqueEnum tipo,

        @NotNull(message = "A quantidade é obrigatória.")
        @Positive(message = "A quantidade movimentada deve ser maior que zero.")
        Integer quantidade,

        String observacao
) {}
