package br.ufs.dcomp.collina.chat.service;

import br.ufs.dcomp.collina.chat.dto.AcaoPendenteDTO;
import br.ufs.dcomp.collina.chat.dto.ResultadoAcaoDTO;
import br.ufs.dcomp.collina.chat.tools.AdminEstoqueChatTools;
import br.ufs.dcomp.collina.chat.tools.PedidoChatTools;
import br.ufs.dcomp.collina.model.auth.PerfilUsuarioEnum;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_TAMANHO_NOME_PROMPT = 80;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ChatAcaoPendenteService acaoPendenteService;
    private final PedidoChatTools pedidoChatTools;
    private final AdminEstoqueChatTools adminEstoqueChatTools;

    // Serializa chamadas simultâneas para a MESMA conversa sem bloquear outras conversas/usuários
    private final ConcurrentHashMap<String, ReentrantLock> locksPorConversa = new ConcurrentHashMap<>();

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            PedidoChatTools pedidoChatTools,
            AdminEstoqueChatTools adminEstoqueChatTools,
            ChatMemory chatMemory,
            ChatAcaoPendenteService acaoPendenteService) {
        this.chatMemory = chatMemory;
        this.acaoPendenteService = acaoPendenteService;
        this.pedidoChatTools = pedidoChatTools;
        this.adminEstoqueChatTools = adminEstoqueChatTools;

        // O System Prompt e as Tools são definidos dinamicamente por perfil a cada requisição.
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public record RespostaChat(String resposta, String conversaId, boolean erro, List<AcaoPendenteDTO> acoesPendentes) {
    }

    private record ContextoPerfil(String systemPrompt, Object[] ferramentas) {
    }

    public RespostaChat responder(String mensagemUsuario, String conversaId, UsuarioAutenticado usuario) {
        ContextoPerfil contexto = resolverContextoPerfil(usuario);

        String id = (conversaId != null && !conversaId.isBlank())
                ? conversaId.trim()
                : UUID.randomUUID().toString();

        // Isola o histórico de memória por UID do usuário logado para evitar vazamento entre contas
        String chaveMemoria = chaveMemoria(usuario.uid(), id);
        ReentrantLock lock = locksPorConversa.computeIfAbsent(chaveMemoria, k -> new ReentrantLock());

        lock.lock();
        try {
            // Faz backup do histórico antes da chamada para restaurar em caso de falha da API do Gemini,
            // evitando que mensagens órfãs corrompam os turnos seguintes da conversa.
            List<Message> historicoAnterior = List.copyOf(this.chatMemory.get(chaveMemoria));
            long inicioNs = System.nanoTime();
            this.acaoPendenteService.iniciarTurno(usuario.uid(), chaveMemoria);

            try {
                ChatResponse response = this.chatClient.prompt()
                        .system(contexto.systemPrompt())
                        .user(mensagemUsuario)
                        .tools(contexto.ferramentas())
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chaveMemoria))
                        .call()
                        .chatResponse();

                long duracaoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNs);
                String textoResposta = extrairTexto(response);
                List<AcaoPendenteDTO> acoesPendentes = this.acaoPendenteService.coletarAcoesDoTurno();

                if (textoResposta == null || textoResposta.isBlank()) {
                    if (!acoesPendentes.isEmpty()) {
                        registrarMetricas(usuario.perfil(), id, duracaoMs, response);
                        return new RespostaChat(
                                "Preparei a alteração abaixo. Revise os detalhes e clique em Confirmar ou Cancelar:",
                                id,
                                false,
                                acoesPendentes
                        );
                    }
                    this.acaoPendenteService.descartarAcoesDoTurno();
                    restaurarHistorico(chaveMemoria, historicoAnterior);
                    log.warn("Chat retornou texto vazio | perfil={} | conversa={} | tempo={}ms",
                            usuario.perfil(), id, duracaoMs);
                    return new RespostaChat(
                            "Não recebi uma resposta válida do modelo de IA agora. Pode tentar enviar novamente?",
                            id,
                            true,
                            List.of()
                    );
                }

                registrarMetricas(usuario.perfil(), id, duracaoMs, response);
                return new RespostaChat(textoResposta, id, false, acoesPendentes);
            } catch (AccessDeniedException ex) {
                this.acaoPendenteService.descartarAcoesDoTurno();
                restaurarHistorico(chaveMemoria, historicoAnterior);
                throw ex;
            } catch (Exception ex) {
                long duracaoMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNs);
                this.acaoPendenteService.descartarAcoesDoTurno();
                restaurarHistorico(chaveMemoria, historicoAnterior);
                log.error("Erro ao consultar Gemini | perfil={} | conversa={} | tempo={}ms | erro={}",
                        usuario.perfil(), id, duracaoMs, ex.getMessage(), ex);
                return new RespostaChat(traduzirErroGemini(ex), id, true, List.of());
            } finally {
                this.acaoPendenteService.finalizarTurno();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Confirma e executa uma ação de escrita proposta anteriormente pela IA.
     */
    public ResultadoAcaoDTO confirmarAcao(String acaoId, UsuarioAutenticado usuario) {
        resolverContextoPerfil(usuario);
        return this.acaoPendenteService.confirmar(acaoId, usuario);
    }

    /**
     * Cancela uma ação de escrita proposta anteriormente pela IA sem alterar o banco.
     */
    public ResultadoAcaoDTO cancelarAcao(String acaoId, UsuarioAutenticado usuario) {
        resolverContextoPerfil(usuario);
        return this.acaoPendenteService.cancelar(acaoId, usuario);
    }

    /**
     * Remove o histórico e ações pendentes de uma conversa encerrada pelo usuário.
     */
    public void limparConversa(String conversaId, UsuarioAutenticado usuario) {
        resolverContextoPerfil(usuario);

        if (conversaId == null || conversaId.isBlank()) {
            return;
        }

        String chaveMemoria = chaveMemoria(usuario.uid(), conversaId.trim());
        ReentrantLock lock = locksPorConversa.get(chaveMemoria);

        if (lock != null) {
            lock.lock();
            try {
                this.chatMemory.clear(chaveMemoria);
                this.acaoPendenteService.limparPorConversa(chaveMemoria);
                locksPorConversa.remove(chaveMemoria, lock);
            } finally {
                lock.unlock();
            }
        } else {
            this.chatMemory.clear(chaveMemoria);
            this.acaoPendenteService.limparPorConversa(chaveMemoria);
        }
    }

    private ContextoPerfil resolverContextoPerfil(UsuarioAutenticado usuario) {
        if (usuario == null || usuario.perfil() == null) {
            throw new AccessDeniedException("Usuário autenticado sem perfil definido para utilizar o assistente.");
        }

        PerfilUsuarioEnum perfil = usuario.perfil();
        return switch (perfil) {
            case ADMIN -> new ContextoPerfil(
                    montarPromptAdmin(usuario),
                    new Object[]{pedidoChatTools, adminEstoqueChatTools}
            );
            // Futuro: configurar prompts e conjuntos restritos de ferramentas para ASSOCIADO, ESTOQUE, RECEPCAO, etc.
            default -> throw new AccessDeniedException(
                    "A assistente Carollina AI ainda não está habilitada para o perfil " + perfil.name() + ".");
        };
    }

    private String chaveMemoria(String uid, String conversaId) {
        return uid + ":" + conversaId;
    }

    private String extrairTexto(ChatResponse response) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return null;
        }
        // O Gemini pode dividir uma única resposta em múltiplas Parts (cada Part vira uma Generation em getResults()).
        // Concatenar todas as Generations não-thought evita respostas cortadas no meio (getResult() pegava só a 1ª parte).
        StringBuilder sb = new StringBuilder();
        for (Generation generation : response.getResults()) {
            if (generation == null || generation.getOutput() == null) {
                continue;
            }
            var output = generation.getOutput();
            if (output.getMetadata() != null
                    && Boolean.TRUE.equals(output.getMetadata().get("isThought"))) {
                continue;
            }
            String texto = output.getText();
            if (texto != null) {
                sb.append(texto);
            }
        }
        return sb.toString().trim();
    }

    private void registrarMetricas(PerfilUsuarioEnum perfil, String conversaId, long duracaoMs, ChatResponse response) {
        Usage usage = (response != null && response.getMetadata() != null)
                ? response.getMetadata().getUsage()
                : null;
        int qtdPartes = (response != null && response.getResults() != null) ? response.getResults().size() : 0;
        String finishReason = (response != null && response.getResult() != null && response.getResult().getMetadata() != null)
                ? response.getResult().getMetadata().getFinishReason()
                : "UNKNOWN";

        if (usage != null) {
            log.info("Chat OK | perfil={} | conversa={} | tempo={}ms | partes={} | finish={} | tokens(in={}, out={}, total={})",
                    perfil, conversaId, duracaoMs, qtdPartes, finishReason,
                    usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
        } else {
            log.info("Chat OK | perfil={} | conversa={} | tempo={}ms | partes={} | finish={}",
                    perfil, conversaId, duracaoMs, qtdPartes, finishReason);
        }
    }

    private void restaurarHistorico(String chaveMemoria, List<Message> historicoAnterior) {
        this.chatMemory.clear(chaveMemoria);
        if (!historicoAnterior.isEmpty()) {
            this.chatMemory.add(chaveMemoria, historicoAnterior);
        }
    }

    private String traduzirErroGemini(Throwable ex) {
        String detalhe = concatenarMensagens(ex).toLowerCase();
        if (detalhe.contains("429") || detalhe.contains("resource_exhausted") || detalhe.contains("quota")) {
            return "Atingimos o limite temporário de requisições da API do Gemini (plano gratuito). Aguarde cerca de 30 a 60 segundos e tente novamente.";
        }
        if (detalhe.contains("503") || detalhe.contains("unavailable") || detalhe.contains("overloaded")) {
            return "O serviço do Gemini está temporariamente sobrecarregado. Aguarde alguns instantes e envie sua mensagem novamente.";
        }
        if (detalhe.contains("401") || detalhe.contains("403") || detalhe.contains("api key") || detalhe.contains("permission_denied")) {
            return "Falha de autenticação com a API do Gemini. Verifique se a variável de ambiente GEMINI_API_KEY está configurada e válida.";
        }
        if (detalhe.contains("timeout") || detalhe.contains("timed out")) {
            return "A resposta do Gemini demorou mais do que o esperado. Tente enviar uma pergunta mais direta novamente.";
        }
        return "Ocorreu uma instabilidade ao consultar a IA no momento. Tente enviar sua mensagem novamente.";
    }

    private String concatenarMensagens(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable atual = ex;
        int limite = 6;
        while (atual != null && limite-- > 0) {
            if (atual.getMessage() != null) {
                sb.append(atual.getMessage()).append(' ');
            }
            atual = atual.getCause();
        }
        return sb.toString();
    }

    private String sanitizarParaPrompt(String valor, String fallback) {
        String origem = (valor != null && !valor.isBlank()) ? valor : fallback;
        if (origem == null) {
            return "Administrador";
        }
        String limpo = origem.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        if (limpo.length() > MAX_TAMANHO_NOME_PROMPT) {
            limpo = limpo.substring(0, MAX_TAMANHO_NOME_PROMPT);
        }
        return limpo.isEmpty() ? "Administrador" : limpo;
    }

    private String montarPromptAdmin(UsuarioAutenticado usuario) {
        String nome = sanitizarParaPrompt(usuario.nome(), usuario.email());
        String email = sanitizarParaPrompt(usuario.email(), "não informado");
        LocalDate hojeDate = LocalDate.now(FUSO_BRASIL);
        String hoje = hojeDate.format(FORMATO_DATA);
        String hojeIso = hojeDate.toString();

        return """
                Você é a Carollina AI, assistente virtual inteligente do Clube Collina.
                Data de hoje: %s (ISO: %s).
                Usuário logado: %s (%s) — Perfil: ADMIN.
                Sua função é auxiliar na gestão operacional de estoque, produtos e pedidos com agilidade, precisão e cordialidade.

                Diretrizes de autonomia e regras fundamentais:
                1. NUNCA invente IDs de produtos, categorias ou pedidos. Para localizar um produto pelo nome (ex: "coca", "agua"), use 'buscarProdutos'; para descobrir IDs de categorias, use 'listarCategorias'.
                2. Para ver o cardápio geral disponível (estoque > 0), use 'buscarProdutosDisponiveis'. Para filtrar por categoria (ex: "bebidas", "lanches"), use 'buscarCardapioPorCategoria'. Ao exibir o cardápio, não informe quantidades em estoque.
                3. Para ver o catálogo completo de todos os cadastros (incluindo esgotados e saldo exato), use 'buscarTodosProdutos'.
                4. Para responder qual é o valor total em dinheiro / valor financeiro do estoque (soma de quantidade × preço), use 'consultarResumoValorEstoque' (nunca tente somar de cabeça).
                5. Para saber quais itens estão acabando ou abaixo do estoque mínimo, use 'consultarItensComEstoqueBaixo'. Para saldo detalhado de um item específico, use 'consultarEstoqueProduto'. Para histórico de entradas/saídas de um produto, use 'consultarHistoricoMovimentacoes'.
                6. Para cadastrar, editar ou remover produtos, use 'criarProduto', 'atualizarProduto' ou 'removerProduto'. Para criar, editar ou remover categorias, use 'criarCategoria', 'editarCategoria' ou 'removerCategoria'.
                7. Para registrar entradas (COMPRA_FORNECEDOR) ou saídas manuais (USO_INTERNO, PERDA_DANO) de estoque, use 'registrarMovimentacaoEstoque'. Se faltar o tipo ou a quantidade, pergunte antes de registrar. Para ajustar o estoque mínimo, use 'atualizarEstoqueMinimo'.
                8. Para listar pedidos (em aberto, pagos, entregues, cancelados, por período como hoje/semana ou por cliente), use 'listarPedidos' informando status, datas no formato YYYY-MM-DD e/ou nome/e-mail do cliente (ex: pedidos em aberto -> status='AGUARDANDO_PAGAMENTO'; pedidos de hoje -> dataInicio='%s', dataFim='%s').
                9. Para ver detalhes/itens de um pedido específico, use 'consultarPedido'. Para marcar um pedido já pago como entregue, use 'marcarPedidoComoEntregue'.
                10. Para abrir um novo pedido, use 'criarPedido' enviando todos os itens solicitados de uma única vez na lista e, se o administrador informar para qual cliente/usuário é o pedido, passe o nome ou e-mail no parâmetro opcional 'cliente' (se omitido, vincula ao usuário logado). Para pedidos já existentes, use 'adicionarItemAoPedido', 'alterarQuantidadeItem', 'removerItemDoPedido' ou 'cancelarPedido'.
                11. Você NÃO realiza nem confirma pagamentos de pedidos; se solicitado, informe que o pagamento deve ser registrado na tela de Pedidos.
                12. Se alguma ferramenta retornar erro (como estoque insuficiente ou pedido inexistente), explique com clareza ao administrador.
                13. Toda alteração no banco de dados passa por confirmação obrigatória em um card interativo na interface. Quando uma ferramenta retornar AGUARDANDO CONFIRMAÇÃO, NUNCA diga que a alteração já foi salva/concluída — diga brevemente que preparou a ação e peça ao administrador para clicar em Confirmar ou Cancelar no card abaixo.
                14. Responda de forma curta, direta e bem organizada em tópicos simples (evite tabelas Markdown largas ou textos muito extensos, pois a leitura é em um painel lateral).
                """.formatted(hoje, hojeIso, nome, email, hojeIso, hojeIso);
    }
}
