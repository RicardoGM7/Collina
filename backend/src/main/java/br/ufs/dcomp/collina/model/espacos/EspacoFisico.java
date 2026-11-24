package br.ufs.dcomp.collina.model.espacos;

import jakarta.persistence.*;

// Local do clube que pode ser reservado (quadra, salão, churrasqueira...). RF-ESP01 / RD-04.
// Escrita sem Lombok de propósito (getters/setters explícitos).
@Entity
@Table(name = "tb_espaco_fisico")
public class EspacoFisico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    // Espaço público = livre acesso, sem controle de ocupação (a capacidade não se aplica)
    @Column(name = "is_publico", nullable = false)
    private boolean publico = false;

    // Espaço de uso exclusivo para treino: não aceita reservas de associados
    @Column(name = "uso_exclusivo_treino", nullable = false)
    private boolean usoExclusivoTreino = false;

    @Column(name = "limite_convidados", nullable = false)
    private Integer limiteConvidados;

    // Capacidade máxima do AVCB. Obrigatória quando o espaço não é público (RD-04).
    @Column(name = "capacidade_maxima")
    private Integer capacidadeMaxima;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEspacoEnum estado = EstadoEspacoEnum.ATIVO;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public boolean isPublico() { return publico; }
    public void setPublico(boolean publico) { this.publico = publico; }

    public boolean isUsoExclusivoTreino() { return usoExclusivoTreino; }
    public void setUsoExclusivoTreino(boolean usoExclusivoTreino) { this.usoExclusivoTreino = usoExclusivoTreino; }

    public Integer getLimiteConvidados() { return limiteConvidados; }
    public void setLimiteConvidados(Integer limiteConvidados) { this.limiteConvidados = limiteConvidados; }

    public Integer getCapacidadeMaxima() { return capacidadeMaxima; }
    public void setCapacidadeMaxima(Integer capacidadeMaxima) { this.capacidadeMaxima = capacidadeMaxima; }

    public EstadoEspacoEnum getEstado() { return estado; }
    public void setEstado(EstadoEspacoEnum estado) { this.estado = estado; }

    // Espaço que associados podem enxergar e reservar
    public boolean isReservavel() {
        return estado == EstadoEspacoEnum.ATIVO && !usoExclusivoTreino;
    }
}
