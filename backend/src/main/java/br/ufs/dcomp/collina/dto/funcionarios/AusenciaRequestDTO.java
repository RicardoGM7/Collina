package br.ufs.dcomp.collina.dto.funcionarios;

import jakarta.validation.constraints.NotNull;

public record AusenciaRequestDTO(
        @NotNull(message = "O ID da alocação é obrigatório.")
        Integer alocacaoId,

        boolean justificada,

        String motivo
) {}
