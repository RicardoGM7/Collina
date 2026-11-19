package br.ufs.dcomp.collina.model.associados;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

// Dependente não é usuário: é uma Pessoa vinculada a um titular.
// O vínculo é histórico (nunca apagado) para suportar RF-AS03/AS04/AS06.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_vinculo_dependencia")
public class VinculoDependencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "titular_id")
    private Associado titular;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dependente_id")
    private Pessoa dependente;

    private String parentesco;

    @Column(nullable = false)
    private LocalDate dataInicio;

    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusVinculoEnum status = StatusVinculoEnum.ATIVO;

    @Enumerated(EnumType.STRING)
    private MotivoInativacaoVinculoEnum motivoInativacao;
}
