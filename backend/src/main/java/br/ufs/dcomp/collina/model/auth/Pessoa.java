package br.ufs.dcomp.collina.model.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_pessoa")
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    // RF-AS01: o CPF identifica unicamente a pessoa (titular, dependente ou funcionário)
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(nullable = false)
    private LocalDate dataNascimento;

    // RF-AS04: idade na data informada (ex.: data de utilização da reserva)
    public int idadeEm(LocalDate data) {
        return Period.between(dataNascimento, data).getYears();
    }
}
