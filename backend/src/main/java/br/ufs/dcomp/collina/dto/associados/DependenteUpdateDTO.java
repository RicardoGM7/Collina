package br.ufs.dcomp.collina.dto.associados;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

// RF-AS03: atualização dos dados pessoais do dependente (feita por funcionário; o CPF não muda)
public record DependenteUpdateDTO(
        @NotBlank(message = "O nome do dependente é obrigatório.")
        String nome,

        @NotNull(message = "A data de nascimento do dependente é obrigatória.")
        @Past(message = "A data de nascimento deve estar no passado.")
        LocalDate dataNascimento,

        @NotBlank(message = "O parentesco com o titular é obrigatório.")
        String parentesco
) {
}
