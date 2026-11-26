package br.ufs.dcomp.collina.model.funcionarios;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_turno")
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFim;

    /**
     * Duração calculada em minutos a partir de horaInicio e horaFim.
     * Suporta turnos noturnos que atravessam a meia-noite (ex: 22:00 às 06:00).
     */
    public long getDuracaoMinutos() {
        if (horaInicio == null || horaFim == null) {
            return 0;
        }
        long minutos = ChronoUnit.MINUTES.between(horaInicio, horaFim);
        if (minutos <= 0) {
            minutos += 24 * 60;
        }
        return minutos;
    }

    /**
     * Duração calculada em horas decimais (ex: 8.0).
     */
    public double getDuracaoHoras() {
        return getDuracaoMinutos() / 60.0;
    }

    /**
     * Duração formatada (ex: "08h00").
     */
    public String getDuracao() {
        long minutos = getDuracaoMinutos();
        long horas = minutos / 60;
        long resto = minutos % 60;
        return String.format("%02dh%02d", horas, resto);
    }
}
