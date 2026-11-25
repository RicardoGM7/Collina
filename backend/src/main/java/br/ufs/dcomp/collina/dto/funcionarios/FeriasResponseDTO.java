package br.ufs.dcomp.collina.dto.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.Ferias;
import br.ufs.dcomp.collina.model.funcionarios.TipoFerias;

import java.time.LocalDate;

public record FeriasResponseDTO(
        Integer id,
        Integer funcionarioId,
        String nomeFuncionario,
        LocalDate dataInicio,
        LocalDate dataFim,
        TipoFerias tipo
) {
    public FeriasResponseDTO(Ferias f) {
        this(
                f.getId(),
                f.getFuncionario() != null ? f.getFuncionario().getId() : null,
                f.getFuncionario() != null ? f.getFuncionario().getNome() : null,
                f.getDataInicio(),
                f.getDataFim(),
                f.getTipo()
        );
    }
}
