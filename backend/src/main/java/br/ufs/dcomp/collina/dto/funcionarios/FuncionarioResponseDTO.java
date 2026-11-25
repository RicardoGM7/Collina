package br.ufs.dcomp.collina.dto.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.Funcionario;
import br.ufs.dcomp.collina.model.funcionarios.RelacaoContratual;
import br.ufs.dcomp.collina.model.funcionarios.StatusFuncionario;
import br.ufs.dcomp.collina.model.funcionarios.TipoContrato;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FuncionarioResponseDTO(
        Integer id,
        String nome,
        String cpf,
        LocalDate dataNascimento,
        LocalDate dataAdmissao,
        String cargo,
        StatusFuncionario status,
        BigDecimal salarioVigente,
        TipoContrato tipoContratoVigente
) {
    public FuncionarioResponseDTO(Funcionario f) {
        this(f, null);
    }

    public FuncionarioResponseDTO(Funcionario f, RelacaoContratual contratoVigente) {
        this(
                f.getId(),
                f.getNome(),
                f.getCpf(),
                f.getDataNascimento(),
                f.getDataAdmissao(),
                f.getCargo(),
                f.getStatus(),
                contratoVigente != null ? contratoVigente.getSalario() : null,
                contratoVigente != null ? contratoVigente.getTipo() : null
        );
    }
}
