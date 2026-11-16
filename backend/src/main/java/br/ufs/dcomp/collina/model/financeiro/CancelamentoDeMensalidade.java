package br.ufs.dcomp.collina.model.financeiro;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity 
@Table(name= "tb_cancelamento_de_mensalidade")
public class CancelamentoDeMensalidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length= 500)
    private String justificativa;

    @Column(nullable = false)
    private LocalDateTime dataCancelamento;

    //FK - Mensalidade
    @OneToOne
    @JoinColumn(name = "mensalidade_cancelada_id", nullable = false, unique = true)
    private Mensalidade mensalidadeCancelada;

    //FK - Mensalidade
    @OneToOne
    @JoinColumn(name = "mensalidade_substituta_id", nullable = false, unique = true)
    private Mensalidade mensalidadeSubstituta;

    //FK - Funcionario
    /*
    @ManyToOne
    @JoinColumn(name = "funcionario_id", nullable = false)
    */
    @Column(nullable= false)
    private Long funcionarioId;

    public CancelamentoDeMensalidade(String justificativa, LocalDateTime dataCancelamento,
            Mensalidade mensalidadeCancelada, Mensalidade mensalidadeSubstituta, Long funcionario) {
        
        if(justificativa == null){throw (new IllegalArgumentException("Justificativa não pode ser nula"));}
        if(mensalidadeCancelada == null){throw (new IllegalArgumentException("Mensalidade cancelada não pode ser nula"));}
        if(mensalidadeSubstituta == null){throw (new IllegalArgumentException("Mensalidade substituta não pode ser nula"));}
        if(dataCancelamento == null){throw (new IllegalArgumentException("Data de cancelamento não pode ser nula"));}
        if(funcionario == null){throw (new IllegalArgumentException("Funcionario não pode ser nulo"));}

        if(!mensalidadeSubstituta.estaPendente()){throw (new IllegalStateException("Mensalidade substituta com estado invalido"));}
        if(!mensalidadeCancelada.estaCancelada()){throw (new IllegalStateException("Mensalidade cancelada com estado invalido"));}

        this.justificativa = justificativa;
        this.dataCancelamento = dataCancelamento;
        this.mensalidadeCancelada = mensalidadeCancelada;
        this.mensalidadeSubstituta = mensalidadeSubstituta;
        this.funcionarioId = funcionario;
    }

}
