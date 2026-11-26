package br.ufs.dcomp.collina.dto.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.TipoAfastamento;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AfastamentoRequestDTO(
        @NotNull(message = "O ID do funcionário é obrigatório.")
        Integer funcionarioId,

        @NotNull(message = "A data de início é obrigatória.")
        LocalDate dataInicio,

        @NotNull(message = "A data de fim é obrigatória.")
        LocalDate dataFim,

        @NotNull(message = "O tipo de afastamento é obrigatório.")
        TipoAfastamento tipo
) {}
