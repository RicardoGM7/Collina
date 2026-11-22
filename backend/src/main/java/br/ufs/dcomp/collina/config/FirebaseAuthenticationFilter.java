package br.ufs.dcomp.collina.config;

import br.ufs.dcomp.collina.model.auth.PerfilUsuarioEnum;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
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

// Lê "Authorization: Bearer <ID token do Firebase>", valida o token e autentica a requisição.
// Token ausente ou inválido: o filtro não autentica e o Spring Security responde 401.
// Não é um @Component de propósito: é criado no SecurityConfig para não ser registrado duas vezes.
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final FirebaseAuth firebaseAuth;
    private final boolean verificarRevogacao;

    public FirebaseAuthenticationFilter(FirebaseAuth firebaseAuth, boolean verificarRevogacao) {
        this.firebaseAuth = firebaseAuth;
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

                PerfilUsuarioEnum perfil = PerfilUsuarioEnum.deClaim(decodificado.getClaims().get(PerfilUsuarioEnum.CLAIM))
                        .orElse(null);

                UsuarioAutenticado usuario = new UsuarioAutenticado(
                        decodificado.getUid(),
                        decodificado.getEmail(),
                        decodificado.getName(),
                        perfil,
                        decodificado.isEmailVerified()
                );

                List<GrantedAuthority> authorities = perfil == null
                        ? List.of()
                        : List.of(new SimpleGrantedAuthority(perfil.authority()));

                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(usuario, token, authorities));
            } catch (FirebaseAuthException | IllegalArgumentException e) {
                // Token inválido/expirado/revogado: segue sem autenticação (resultado: 401)
                SecurityContextHolder.clearContext();
                logger.debug("ID token do Firebase rejeitado: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
