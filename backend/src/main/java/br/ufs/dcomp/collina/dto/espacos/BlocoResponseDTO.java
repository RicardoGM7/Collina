package br.ufs.dcomp.collina.dto.espacos;

import br.ufs.dcomp.collina.model.espacos.BlocoHorario;
import br.ufs.dcomp.collina.model.espacos.TipoBlocoEnum;

import java.time.format.DateTimeFormatter;

public record BlocoResponseDTO(
        Integer id,
        Integer espacoId,
        String horaInicio,
        String horaFim,
        TipoBlocoEnum tipo
) {
    public static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public BlocoResponseDTO(BlocoHorario bloco) {
        this(
                bloco.getId(),
                bloco.getEspaco().getId(),
                bloco.getHoraInicio().format(HORA),
                bloco.getHoraFim().format(HORA),
                bloco.getTipo()
        );
    }
}
