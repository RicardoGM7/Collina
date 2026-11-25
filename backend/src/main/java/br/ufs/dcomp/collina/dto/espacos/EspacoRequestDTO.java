package br.ufs.dcomp.collina.dto.espacos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// RF-ESP01. As regras entre campos (capacidade obrigatória se não for público; convidados <= capacidade)
// são validadas no EspacoService.
public record EspacoRequestDTO(
        @NotBlank(message = "O nome do espaço é obrigatório.")
        @Size(max = 100, message = "O nome do espaço deve ter no máximo 100 caracteres.")
        String nome,

        Boolean publico,

        Boolean usoExclusivoTreino,

        @NotNull(message = "O limite de convidados é obrigatório.")
        @PositiveOrZero(message = "O limite de convidados não pode ser negativo.")
        Integer limiteConvidados,

        @Positive(message = "A capacidade máxima deve ser maior que zero.")
        Integer capacidadeMaxima
) {
}
