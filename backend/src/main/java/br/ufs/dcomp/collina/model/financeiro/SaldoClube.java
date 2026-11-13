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
@Table(name = "tb_saldo_clube")
public class SaldoClube {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    public void creditar(BigDecimal valor) {
        this.valor = this.valor.add(valor);
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void debitar(BigDecimal valor) {
        this.valor = this.valor.subtract(valor);
        this.dataAtualizacao = LocalDateTime.now();
    }

    public boolean possuiSaldoPara(BigDecimal valor) { return this.valor.compareTo(valor) >= 0; }
}