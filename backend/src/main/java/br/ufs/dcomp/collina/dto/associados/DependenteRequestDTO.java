package br.ufs.dcomp.collina.dto.associados;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

// RF-AS03: nome, CPF, data de nascimento e vínculo (parentesco) com o titular
public record DependenteRequestDTO(
        @NotBlank(message = "O nome do dependente é obrigatório.")
        String nome,

        @NotBlank(message = "O CPF do dependente é obrigatório.")
        @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos numéricos.")
        String cpf,

        @NotNull(message = "A data de nascimento do dependente é obrigatória.")
        @Past(message = "A data de nascimento deve estar no passado.")
        LocalDate dataNascimento,

        @NotBlank(message = "O parentesco com o titular é obrigatório.")
        String parentesco
) {
}
