package br.ufs.dcomp.collina.dto.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.RelacaoContratual;
import br.ufs.dcomp.collina.model.funcionarios.TipoContrato;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RelacaoContratualResponseDTO(
        Integer id,
        Integer funcionarioId,
        String nomeFuncionario,
        TipoContrato tipo,
        BigDecimal salario,
        LocalDate dataInicio,
        LocalDate dataFim,
        boolean vigente
) {
    public RelacaoContratualResponseDTO(RelacaoContratual r) {
        this(
                r.getId(),
                r.getFuncionario() != null ? r.getFuncionario().getId() : null,
                r.getFuncionario() != null ? r.getFuncionario().getNome() : null,
                r.getTipo(),
                r.getSalario(),
                r.getDataInicio(),
                r.getDataFim(),
                r.isVigenteEm(LocalDate.now())
        );
    }
}
