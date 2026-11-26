package br.ufs.dcomp.collina.dto.funcionarios;

import br.ufs.dcomp.collina.model.funcionarios.Presenca;
import br.ufs.dcomp.collina.model.funcionarios.StatusPresenca;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PresencaResponseDTO(
        Integer id,
        Integer alocacaoId,
        Integer funcionarioId,
        String nomeFuncionario,
        LocalDate dataAlocacao,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime entrada,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime saida,
        StatusPresenca status,
        String duracaoPresenca,
        Long duracaoPresencaMinutos
) {
    public PresencaResponseDTO(Presenca presenca) {
        this(
                presenca.getId(),
                presenca.getAlocacao() != null ? presenca.getAlocacao().getId() : null,
                (presenca.getAlocacao() != null && presenca.getAlocacao().getFuncionario() != null)
                        ? presenca.getAlocacao().getFuncionario().getId() : null,
                (presenca.getAlocacao() != null && presenca.getAlocacao().getFuncionario() != null)
                        ? presenca.getAlocacao().getFuncionario().getNome() : null,
                presenca.getAlocacao() != null ? presenca.getAlocacao().getData() : null,
                presenca.getEntrada(),
                presenca.getSaida(),
                presenca.getStatus(),
                presenca.getDuracaoPresenca(),
                presenca.getDuracaoPresencaMinutos()
        );
    }
}
