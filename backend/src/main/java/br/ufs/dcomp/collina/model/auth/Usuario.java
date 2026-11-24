package br.ufs.dcomp.collina.model.auth;

import br.ufs.dcomp.collina.model.associados.Pessoa;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Dados de acesso do usuário no sistema (separados dos dados civis em tb_pessoa).
// A fonte única de verdade para perfil (RBAC) e status é esta tabela, e não o token do Firebase.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "firebase_uid", nullable = false, unique = true)
    private String firebaseUid;

    @Column(nullable = false, unique = true)
    private String email;

    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PerfilUsuarioEnum perfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusUsuarioEnum status = StatusUsuarioEnum.ATIVO;

    // Cardinalidade 0..1: nem toda pessoa possui conta de acesso (ex.: dependentes),
    // e uma conta de acesso vincula-se a no máximo uma pessoa.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id", unique = true)
    private Pessoa pessoa;

    public boolean isAtivo() {
        return status == StatusUsuarioEnum.ATIVO;
    }
}
