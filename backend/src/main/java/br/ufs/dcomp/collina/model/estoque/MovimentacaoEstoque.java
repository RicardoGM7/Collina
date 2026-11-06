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
    private Instant data;

    @Enumerated(EnumType.STRING)
    private MovimentacaoEstoqueEnum tipo;
    private Integer quantidade;
    //private Funcionario responsavel;
    private String observacao;

    @ManyToOne
    @JoinColumn(name = "estoque_id")
    private Estoque estoque;

}
