package br.ufs.dcomp.collina.service.auth;

import br.ufs.dcomp.collina.dto.auth.UsuarioCriarRequestDTO;
import br.ufs.dcomp.collina.dto.auth.UsuarioResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.associados.Associado;
import br.ufs.dcomp.collina.model.auth.Pessoa;
import br.ufs.dcomp.collina.model.auth.PerfilUsuarioEnum;
import br.ufs.dcomp.collina.model.auth.StatusUsuarioEnum;
import br.ufs.dcomp.collina.model.auth.Usuario;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import br.ufs.dcomp.collina.repository.associados.AssociadoRepository;
import br.ufs.dcomp.collina.repository.auth.UsuarioRepository;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// Gerencia contas no Firebase Auth e mantém tb_usuario (PostgreSQL) como fonte única de perfil e status.
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final FirebaseAuth firebaseAuth;
    private final UsuarioRepository usuarioRepository;
    private final AssociadoRepository associadoRepository;
    private final Set<String> emailsAdmin;

    public UsuarioService(FirebaseAuth firebaseAuth,
                          UsuarioRepository usuarioRepository,
                          AssociadoRepository associadoRepository,
                          @Value("${collina.auth.admin-emails:}") String emailsAdminCsv) {
        this.firebaseAuth = firebaseAuth;
        this.usuarioRepository = usuarioRepository;
        this.associadoRepository = associadoRepository;
        this.emailsAdmin = Arrays.stream(emailsAdminCsv.split(","))
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toSet());
    }

    // Retorna os dados do usuário autenticado a partir de tb_usuario (ou perfil=null se ainda não sincronizado).
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarMe(UsuarioAutenticado principal) {
        return usuarioRepository.findByFirebaseUid(principal.uid())
                .map(u -> paraDTO(u, principal.emailVerificado()))
                .orElseGet(() -> new UsuarioResponseDTO(
                        principal.uid(),
                        principal.email(),
                        principal.nome(),
                        null,
                        true,
                        principal.emailVerificado()
                ));
    }

    // Garante que o usuário autenticado exista em tb_usuario (idempotente e nunca rebaixa ninguém):
    // - contas antigas do Firebase são importadas preservando a claim legada (se houver), status e vínculo com Associado
    // - e-mail configurado em collina.auth.admin-emails (e verificado) -> ADMIN
    // - novo cadastro sem perfil -> ASSOCIADO
    // Usuários INATIVOS são rejeitados (401).
    @Transactional
    public UsuarioResponseDTO sincronizar(String uid) {
        UserRecord fbUser = buscarNoFirebase(uid);
        Usuario usuario = usuarioRepository.findByFirebaseUid(uid)
                .map(existente -> atualizarExistenteNaSincronizacao(existente, fbUser))
                .orElseGet(() -> importarDoFirebase(fbUser));

        if (!usuario.isAtivo()) {
            throw new DisabledException("Usuário inativo.");
        }

        return paraDTO(usuario, fbUser.isEmailVerified());
    }

    // Lista todos os usuários a partir de tb_usuario, importando antes eventuais contas legadas do Firebase Auth.
    @Transactional
    public List<UsuarioResponseDTO> listar() {
        Map<String, Boolean> emailVerificadoPorUid = sincronizarContasDoFirebase();

        return usuarioRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        u -> (u.getNome() != null && !u.getNome().isBlank() ? u.getNome() : u.getEmail())
                                .toLowerCase(Locale.ROOT)))
                .map(u -> paraDTO(u, emailVerificadoPorUid.getOrDefault(u.getFirebaseUid(), false)))
                .toList();
    }

    // Cria a conta no Firebase Auth e persiste em tb_usuario já com perfil e status ATIVO.
    @Transactional
    public UsuarioResponseDTO criar(UsuarioCriarRequestDTO dto) {
        String emailNormalizado = normalizarEmail(dto.email());
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new IllegalStateException("Já existe um usuário cadastrado com este e-mail.");
        }

        UserRecord criado;
        try {
            UserRecord.CreateRequest requisicao = new UserRecord.CreateRequest()
                    .setEmail(emailNormalizado)
                    .setPassword(dto.senha())
                    .setDisplayName(dto.nome().trim());
            criado = firebaseAuth.createUser(requisicao);
        } catch (FirebaseAuthException e) {
            throw traduzir(e);
        }

        try {
            Usuario usuario = new Usuario();
            usuario.setFirebaseUid(criado.getUid());
            usuario.setEmail(emailNormalizado);
            usuario.setNome(dto.nome().trim());
            usuario.setPerfil(dto.perfil());
            usuario.setStatus(StatusUsuarioEnum.ATIVO);
            Usuario salvo = usuarioRepository.save(usuario);
            return paraDTO(salvo, criado.isEmailVerified());
        } catch (RuntimeException e) {
            // Evita deixar conta órfã no Firebase se a gravação em tb_usuario falhar
            deletarNoFirebaseSilencioso(criado.getUid());
            throw e;
        }
    }

    @Transactional
    public UsuarioResponseDTO alterarPerfil(String uidAlvo, PerfilUsuarioEnum novoPerfil, String uidSolicitante) {
        if (uidAlvo.equals(uidSolicitante) && novoPerfil != PerfilUsuarioEnum.ADMIN) {
            throw new IllegalStateException("Você não pode remover o seu próprio perfil de administrador.");
        }

        Usuario usuario = obterOuImportar(uidAlvo);
        usuario.setPerfil(novoPerfil);
        Usuario salvo = usuarioRepository.save(usuario);
        return paraDTO(salvo, verificarEmailNoFirebase(uidAlvo));
    }

    @Transactional
    public UsuarioResponseDTO alterarStatus(String uidAlvo, boolean ativo, String uidSolicitante) {
        if (uidAlvo.equals(uidSolicitante) && !ativo) {
            throw new IllegalStateException("Você não pode desativar a sua própria conta.");
        }

        Usuario usuario = obterOuImportar(uidAlvo);
        usuario.setStatus(ativo ? StatusUsuarioEnum.ATIVO : StatusUsuarioEnum.INATIVO);
        Usuario salvo = usuarioRepository.save(usuario);

        try {
            firebaseAuth.updateUser(new UserRecord.UpdateRequest(uidAlvo).setDisabled(!ativo));
        } catch (FirebaseAuthException e) {
            throw traduzir(e);
        }
        if (!ativo) {
            revogarSessoes(uidAlvo);
        }
        return paraDTO(salvo, verificarEmailNoFirebase(uidAlvo));
    }

    // Usado por outros módulos (ex.: AssociadoService ao inativar/reativar um titular)
    // para manter tb_usuario e Firebase Auth alinhados com a situação cadastral.
    @Transactional
    public void sincronizarStatusPorSistema(String firebaseUid, boolean ativo) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return;
        }
        String uid = firebaseUid.trim();

        usuarioRepository.findByFirebaseUid(uid).ifPresent(usuario -> {
            StatusUsuarioEnum novoStatus = ativo ? StatusUsuarioEnum.ATIVO : StatusUsuarioEnum.INATIVO;
            if (usuario.getStatus() != novoStatus) {
                usuario.setStatus(novoStatus);
                usuarioRepository.save(usuario);
            }
        });

        try {
            firebaseAuth.updateUser(new UserRecord.UpdateRequest(uid).setDisabled(!ativo));
            if (!ativo) {
                firebaseAuth.revokeRefreshTokens(uid);
            }
        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
                log.warn("Conta Firebase '{}' vinculada ao cadastro não foi encontrada ao atualizar status.", uid);
            } else {
                log.warn("Falha ao sincronizar status no Firebase para uid '{}': {}", uid, e.getMessage());
            }
        } catch (RuntimeException e) {
            log.warn("Erro inesperado ao sincronizar status no Firebase para uid '{}': {}", uid, e.getMessage());
        }
    }

    // Vincula uma Pessoa existente (tb_pessoa) à conta em tb_usuario (relação 0..1).
    @Transactional
    public void vincularPessoa(String firebaseUid, Pessoa pessoa) {
        if (firebaseUid == null || firebaseUid.isBlank() || pessoa == null || pessoa.getId() == null) {
            return;
        }
        String uid = firebaseUid.trim();
        Optional<Usuario> opt = usuarioRepository.findByFirebaseUid(uid);
        if (opt.isEmpty()) {
            return;
        }
        Usuario usuario = opt.get();
        if (usuario.getPessoa() != null && usuario.getPessoa().getId().equals(pessoa.getId())) {
            return;
        }
        Optional<Usuario> outroVinculo = usuarioRepository.findByPessoaId(pessoa.getId());
        if (outroVinculo.isPresent() && !outroVinculo.get().getId().equals(usuario.getId())) {
            return;
        }
        usuario.setPessoa(pessoa);
        if ((usuario.getNome() == null || usuario.getNome().isBlank()) && pessoa.getNome() != null) {
            usuario.setNome(pessoa.getNome());
        }
        usuarioRepository.save(usuario);
    }

    // Executado na inicialização: garante em tb_usuario os e-mails de collina.auth.admin-emails
    // que já existem e estão verificados no Firebase. Nunca derruba a aplicação se o Firebase falhar.
    @Transactional
    public void promoverAdministradoresConfigurados() {
        for (String email : emailsAdmin) {
            try {
                UserRecord fbUser = firebaseAuth.getUserByEmail(email);
                if (!fbUser.isEmailVerified()) {
                    log.warn("Admin configurado '{}' existe, mas o e-mail não está verificado: perfil ADMIN não aplicado.", email);
                    continue;
                }
                Optional<Usuario> opt = usuarioRepository.findByFirebaseUid(fbUser.getUid());
                if (opt.isPresent()) {
                    Usuario existente = opt.get();
                    if (existente.getPerfil() != PerfilUsuarioEnum.ADMIN) {
                        existente.setPerfil(PerfilUsuarioEnum.ADMIN);
                        usuarioRepository.save(existente);
                        log.info("Usuário '{}' promovido a ADMIN em tb_usuario.", email);
                    }
                } else {
                    importarDoFirebase(fbUser);
                    log.info("Admin configurado '{}' importado para tb_usuario como ADMIN.", email);
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

    private Usuario obterOuImportar(String uid) {
        return usuarioRepository.findByFirebaseUid(uid)
                .orElseGet(() -> importarDoFirebase(buscarNoFirebase(uid)));
    }

    private Map<String, Boolean> sincronizarContasDoFirebase() {
        Map<String, Boolean> verificados = new HashMap<>();
        try {
            for (ExportedUserRecord registro : firebaseAuth.listUsers(null).iterateAll()) {
                verificados.put(registro.getUid(), registro.isEmailVerified());
                if (!usuarioRepository.existsByFirebaseUid(registro.getUid())) {
                    String email = normalizarEmail(registro.getEmail());
                    if (email != null && !usuarioRepository.existsByEmailIgnoreCase(email)) {
                        importarDoFirebase(registro);
                    }
                }
            }
        } catch (FirebaseAuthException | RuntimeException e) {
            log.warn("Não foi possível sincronizar lista de contas com o Firebase Auth: {}", e.getMessage());
        }
        return verificados;
    }

    private Usuario atualizarExistenteNaSincronizacao(Usuario existente, UserRecord fbUser) {
        boolean alterado = false;

        String emailFb = normalizarEmail(fbUser.getEmail());
        if (emailFb != null && !emailFb.equalsIgnoreCase(existente.getEmail())
                && !usuarioRepository.existsByEmailIgnoreCase(emailFb)) {
            existente.setEmail(emailFb);
            alterado = true;
        }

        if (fbUser.getDisplayName() != null && !fbUser.getDisplayName().isBlank()
                && (existente.getNome() == null || existente.getNome().isBlank())) {
            existente.setNome(fbUser.getDisplayName().trim());
            alterado = true;
        }

        if (fbUser.isDisabled() && existente.isAtivo()) {
            existente.setStatus(StatusUsuarioEnum.INATIVO);
            alterado = true;
        }

        if (ehAdminConfigurado(fbUser) && existente.getPerfil() != PerfilUsuarioEnum.ADMIN) {
            existente.setPerfil(PerfilUsuarioEnum.ADMIN);
            alterado = true;
        }

        if (existente.getPessoa() == null) {
            Optional<Pessoa> pessoaAssociado = associadoRepository.findByUsuarioId(existente.getFirebaseUid())
                    .map(Associado::getPessoa);
            if (pessoaAssociado.isPresent()
                    && usuarioRepository.findByPessoaId(pessoaAssociado.get().getId()).isEmpty()) {
                existente.setPessoa(pessoaAssociado.get());
                alterado = true;
            }
        }

        return alterado ? usuarioRepository.save(existente) : existente;
    }

    private Usuario importarDoFirebase(UserRecord fbUser) {
        String email = normalizarEmail(fbUser.getEmail());
        if (email == null) {
            throw new IllegalStateException("A conta no Firebase Auth não possui um e-mail associado.");
        }

        PerfilUsuarioEnum perfilInicial;
        if (ehAdminConfigurado(fbUser)) {
            perfilInicial = PerfilUsuarioEnum.ADMIN;
        } else {
            perfilInicial = perfilLegadoDe(fbUser).orElse(PerfilUsuarioEnum.ASSOCIADO);
        }

        Optional<Associado> associadoVinculado = associadoRepository.findByUsuarioId(fbUser.getUid());
        StatusUsuarioEnum statusInicial = fbUser.isDisabled()
                ? StatusUsuarioEnum.INATIVO
                : StatusUsuarioEnum.ATIVO;

        // Se o associado vinculado já estiver INATIVO no banco, preserva o bloqueio ao importar
        if (associadoVinculado.isPresent()
                && associadoVinculado.get().getSituacaoCadastral() == br.ufs.dcomp.collina.model.associados.SituacaoCadastralEnum.INATIVO) {
            statusInicial = StatusUsuarioEnum.INATIVO;
        }

        Usuario novo = new Usuario();
        novo.setFirebaseUid(fbUser.getUid());
        novo.setEmail(email);
        String nome = fbUser.getDisplayName();
        if ((nome == null || nome.isBlank()) && associadoVinculado.isPresent()) {
            nome = associadoVinculado.get().getPessoa().getNome();
        }
        novo.setNome(nome != null && !nome.isBlank() ? nome.trim() : null);
        novo.setPerfil(perfilInicial);
        novo.setStatus(statusInicial);

        if (associadoVinculado.isPresent()) {
            Pessoa pessoa = associadoVinculado.get().getPessoa();
            if (usuarioRepository.findByPessoaId(pessoa.getId()).isEmpty()) {
                novo.setPessoa(pessoa);
            }
        }

        return usuarioRepository.save(novo);
    }

    private UserRecord buscarNoFirebase(String uid) {
        try {
            return firebaseAuth.getUser(uid);
        } catch (FirebaseAuthException e) {
            throw traduzir(e);
        }
    }

    private boolean verificarEmailNoFirebase(String uid) {
        try {
            return firebaseAuth.getUser(uid).isEmailVerified();
        } catch (FirebaseAuthException | RuntimeException e) {
            return false;
        }
    }

    private void deletarNoFirebaseSilencioso(String uid) {
        try {
            firebaseAuth.deleteUser(uid);
        } catch (FirebaseAuthException | RuntimeException e) {
            log.warn("Falha ao remover conta órfã '{}' no Firebase Auth após erro de persistência: {}", uid, e.getMessage());
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

    private Optional<PerfilUsuarioEnum> perfilLegadoDe(UserRecord usuario) {
        Map<String, Object> claims = usuario.getCustomClaims();
        if (claims == null) {
            return Optional.empty();
        }
        return PerfilUsuarioEnum.deClaim(claims.get(PerfilUsuarioEnum.CLAIM));
    }

    private String normalizarEmail(String email) {
        if (email == null || email.trim().isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UsuarioResponseDTO paraDTO(Usuario usuario, boolean emailVerificado) {
        return new UsuarioResponseDTO(
                usuario.getFirebaseUid(),
                usuario.getEmail(),
                usuario.getNome(),
                usuario.getPerfil(),
                usuario.isAtivo(),
                emailVerificado
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
