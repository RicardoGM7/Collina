package br.ufs.dcomp.collina.dto.financeiro;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record GastoAvulsoRequestDTO(
    @NotNull(message = "Informe o ID da pessoa responsável pelo gasto")
    Integer pessoaId,

    @NotNull(message = "O valor do gasto não pode ser nulo")
    @PositiveOrZero(message = "O valor do gasto não pode ser negativo")
    @Digits(integer = 13, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
    BigDecimal valor,

    @Size(max = 500)
    @NotBlank(message = "Informe a descrição do gasto")
    String descricao,

    @NotNull(message = "Informe o funcionário responsável pelo lançamento do gasto")
    Long funcionarioId
) {}
