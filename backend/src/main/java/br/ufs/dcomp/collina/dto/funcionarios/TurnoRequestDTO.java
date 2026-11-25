package br.ufs.dcomp.collina.dto.funcionarios;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record TurnoRequestDTO(
        @NotBlank(message = "O nome do turno é obrigatório.")
        String nome,

        @NotNull(message = "A hora de início é obrigatória.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaInicio,

        @NotNull(message = "A hora de fim é obrigatória.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaFim
) {}
