package br.ufs.dcomp.collina.model.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity 
@Table(name = "tb_mensalidade")
public class Mensalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusMensalidadeEnum statusMensalidade;

    @Column(nullable = false, length = 7)
    private String competencia;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorBase;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorFinal;

    @Column(nullable = true)
    private LocalDate dataVencimento;

    // FK - associado
    @Column(nullable = false, name= "associado_titular_id")
    /*
    @OneToMany
    @JoinColumn("name = associado_titular_id", nullable = false)
    */
    private Long associadoTitular;

    // Composição com gastos avulsos 
    @OneToMany(mappedBy = "mensalidade", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GastoAvulso> gastosAvulsos = new ArrayList<>();

    
    //--------------------------------
    // Metodos

    public Mensalidade(String competencia, BigDecimal valorBase, Long associadoTitular) {

        if(competencia == null){throw (new IllegalArgumentException("competencia não pode ser nula"));}
        if(associadoTitular == null){throw (new IllegalArgumentException("Associado titular não pode ser nulo"));}
        if(valorBase == null){throw (new IllegalArgumentException("valor base não pode ser nulo"));}

        if(valorBase.compareTo(BigDecimal.ZERO) < 0){throw (new IllegalArgumentException("Valor base não pode ser negativo"));}

        this.competencia = competencia;
        this.valorFinal = valorBase; /*valor final iniciado como valor base */
        this.valorBase = valorBase; 
        this.associadoTitular = associadoTitular;

        /*TODA mensalidade é gerada com status EM ABERTO*/
        this.statusMensalidade = StatusMensalidadeEnum.EM_ABERTO;
        /*Data de vencimento só é atribuida depois que a mensalidade é fechada */
        this.dataVencimento = null;
    }

    public BigDecimal calcularValorTotal(){
        if(!this.estaEmAberto()){
            throw (new IllegalStateException("Mensalidades com status: " + this.statusMensalidade + " não podem ter seu valor recalculado"));
        }

        BigDecimal acumulador = BigDecimal.ZERO;
        // soma os valores de todos os gastos avulsos
        for (GastoAvulso gasto : this.gastosAvulsos) {
            acumulador = acumulador.add(gasto.getValor());
        }

        return this.valorBase.add(acumulador);
    }

    public Mensalidade reemitir(BigDecimal novoValor, LocalDate novoVencimento){
        if(this.statusMensalidade != StatusMensalidadeEnum.CANCELADA){
            throw (new IllegalStateException("Apenas mensalidades CANCELADAS podem ser reemitidas, a mensalidade atual tem status: " + this.statusMensalidade));
        }

        if(novoValor == null){
            throw (new IllegalArgumentException("Insira um valor valido"));
        }

        if(novoVencimento == null){
            throw (new IllegalArgumentException("Insira uma data de vencimento valida"));
        }

        if(novoValor.compareTo(BigDecimal.ZERO) < 0){
            throw (new IllegalArgumentException("O novo valor não pode ser negativo"));
        }
        
        Mensalidade substituta = new Mensalidade(this.competencia, this.valorBase, this.associadoTitular);

        substituta.setDataVencimento(novoVencimento);
        substituta.setValorFinal(novoValor);
        substituta.setStatusMensalidade(StatusMensalidadeEnum.PENDENTE);
        
        return substituta;
    }

    public void adicionarGastoAvulso(GastoAvulso gasto){
        if(gasto == null){
            throw (new IllegalArgumentException("Insaria um gasto avulso valido"));
        }

        if(!this.estaEmAberto()){
            throw (new IllegalStateException("Mensalidades com status: " + this.statusMensalidade + " não aceitam gastos avulsos"));  
        }

        if(gasto.getValor() == null){
            throw (new IllegalArgumentException("Gasto avulso com valor invalido"));
        }

        if(gasto.getValor().compareTo(BigDecimal.ZERO) < 0){
            throw (new IllegalArgumentException("O valor do gasto não pode ser negativo"));
        }

        if(gasto.getDataLancamento() == null){
            throw (new IllegalArgumentException("A data de lançamento não pode ser nula"));
        }

        gasto.setMensalidade(this);
        this.gastosAvulsos.add(gasto);
        this.valorFinal = this.valorFinal.add(gasto.getValor());
    }

    public void fechar(LocalDate vencimento){
        if(!this.estaEmAberto()){
            throw (new IllegalStateException("Mensalidade com status: " + this.statusMensalidade + " não pode ser fechada"));
        }

        if(vencimento == null){
            throw (new IllegalArgumentException("Data de vencimento invalida"));
        }

        this.valorFinal = this.calcularValorTotal();
        this.statusMensalidade = StatusMensalidadeEnum.PENDENTE;
        this.dataVencimento = vencimento;
    }

    public void cancelar(){
        // Apenas mensalidades atrasadas ou pendentes podem ser canceladas
        if(!(this.estaPendente() || this.estaAtrasada())){
            throw (new IllegalStateException("Mensalidade com status: "+ this.statusMensalidade +" não pode ser cancelada"));
        }

        this.statusMensalidade = StatusMensalidadeEnum.CANCELADA;
    }

    public void marcarComoPaga(){
        if(this.statusMensalidade == StatusMensalidadeEnum.PAGA){
            throw (new IllegalStateException("Mensalidade já paga"));
        }

        if(!(this.estaPendente() || this.estaAtrasada())){
            throw (new IllegalStateException("Mensalidade com status: "+ this.statusMensalidade +" não pode ser marcada como paga"));
        }
        
        this.statusMensalidade = StatusMensalidadeEnum.PAGA;
    }

    public void marcarComoAtrasada(LocalDate hoje){
        if(hoje == null){
            throw (new IllegalArgumentException("insira uma data valida"));
        }
        
        if(!(this.statusMensalidade == StatusMensalidadeEnum.PENDENTE)){
            throw (new IllegalStateException("Apenas mensalidades PENDENTES podem ser marcadas como ATRASADAS"));
        }

        // Não confere dataVencimento == null, pois, para transitar para o estado
        // PENDENTE, ela obrigatoriamente recebe a data de vencimento
        if(!hoje.isAfter(this.dataVencimento)){
            throw (new IllegalStateException("Data de vencimento: "+ this.dataVencimento + " ainda não ultrapassada"));
        }

        this.statusMensalidade = StatusMensalidadeEnum.ATRASADA;
    }

    public boolean estaCancelada(){return this.statusMensalidade == StatusMensalidadeEnum.CANCELADA;}
    public boolean estaPendente(){return this.statusMensalidade == StatusMensalidadeEnum.PENDENTE;}
    public boolean estaAtrasada(){return this.statusMensalidade == StatusMensalidadeEnum.ATRASADA;}
    public boolean estaEmAberto(){return this.statusMensalidade == StatusMensalidadeEnum.EM_ABERTO;}
}