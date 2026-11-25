package br.ufs.dcomp.collina.dto.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.Alocacao;
import br.ufs.dcomp.collina.model.funcionarios.StatusAlocacao;

import java.time.LocalDate;

public record AlocacaoResponseDTO(
        Integer id,
        Integer funcionarioId,
        String nomeFuncionario,
        String cargoFuncionario,
        Integer escalaId,
        String periodoEscala,
        Integer turnoId,
        String nomeTurno,
        String duracaoTurno,
        LocalDate data,
        StatusAlocacao status,
        boolean presencaRegistrada
) {
    public AlocacaoResponseDTO(Alocacao alocacao) {
        this(
                alocacao.getId(),
                alocacao.getFuncionario() != null ? alocacao.getFuncionario().getId() : null,
                alocacao.getFuncionario() != null ? alocacao.getFuncionario().getNome() : null,
                alocacao.getFuncionario() != null ? alocacao.getFuncionario().getCargo() : null,
                alocacao.getEscala() != null ? alocacao.getEscala().getId() : null,
                alocacao.getEscala() != null ? alocacao.getEscala().getPeriodo() : null,
                alocacao.getTurno() != null ? alocacao.getTurno().getId() : null,
                alocacao.getTurno() != null ? alocacao.getTurno().getNome() : null,
                alocacao.getTurno() != null ? alocacao.getTurno().getDuracao() : null,
                alocacao.getData(),
                alocacao.getStatus(),
                alocacao.getPresenca() != null
        );
    }
}
