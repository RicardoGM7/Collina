package br.ufs.dcomp.collina.dto.espacos;

import br.ufs.dcomp.collina.model.espacos.TipoBlocoEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// UC-GE05: horários no formato HH:mm; a duração (exatamente 2h) é validada no EspacoService.
public record BlocoRequestDTO(
        @NotBlank(message = "O horário de início é obrigatório.")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Horário de início inválido. Use HH:mm.")
        String horaInicio,

        @NotBlank(message = "O horário de término é obrigatório.")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Horário de término inválido. Use HH:mm.")
        String horaFim,

        // Opcional: se ausente, o bloco é LIVRE
        TipoBlocoEnum tipo
) {
}
