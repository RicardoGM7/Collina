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
@Table(name = "tb_movimentacao_financeira")
public class MovimentacaoFinanceira {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimentacaoEnum tipo; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemMovimentacaoEnum origem;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    private String descricao;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoMovimentacaoEnum situacao;

    private String motivoCancelamento;

    // FK da classe

    @Column(name = "pagamento_id", nullable = true, unique= true)
    private Long pagamentoId;
    
    private Long funcionarioId;
    /* 
    @ManyToOne 
    private Funcionario funcionarioId;
    */

    public void atualizar(String descricao, BigDecimal valor) {
        this.descricao = descricao;
        this.valor = valor;
    }

    public void cancelar(String motivo) {
        this.situacao = SituacaoMovimentacaoEnum.CANCELADO;
        this.motivoCancelamento = motivo;
    }

    public boolean ehEntrada() { return tipo == TipoMovimentacaoEnum.ENTRADA; }

    public boolean estaAtiva() { return situacao == SituacaoMovimentacaoEnum.ATIVO; }
}
