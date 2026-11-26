package br.ufs.dcomp.collina.dto.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.Afastamento;
import br.ufs.dcomp.collina.model.funcionarios.TipoAfastamento;

import java.time.LocalDate;

public record AfastamentoResponseDTO(
        Integer id,
        Integer funcionarioId,
        String nomeFuncionario,
        LocalDate dataInicio,
        LocalDate dataFim,
        TipoAfastamento tipo
) {
    public AfastamentoResponseDTO(Afastamento a) {
        this(
                a.getId(),
                a.getFuncionario() != null ? a.getFuncionario().getId() : null,
                a.getFuncionario() != null ? a.getFuncionario().getNome() : null,
                a.getDataInicio(),
                a.getDataFim(),
                a.getTipo()
        );
    }
}
