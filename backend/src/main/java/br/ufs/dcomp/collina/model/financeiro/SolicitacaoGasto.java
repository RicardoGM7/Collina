package br.ufs.dcomp.collina.model.financeiro;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor 
@Entity 
@Table(name = "tb_solicitacao_gasto")
public class SolicitacaoGasto {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioridadeGastoEnum prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSolicitacaoGastoEnum status = StatusSolicitacaoGastoEnum.PENDENTE;

    @Column(nullable = false)
    private LocalDateTime dataSolicitacao;

    // FK da classe
    private Long funcionarioId;
    /*
    @ManyToOne
    private Funcionario funcionarioId;
    */

    public void efetivar() {
        this.status = StatusSolicitacaoGastoEnum.EFETIVADA;
    }

    public boolean estaPendente() {
        return status == StatusSolicitacaoGastoEnum.PENDENTE;
    }
}
