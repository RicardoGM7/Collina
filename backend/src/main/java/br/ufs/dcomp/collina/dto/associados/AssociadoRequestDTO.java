package br.ufs.dcomp.collina.dto.associados;

import br.ufs.dcomp.collina.model.associados.CategoriaAssociadoEnum;
import br.ufs.dcomp.collina.model.associados.TipoPlanoEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

// RF-AS01 + RF-AS02: cadastro do titular já vinculado a um plano.
// Se o CPF já existir como Pessoa (ex-dependente, RF-AS04), os dados pessoais já registrados são reaproveitados.
public record AssociadoRequestDTO(
        @NotBlank(message = "O nome é obrigatório.")
        String nome,

        @NotBlank(message = "O CPF é obrigatório.")
        @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos numéricos.")
        String cpf,

        @NotNull(message = "A data de nascimento é obrigatória.")
        @Past(message = "A data de nascimento deve estar no passado.")
        LocalDate dataNascimento,

        @NotNull(message = "A categoria é obrigatória.")
        CategoriaAssociadoEnum categoria,

        @NotNull(message = "O plano é obrigatório.")
        TipoPlanoEnum tipoPlano,

        // Id do usuário no módulo de autenticação (AUTH01), quando já existir
        String usuarioId
) {
}
