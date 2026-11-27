package br.ufs.dcomp.collina.model.associados;

import br.ufs.dcomp.collina.model.auth.Pessoa;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Associado titular (Proprietário ou Contribuinte)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_associado")
public class Associado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(optional = false)
    @JoinColumn(name = "pessoa_id", unique = true)
    private Pessoa pessoa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaAssociadoEnum categoria;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plano_id")
    private Plano plano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoCadastralEnum situacaoCadastral = SituacaoCadastralEnum.ATIVO;

    private LocalDate dataAdmissao;

    // Vínculo com o usuário/credenciais do módulo de autenticação (AUTH01)
    private String usuarioId;

    @OneToMany(mappedBy = "titular")
    private List<VinculoDependencia> vinculos = new ArrayList<>();
}
