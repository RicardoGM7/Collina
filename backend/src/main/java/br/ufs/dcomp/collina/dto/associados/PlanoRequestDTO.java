package br.ufs.dcomp.collina.dto.associados;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

// RF-AS02: parâmetros do plano
public record PlanoRequestDTO(
        @NotNull(message = "O valor periódico do plano é obrigatório.")
        @PositiveOrZero(message = "O valor periódico não pode ser negativo.")
        @Digits(integer = 8, fraction = 2, message = "O valor periódico deve ter no máximo 2 casas decimais.")
        BigDecimal valorPeriodico,

        @NotNull(message = "O limite mensal de reservas é obrigatório.")
        @PositiveOrZero(message = "O limite mensal de reservas não pode ser negativo.")
        Integer limiteMensalReservas,

        @NotNull(message = "A quantidade máxima de dependentes ativos é obrigatória.")
        @PositiveOrZero(message = "A quantidade máxima de dependentes ativos não pode ser negativa.")
        Integer maxDependentesAtivos
) {
}
