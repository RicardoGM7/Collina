package br.ufs.dcomp.collina.model.espacos;

import br.ufs.dcomp.collina.model.associados.Associado;
import br.ufs.dcomp.collina.model.associados.VinculoDependencia;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

// Reserva de UM bloco de 2h. Reservar vários blocos gera várias reservas, e cada uma conta
// individualmente na cota mensal do plano (RF-AS02).
@Entity
@Table(
        name = "tb_reserva",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_reserva_slot_ativo",
                columnNames = {"espaco_fisico_id", "bloco_horario_id", "data_uso", "slot_ativo"}))
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "data_uso", nullable = false)
    private LocalDate dataUso;

    @Column(name = "qtd_convidados", nullable = false)
    private Integer qtdConvidados = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusReservaEnum status = StatusReservaEnum.CONFIRMADA;

    @ManyToOne(optional = false)
    @JoinColumn(name = "espaco_fisico_id")
    private EspacoFisico espaco;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bloco_horario_id")
    private BlocoHorario bloco;

    @ManyToOne(optional = false)
    @JoinColumn(name = "associado_titular_id")
    private Associado titular;

    // Dependente beneficiário (opcional)
    @ManyToOne
    @JoinColumn(name = "dependente_id")
    private VinculoDependencia dependente;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "cancelada_em")
    private Instant canceladaEm;

    // Trava contra reserva duplicada no banco: vale TRUE enquanto a reserva está CONFIRMADA e vira
    // NULL ao cancelar. Como o PostgreSQL trata NULLs como distintos na restrição única, só pode existir
    // uma reserva confirmada por (espaço, bloco, data), e o horário cancelado volta a ficar livre.
    @Column(name = "slot_ativo")
    private Boolean slotAtivo = Boolean.TRUE;

    public void cancelar(Instant quando) {
        this.status = StatusReservaEnum.CANCELADA;
        this.canceladaEm = quando;
        this.slotAtivo = null;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public LocalDate getDataUso() { return dataUso; }
    public void setDataUso(LocalDate dataUso) { this.dataUso = dataUso; }

    public Integer getQtdConvidados() { return qtdConvidados; }
    public void setQtdConvidados(Integer qtdConvidados) { this.qtdConvidados = qtdConvidados; }

    public StatusReservaEnum getStatus() { return status; }
    public void setStatus(StatusReservaEnum status) { this.status = status; }

    public EspacoFisico getEspaco() { return espaco; }
    public void setEspaco(EspacoFisico espaco) { this.espaco = espaco; }

    public BlocoHorario getBloco() { return bloco; }
    public void setBloco(BlocoHorario bloco) { this.bloco = bloco; }

    public Associado getTitular() { return titular; }
    public void setTitular(Associado titular) { this.titular = titular; }

    public VinculoDependencia getDependente() { return dependente; }
    public void setDependente(VinculoDependencia dependente) { this.dependente = dependente; }

    public Instant getCriadaEm() { return criadaEm; }
    public void setCriadaEm(Instant criadaEm) { this.criadaEm = criadaEm; }

    public Instant getCanceladaEm() { return canceladaEm; }
    public void setCanceladaEm(Instant canceladaEm) { this.canceladaEm = canceladaEm; }

    public Boolean getSlotAtivo() { return slotAtivo; }
    public void setSlotAtivo(Boolean slotAtivo) { this.slotAtivo = slotAtivo; }
}
