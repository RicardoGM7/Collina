package br.ufs.dcomp.collina.dto.espacos;

import br.ufs.dcomp.collina.model.espacos.Reserva;
import br.ufs.dcomp.collina.model.espacos.StatusReservaEnum;

import java.time.LocalDate;

public record ReservaResponseDTO(
        Integer id,
        LocalDate dataUso,
        String horaInicio,
        String horaFim,
        Integer espacoId,
        String espacoNome,
        Integer blocoId,
        Integer qtdConvidados,
        StatusReservaEnum status,
        Integer titularId,
        String titularNome,
        Integer dependenteId,
        String dependenteNome
) {
    public ReservaResponseDTO(Reserva reserva) {
        this(
                reserva.getId(),
                reserva.getDataUso(),
                reserva.getBloco().getHoraInicio().format(BlocoResponseDTO.HORA),
                reserva.getBloco().getHoraFim().format(BlocoResponseDTO.HORA),
                reserva.getEspaco().getId(),
                reserva.getEspaco().getNome(),
                reserva.getBloco().getId(),
                reserva.getQtdConvidados(),
                reserva.getStatus(),
                reserva.getTitular().getId(),
                reserva.getTitular().getPessoa().getNome(),
                reserva.getDependente() != null ? reserva.getDependente().getId() : null,
                reserva.getDependente() != null ? reserva.getDependente().getDependente().getNome() : null
        );
    }
}
