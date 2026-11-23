package br.ufs.dcomp.collina.model.financeiro;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.ufs.dcomp.collina.model.associados.VinculoDependencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor 
@NoArgsConstructor
@Entity 
@Table(name= "tb_gasto_avulso")
public class GastoAvulso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable= false, precision= 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(nullable = false)
    private LocalDateTime dataLancamento;

    //FK - Mensalidade
    @ManyToOne
    @JoinColumn(name = "mensalidade_id", nullable = false)
    private Mensalidade mensalidade;

    //FK - Funcionario
    /*
    @ManyToOne
    @JoinColumn(name = "funcionario_id", nullable = false)
    */
    @Column(nullable= false)
    private Long funcionario;

    //FK - Dependencia
    @ManyToOne
    @JoinColumn(name = "vinculo_dependencia_id", nullable = true)
    private VinculoDependencia vinculoDependencia;

    public GastoAvulso(BigDecimal valor, String descricao, LocalDateTime dataLancamento, Long funcionario,
            VinculoDependencia vinculoDependencia) {
        this.valor = valor;
        this.descricao = descricao;
        this.dataLancamento = dataLancamento;
        this.funcionario = funcionario;
        this.vinculoDependencia = vinculoDependencia;
    }
}
