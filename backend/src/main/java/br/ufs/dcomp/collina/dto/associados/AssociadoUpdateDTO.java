package br.ufs.dcomp.collina.dto.associados;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

// RF-AS05: campos que o próprio titular pode alterar (CPF, categoria, plano e situação ficam de fora)
public record AssociadoUpdateDTO(
        @NotBlank(message = "O nome é obrigatório.")
        String nome,

        @NotNull(message = "A data de nascimento é obrigatória.")
        @Past(message = "A data de nascimento deve estar no passado.")
        LocalDate dataNascimento
) {
}
