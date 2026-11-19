package br.ufs.dcomp.collina.model.associados;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_plano")
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private TipoPlanoEnum tipo;

    // RF-AS02: parâmetros do plano
    @Column(nullable = false)
    private BigDecimal mensalidade;

    @Column(nullable = false)
    private Integer limiteMensalReservas;

    @Column(nullable = false)
    private Integer maxDependentesAtivos;
}
