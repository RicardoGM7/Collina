package br.ufs.dcomp.collina.service.auth;

import br.ufs.dcomp.collina.dto.auth.UsuarioCriarRequestDTO;
import br.ufs.dcomp.collina.dto.auth.UsuarioResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.auth.PerfilUsuarioEnum;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays;

// Gerencia usuários e perfis no Firebase Auth. O perfil fica na custom claim "perfil" do usuário.
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final FirebaseAuth firebaseAuth;
    private final Set<String> emailsAdmin;

    public UsuarioService(FirebaseAuth firebaseAuth,
                          @Value("${collina.auth.admin-emails:}") String emailsAdminCsv) {
        this.firebaseAuth = firebaseAuth;
        this.emailsAdmin = Arrays.stream(emailsAdminCsv.split(","))
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toSet());
    }

    // Garante que o usuário tenha perfil. Idempotente e nunca rebaixa ninguém:
    // - e-mail configurado em collina.auth.admin-emails (e verificado) -> ADMIN
    // - usuário sem perfil (cadastro pela tela de Register) -> ASSOCIADO
    // Depois de chamar, o front deve renovar o token (getIdToken(true)) para enxergar a claim nova.
    public UsuarioResponseDTO sincronizar(String uid) {
        UserRecord usuario = buscar(uid);
        PerfilUsuarioEnum atual = perfilDe(usuario);
        PerfilUsuarioEnum alvo = atual;

        if (ehAdminConfigurado(usuario)) {
            alvo = PerfilUsuarioEnum.ADMIN;
        } else if (atual == null) {
            alvo = PerfilUsuarioEnum.ASSOCIADO;
        }

        if (alvo != atual) {
            // Sem revogar tokens: a própria sessão do usuário precisa continuar válida para renovar o token
            definirPerfil(uid, usuario, alvo);
            usuario = buscar(uid);
        }
        return paraDTO(usuario);
    }

    public List<UsuarioResponseDTO> listar() {
        List<UsuarioResponseDTO> lista = new ArrayList<>();
        try {
            for (ExportedUserRecord registro : firebaseAuth.listUsers(null).iterateAll()) {
                lista.add(paraDTO(registro));
            }
        } catch (FirebaseAuthException e) {
            throw traduzir(e);
        }
        return lista;
    }

    // Cria o usuário já com perfil (uso do administrador: funcionários e associados criados pelo clube)
    public UsuarioResponseDTO criar(UsuarioCriarRequestDTO dto) {
        try {
            UserRecord.CreateRequest requisicao = new UserRecord.CreateRequest()
                    .setEmail(dto.email().trim())
                    .setPassword(dto.senha())
                    .setDisplayName(dto.nome().trim());
            UserRecord criado = firebaseAuth.createUser(requisicao);
            definirPerfil(criado.getUid(), criado, dto.perfil());
            return paraDTO(buscar(criado.getUid()));
        } catch (FirebaseAuthException e) {
            throw traduzir(e);
        }
    }

    public UsuarioResponseDTO alterarPerfil(String uidAlvo, PerfilUsuarioEnum novoPerfil, String uidSolicitante) {
        if (uidAlvo.equals(uidSolicitante) && novoPerfil != PerfilUsuarioEnum.ADMIN) {
            throw new IllegalStateException("Você não pode remover o seu próprio perfil de administrador.");
        }
        UserRecord usuario = buscar(uidAlvo);
        definirPerfil(uidAlvo, usuario, novoPerfil);
        // Revoga as sessões para o novo perfil valer assim que o token atual expirar
        revogarSessoes(uidAlvo);
        return paraDTO(buscar(uidAlvo));
    }

    public UsuarioResponseDTO alterarStatus(String uidAlvo, boolean ativo, String uidSolicitante) {
        if (uidAlvo.equals(uidSolicitante) && !ativo) {
            throw new IllegalStateException("Você não pode desativar a sua própria conta.");
        }
        buscar(uidAlvo);
        try {
            firebaseAuth.updateUser(new UserRecord.UpdateRequest(uidAlvo).setDisabled(!ativo));
        } catch (FirebaseAuthException e) {
            throw traduzir(e);
        }
        if (!ativo) {
            revogarSessoes(uidAlvo);
        }
        return paraDTO(buscar(uidAlvo));
    }

    // Executado na inicialização: promove a ADMIN os e-mails de collina.auth.admin-emails que já existem no Firebase.
    // Nunca derruba a aplicação se o Firebase estiver indisponível.
    public void promoverAdministradoresConfigurados() {
        for (String email : emailsAdmin) {
            try {
                UserRecord usuario = firebaseAuth.getUserByEmail(email);
                if (!usuario.isEmailVerified()) {
                    log.warn("Admin configurado '{}' existe, mas o e-mail não está verificado: perfil ADMIN não aplicado.", email);
                } else if (perfilDe(usuario) != PerfilUsuarioEnum.ADMIN) {
                    definirPerfil(usuario.getUid(), usuario, PerfilUsuarioEnum.ADMIN);
                    log.info("Usuário '{}' promovido a ADMIN.", email);
                }
            } catch (FirebaseAuthException e) {
                if (e.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
                    log.info("Admin configurado '{}' ainda não possui conta no Firebase.", email);
                } else {
                    log.warn("Não foi possível verificar o admin configurado '{}': {}", email, e.getMessage());
                }
            } catch (RuntimeException e) {
                log.warn("Não foi possível verificar o admin configurado '{}': {}", email, e.getMessage());
            }
        }
    }

    // ---------- helpers ----------

    private UserRecord buscar(String uid) {
        try {
            return firebaseAuth.getUser(uid);
        } catch (FirebaseAuthException e) {
            throw traduzir(e);
        }
    }

    private void definirPerfil(String uid, UserRecord usuario, PerfilUsuarioEnum perfil) {
        // Preserva outras claims que o usuário eventualmente tenha
        Map<String, Object> claims = new HashMap<>(usuario.getCustomClaims());
        claims.put(PerfilUsuarioEnum.CLAIM, perfil.name());
        try {
            firebaseAuth.setCustomUserClaims(uid, claims);
        } catch (FirebaseAuthException e) {
            throw traduzir(e);
        }
    }

    private void revogarSessoes(String uid) {
        try {
            firebaseAuth.revokeRefreshTokens(uid);
        } catch (FirebaseAuthException e) {
            throw traduzir(e);
        }
    }

    private boolean ehAdminConfigurado(UserRecord usuario) {
        return usuario.getEmail() != null
                && usuario.isEmailVerified()
                && emailsAdmin.contains(usuario.getEmail().toLowerCase(Locale.ROOT));
    }

    private PerfilUsuarioEnum perfilDe(UserRecord usuario) {
        return PerfilUsuarioEnum.deClaim(usuario.getCustomClaims().get(PerfilUsuarioEnum.CLAIM)).orElse(null);
    }

    private UsuarioResponseDTO paraDTO(UserRecord usuario) {
        return new UsuarioResponseDTO(
                usuario.getUid(),
                usuario.getEmail(),
                usuario.getDisplayName(),
                perfilDe(usuario),
                !usuario.isDisabled(),
                usuario.isEmailVerified()
        );
    }

    private RuntimeException traduzir(FirebaseAuthException e) {
        AuthErrorCode codigo = e.getAuthErrorCode();
        if (codigo == AuthErrorCode.USER_NOT_FOUND) {
            return new ResourceNotFoundException("Usuário não encontrado.");
        }
        if (codigo == AuthErrorCode.EMAIL_ALREADY_EXISTS) {
            return new IllegalStateException("Já existe um usuário cadastrado com este e-mail.");
        }
        log.error("Falha na comunicação com o Firebase Auth", e);
        // Vira 500 genérico no GlobalExceptionHandler (sem vazar detalhes internos)
        return new RuntimeException("Falha na comunicação com o Firebase Auth", e);
    }
}
