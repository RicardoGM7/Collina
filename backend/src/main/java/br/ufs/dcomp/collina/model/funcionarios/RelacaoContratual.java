package br.ufs.dcomp.collina.model.funcionarios;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_relacao_contratual")
public class RelacaoContratual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoContrato tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal salario;

    @Column(nullable = false)
    private LocalDate dataInicio;

    @Column(nullable = true)
    private LocalDate dataFim;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    public boolean isVigenteEm(LocalDate data) {
        if (data == null) {
            return false;
        }
        boolean aposOuNoInicio = !data.isBefore(dataInicio);
        boolean antesOuNoFim = (dataFim == null || !data.isAfter(dataFim));
        return aposOuNoInicio && antesOuNoFim;
    }
}
