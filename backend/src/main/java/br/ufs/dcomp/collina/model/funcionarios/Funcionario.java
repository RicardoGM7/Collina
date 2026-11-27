package br.ufs.dcomp.collina.model.funcionarios;

import br.ufs.dcomp.collina.model.auth.Pessoa;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_funcionario")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false, unique = true)
    private Pessoa pessoa;

    @Column(nullable = false)
    private LocalDate dataAdmissao;

    @Column(nullable = false)
    private String cargo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusFuncionario status = StatusFuncionario.ATIVO;

    @JsonIgnore
    @OneToMany(mappedBy = "funcionario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RelacaoContratual> relacoesContratuais = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "funcionario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ferias> ferias = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "funcionario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Afastamento> afastamentos = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "funcionario")
    private List<Alocacao> alocacoes = new ArrayList<>();

    public boolean isAtivo() {
        return StatusFuncionario.ATIVO.equals(this.status);
    }

    public String getNome() {
        return pessoa != null ? pessoa.getNome() : null;
    }

    public String getCpf() {
        return pessoa != null ? pessoa.getCpf() : null;
    }

    public LocalDate getDataNascimento() {
        return pessoa != null ? pessoa.getDataNascimento() : null;
    }
}
