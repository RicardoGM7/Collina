package br.ufs.dcomp.collina.config;

import br.ufs.dcomp.collina.repository.auth.UsuarioRepository;
import com.google.firebase.auth.FirebaseAuth;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Configuration
@EnableWebSecurity
// Permite @PreAuthorize("hasRole('ADMIN')") etc. nos controllers/services dos módulos
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   FirebaseAuth firebaseAuth,
                                                   UsuarioRepository usuarioRepository,
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   @Value("${firebase.check-revoked:false}") boolean verificarRevogacao)
            throws Exception {
        return http
                // Desativa proteção CSRF (API REST stateless, autenticada por token no header)
                .csrf(AbstractHttpConfigurer::disable)
                // Habilita configuração de CORS para requisições do React
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Define a política de sessão como Stateless (sem sessão no servidor)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Valida o ID token do Firebase e carrega perfil/status de tb_usuario antes dos filtros do Spring
                .addFilterBefore(new FirebaseAuthenticationFilter(firebaseAuth, usuarioRepository, verificarRevogacao),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> escreverErro(response,
                                HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado",
                                "Token de acesso ausente, inválido ou expirado.", request.getRequestURI()))
                        .accessDeniedHandler((request, response, e) -> escreverErro(response,
                                HttpServletResponse.SC_FORBIDDEN, "Acesso negado",
                                "Seu perfil não tem permissão para acessar este recurso.", request.getRequestURI())))
                .authorizeHttpRequests(authorize -> authorize
                        // Pré-flight do CORS e página de erro do Spring
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Webhook de pagamento externo: comunicação servidor-servidor sem token Firebase
                        .requestMatchers(HttpMethod.POST, "/api/webhooks/pagamentos").permitAll()
                        .requestMatchers("/api/stub-gateway/**").permitAll()

                        // Gestão de usuários e perfis: somente administrador
                        .requestMatchers("/api/auth/usuarios/**").hasRole("ADMIN")
                        // /me e /sincronizar: qualquer usuário autenticado (mesmo sem perfil ainda)
                        .requestMatchers("/api/auth/**").authenticated()

                        // Módulo de estoque e pedidos.
                        // Catálogo (produtos, categorias e saldo) é visível a qualquer usuário logado; o associado
                        // só pode CRIAR pedido (RF-E01). Pedidos não têm dono no modelo atual, então listar/cancelar/pagar
                        // continua restrito ao estoque/admin para não expor pedidos de terceiros.
                        .requestMatchers(HttpMethod.GET, "/api/produtos/**", "/api/categorias/**", "/api/estoques")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasAnyRole("ADMIN", "ESTOQUE", "ASSOCIADO")
                        .requestMatchers("/api/estoques/**", "/api/produtos/**", "/api/categorias/**", "/api/pedidos/**")
                        .hasAnyRole("ADMIN", "ESTOQUE")

                        // Planos: leitura pública (usada também no autocadastro); alteração de parâmetros só pelo administrador
                        .requestMatchers(HttpMethod.GET, "/api/planos/**").permitAll()
                        .requestMatchers("/api/planos/**").hasRole("ADMIN")

                        // Próprio associado consultando/alterando seu cadastro (RF-AS05). O dono é resolvido pelo uid do token.
                        .requestMatchers("/api/associados/me", "/api/associados/me/**").hasRole("ASSOCIADO")

                        // Módulo de associados: consulta por CPF e dependentes também acessível ao FINANCEIRO para atendimento de mensalidades.
                        .requestMatchers(HttpMethod.GET, "/api/associados/cpf/*", "/api/associados/*/dependentes")
                        .hasAnyRole("ADMIN", "RECEPCAO", "FINANCEIRO")
                        .requestMatchers("/api/associados/**", "/api/dependentes/**").hasAnyRole("ADMIN", "RECEPCAO")

                        // Módulo financeiro e mensalidades: permissões granulares por operação
                        // 1. Iniciar cobrança via gateway para sua mensalidade: ASSOCIADO (e ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/mensalidades/*/cobrancas").hasAnyRole("ASSOCIADO", "ADMIN")
                        // 2. Confirmar pagamento manual: ADMIN e RECEPCAO
                        .requestMatchers(HttpMethod.POST, "/api/mensalidades/*/confirmar-pagamento").hasAnyRole("ADMIN", "RECEPCAO")
                        // 3. Cancelar e reemitir mensalidade: ADMIN, RECEPCAO e FINANCEIRO
                        .requestMatchers(HttpMethod.POST, "/api/mensalidades/*/cancelar-e-reemitir").hasAnyRole("ADMIN", "RECEPCAO", "FINANCEIRO")
                        // 4. Consultar mensalidades de qualquer titular e lançar gastos avulsos no atendimento
                        .requestMatchers(HttpMethod.GET, "/api/mensalidades").hasAnyRole("ADMIN", "FINANCEIRO", "RECEPCAO")
                        .requestMatchers(HttpMethod.POST, "/api/mensalidades/gastos-avulsos").hasAnyRole("ADMIN", "FINANCEIRO", "RECEPCAO")
                        // 5. Relatórios financeiros e inadimplentes
                        .requestMatchers("/api/financeiro/relatorio/**").hasAnyRole("ADMIN", "FINANCEIRO")
                        // 6. Demais rotas financeiras, histórico e pagamentos administrativos: restrito a ADMIN e FINANCEIRO
                        .requestMatchers("/api/financeiro/**", "/api/pagamentos", "/api/pagamentos/**", "/api/mensalidades/**").hasAnyRole("ADMIN", "FINANCEIRO")
                        // Módulo de espaços e reservas. As regras mais específicas vêm primeiro.
                        // Consulta de espaços/blocos/disponibilidade: associado e funcionários (o associado só enxerga
                        // os espaços reserváveis: filtro no EspacoService).
                        .requestMatchers(HttpMethod.GET, "/api/espacos/**").hasAnyRole("ADMIN", "RECEPCAO", "ASSOCIADO")
                        // UC-GE05: só o administrador configura blocos de horário
                        .requestMatchers(HttpMethod.POST, "/api/espacos/*/blocos").hasRole("ADMIN")
                        // UC-GE04: cadastro, edição e estado dos espaços
                        .requestMatchers("/api/espacos/**").hasAnyRole("ADMIN", "RECEPCAO")
                        // RF-AS08: só o titular solicita reserva; consulta das próprias reservas
                        .requestMatchers(HttpMethod.POST, "/api/reservas").hasRole("ASSOCIADO")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/minhas").hasRole("ASSOCIADO")
                        // RF-AS09: dono cancela a sua (verificado no service); recepção/admin cancelam em nome do titular
                        .requestMatchers(HttpMethod.PATCH, "/api/reservas/*/cancelar").hasAnyRole("ADMIN", "RECEPCAO", "ASSOCIADO")
                        // Consulta de reservas por associado: recepção e administrador
                        .requestMatchers("/api/reservas/**").hasAnyRole("ADMIN", "RECEPCAO")

                        // Qualquer outra rota exige login. Novos módulos: adicione a regra de perfil acima desta linha.
                        .anyRequest().authenticated()
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${collina.cors.allowed-origins:http://localhost:5173}") String[] origensPermitidas) {
        CorsConfiguration configuration = new CorsConfiguration();
        // Origens do front-end (collina.cors.allowed-origins)
        configuration.setAllowedOrigins(List.of(origensPermitidas));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Resposta JSON no mesmo formato do ApiError (usada nos erros 401/403 que ocorrem antes dos controllers)
    private static void escreverErro(HttpServletResponse response, int status, String erro, String mensagem, String path)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = "{\"timestamp\":\"" + Instant.now() + "\",\"status\":" + status
                + ",\"error\":\"" + escapar(erro) + "\",\"message\":\"" + escapar(mensagem)
                + "\",\"path\":\"" + escapar(path) + "\"}";
        response.getWriter().write(json);
    }

    private static String escapar(String valor) {
        return valor == null ? "" : valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
