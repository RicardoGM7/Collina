package br.ufs.dcomp.collina.dto.espacos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.List;

// UC-GE02: o titular é identificado pelo token (nunca pelo corpo da requisição).
public record ReservaRequestDTO(
        @NotNull(message = "O espaço é obrigatório.")
        Integer espacoId,

        @NotNull(message = "A data de uso é obrigatória.")
        LocalDate data,

        @NotEmpty(message = "Informe ao menos um bloco de horário.")
        List<@NotNull(message = "Bloco de horário inválido.") Integer> blocoIds,

        @PositiveOrZero(message = "A quantidade de convidados não pode ser negativa.")
        Integer qtdConvidados,

        // Id do vínculo de dependência (opcional)
        Integer dependenteId
) {
}
