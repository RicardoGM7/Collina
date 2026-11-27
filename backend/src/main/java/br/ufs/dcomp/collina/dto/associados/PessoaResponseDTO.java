package br.ufs.dcomp.collina.dto.associados;

import br.ufs.dcomp.collina.model.auth.Pessoa;

import java.time.LocalDate;

public record PessoaResponseDTO(
        Integer id,
        String nome,
        String cpf,
        LocalDate dataNascimento) {

    public PessoaResponseDTO(Pessoa pessoa) {
        this(pessoa.getId(), pessoa.getNome(), pessoa.getCpf(), pessoa.getDataNascimento());
    }
}
