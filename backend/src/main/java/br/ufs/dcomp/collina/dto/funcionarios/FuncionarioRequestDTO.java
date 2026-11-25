package br.ufs.dcomp.collina.dto.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.StatusFuncionario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

public record FuncionarioRequestDTO(
        @NotBlank(message = "O nome é obrigatório.")
        String nome,

        @NotBlank(message = "O CPF é obrigatório.")
        String cpf,

        @NotNull(message = "A data de nascimento é obrigatória.")
        LocalDate dataNascimento,

        @NotNull(message = "A data de admissão é obrigatória.")
        LocalDate dataAdmissao,

        @NotBlank(message = "O cargo é obrigatório.")
        String cargo,

        StatusFuncionario status
) {}
