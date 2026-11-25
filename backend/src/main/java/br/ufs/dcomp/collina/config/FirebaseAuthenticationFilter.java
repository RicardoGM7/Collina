package br.ufs.dcomp.collina.config;

import br.ufs.dcomp.collina.model.auth.PerfilUsuarioEnum;
import br.ufs.dcomp.collina.model.auth.Usuario;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import br.ufs.dcomp.collina.repository.auth.UsuarioRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

// Lê "Authorization: Bearer <ID token do Firebase>", valida o token no Firebase
// e carrega perfil e status a partir de tb_usuario (PostgreSQL) a cada requisição.
// Token ausente/inválido ou usuário INATIVO em tb_usuario: não autentica (resultado: 401).
// Não é um @Component de propósito: é criado no SecurityConfig para não ser registrado duas vezes.
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final FirebaseAuth firebaseAuth;
    private final UsuarioRepository usuarioRepository;
    private final boolean verificarRevogacao;

    public FirebaseAuthenticationFilter(FirebaseAuth firebaseAuth,
                                        UsuarioRepository usuarioRepository,
                                        boolean verificarRevogacao) {
        this.firebaseAuth = firebaseAuth;
        this.usuarioRepository = usuarioRepository;
        this.verificarRevogacao = verificarRevogacao;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(PREFIXO)) {
            String token = header.substring(PREFIXO.length()).trim();
            try {
                FirebaseToken decodificado = firebaseAuth.verifyIdToken(token, verificarRevogacao);
                Optional<Usuario> usuarioBanco = usuarioRepository.findByFirebaseUid(decodificado.getUid());

                if (usuarioBanco.isPresent()) {
                    Usuario registro = usuarioBanco.get();
                    if (!registro.isAtivo()) {
                        // Usuário inativo em tb_usuario é bloqueado imediatamente em todas as requisições
                        SecurityContextHolder.clearContext();
                        logger.debug("Requisição rejeitada: usuário inativo em tb_usuario (uid=" + registro.getFirebaseUid() + ")");
                    } else {
                        PerfilUsuarioEnum perfil = registro.getPerfil();
                        if (perfil == null) {
                            // Usuário ativo mas sem perfil em tb_usuario é rejeitado de maneira controlada (sem NPE)
                            SecurityContextHolder.clearContext();
                            logger.debug("Requisição rejeitada: usuário sem perfil em tb_usuario (uid=" + registro.getFirebaseUid() + ")");
                        } else {
                            String email = registro.getEmail() != null ? registro.getEmail() : decodificado.getEmail();
                            String nome = registro.getNome() != null && !registro.getNome().isBlank()
                                    ? registro.getNome()
                                    : decodificado.getName();

                            UsuarioAutenticado principal = new UsuarioAutenticado(
                                    registro.getFirebaseUid(),
                                    email,
                                    nome,
                                    perfil,
                                    decodificado.isEmailVerified()
                            );
                            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(perfil.authority()));
                            SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(principal, token, authorities));
                        }
                    }
                } else if (ehRotaBootstrapAuth(request)) {
                    // Conta ainda não sincronizada em tb_usuario (primeiro acesso/cadastro ou migração):
                    // só pode acessar /api/auth/sincronizar e /api/auth/me sem nenhuma role.
                    UsuarioAutenticado principal = new UsuarioAutenticado(
                            decodificado.getUid(),
                            decodificado.getEmail(),
                            decodificado.getName(),
                            null,
                            decodificado.isEmailVerified()
                    );
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(principal, token, List.of()));
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (FirebaseAuthException | IllegalArgumentException e) {
                // Token inválido/expirado/revogado: segue sem autenticação (resultado: 401)
                SecurityContextHolder.clearContext();
                logger.debug("ID token do Firebase rejeitado: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean ehRotaBootstrapAuth(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "/api/auth/sincronizar".equals(uri) || "/api/auth/me".equals(uri);
    }
}
