package br.ufs.dcomp.collina.dto.funcionarios;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AlocacaoRequestDTO(
        @NotNull(message = "O ID do funcionário é obrigatório.")
        Integer funcionarioId,

        @NotNull(message = "O ID da escala é obrigatório.")
        Integer escalaId,

        @NotNull(message = "O ID do turno é obrigatório.")
        Integer turnoId,

        @NotNull(message = "A data da alocação é obrigatória.")
        LocalDate data
) {}
