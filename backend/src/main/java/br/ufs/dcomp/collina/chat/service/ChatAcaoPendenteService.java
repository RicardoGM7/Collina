package br.ufs.dcomp.collina.chat.service;

import br.ufs.dcomp.collina.chat.dto.AcaoPendenteDTO;
import br.ufs.dcomp.collina.chat.dto.ResultadoAcaoDTO;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Gerencia ações de escrita no banco de dados propostas pela IA (padrão Human-in-the-Loop).
 * Nenhuma tool altera o PostgreSQL diretamente: primeiro ela registra a ação aqui, o front-end exibe
 * um card de confirmação e somente após o clique em "Confirmar" o Service real é executado.
 */
@Service
public class ChatAcaoPendenteService {

    private static final Logger log = LoggerFactory.getLogger(ChatAcaoPendenteService.class);
    private static final Duration TTL_ACAO = Duration.ofMinutes(10);

    private record ContextoTurno(String usuarioUid, String chaveMemoria, List<AcaoPendenteDTO> coletadas) {
    }

    private record AcaoPendenteInterna(
            String acaoId,
            String usuarioUid,
            String chaveMemoria,
            AcaoPendenteDTO dto,
            Instant expiraEm,
            Supplier<String> executor
    ) {
    }

    private final ChatMemory chatMemory;
    private final ConcurrentHashMap<String, AcaoPendenteInterna> acoesPendentes = new ConcurrentHashMap<>();
    private final ThreadLocal<ContextoTurno> contextoAtual = new ThreadLocal<>();

    public ChatAcaoPendenteService(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    public void iniciarTurno(String usuarioUid, String chaveMemoria) {
        limparExpiradas();
        contextoAtual.set(new ContextoTurno(usuarioUid, chaveMemoria, new ArrayList<>()));
    }

    public List<AcaoPendenteDTO> coletarAcoesDoTurno() {
        ContextoTurno ctx = contextoAtual.get();
        return ctx != null ? List.copyOf(ctx.coletadas()) : List.of();
    }

    public void finalizarTurno() {
        contextoAtual.remove();
    }

    /**
     * Descarta todas as ações pendentes coletadas durante um turno que falhou.
     */
    public void descartarAcoesDoTurno() {
        ContextoTurno ctx = contextoAtual.get();
        if (ctx != null) {
            for (AcaoPendenteDTO dto : ctx.coletadas()) {
                acoesPendentes.remove(dto.acaoId());
            }
            ctx.coletadas().clear();
        }
    }

    /**
     * Remove ações pendentes de uma conversa encerrada pelo usuário.
     */
    public void limparPorConversa(String chaveMemoria) {
        acoesPendentes.entrySet().removeIf(entry -> chaveMemoria.equals(entry.getValue().chaveMemoria()));
    }

    /**
     * Registra uma ação pendente proposta por uma @Tool e devolve o texto de instrução para o modelo de IA.
     */
    public String proporAcao(
            String titulo,
            String descricao,
            List<String> detalhes,
            boolean critica,
            Supplier<String> executor) {

        ContextoTurno ctx = contextoAtual.get();
        if (ctx == null) {
            throw new IllegalStateException("Contexto de chat não inicializado para registro de ação pendente.");
        }

        String acaoId = UUID.randomUUID().toString();
        List<String> detalhesImutaveis = detalhes != null ? List.copyOf(detalhes) : List.of();
        AcaoPendenteDTO dto = new AcaoPendenteDTO(acaoId, titulo, descricao, detalhesImutaveis, critica);

        AcaoPendenteInterna interna = new AcaoPendenteInterna(
                acaoId,
                ctx.usuarioUid(),
                ctx.chaveMemoria(),
                dto,
                Instant.now().plus(TTL_ACAO),
                executor
        );

        acoesPendentes.put(acaoId, interna);
        ctx.coletadas().add(dto);

        log.info("Ação pendente criada | acaoId={} | titulo='{}' | usuario={}", acaoId, titulo, ctx.usuarioUid());

        return String.format(
                "AGUARDANDO CONFIRMAÇÃO DO ADMINISTRADOR: A ação '%s' (%s) foi preparada e será exibida no card de confirmação abaixo. "
                        + "NENHUMA alteração foi gravada no banco de dados ainda. "
                        + "Informe brevemente o que será feito e peça para o administrador clicar em Confirmar ou Cancelar no card.",
                titulo, descricao
        );
    }

    /**
     * Confirma e executa efetivamente a alteração no banco de dados (chamado diretamente pelo botão Confirmar).
     */
    public ResultadoAcaoDTO confirmar(String acaoId, UsuarioAutenticado usuario) {
        validarUsuario(usuario);
        limparExpiradas();

        AcaoPendenteInterna acao = acoesPendentes.get(acaoId);
        if (acao == null) {
            return new ResultadoAcaoDTO(
                    acaoId,
                    "EXPIRADA",
                    "Esta ação não está mais disponível (já foi processada ou expirou). Peça à IA novamente se precisar.",
                    true
            );
        }

        if (!acao.usuarioUid().equals(usuario.uid())) {
            throw new AccessDeniedException("Você não tem permissão para confirmar esta ação.");
        }

        // Remove atomicamente ANTES de executar para garantir idempotência contra clique duplo
        AcaoPendenteInterna removida = acoesPendentes.remove(acaoId);
        if (removida == null) {
            return new ResultadoAcaoDTO(acaoId, "EXPIRADA", "Esta ação já foi processada.", true);
        }

        if (Instant.now().isAfter(removida.expiraEm())) {
            return new ResultadoAcaoDTO(
                    acaoId,
                    "EXPIRADA",
                    "O tempo de confirmação desta ação expirou (10 minutos). Solicite novamente à IA.",
                    true
            );
        }

        try {
            String resultado = removida.executor().get();
            registrarNaMemoria(removida.chaveMemoria(),
                    "[Ação confirmada pelo administrador] " + removida.dto().titulo() + " -> " + resultado);
            log.info("Ação confirmada e executada | acaoId={} | titulo='{}' | usuario={}",
                    acaoId, removida.dto().titulo(), usuario.uid());
            return new ResultadoAcaoDTO(acaoId, "CONFIRMADA", resultado, false);
        } catch (Exception ex) {
            log.warn("Falha ao executar ação confirmada | acaoId={} | erro={}", acaoId, ex.getMessage());
            String msgErro = "Não foi possível concluir a ação: " + ex.getMessage();
            registrarNaMemoria(removida.chaveMemoria(),
                    "[Falha ao executar ação confirmada] " + removida.dto().titulo() + " -> " + msgErro);
            return new ResultadoAcaoDTO(acaoId, "ERRO", msgErro, true);
        }
    }

    /**
     * Cancela a ação pendente sem realizar nenhuma alteração no banco de dados.
     */
    public ResultadoAcaoDTO cancelar(String acaoId, UsuarioAutenticado usuario) {
        validarUsuario(usuario);
        limparExpiradas();

        AcaoPendenteInterna acao = acoesPendentes.get(acaoId);
        if (acao == null) {
            return new ResultadoAcaoDTO(acaoId, "CANCELADA", "Ação já cancelada ou expirada.", false);
        }

        if (!acao.usuarioUid().equals(usuario.uid())) {
            throw new AccessDeniedException("Você não tem permissão para cancelar esta ação.");
        }

        acoesPendentes.remove(acaoId);
        registrarNaMemoria(acao.chaveMemoria(),
                "[Ação cancelada pelo administrador] Nenhuma alteração foi feita para: " + acao.dto().titulo());
        log.info("Ação cancelada pelo administrador | acaoId={} | titulo='{}' | usuario={}",
                acaoId, acao.dto().titulo(), usuario.uid());

        return new ResultadoAcaoDTO(
                acaoId,
                "CANCELADA",
                "Ação cancelada. Nenhuma alteração foi feita no banco de dados.",
                false
        );
    }

    private void registrarNaMemoria(String chaveMemoria, String texto) {
        try {
            chatMemory.add(chaveMemoria, new AssistantMessage(texto));
        } catch (Exception ex) {
            log.debug("Não foi possível registrar resultado da ação na ChatMemory: {}", ex.getMessage());
        }
    }

    private void validarUsuario(UsuarioAutenticado usuario) {
        if (usuario == null || usuario.uid() == null || usuario.uid().isBlank()) {
            throw new AccessDeniedException("Usuário autenticado inválido.");
        }
    }

    private void limparExpiradas() {
        Instant agora = Instant.now();
        acoesPendentes.entrySet().removeIf(entry -> agora.isAfter(entry.getValue().expiraEm()));
    }
}
