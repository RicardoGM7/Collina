package br.ufs.dcomp.collina.dto.espacos;

import br.ufs.dcomp.collina.model.espacos.EspacoFisico;
import br.ufs.dcomp.collina.model.espacos.EstadoEspacoEnum;

public record EspacoResponseDTO(
        Integer id,
        String nome,
        boolean publico,
        boolean usoExclusivoTreino,
        Integer limiteConvidados,
        Integer capacidadeMaxima,
        EstadoEspacoEnum estado
) {
    public EspacoResponseDTO(EspacoFisico espaco) {
        this(
                espaco.getId(),
                espaco.getNome(),
                espaco.isPublico(),
                espaco.isUsoExclusivoTreino(),
                espaco.getLimiteConvidados(),
                espaco.getCapacidadeMaxima(),
                espaco.getEstado()
        );
    }
}
