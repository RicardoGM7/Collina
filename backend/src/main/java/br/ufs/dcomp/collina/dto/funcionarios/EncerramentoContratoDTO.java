package br.ufs.dcomp.collina.dto.funcionarios;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EncerramentoContratoDTO(
        @NotNull(message = "A data de fim é obrigatória.")
        LocalDate dataFim,

        @NotNull(message = "A quantidade de dias trabalhados no período é obrigatória.")
        @Min(value = 1, message = "A quantidade de dias trabalhados deve ser de no mínimo 1.")
        Integer diasTrabalhados
) {}
