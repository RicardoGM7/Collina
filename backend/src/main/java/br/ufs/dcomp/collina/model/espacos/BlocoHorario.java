package br.ufs.dcomp.collina.model.espacos;

import jakarta.persistence.*;

import java.time.LocalTime;

// Bloco padrão e indivisível de 2 horas de um espaço (RF-ESP02). Cada espaço tem a sua grade.
@Entity
@Table(
        name = "tb_bloco_horario",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_bloco_espaco_inicio",
                columnNames = {"espaco_fisico_id", "hora_inicio"}))
public class BlocoHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "espaco_fisico_id")
    private EspacoFisico espaco;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoBlocoEnum tipo = TipoBlocoEnum.LIVRE;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public EspacoFisico getEspaco() { return espaco; }
    public void setEspaco(EspacoFisico espaco) { this.espaco = espaco; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }

    public TipoBlocoEnum getTipo() { return tipo; }
    public void setTipo(TipoBlocoEnum tipo) { this.tipo = tipo; }
}
