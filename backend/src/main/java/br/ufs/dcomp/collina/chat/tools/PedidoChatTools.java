package br.ufs.dcomp.collina.chat.tools;

import br.ufs.dcomp.collina.dto.estoque.EstoqueResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.PedidoRequestDTO;
import br.ufs.dcomp.collina.dto.estoque.PedidoResponseDTO;
import br.ufs.dcomp.collina.model.estoque.Categoria;
import br.ufs.dcomp.collina.model.estoque.ItemPedido;
import br.ufs.dcomp.collina.model.estoque.Produto;
import br.ufs.dcomp.collina.repository.estoque.EstoqueRepository;
import br.ufs.dcomp.collina.repository.estoque.ItemPedidoRepository;
import br.ufs.dcomp.collina.repository.estoque.ProdutoRepository;
import br.ufs.dcomp.collina.service.estoque.EstoqueService;
import br.ufs.dcomp.collina.service.estoque.PedidoService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class PedidoChatTools {

    private final ProdutoRepository produtoRepository;
    private final PedidoService pedidoService;
    private final EstoqueService estoqueService;
    private final EstoqueRepository estoqueRepository;
    private final ItemPedidoRepository itemPedidoRepository;

    public PedidoChatTools(
            ProdutoRepository produtoRepository,
            PedidoService pedidoService,
            EstoqueService estoqueService,
            EstoqueRepository estoqueRepository,
            ItemPedidoRepository itemPedidoRepository) {
        this.produtoRepository = produtoRepository;
        this.pedidoService = pedidoService;
        this.estoqueService = estoqueService;
        this.estoqueRepository = estoqueRepository;
        this.itemPedidoRepository = itemPedidoRepository;
    }

    /**
     * Ferramenta: Busca produtos específicos pelo nome/termo no catálogo.
     */
    @Tool(description = "Busca produtos específicos pelo nome ou termo. Retorna os IDs, nomes e preços dos produtos encontrados.")
    public String buscarProdutos(
            @ToolParam(description = "Termo de busca digitado pelo usuário, ex: 'coca', 'hambúrguer'") String termo) {

        List<Produto> encontrados = produtoRepository.findAll().stream()
                .filter(p -> p.getNome() != null && p.getNome().toLowerCase().contains(termo.toLowerCase()))
                .toList();

        if (encontrados.isEmpty()) {
            return "Nenhum produto encontrado com o termo: '" + termo + "'.";
        }

        return encontrados.stream()
                .map(p -> String.format("ID: %d | Nome: %s | Preço: R$ %.2f", p.getId(), p.getNome(), p.getPreco()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Ferramenta: Exibe o cardápio formatado com APENAS os produtos com estoque disponível (> 0).
     */
    @Tool(description = "USE ESTA FERRAMENTA quando o usuário pedir o CARDÁPIO, menu de comidas, bebidas ou produtos disponíveis para consumo/pedido. Mostra APENAS produtos que possuem estoque maior que zero, organizados por categoria. Produtos sem estoque NUNCA são exibidos.")
    public String buscarProdutosDisponiveis() {
        try {
            List<Produto> todosProdutos = produtoRepository.findAll();
            if (todosProdutos.isEmpty()) {
                return "Não há produtos cadastrados no sistema.";
            }

            Map<Integer, Integer> estoquePorProduto = estoqueRepository.findAll().stream()
                    .filter(e -> e.getProduto() != null && e.getProduto().getId() != null)
                    .collect(Collectors.toMap(
                            e -> e.getProduto().getId(),
                            e -> e.getQuantidade() != null ? e.getQuantidade() : 0,
                            (q1, q2) -> q1
                    ));

            // Filtra estritamente apenas os produtos que possuem estoque maior que zero
            List<Produto> produtosDisponiveis = todosProdutos.stream()
                    .filter(p -> estoquePorProduto.getOrDefault(p.getId(), 0) > 0)
                    .toList();

            if (produtosDisponiveis.isEmpty()) {
                return "No momento não há nenhum produto com estoque disponível no cardápio.";
            }

            // Agrupa por categoria para exibição de cardápio real
            Map<String, List<Produto>> produtosPorCategoria = new TreeMap<>();
            for (Produto p : produtosDisponiveis) {
                if (p.getCategorias() == null || p.getCategorias().isEmpty()) {
                    produtosPorCategoria.computeIfAbsent("Diversos", k -> new ArrayList<>()).add(p);
                } else {
                    for (Categoria c : p.getCategorias()) {
                        String nomeCat = (c.getNome() != null && !c.getNome().isBlank()) ? c.getNome() : "Diversos";
                        produtosPorCategoria.computeIfAbsent(nomeCat, k -> new ArrayList<>()).add(p);
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📋 === CARDÁPIO DO CLUBE COLLINA (PRODUTOS DISPONÍVEIS) ===\n\n");

            for (Map.Entry<String, List<Produto>> entry : produtosPorCategoria.entrySet()) {
                sb.append(String.format("🍽️ [%s]\n", entry.getKey().toUpperCase()));
                for (Produto p : entry.getValue()) {
                    sb.append(String.format("• ID: %d | %s — R$ %.2f\n",
                            p.getId(), p.getNome(), p.getPreco() != null ? p.getPreco() : BigDecimal.ZERO));
                    if (p.getDescricao() != null && !p.getDescricao().isBlank()) {
                        sb.append(String.format("  Detalhes: %s\n", p.getDescricao().trim()));
                    }
                }
                sb.append("\n");
            }

            return sb.toString().trim();
        } catch (Exception e) {
            return "Erro ao montar o cardápio de produtos disponíveis: " + e.getMessage();
        }
    }

    /**
     * Ferramenta: Exibe os itens do cardápio filtrados por uma categoria específica (apenas produtos com estoque > 0).
     */
    @Tool(description = "Busca e exibe os produtos disponíveis no cardápio de uma categoria específica (ex: 'bebidas', 'lanches', 'sobremesas'). Mostra apenas produtos com estoque maior que zero, no formato de cardápio com preços e descrições. Não exibe a quantidade de estoque.")
    public String buscarCardapioPorCategoria(
            @ToolParam(description = "Nome ou termo da categoria desejada, ex: 'bebidas', 'lanches', 'sobremesas'") String categoria) {
        try {
            if (categoria == null || categoria.trim().isBlank()) {
                return "Informe o nome de uma categoria para filtrar o cardápio.";
            }

            String termoCategoria = categoria.trim().toLowerCase();
            String termoLimpo = (termoCategoria.endsWith("s") && termoCategoria.length() > 3)
                    ? termoCategoria.substring(0, termoCategoria.length() - 1)
                    : termoCategoria;

            List<Produto> todosProdutos = produtoRepository.findAll();
            if (todosProdutos.isEmpty()) {
                return "Não há produtos cadastrados no sistema.";
            }

            Map<Integer, Integer> estoquePorProduto = estoqueRepository.findAll().stream()
                    .filter(e -> e.getProduto() != null && e.getProduto().getId() != null)
                    .collect(Collectors.toMap(
                            e -> e.getProduto().getId(),
                            e -> e.getQuantidade() != null ? e.getQuantidade() : 0,
                            (q1, q2) -> q1
                    ));

            // Filtra produtos com estoque > 0 que pertençam à categoria informada
            List<Produto> produtosDaCategoria = todosProdutos.stream()
                    .filter(p -> estoquePorProduto.getOrDefault(p.getId(), 0) > 0)
                    .filter(p -> p.getCategorias() != null && p.getCategorias().stream()
                            .anyMatch(c -> {
                                if (c.getNome() == null) return false;
                                String nomeCat = c.getNome().toLowerCase();
                                return nomeCat.contains(termoCategoria)
                                        || termoCategoria.contains(nomeCat)
                                        || nomeCat.contains(termoLimpo);
                            }))
                    .toList();

            if (produtosDaCategoria.isEmpty()) {
                return String.format("No momento não há produtos disponíveis com estoque na categoria '%s'.", categoria);
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("🍽️ === CARDÁPIO: %s (PRODUTOS DISPONÍVEIS) ===\n\n", categoria.toUpperCase()));

            for (Produto p : produtosDaCategoria) {
                sb.append(String.format("• ID: %d | %s — R$ %.2f\n",
                        p.getId(), p.getNome(), p.getPreco() != null ? p.getPreco() : BigDecimal.ZERO));
                if (p.getDescricao() != null && !p.getDescricao().isBlank()) {
                    sb.append(String.format("  Detalhes: %s\n", p.getDescricao().trim()));
                }
            }

            return sb.toString().trim();
        } catch (Exception e) {
            return "Erro ao buscar produtos da categoria '" + categoria + "': " + e.getMessage();
        }
    }

    /**
     * Ferramenta: Lista TODOS os produtos cadastrados no sistema (inclusive sem estoque).
     */
    @Tool(description = "USE ESTA FERRAMENTA APENAS quando o usuário solicitar explicitamente o relatório ou catálogo COMPLETO/GERAL de todos os cadastros do sistema (incluindo esgotados/fora de estoque). NUNCA use esta ferramenta para pedidos de cardápio comum.")
    public String buscarTodosProdutos() {
        try {
            List<Produto> produtos = produtoRepository.findAll();
            if (produtos.isEmpty()) {
                return "Nenhum produto cadastrado no sistema.";
            }

            Map<Integer, Integer> estoquePorProduto = estoqueRepository.findAll().stream()
                    .filter(e -> e.getProduto() != null && e.getProduto().getId() != null)
                    .collect(Collectors.toMap(
                            e -> e.getProduto().getId(),
                            e -> e.getQuantidade() != null ? e.getQuantidade() : 0,
                            (q1, q2) -> q1
                    ));

            StringBuilder sb = new StringBuilder("=== TODOS OS PRODUTOS CADASTRADOS (RELATÓRIO GERAL) ===\n");
            for (Produto p : produtos) {
                int qtdEstoque = estoquePorProduto.getOrDefault(p.getId(), 0);
                String statusEstoque = qtdEstoque > 0 ? String.format("Disponível (%d un)", qtdEstoque) : "Esgotado (0 un)";
                String categorias = p.getCategorias() != null && !p.getCategorias().isEmpty()
                        ? p.getCategorias().stream().map(Categoria::getNome).collect(Collectors.joining(", "))
                        : "Sem categoria";

                sb.append(String.format("ID: %d | Nome: %s | Preço: R$ %.2f | Categorias: [%s] | Status: %s\n",
                        p.getId(), p.getNome(), p.getPreco() != null ? p.getPreco() : BigDecimal.ZERO, categorias, statusEstoque));
                if (p.getDescricao() != null && !p.getDescricao().isBlank()) {
                    sb.append(String.format("   Descrição: %s\n", p.getDescricao().trim()));
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Erro ao buscar todos os produtos: " + e.getMessage();
        }
    }

    /**
     * Ferramenta: Cria um novo pedido com um item inicial.
     */
    @Tool(description = "Cria um novo pedido com um item inicial obrigatório. Retorna o ID do pedido gerado, seu status e o total inicial.")
    @Transactional
    public String criarPedido(
            @ToolParam(description = "ID do produto inicial a ser incluído no novo pedido") Integer produtoId,
            @ToolParam(description = "Quantidade do produto inicial") Integer quantidade) {
        try {
            PedidoRequestDTO.ItemPedidoRequestDTO itemDto = new PedidoRequestDTO.ItemPedidoRequestDTO(produtoId, quantidade);
            PedidoRequestDTO pedidoRequest = new PedidoRequestDTO(Set.of(itemDto));

            PedidoResponseDTO pedidoCriado = pedidoService.criarPedido(pedidoRequest);

            return String.format("Pedido #%d criado com sucesso! Status: %s | Total inicial: R$ %.2f.",
                    pedidoCriado.id(), pedidoCriado.statusPedido(), pedidoCriado.total());
        } catch (Exception e) {
            return "Erro ao criar pedido: " + e.getMessage();
        }
    }

    /**
     * Ferramenta: Consulta dados e itens detalhados de um pedido.
     */
    @Tool(description = "Consulta os dados e todos os itens de um pedido pelo seu ID. Retorna o status, valor total acumulado e a lista detalhada de itens com IDs, quantidades e preços.")
    public String consultarPedido(
            @ToolParam(description = "ID do pedido a ser consultado") Integer pedidoId) {
        try {
            PedidoResponseDTO pedido = pedidoService.buscarPorId(pedidoId);
            List<ItemPedido> itens = itemPedidoRepository.findByPedidoId(pedidoId);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Pedido #%d | Status: %s | Total: R$ %.2f\n",
                    pedido.id(), pedido.statusPedido(), pedido.total()));

            if (itens == null || itens.isEmpty()) {
                sb.append("Nenhum item adicionado a este pedido até o momento.");
            } else {
                sb.append("Itens no pedido:\n");
                for (ItemPedido item : itens) {
                    String nome = (item.getProduto() != null) ? item.getProduto().getNome() : "Desconhecido";
                    Integer prodId = (item.getProduto() != null) ? item.getProduto().getId() : null;
                    BigDecimal precoUnitario = item.getPreco() != null ? item.getPreco() : BigDecimal.ZERO;
                    int qtd = item.getQuantidade() != null ? item.getQuantidade() : 0;
                    BigDecimal subtotal = precoUnitario.multiply(BigDecimal.valueOf(qtd));

                    sb.append(String.format("- ItemID: %d | Produto: %s (ProdID: %s) | Qtd: %d | Preço Unit: R$ %.2f | Subtotal: R$ %.2f\n",
                            item.getId(), nome, prodId, qtd, precoUnitario, subtotal));
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Erro ao consultar pedido: " + e.getMessage();
        }
    }

    /**
     * Ferramenta: Adiciona um item a um pedido já aberto.
     */
    @Tool(description = "Adiciona um produto a um pedido existente. Valida estoque e regras de negócio.")
    @Transactional
    public String adicionarItemAoPedido(
            @ToolParam(description = "ID do pedido onde o item será adicionado") Integer pedidoId,
            @ToolParam(description = "ID do produto a ser adicionado") Integer produtoId,
            @ToolParam(description = "Quantidade de itens") Integer quantidade) {

        try {
            PedidoResponseDTO.ItemPedidoResponseDTO itemAdicionado =
                    pedidoService.adicionarItemAoPedido(pedidoId, produtoId, quantidade);

            return String.format("Sucesso! %d unidade(s) do produto '%s' adicionada(s) ao pedido %d. Preço unitário: R$ %.2f.",
                    quantidade, itemAdicionado.nomeDoProduto(), pedidoId, itemAdicionado.preco());

        } catch (Exception e) {
            return "Erro ao adicionar item: " + e.getMessage();
        }
    }

    /**
     * Ferramenta: Altera a quantidade de um item no pedido.
     */
    @Tool(description = "Altera a quantidade de um item em um pedido já aberto. O identificadorItem pode ser o ID do item no pedido (ItemID) ou o ID do produto.")
    @Transactional
    public String alterarQuantidadeItem(
            @ToolParam(description = "ID do pedido") Integer pedidoId,
            @ToolParam(description = "ID do item (ItemID obtido na consulta do pedido) ou ID do produto") Integer identificadorItem,
            @ToolParam(description = "Nova quantidade desejada") Integer novaQuantidade) {

        try {
            List<ItemPedido> itens = itemPedidoRepository.findByPedidoId(pedidoId);
            Optional<ItemPedido> itemEncontrado = itens.stream()
                    .filter(i -> i.getId().equals(identificadorItem))
                    .findFirst();

            if (itemEncontrado.isEmpty()) {
                itemEncontrado = itens.stream()
                        .filter(i -> i.getProduto() != null && i.getProduto().getId().equals(identificadorItem))
                        .findFirst();
            }

            if (itemEncontrado.isEmpty()) {
                return "Nenhum item encontrado no pedido " + pedidoId + " para o identificador: " + identificadorItem + ".";
            }

            Integer itemIdReal = itemEncontrado.get().getId();
            PedidoResponseDTO.ItemPedidoResponseDTO atualizado = pedidoService.alterarQuantidadeItem(pedidoId, itemIdReal, novaQuantidade);

            return String.format("Sucesso! A quantidade do produto '%s' no pedido %d foi atualizada para %d.",
                    atualizado.nomeDoProduto(), pedidoId, atualizado.quantidade());
        } catch (Exception e) {
            return "Erro ao alterar quantidade: " + e.getMessage();
        }
    }

    /**
     * Ferramenta: Remove um item de um pedido em aberto.
     */
    @Tool(description = "Remove um item de um pedido em aberto. O identificadorItem pode ser o ID do item no pedido (ItemID) ou o ID do produto a remover.")
    @Transactional
    public String removerItemDoPedido(
            @ToolParam(description = "ID do pedido") Integer pedidoId,
            @ToolParam(description = "ID do item (ItemID obtido na consulta do pedido) ou ID do produto a remover") Integer identificadorItem) {

        try {
            List<ItemPedido> itens = itemPedidoRepository.findByPedidoId(pedidoId);
            Optional<ItemPedido> itemEncontrado = itens.stream()
                    .filter(i -> i.getId().equals(identificadorItem))
                    .findFirst();

            if (itemEncontrado.isEmpty()) {
                itemEncontrado = itens.stream()
                        .filter(i -> i.getProduto() != null && i.getProduto().getId().equals(identificadorItem))
                        .findFirst();
            }

            if (itemEncontrado.isEmpty()) {
                return "Nenhum item correspondente encontrado no pedido " + pedidoId + " para o identificador: " + identificadorItem + ".";
            }

            Integer itemIdReal = itemEncontrado.get().getId();
            String nomeProduto = itemEncontrado.get().getProduto() != null
                    ? itemEncontrado.get().getProduto().getNome()
                    : "Item #" + itemIdReal;

            pedidoService.removerItem(pedidoId, itemIdReal);
            return String.format("Sucesso! O item '%s' foi removido do pedido %d.", nomeProduto, pedidoId);
        } catch (Exception e) {
            return "Erro ao remover item: " + e.getMessage();
        }
    }

    /**
     * Ferramenta: Cancela um pedido em aberto.
     */
    @Tool(description = "Cancela um pedido em aberto (status AGUARDANDO_PAGAMENTO). Não permite cancelar pedidos já finalizados.")
    @Transactional
    public String cancelarPedido(
            @ToolParam(description = "ID do pedido a ser cancelado") Integer pedidoId) {
        try {
            PedidoResponseDTO cancelado = pedidoService.cancelarPedido(pedidoId);
            return String.format("Pedido #%d cancelado com sucesso. Status atual: %s.",
                    cancelado.id(), cancelado.statusPedido());
        } catch (Exception e) {
            return "Erro ao cancelar pedido: " + e.getMessage();
        }
    }

    /**
     * Ferramenta: Consulta saldo em estoque de um produto.
     */
    @Tool(description = "Consulta a quantidade disponível em estoque e o estoque mínimo de um produto pelo ID do produto.")
    public String consultarEstoqueProduto(
            @ToolParam(description = "ID do produto") Integer produtoId) {
        try {
            EstoqueResponseDTO estoque = estoqueService.buscarPorProdutoId(produtoId);
            return String.format("Produto: %s (ID: %d) | Saldo disponível em estoque: %d unidades | Estoque mínimo: %d.",
                    estoque.nomeProduto(), estoque.produtoId(), estoque.quantidade(), estoque.estoqueMinimo());
        } catch (Exception e) {
            return "Erro ao consultar estoque: " + e.getMessage();
        }
    }
}
