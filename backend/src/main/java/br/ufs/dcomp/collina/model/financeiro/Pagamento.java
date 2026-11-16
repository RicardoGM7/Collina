package br.ufs.dcomp.collina.model.financeiro;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter 
@NoArgsConstructor
@Entity 
@Table(name= "tb_pagamento")
public class Pagamento {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable= false, precision= 15, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormaPagamentoEnum formaPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusTransacaoEnum status;

    @Column
    private String idExternoGateway;

    @Column
    private String comprovante;

    @Column(nullable = true)
    private LocalDateTime dataPagamento;

    //FK - Mensalidade
    @ManyToOne
    @JoinColumn(name = "mensalidade_id", nullable = true)
    private Mensalidade mensalidade;

    public Pagamento(BigDecimal valor, FormaPagamentoEnum formaPagamento, Mensalidade mensalidade) {
        this.valor = valor;
        this.formaPagamento = formaPagamento;
        this.mensalidade = mensalidade;

        this.status = StatusTransacaoEnum.AGUARDANDO;
    }

    public void confirmar(LocalDateTime data){
        if(!this.estaAguardando()){
            throw (new IllegalStateException("Pagamentos com status: " + this.status+ " não podem ser confirmados"));
        }

        this.status = StatusTransacaoEnum.CONFIRMADO;
        this.dataPagamento = data;
    }

    public void recusar(){
        if(!this.estaAguardando()){
            throw (new IllegalStateException("Pagamentos com status: " + this.status+ " não podem ser recusados"));
        }
        
        this.status = StatusTransacaoEnum.RECUSADO;
    }

    public void expirar(){
        if(!this.estaAguardando()){
            throw (new IllegalStateException("Pagamentos com status: " + this.status+ " não podem ser expirados"));
        }
        
        this.status = StatusTransacaoEnum.EXPIRADO;
    }

    public boolean estaAguardando(){return this.status == StatusTransacaoEnum.AGUARDANDO;}
    public boolean foiConfirmado(){return this.status == StatusTransacaoEnum.CONFIRMADO;}
}
