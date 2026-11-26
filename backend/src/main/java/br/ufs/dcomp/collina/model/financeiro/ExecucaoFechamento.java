package br.ufs.dcomp.collina.model.financeiro;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro persistido de uma execução do fechamento mensal de mensalidades.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_execucao_fechamento")
public class ExecucaoFechamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 7)
    private String competencia;

    @Column(nullable = false)
    private int processados;

    @Column(nullable = false)
    private int ignorados;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_execucao_fechamento_falha", joinColumns = @JoinColumn(name = "execucao_fechamento_id"))
    @Column(name = "associado_id")
    private List<Integer> titularesComFalha = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime dataExecucao;

    public ExecucaoFechamento(
            String competencia,
            int processados,
            int ignorados,
            List<Integer> titularesComFalha,
            LocalDateTime dataExecucao) {
        this.competencia = competencia;
        this.processados = processados;
        this.ignorados = ignorados;
        this.titularesComFalha = titularesComFalha != null ? new ArrayList<>(titularesComFalha) : new ArrayList<>();
        this.dataExecucao = dataExecucao;
    }
}
