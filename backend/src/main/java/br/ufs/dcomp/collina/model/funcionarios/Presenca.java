package br.ufs.dcomp.collina.model.funcionarios;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_presenca")
public class Presenca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = true)
    private LocalDateTime entrada;

    @Column(nullable = true)
    private LocalDateTime saida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPresenca status = StatusPresenca.PRESENTE;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_alocacao", nullable = false, unique = true)
    private Alocacao alocacao;

    /**
     * Duração da presença calculada em minutos a partir de entrada e saída.
     */
    public long getDuracaoPresencaMinutos() {
        if (entrada == null || saida == null) {
            return 0;
        }
        long minutos = ChronoUnit.MINUTES.between(entrada, saida);
        return Math.max(0, minutos);
    }

    /**
     * Duração da presença em horas decimais.
     */
    public double getDuracaoPresencaHoras() {
        return getDuracaoPresencaMinutos() / 60.0;
    }

    /**
     * Duração da presença formatada (ex: "07h45").
     */
    public String getDuracaoPresenca() {
        long minutos = getDuracaoPresencaMinutos();
        long horas = minutos / 60;
        long resto = minutos % 60;
        return String.format("%02dh%02d", horas, resto);
    }
}
