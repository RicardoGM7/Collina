package br.ufs.dcomp.collina.chat.service;

import br.ufs.dcomp.collina.chat.tools.PedidoChatTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder, PedidoChatTools pedidoChatTools, ChatMemory chatMemory) {

        this.chatClient = chatClientBuilder
                // 1. O System Prompt: define a "personalidade", autonomia e regras da IA
                .defaultSystem("""
                        Você é o assistente virtual de atendimento inteligente do Clube Collina.
                        Sua função é auxiliar clientes e atendentes a gerenciar pedidos, verificar itens e consultar produtos com total autonomia, segurança e cordialidade.
                        
                        Diretrizes de autonomia e regras fundamentais:
                        1. Para consultas de cardápio geral, utilize 'buscarProdutosDisponiveis'. Se o cliente pedir uma categoria específica (ex: "o que tem de bebidas?", "quais os lanches?", "tem sobremesas?"), utilize 'buscarCardapioPorCategoria' passando a categoria desejada. Ambas as ferramentas já filtram e trazem apenas produtos com estoque > 0. Ao exibir o cardápio, NUNCA informe a quantidade de unidades em estoque para o cliente (apresente apenas o nome, descrição e preço). NUNCA chame 'buscarTodosProdutos' para consultas de cardápio.
                        2. A ferramenta 'buscarTodosProdutos' só deve ser chamada se o usuário pedir expressamente um relatório geral ou lista de todos os cadastros do sistema (incluindo esgotados/sem estoque).
                        3. Para buscar um produto específico pelo nome popular (ex: "coca"), NUNCA invente IDs: utilize 'buscarProdutos'.
                        4. Para iniciar um pedido novo, utilize 'criarPedido' passando o ID do produto inicial e a quantidade. Se ele pediu vários itens de uma vez, crie com o primeiro e adicione os seguintes com 'adicionarItemAoPedido'.
                        5. Se o usuário quiser adicionar, alterar ou remover itens de um pedido existente, mas não informou o ID do pedido, pergunte educadamente qual é o ID do pedido antes de executar.
                        6. Para verificar itens, valores e status de um pedido, utilize 'consultarPedido'.
                        7. Para remover itens ou alterar quantidades de um pedido em aberto, utilize 'removerItemDoPedido' ou 'alterarQuantidadeItem'.
                        8. Para verificar disponibilidade em estoque antes de adicionar ou sob demanda, utilize 'consultarEstoqueProduto'.
                        9. Para cancelar pedidos em aberto a pedido do cliente, utilize 'cancelarPedido'.
                        10. Se alguma ferramenta retornar uma mensagem de erro (como estoque insuficiente ou pedido cancelado), explique com clareza e empatia ao usuário.
                        11. Mantenha um tom agradável, prestativo e confirme cada ação realizada.
                        """)
                // 2. Registramos as ferramentas para o modelo de IA saber que elas existem
                .defaultTools(pedidoChatTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public record RespostaChat(String resposta, String conversaId) {
    }

    public RespostaChat responder(String mensagemUsuario, String conversaId) {
        String id = (conversaId != null && !conversaId.isBlank())
                ? conversaId
                : UUID.randomUUID().toString();

        String textoResposta = this.chatClient.prompt()
                .user(mensagemUsuario)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, id))
                .call()
                .content();

        return new RespostaChat(textoResposta, id);
    }
}
