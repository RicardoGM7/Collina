package br.ufs.dcomp.collina.model.estoque;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_movimentacaoEstoque")
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Instant data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovimentacaoEstoqueEnum tipo;

    @Column(nullable = false)
    private Integer quantidade;
    //private Funcionario responsavel;
    private String observacao;

    @ManyToOne
    @JoinColumn(name = "estoque_id", nullable = false)
    private Estoque estoque;

}
