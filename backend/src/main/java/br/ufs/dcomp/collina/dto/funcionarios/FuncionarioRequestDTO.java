package br.ufs.dcomp.collina.dto.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.StatusFuncionario;
import br.ufs.dcomp.collina.model.funcionarios.TipoContrato;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
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

        StatusFuncionario status,

        // Campos contratuais opcionais no cadastro inicial
        TipoContrato tipoContrato,
        BigDecimal salario
) {
    public FuncionarioRequestDTO(String nome, String cpf, LocalDate dataNascimento, LocalDate dataAdmissao, String cargo, StatusFuncionario status) {
        this(nome, cpf, dataNascimento, dataAdmissao, cargo, status, null, null);
    }
}
