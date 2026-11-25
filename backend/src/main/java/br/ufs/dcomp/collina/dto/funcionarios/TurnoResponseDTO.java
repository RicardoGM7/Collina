package br.ufs.dcomp.collina.dto.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.Turno;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record TurnoResponseDTO(
        Integer id,
        String nome,
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaFim,
        String duracao,
        Long duracaoMinutos,
        Double duracaoHoras
) {
    public TurnoResponseDTO(Turno turno) {
        this(
                turno.getId(),
                turno.getNome(),
                turno.getHoraInicio(),
                turno.getHoraFim(),
                turno.getDuracao(),
                turno.getDuracaoMinutos(),
                turno.getDuracaoHoras()
        );
    }
}
