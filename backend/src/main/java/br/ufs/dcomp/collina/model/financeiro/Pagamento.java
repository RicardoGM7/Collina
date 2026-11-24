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

    // unique: o webhook localiza o pagamento por este id, então não pode haver dois iguais.
    // (O PostgreSQL aceita vários nulos em coluna única: pagamentos manuais não têm id externo.)
    @Column(unique = true)
    private String idExternoGateway;

    @Column
    private String comprovante;

    @Column(nullable = true)
    private LocalDateTime dataPagamento;

    // Dados da cobrança no gateway (preenchidos só nos pagamentos feitos pelo gateway).
    // Ficam guardados para devolver a mesma cobrança se o usuário pedir de novo antes de ela vencer.
    @Column
    private LocalDateTime expiraEm;

    @Column(length = 500)
    private String urlPagamento;

    @Column(length = 500)
    private String codigoPix;

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

    // Guarda os dados da cobrança criada no gateway. Só vale enquanto o pagamento está AGUARDANDO.
    public void vincularCobranca(String idExternoGateway, String urlPagamento, String codigoPix, LocalDateTime expiraEm){
        if(!this.estaAguardando()){
            throw (new IllegalStateException("Pagamentos com status: " + this.status + " não podem receber uma cobrança"));
        }

        if(idExternoGateway == null || idExternoGateway.isBlank()){
            throw (new IllegalArgumentException("O id externo da cobrança é obrigatório"));
        }

        if(expiraEm == null){
            throw (new IllegalArgumentException("A validade da cobrança é obrigatória"));
        }

        this.idExternoGateway = idExternoGateway;
        this.urlPagamento = urlPagamento;
        this.codigoPix = codigoPix;
        this.expiraEm = expiraEm;
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

    // Cobrança sem validade registrada (ainda não criada no gateway) não é considerada vencida.
    public boolean cobrancaVencida(LocalDateTime agora){
        return this.expiraEm != null && !agora.isBefore(this.expiraEm);
    }

    public boolean estaAguardando(){return this.status == StatusTransacaoEnum.AGUARDANDO;}
    public boolean foiConfirmado(){return this.status == StatusTransacaoEnum.CONFIRMADO;}
}
