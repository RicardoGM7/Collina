package br.ufs.dcomp.collina.model.estoque;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String nome;
    private String descricao;
    private Double preco;

    @OneToMany(mappedBy = "produto")
    private List<ItemPedido> itemPedidoList = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "tb_produto_categoria",
            joinColumns =  @JoinColumn(name = "produto_id"),
            inverseJoinColumns =  @JoinColumn(name = "categoria_id")
    )
    private Set<Categoria> categorias = new HashSet<>();


    @OneToOne(mappedBy = "produto", cascade = CascadeType.ALL)
    private Estoque estoque;
}
