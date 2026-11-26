package br.ufs.dcomp.collina.chat.tools;

import br.ufs.dcomp.collina.chat.service.ChatAcaoPendenteService;
import br.ufs.dcomp.collina.dto.estoque.CategoriaResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.ClientePedidoResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.EstoqueResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.PedidoRequestDTO;
import br.ufs.dcomp.collina.dto.estoque.PedidoResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.ProdutoResponseDTO;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import br.ufs.dcomp.collina.model.estoque.StatusTransacaoEnum;
import br.ufs.dcomp.collina.service.estoque.CategoriaService;
import br.ufs.dcomp.collina.service.estoque.EstoqueService;
import br.ufs.dcomp.collina.service.estoque.PedidoService;
import br.ufs.dcomp.collina.service.estoque.ProdutoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Ferramentas de cardápio e pedidos para o assistente IA.
 * - Consultas (leitura): executadas imediatamente.
 * - Alterações no banco (escrita): pré-validadas e registradas como ação pendente
 *   via ChatAcaoPendenteService (padrão Human-in-the-Loop).
 */
@Component
public class PedidoChatTools {

    private static final Logger log = LoggerFactory.getLogger(PedidoChatTools.class);
    private static final Locale LOCALE_BR = Locale.of("pt", "BR");

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;
    private final EstoqueService estoqueService;
    private final PedidoService pedidoService;
    private final ChatAcaoPendenteService acaoPendenteService;

    public PedidoChatTools(
            ProdutoService produtoService,
            CategoriaService categoriaService,
            EstoqueService estoqueService,
            PedidoService pedidoService,
            ChatAcaoPendenteService acaoPendenteService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
        this.estoqueService = estoqueService;
        this.pedidoService = pedidoService;
        this.acaoPendenteService = acaoPendenteService;
    }

    public record ItemPedidoInput(
            @ToolParam(description = "ID do produto") Integer produtoId,
            @ToolParam(description = "Quantidade desejada (inteiro maior que zero)") Integer quantidade
    ) {
    }

    // ==========================================
    // 1. Cardápio, Categorias e Busca (Somente Leitura)
    // ==========================================

    @Tool(description = "Lista todas as categorias cadastradas no cardápio (ex: Bebidas, Lanches, Sobremesas). Útil para descobrir os nomes exatos das categorias disponíveis.")
    public String listarCategorias() {
        try {
            List<CategoriaResponseDTO> categorias = categoriaService.listarTodas();
            if (categorias.isEmpty()) {
                return "Nenhuma categoria cadastrada no sistema.";
            }
            return categorias.stream()
                    .sorted(Comparator.comparing(c -> normalizar(c.nome())))
                    .map(c -> String.format("- ID: %d | %s", c.id(), c.nome()))
                    .collect(Collectors.joining("\n", "📂 Categorias disponíveis:\n", ""));
        } catch (Exception e) {
            log.warn("Erro na tool listarCategorias: {}", e.getMessage());
            return "Erro ao listar categorias: " + e.getMessage();
        }
    }

    @Tool(description = "Busca produtos específicos pelo nome ou termo (ignora acentos, maiúsculas e pontuação). Retorna IDs, nomes e preços dos produtos encontrados.")
    public String buscarProdutos(
            @ToolParam(description = "Termo de busca digitado pelo usuário, ex: 'coca', 'agua', 'hamburguer'") String termo) {
        try {
            String termoNorm = normalizar(termo);
            if (termoNorm.isBlank()) {
                return "Informe um termo de busca para localizar produtos.";
            }

            List<ProdutoResponseDTO> encontrados = produtoService.listarTodos().stream()
                    .filter(p -> {
                        String nomeNorm = normalizar(p.nome());
                        return !nomeNorm.isEmpty() && nomeNorm.contains(termoNorm);
                    })
                    .toList();

            if (encontrados.isEmpty()) {
                return "Nenhum produto encontrado com o termo: '" + termo + "'.";
            }

            return encontrados.stream()
                    .map(p -> String.format("ID: %d | Nome: %s | Preço: %s",
                            p.id(), p.nome(), formatarMoeda(p.preco())))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("Erro na tool buscarProdutos('{}'): {}", termo, e.getMessage());
            return "Erro ao buscar produtos: " + e.getMessage();
        }
    }

    @Tool(description = "USE ESTA FERRAMENTA quando o usuário pedir o CARDÁPIO geral ou opções disponíveis para consumo/pedido. Mostra APENAS produtos com estoque maior que zero, organizados por categoria, sem exibir quantidade de estoque.")
    public String buscarProdutosDisponiveis() {
        try {
            List<ProdutoResponseDTO> todosProdutos = produtoService.listarTodos();
            if (todosProdutos.isEmpty()) {
                return "Não há produtos cadastrados no sistema.";
            }

            Map<Integer, Integer> estoquePorProduto = carregarMapaEstoque();

            List<ProdutoResponseDTO> produtosDisponiveis = todosProdutos.stream()
                    .filter(p -> estoquePorProduto.getOrDefault(p.id(), 0) > 0)
                    .toList();

            if (produtosDisponiveis.isEmpty()) {
                return "No momento não há nenhum produto com estoque disponível no cardápio.";
            }

            Map<String, List<ProdutoResponseDTO>> produtosPorCategoria = new TreeMap<>();
            for (ProdutoResponseDTO p : produtosDisponiveis) {
                if (p.categorias() == null || p.categorias().isEmpty()) {
                    produtosPorCategoria.computeIfAbsent("Diversos", k -> new ArrayList<>()).add(p);
                } else {
                    for (CategoriaResponseDTO c : p.categorias()) {
                        String nomeCat = (c.nome() != null && !c.nome().isBlank()) ? c.nome() : "Diversos";
                        produtosPorCategoria.computeIfAbsent(nomeCat, k -> new ArrayList<>()).add(p);
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📋 === CARDÁPIO DO CLUBE COLLINA (PRODUTOS DISPONÍVEIS) ===\n\n");

            for (Map.Entry<String, List<ProdutoResponseDTO>> entry : produtosPorCategoria.entrySet()) {
                sb.append(String.format("🍽️ [%s]\n", entry.getKey().toUpperCase(LOCALE_BR)));
                for (ProdutoResponseDTO p : entry.getValue()) {
                    anexarLinhaCardapio(sb, p);
                }
                sb.append("\n");
            }

            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Erro na tool buscarProdutosDisponiveis: {}", e.getMessage());
            return "Erro ao montar o cardápio de produtos disponíveis: " + e.getMessage();
        }
    }

    @Tool(description = "Busca e exibe os produtos disponíveis no cardápio de uma categoria específica (ex: 'bebidas', 'lanches', 'sobremesas'). Mostra apenas produtos com estoque maior que zero, sem exibir a quantidade de estoque.")
    public String buscarCardapioPorCategoria(
            @ToolParam(description = "Nome ou termo da categoria desejada, ex: 'bebidas', 'lanches', 'sobremesas'") String categoria) {
        try {
            if (categoria == null || categoria.trim().isBlank()) {
                return "Informe o nome de uma categoria para filtrar o cardápio.";
            }

            String termoCategoria = normalizar(categoria);
            String termoLimpo = (termoCategoria.endsWith("s") && termoCategoria.length() > 3)
                    ? termoCategoria.substring(0, termoCategoria.length() - 1)
                    : termoCategoria;

            List<ProdutoResponseDTO> todosProdutos = produtoService.listarTodos();
            if (todosProdutos.isEmpty()) {
                return "Não há produtos cadastrados no sistema.";
            }

            Map<Integer, Integer> estoquePorProduto = carregarMapaEstoque();

            List<ProdutoResponseDTO> produtosDaCategoria = todosProdutos.stream()
                    .filter(p -> estoquePorProduto.getOrDefault(p.id(), 0) > 0)
                    .filter(p -> p.categorias() != null && p.categorias().stream()
                            .anyMatch(c -> {
                                String nomeCat = normalizar(c.nome());
                                if (nomeCat.isEmpty()) return false;
                                return nomeCat.contains(termoCategoria)
                                        || termoCategoria.contains(nomeCat)
                                        || nomeCat.contains(termoLimpo);
                            }))
                    .toList();

            if (produtosDaCategoria.isEmpty()) {
                return String.format("No momento não há produtos disponíveis com estoque na categoria '%s'.", categoria);
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("🍽️ === CARDÁPIO: %s (PRODUTOS DISPONÍVEIS) ===\n\n", categoria.toUpperCase(LOCALE_BR)));
            for (ProdutoResponseDTO p : produtosDaCategoria) {
                anexarLinhaCardapio(sb, p);
            }

            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Erro na tool buscarCardapioPorCategoria('{}'): {}", categoria, e.getMessage());
            return "Erro ao buscar produtos da categoria '" + categoria + "': " + e.getMessage();
        }
    }

    @Tool(description = "Consulta os dados e todos os itens de um pedido pelo seu ID. Retorna cliente vinculado, status, valor total acumulado e a lista detalhada de itens com ItemID, ProdID, quantidades e preços.")
    public String consultarPedido(
            @ToolParam(description = "ID do pedido a ser consultado") Integer pedidoId) {
        try {
            PedidoResponseDTO pedido = pedidoService.buscarPorId(pedidoId);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Pedido #%d | Cliente: %s | Status: %s | Total: %s\n",
                    pedido.id(),
                    formatarCliente(pedido.usuarioNome(), pedido.usuarioEmail()),
                    pedido.statusPedido(),
                    formatarMoeda(pedido.total())));

            if (pedido.itens() == null || pedido.itens().isEmpty()) {
                sb.append("Nenhum item adicionado a este pedido até o momento.");
            } else {
                sb.append("Itens no pedido:\n");
                pedido.itens().stream()
                        .sorted(Comparator.comparing(i -> i.id() != null ? i.id() : Integer.MAX_VALUE))
                        .forEach(item -> sb.append(String.format(
                                "- ItemID: %d | Produto: %s (ProdID: %s) | Qtd: %d | Preço Unit: %s | Subtotal: %s\n",
                                item.id(),
                                item.nomeDoProduto() != null ? item.nomeDoProduto() : "Desconhecido",
                                item.produtoId(),
                                item.quantidade() != null ? item.quantidade() : 0,
                                formatarMoeda(item.preco()),
                                formatarMoeda(item.subTotal()))));
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Erro na tool consultarPedido({}): {}", pedidoId, e.getMessage());
            return "Erro ao consultar pedido: " + e.getMessage();
        }
    }

    // ==========================================
    // 2. Alterações de Pedidos (Exigem Confirmação do Usuário)
    // ==========================================

    @Tool(description = "Prepara a criação de um novo pedido com um ou mais itens para confirmação do usuário. Permite informar opcionalmente o nome ou e-mail de outro usuário/cliente dono do pedido (se omitido, vincula ao usuário logado). Valida estoque antes e exibe um card de confirmação (não grava no banco até o usuário confirmar).")
    public String criarPedido(
            @ToolParam(description = "Lista não vazia de itens (cada item com produtoId e quantidade)") List<ItemPedidoInput> itens,
            @ToolParam(required = false, description = "Nome ou e-mail do usuário/cliente dono do pedido (opcional; se omitido, vincula ao usuário logado)") String cliente) {
        try {
            if (itens == null || itens.isEmpty()) {
                return "Erro ao criar pedido: informe pelo menos um item com produtoId e quantidade.";
            }

            UsuarioAutenticado solicitante = obterUsuarioLogado();
            if (solicitante == null) {
                return "Erro ao criar pedido: usuário autenticado não identificado.";
            }

            Integer usuarioIdAlvo = null;
            String rotuloCliente;
            if (cliente != null && !cliente.isBlank()) {
                String termoBusca = normalizar(cliente);
                List<ClientePedidoResponseDTO> ativos = pedidoService.listarClientesParaPedido();

                List<ClientePedidoResponseDTO> exatos = ativos.stream()
                        .filter(u -> normalizar(u.nome()).equals(termoBusca)
                                || normalizar(u.email()).equals(termoBusca))
                        .toList();

                List<ClientePedidoResponseDTO> encontrados = !exatos.isEmpty()
                        ? exatos
                        : ativos.stream()
                                .filter(u -> normalizar(u.nome()).contains(termoBusca)
                                        || normalizar(u.email()).contains(termoBusca))
                                .toList();

                if (encontrados.isEmpty()) {
                    return String.format("Nenhum usuário ativo encontrado para '%s'. Informe outro nome/e-mail ou crie sem especificar cliente para vincular à sua própria conta.", cliente.trim());
                }
                if (encontrados.size() > 1) {
                    StringBuilder opcoes = new StringBuilder();
                    opcoes.append(String.format("Mais de um usuário corresponde a '%s'. Informe o nome completo ou e-mail exato:\n", cliente.trim()));
                    for (ClientePedidoResponseDTO u : encontrados) {
                        opcoes.append(String.format("- %s (%s)\n",
                                u.nome() != null && !u.nome().isBlank() ? u.nome() : u.email(),
                                u.email()));
                    }
                    return opcoes.toString().trim();
                }

                ClientePedidoResponseDTO alvo = encontrados.get(0);
                usuarioIdAlvo = alvo.id();
                rotuloCliente = formatarCliente(alvo.nome(), alvo.email());
            } else {
                rotuloCliente = formatarCliente(solicitante.nome(), solicitante.email()) + " (você)";
            }

            Map<Integer, Integer> quantidadesPorProduto = new LinkedHashMap<>();
            for (ItemPedidoInput item : itens) {
                if (item == null || item.produtoId() == null) {
                    return "Erro ao criar pedido: o ID do produto é obrigatório.";
                }
                if (item.quantidade() == null || item.quantidade() <= 0) {
                    return "Erro ao criar pedido: a quantidade de cada item deve ser maior que zero.";
                }
                quantidadesPorProduto.merge(item.produtoId(), item.quantidade(), Integer::sum);
            }

            List<String> detalhes = new ArrayList<>();
            detalhes.add("Cliente: " + rotuloCliente);
            BigDecimal totalEstimado = BigDecimal.ZERO;
            List<PedidoRequestDTO.ItemPedidoRequestDTO> itensRequest = new ArrayList<>();

            for (Map.Entry<Integer, Integer> entry : quantidadesPorProduto.entrySet()) {
                Integer prodId = entry.getKey();
                Integer qtd = entry.getValue();

                ProdutoResponseDTO produto = produtoService.buscarPorId(prodId);
                EstoqueResponseDTO estoque = estoqueService.buscarPorProdutoId(prodId);
                int saldo = estoque.quantidade() != null ? estoque.quantidade() : 0;

                if (saldo < qtd) {
                    return String.format("Estoque insuficiente para '%s'. Saldo disponível: %d, solicitado: %d.",
                            produto.nome(), saldo, qtd);
                }

                BigDecimal preco = produto.preco() != null ? produto.preco() : BigDecimal.ZERO;
                BigDecimal subtotal = preco.multiply(BigDecimal.valueOf(qtd));
                totalEstimado = totalEstimado.add(subtotal);

                detalhes.add(String.format("%dx %s — %s", qtd, produto.nome(), formatarMoeda(subtotal)));
                itensRequest.add(new PedidoRequestDTO.ItemPedidoRequestDTO(prodId, qtd));
            }

            detalhes.add("Total estimado: " + formatarMoeda(totalEstimado));
            String resumo = String.format("%s • %d item(ns) • Total %s", rotuloCliente, itensRequest.size(), formatarMoeda(totalEstimado));
            final Integer usuarioIdFinal = usuarioIdAlvo;

            return acaoPendenteService.proporAcao(
                    "Criar novo pedido",
                    resumo,
                    detalhes,
                    false,
                    () -> {
                        UsuarioAutenticado executor = obterUsuarioLogado();
                        PedidoResponseDTO pedidoCriado = pedidoService.criarPedido(
                                new PedidoRequestDTO(itensRequest, usuarioIdFinal),
                                executor != null ? executor : solicitante
                        );
                        return String.format("Pedido #%d criado com sucesso para %s! Status: %s | Total: %s.",
                                pedidoCriado.id(),
                                formatarCliente(pedidoCriado.usuarioNome(), pedidoCriado.usuarioEmail()),
                                pedidoCriado.statusPedido(),
                                formatarMoeda(pedidoCriado.total()));
                    }
            );
        } catch (Exception e) {
            log.warn("Erro na tool criarPedido: {}", e.getMessage());
            return "Erro ao preparar criação de pedido: " + e.getMessage();
        }
    }

    @Tool(description = "Prepara a adição de um produto a um pedido existente em aberto para confirmação do usuário. Valida estoque antes e não grava no banco até o usuário confirmar.")
    public String adicionarItemAoPedido(
            @ToolParam(description = "ID do pedido onde o item será adicionado") Integer pedidoId,
            @ToolParam(description = "ID do produto a ser adicionado") Integer produtoId,
            @ToolParam(description = "Quantidade de itens") Integer quantidade) {
        try {
            if (quantidade == null || quantidade <= 0) {
                return "Erro ao adicionar item: a quantidade deve ser maior que zero.";
            }

            PedidoResponseDTO pedido = pedidoService.buscarPorId(pedidoId);
            if (pedido.statusPedido() != StatusTransacaoEnum.AGUARDANDO_PAGAMENTO) {
                return "Não é possível alterar itens de um pedido com status: " + pedido.statusPedido();
            }

            ProdutoResponseDTO produto = produtoService.buscarPorId(produtoId);
            EstoqueResponseDTO estoque = estoqueService.buscarPorProdutoId(produtoId);
            int saldo = estoque.quantidade() != null ? estoque.quantidade() : 0;

            int qtdJaNoPedido = pedido.itens() != null
                    ? pedido.itens().stream()
                            .filter(i -> produtoId.equals(i.produtoId()))
                            .mapToInt(i -> i.quantidade() != null ? i.quantidade() : 0)
                            .sum()
                    : 0;

            if (saldo < qtdJaNoPedido + quantidade) {
                return String.format("Estoque insuficiente para '%s'. Saldo disponível: %d, total necessário: %d.",
                        produto.nome(), saldo, qtdJaNoPedido + quantidade);
            }

            BigDecimal preco = produto.preco() != null ? produto.preco() : BigDecimal.ZERO;
            BigDecimal subtotalAdicional = preco.multiply(BigDecimal.valueOf(quantidade));

            List<String> detalhes = List.of(
                    "Pedido: #" + pedidoId,
                    String.format("Produto: %s (ID %d)", produto.nome(), produto.id()),
                    String.format("Quantidade a adicionar: +%d un", quantidade),
                    String.format("Valor adicional: %s (unit. %s)", formatarMoeda(subtotalAdicional), formatarMoeda(preco))
            );

            return acaoPendenteService.proporAcao(
                    "Adicionar item ao pedido #" + pedidoId,
                    String.format("+%dx %s", quantidade, produto.nome()),
                    detalhes,
                    false,
                    () -> {
                        PedidoResponseDTO.ItemPedidoResponseDTO itemAdicionado =
                                pedidoService.adicionarItemAoPedido(pedidoId, produtoId, quantidade);
                        return String.format("Sucesso! %d unidade(s) de '%s' adicionada(s) ao pedido #%d (unitário: %s).",
                                quantidade, itemAdicionado.nomeDoProduto(), pedidoId, formatarMoeda(itemAdicionado.preco()));
                    }
            );
        } catch (Exception e) {
            log.warn("Erro na tool adicionarItemAoPedido(pedido={}, prod={}): {}", pedidoId, produtoId, e.getMessage());
            return "Erro ao preparar adição de item: " + e.getMessage();
        }
    }

    @Tool(description = "Prepara a alteração de quantidade de um item em um pedido aberto para confirmação do usuário. O identificadorItem pode ser o ItemID ou o ProdID.")
    public String alterarQuantidadeItem(
            @ToolParam(description = "ID do pedido") Integer pedidoId,
            @ToolParam(description = "ID do item no pedido (ItemID) ou ID do produto (ProdID)") Integer identificadorItem,
            @ToolParam(description = "Nova quantidade desejada (maior que zero)") Integer novaQuantidade) {
        try {
            if (novaQuantidade == null || novaQuantidade <= 0) {
                return "Erro ao alterar quantidade: a nova quantidade deve ser maior que zero.";
            }

            PedidoResponseDTO pedido = pedidoService.buscarPorId(pedidoId);
            if (pedido.statusPedido() != StatusTransacaoEnum.AGUARDANDO_PAGAMENTO) {
                return "Não é permitido alterar itens de um pedido com status: " + pedido.statusPedido();
            }

            Optional<PedidoResponseDTO.ItemPedidoResponseDTO> itemOpt = localizarItem(pedido, identificadorItem);
            if (itemOpt.isEmpty()) {
                return "Nenhum item encontrado no pedido #" + pedidoId + " para o identificador: " + identificadorItem + ".";
            }

            PedidoResponseDTO.ItemPedidoResponseDTO item = itemOpt.get();
            EstoqueResponseDTO estoque = estoqueService.buscarPorProdutoId(item.produtoId());
            int saldo = estoque.quantidade() != null ? estoque.quantidade() : 0;
            if (saldo < novaQuantidade) {
                return String.format("Estoque insuficiente para '%s'. Saldo disponível: %d, solicitado: %d.",
                        item.nomeDoProduto(), saldo, novaQuantidade);
            }

            Integer itemIdReal = item.id();
            List<String> detalhes = List.of(
                    "Pedido: #" + pedidoId,
                    "Produto: " + item.nomeDoProduto(),
                    String.format("Quantidade: %d un → %d un", item.quantidade(), novaQuantidade)
            );

            return acaoPendenteService.proporAcao(
                    "Alterar quantidade no pedido #" + pedidoId,
                    String.format("%s: %d → %d un", item.nomeDoProduto(), item.quantidade(), novaQuantidade),
                    detalhes,
                    false,
                    () -> {
                        PedidoResponseDTO.ItemPedidoResponseDTO atualizado =
                                pedidoService.alterarQuantidadeItem(pedidoId, itemIdReal, novaQuantidade);
                        return String.format("Sucesso! Quantidade de '%s' no pedido #%d atualizada para %d un.",
                                atualizado.nomeDoProduto(), pedidoId, atualizado.quantidade());
                    }
            );
        } catch (Exception e) {
            log.warn("Erro na tool alterarQuantidadeItem(pedido={}, item={}): {}", pedidoId, identificadorItem, e.getMessage());
            return "Erro ao preparar alteração de quantidade: " + e.getMessage();
        }
    }

    @Tool(description = "Prepara a remoção de um item de um pedido aberto para confirmação do usuário. O identificadorItem pode ser o ItemID ou o ProdID.")
    public String removerItemDoPedido(
            @ToolParam(description = "ID do pedido") Integer pedidoId,
            @ToolParam(description = "ID do item no pedido (ItemID) ou ID do produto (ProdID) a remover") Integer identificadorItem) {
        try {
            PedidoResponseDTO pedido = pedidoService.buscarPorId(pedidoId);
            if (pedido.statusPedido() != StatusTransacaoEnum.AGUARDANDO_PAGAMENTO) {
                return "Não é permitido remover itens de um pedido com status: " + pedido.statusPedido();
            }

            Optional<PedidoResponseDTO.ItemPedidoResponseDTO> itemOpt = localizarItem(pedido, identificadorItem);
            if (itemOpt.isEmpty()) {
                return "Nenhum item correspondente encontrado no pedido #" + pedidoId + " para o identificador: " + identificadorItem + ".";
            }

            PedidoResponseDTO.ItemPedidoResponseDTO item = itemOpt.get();
            Integer itemIdReal = item.id();
            String nomeProduto = item.nomeDoProduto() != null ? item.nomeDoProduto() : "Item #" + itemIdReal;

            List<String> detalhes = List.of(
                    "Pedido: #" + pedidoId,
                    String.format("Item a remover: %dx %s (%s)",
                            item.quantidade(), nomeProduto, formatarMoeda(item.subTotal()))
            );

            return acaoPendenteService.proporAcao(
                    "Remover item do pedido #" + pedidoId,
                    String.format("Remover %dx %s", item.quantidade(), nomeProduto),
                    detalhes,
                    true,
                    () -> {
                        pedidoService.removerItem(pedidoId, itemIdReal);
                        return String.format("Sucesso! O item '%s' foi removido do pedido #%d.", nomeProduto, pedidoId);
                    }
            );
        } catch (Exception e) {
            log.warn("Erro na tool removerItemDoPedido(pedido={}, item={}): {}", pedidoId, identificadorItem, e.getMessage());
            return "Erro ao preparar remoção de item: " + e.getMessage();
        }
    }

    @Tool(description = "Prepara o cancelamento de um pedido em aberto (status AGUARDANDO_PAGAMENTO) para confirmação do usuário. Não cancela no banco até o usuário confirmar.")
    public String cancelarPedido(
            @ToolParam(description = "ID do pedido a ser cancelado") Integer pedidoId) {
        try {
            PedidoResponseDTO pedido = pedidoService.buscarPorId(pedidoId);
            if (pedido.statusPedido() != StatusTransacaoEnum.AGUARDANDO_PAGAMENTO) {
                return "Não é permitido cancelar um pedido com status: " + pedido.statusPedido();
            }

            int qtdItens = pedido.itens() != null ? pedido.itens().size() : 0;
            List<String> detalhes = List.of(
                    "Pedido: #" + pedido.id(),
                    "Status atual: " + pedido.statusPedido() + " → CANCELADO",
                    String.format("Itens distintos: %d | Total: %s", qtdItens, formatarMoeda(pedido.total()))
            );

            return acaoPendenteService.proporAcao(
                    "Cancelar pedido #" + pedido.id(),
                    String.format("Cancelar pedido #%d (%s)", pedido.id(), formatarMoeda(pedido.total())),
                    detalhes,
                    true,
                    () -> {
                        PedidoResponseDTO cancelado = pedidoService.cancelarPedido(pedidoId);
                        return String.format("Pedido #%d cancelado com sucesso! Status atual: %s.",
                                cancelado.id(), cancelado.statusPedido());
                    }
            );
        } catch (Exception e) {
            log.warn("Erro na tool cancelarPedido({}): {}", pedidoId, e.getMessage());
            return "Erro ao preparar cancelamento de pedido: " + e.getMessage();
        }
    }

    // ==========================================
    // Helpers internos
    // ==========================================

    private Map<Integer, Integer> carregarMapaEstoque() {
        return estoqueService.listarTodos().stream()
                .filter(e -> e.produtoId() != null)
                .collect(Collectors.toMap(
                        EstoqueResponseDTO::produtoId,
                        e -> e.quantidade() != null ? e.quantidade() : 0,
                        (q1, q2) -> q1
                ));
    }

    private Optional<PedidoResponseDTO.ItemPedidoResponseDTO> localizarItem(PedidoResponseDTO pedido, Integer identificadorItem) {
        if (identificadorItem == null || pedido.itens() == null || pedido.itens().isEmpty()) {
            return Optional.empty();
        }
        return pedido.itens().stream()
                .filter(i -> identificadorItem.equals(i.id()))
                .findFirst()
                .or(() -> pedido.itens().stream()
                        .filter(i -> identificadorItem.equals(i.produtoId()))
                        .findFirst());
    }

    private void anexarLinhaCardapio(StringBuilder sb, ProdutoResponseDTO p) {
        sb.append(String.format("• ID: %d | %s — %s\n",
                p.id(), p.nome(), formatarMoeda(p.preco())));
        if (p.descricao() != null && !p.descricao().isBlank()) {
            sb.append(String.format("  Detalhes: %s\n", p.descricao().trim()));
        }
    }

    private UsuarioAutenticado obterUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuario;
        }
        return null;
    }

    private String formatarCliente(String nome, String email) {
        boolean temNome = nome != null && !nome.isBlank();
        boolean temEmail = email != null && !email.isBlank();
        if (temNome && temEmail) {
            return nome.trim() + " (" + email.trim() + ")";
        }
        if (temNome) {
            return nome.trim();
        }
        if (temEmail) {
            return email.trim();
        }
        return "Não vinculado";
    }

    private String formatarMoeda(BigDecimal valor) {
        BigDecimal seguro = valor != null ? valor : BigDecimal.ZERO;
        return String.format(LOCALE_BR, "R$ %.2f", seguro);
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
