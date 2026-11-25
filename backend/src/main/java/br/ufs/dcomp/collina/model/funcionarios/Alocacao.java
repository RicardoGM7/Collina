package br.ufs.dcomp.collina.model.funcionarios;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_alocacao")
public class Alocacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAlocacao status = StatusAlocacao.PENDENTE;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_escala", nullable = false)
    private Escala escala;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_turno", nullable = false)
    private Turno turno;

    @JsonIgnore
    @OneToOne(mappedBy = "alocacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private Presenca presenca;
}
