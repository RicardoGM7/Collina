package br.ufs.dcomp.collina.chat.tools;

import br.ufs.dcomp.collina.chat.service.ChatAcaoPendenteService;
import br.ufs.dcomp.collina.dto.estoque.CategoriaRequestDTO;
import br.ufs.dcomp.collina.dto.estoque.CategoriaResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.EstoqueResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.MovimentacaoEstoqueRequestDTO;
import br.ufs.dcomp.collina.dto.estoque.MovimentacaoEstoqueResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.PedidoResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.ProdutoRequestDTO;
import br.ufs.dcomp.collina.dto.estoque.ProdutoResponseDTO;
import br.ufs.dcomp.collina.model.estoque.Categoria;
import br.ufs.dcomp.collina.model.estoque.MovimentacaoEstoqueEnum;
import br.ufs.dcomp.collina.model.estoque.StatusTransacaoEnum;
import br.ufs.dcomp.collina.service.estoque.CategoriaService;
import br.ufs.dcomp.collina.service.estoque.EstoqueService;
import br.ufs.dcomp.collina.service.estoque.PedidoService;
import br.ufs.dcomp.collina.service.estoque.ProdutoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Ferramentas administrativas de estoque, produtos, categorias e pedidos disponíveis exclusivamente para ADMIN.
 * - Consultas (leitura): executadas imediatamente.
 * - Alterações no banco (escrita): pré-validadas e registradas como ação pendente
 *   via ChatAcaoPendenteService (padrão Human-in-the-Loop).
 */
@Component
@PreAuthorize("hasRole('ADMIN')")
public class AdminEstoqueChatTools {

    private static final Logger log = LoggerFactory.getLogger(AdminEstoqueChatTools.class);
    private static final Locale LOCALE_BR = Locale.of("pt", "BR");
    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;
    private final EstoqueService estoqueService;
    private final PedidoService pedidoService;
    private final ChatAcaoPendenteService acaoPendenteService;

    public AdminEstoqueChatTools(
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

    // ==========================================
    // 1. Relatórios e Consultas de Estoque (Somente Leitura)
    // ==========================================

    @Tool(description = "USE ESTA FERRAMENTA APENAS quando o administrador solicitar o relatório ou catálogo COMPLETO/GERAL de todos os produtos cadastrados (incluindo esgotados e saldo exato em estoque). NUNCA use para pedidos de cardápio comum.")
    public String buscarTodosProdutos() {
        try {
            List<ProdutoResponseDTO> produtos = produtoService.listarTodos();
            if (produtos.isEmpty()) {
                return "Nenhum produto cadastrado no sistema.";
            }

            Map<Integer, Integer> estoquePorProduto = carregarMapaEstoque();

            StringBuilder sb = new StringBuilder("=== TODOS OS PRODUTOS CADASTRADOS (RELATÓRIO GERAL) ===\n");
            for (ProdutoResponseDTO p : produtos) {
                int qtdEstoque = estoquePorProduto.getOrDefault(p.id(), 0);
                String statusEstoque = qtdEstoque > 0
                        ? String.format("Disponível (%d un)", qtdEstoque)
                        : "Esgotado (0 un)";
                String categorias = p.categorias() != null && !p.categorias().isEmpty()
                        ? p.categorias().stream().map(CategoriaResponseDTO::nome).collect(Collectors.joining(", "))
                        : "Sem categoria";

                sb.append(String.format("ID: %d | Nome: %s | Preço: %s | Categorias: [%s] | Status: %s\n",
                        p.id(), p.nome(), formatarMoeda(p.preco()), categorias, statusEstoque));
                if (p.descricao() != null && !p.descricao().isBlank()) {
                    sb.append(String.format("   Descrição: %s\n", p.descricao().trim()));
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Erro na tool buscarTodosProdutos: {}", e.getMessage());
            return "Erro ao buscar todos os produtos: " + e.getMessage();
        }
    }

    @Tool(description = "Calcula com exatidão o valor financeiro total de todos os produtos atualmente em estoque (soma de quantidade × preço unitário), total de unidades, quantidade de produtos ativos/esgotados e detalhamento por produto.")
    public String consultarResumoValorEstoque() {
        try {
            List<ProdutoResponseDTO> produtos = produtoService.listarTodos();
            if (produtos.isEmpty()) {
                return "Nenhum produto cadastrado no sistema.";
            }

            Map<Integer, Integer> estoquePorProduto = carregarMapaEstoque();

            record LinhaValorEstoque(ProdutoResponseDTO produto, int quantidade, BigDecimal valorTotal) {
            }

            List<LinhaValorEstoque> linhas = new ArrayList<>();
            BigDecimal valorGeral = BigDecimal.ZERO;
            int totalUnidades = 0;
            int produtosComEstoque = 0;
            int produtosEsgotados = 0;

            for (ProdutoResponseDTO p : produtos) {
                int qtd = estoquePorProduto.getOrDefault(p.id(), 0);
                BigDecimal preco = p.preco() != null ? p.preco() : BigDecimal.ZERO;
                BigDecimal valorItem = preco.multiply(BigDecimal.valueOf(qtd));

                valorGeral = valorGeral.add(valorItem);
                totalUnidades += qtd;
                if (qtd > 0) {
                    produtosComEstoque++;
                } else {
                    produtosEsgotados++;
                }
                linhas.add(new LinhaValorEstoque(p, qtd, valorItem));
            }

            linhas.sort(Comparator.comparing(LinhaValorEstoque::valorTotal).reversed());

            StringBuilder sb = new StringBuilder();
            sb.append("💰 === RESUMO FINANCEIRO DO ESTOQUE ===\n");
            sb.append(String.format("Valor total em estoque: %s\n", formatarMoeda(valorGeral)));
            sb.append(String.format("Total de unidades: %d un\n", totalUnidades));
            sb.append(String.format("Produtos cadastrados: %d (%d com estoque | %d esgotados)\n\n",
                    produtos.size(), produtosComEstoque, produtosEsgotados));
            sb.append("Detalhamento por produto (maior valor primeiro):\n");

            for (LinhaValorEstoque l : linhas) {
                sb.append(String.format("- %s (ID: %d) | %d un × %s = %s\n",
                        l.produto().nome(),
                        l.produto().id(),
                        l.quantidade(),
                        formatarMoeda(l.produto().preco()),
                        formatarMoeda(l.valorTotal())));
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Erro na tool consultarResumoValorEstoque: {}", e.getMessage());
            return "Erro ao calcular o resumo de valor do estoque: " + e.getMessage();
        }
    }

    @Tool(description = "Consulta a quantidade disponível em estoque e o estoque mínimo configurado de um produto específico pelo ID.")
    public String consultarEstoqueProduto(
            @ToolParam(description = "ID do produto") Integer produtoId) {
        try {
            EstoqueResponseDTO estoque = estoqueService.buscarPorProdutoId(produtoId);
            return String.format("Produto: %s (ID: %d) | Saldo disponível em estoque: %d unidades | Estoque mínimo: %d.",
                    estoque.nomeProduto(), estoque.produtoId(), estoque.quantidade(), estoque.estoqueMinimo());
        } catch (Exception e) {
            log.warn("Erro na tool consultarEstoqueProduto({}): {}", produtoId, e.getMessage());
            return "Erro ao consultar estoque: " + e.getMessage();
        }
    }

    @Tool(description = "Lista todos os produtos que estão com estoque baixo ou zerado (saldo atual menor ou igual ao estoque mínimo configurado). Use quando o administrador perguntar quais itens estão acabando ou precisam de reposição.")
    public String consultarItensComEstoqueBaixo() {
        try {
            List<EstoqueResponseDTO> itensBaixos = estoqueService.listarItensComEstoqueBaixo();
            if (itensBaixos.isEmpty()) {
                return "Nenhum item está com estoque baixo no momento. Todos os produtos estão acima do estoque mínimo.";
            }

            StringBuilder sb = new StringBuilder("⚠️ === ITENS COM ESTOQUE BAIXO ===\n");
            for (EstoqueResponseDTO e : itensBaixos) {
                sb.append(String.format("- %s (ProdID: %d) | Saldo atual: %d un | Estoque mínimo: %d un\n",
                        e.nomeProduto(), e.produtoId(), e.quantidade(), e.estoqueMinimo()));
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Erro na tool consultarItensComEstoqueBaixo: {}", e.getMessage());
            return "Erro ao consultar itens com estoque baixo: " + e.getMessage();
        }
    }

    @Tool(description = "Consulta o histórico de movimentações de estoque (entradas por compra, vendas, uso interno, perdas) de um produto específico, da mais recente para a mais antiga.")
    public String consultarHistoricoMovimentacoes(
            @ToolParam(description = "ID do produto") Integer produtoId,
            @ToolParam(required = false, description = "Quantidade máxima de registros a retornar (opcional, padrão 15)") Integer limite) {
        try {
            List<MovimentacaoEstoqueResponseDTO> historico = estoqueService.listarMovimentacoesPorProduto(produtoId);
            if (historico.isEmpty()) {
                return "Nenhuma movimentação de estoque encontrada para o produto ID " + produtoId + ".";
            }

            int max = (limite != null && limite > 0) ? Math.min(limite, 50) : 15;
            List<MovimentacaoEstoqueResponseDTO> recortado = historico.stream().limit(max).toList();

            String nomeProduto = recortado.getFirst().nomeProduto() != null
                    ? recortado.getFirst().nomeProduto()
                    : "Produto #" + produtoId;

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("📜 === HISTÓRICO DE MOVIMENTAÇÕES: %s (exibindo %d de %d) ===\n",
                    nomeProduto, recortado.size(), historico.size()));

            for (MovimentacaoEstoqueResponseDTO m : recortado) {
                String dataFmt = formatarDataHora(m.data());
                String obs = (m.observacao() != null && !m.observacao().isBlank()) ? " | Obs: " + m.observacao() : "";
                sb.append(String.format("- #%d | %s | %s | Qtd: %d un%s\n",
                        m.id(), dataFmt, m.tipo(), m.quantidade(), obs));
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Erro na tool consultarHistoricoMovimentacoes({}): {}", produtoId, e.getMessage());
            return "Erro ao consultar histórico de movimentações: " + e.getMessage();
        }
    }

    @Tool(description = "Lista pedidos do sistema com filtros opcionais por status (AGUARDANDO_PAGAMENTO, PAGO, ENTREGUE, CANCELADO, CONFIRMADO, EXPIRADO), período (dataInicio e dataFim no formato YYYY-MM-DD) e nome/e-mail do cliente. Use para consultar pedidos em aberto, pedidos de hoje, pedidos de um cliente específico, entregues ou cancelados.")
    public String listarPedidos(
            @ToolParam(required = false, description = "Status do pedido (opcional): AGUARDANDO_PAGAMENTO, PAGO, ENTREGUE, CANCELADO, CONFIRMADO ou EXPIRADO") String status,
            @ToolParam(required = false, description = "Data inicial no formato YYYY-MM-DD (opcional)") String dataInicio,
            @ToolParam(required = false, description = "Data final no formato YYYY-MM-DD (opcional)") String dataFim,
            @ToolParam(required = false, description = "Nome ou e-mail do usuário/cliente para filtrar pedidos (opcional)") String cliente) {
        try {
            StatusTransacaoEnum statusEnum = null;
            if (status != null && !status.isBlank()) {
                try {
                    statusEnum = StatusTransacaoEnum.valueOf(status.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    return "Status inválido: '" + status + "'. Use AGUARDANDO_PAGAMENTO, PAGO, ENTREGUE, CANCELADO, CONFIRMADO ou EXPIRADO.";
                }
            }

            LocalDate inicio = parseDataOpcional(dataInicio, "dataInicio");
            LocalDate fim = parseDataOpcional(dataFim, "dataFim");

            List<PedidoResponseDTO> pedidos = pedidoService.listarComFiltro(statusEnum, inicio, fim);
            if (cliente != null && !cliente.isBlank()) {
                String termo = normalizarTexto(cliente);
                pedidos = pedidos.stream()
                        .filter(p -> normalizarTexto(p.usuarioNome()).contains(termo)
                                || normalizarTexto(p.usuarioEmail()).contains(termo))
                        .toList();
            }
            if (pedidos.isEmpty()) {
                return "Nenhum pedido encontrado para os filtros informados.";
            }

            BigDecimal somaTotal = pedidos.stream()
                    .map(p -> p.total() != null ? p.total() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("🧾 === PEDIDOS ENCONTRADOS (%d pedido(s) | Soma total: %s) ===\n",
                    pedidos.size(), formatarMoeda(somaTotal)));

            for (PedidoResponseDTO p : pedidos) {
                int qtdItens = p.itens() != null ? p.itens().size() : 0;
                String dono = (p.usuarioNome() != null && !p.usuarioNome().isBlank())
                        ? p.usuarioNome()
                        : (p.usuarioEmail() != null && !p.usuarioEmail().isBlank() ? p.usuarioEmail() : "Não vinculado");
                sb.append(String.format("- Pedido #%d | Cliente: %s | %s | Status: %s | Itens distintos: %d | Total: %s\n",
                        p.id(), dono, formatarDataHora(p.data()), p.statusPedido(), qtdItens, formatarMoeda(p.total())));
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Erro na tool listarPedidos(status={}, inicio={}, fim={}, cliente={}): {}", status, dataInicio, dataFim, cliente, e.getMessage());
            return "Erro ao listar pedidos: " + e.getMessage();
        }
    }

    // ==========================================
    // 2. CRUD de Produtos (Exigem Confirmação do Administrador)
    // ==========================================

    @Tool(description = "Prepara o cadastro de um novo produto no catálogo e estoque para confirmação do administrador. Sempre consulte listarCategorias antes para obter categoriaIds válidos. Não grava no banco até o administrador confirmar.")
    public String criarProduto(
            @ToolParam(description = "Nome do produto") String nome,
            @ToolParam(description = "Preço unitário do produto em reais (ex: 6.50)") BigDecimal preco,
            @ToolParam(description = "Lista não vazia de IDs das categorias do produto") List<Integer> categoriaIds,
            @ToolParam(required = false, description = "Descrição opcional do produto") String descricao,
            @ToolParam(required = false, description = "Estoque mínimo de alerta (opcional, padrão 0)") Integer estoqueMinimo,
            @ToolParam(required = false, description = "Quantidade inicial em estoque (opcional, padrão 0)") Integer quantidadeInicial) {
        try {
            if (nome == null || nome.trim().isBlank()) {
                return "Erro: o nome do produto é obrigatório.";
            }
            if (preco == null || preco.compareTo(BigDecimal.ZERO) < 0) {
                return "Erro: o preço do produto é obrigatório e não pode ser negativo.";
            }
            if (preco.stripTrailingZeros().scale() > 2) {
                return "Erro: o preço do produto deve ter no máximo 2 casas decimais.";
            }
            if (categoriaIds == null || categoriaIds.isEmpty()) {
                return "Erro: o produto deve estar vinculado a pelo menos uma categoria. Use listarCategorias para ver os IDs disponíveis.";
            }

            int minFinal = (estoqueMinimo != null) ? estoqueMinimo : 0;
            int qtdFinal = (quantidadeInicial != null) ? quantidadeInicial : 0;
            if (minFinal < 0) {
                return "Erro: o estoque mínimo não pode ser negativo.";
            }
            if (qtdFinal < 0) {
                return "Erro: a quantidade inicial não pode ser negativa.";
            }

            Set<Integer> catIdsSet = new LinkedHashSet<>(categoriaIds);
            List<String> nomesCategorias = new ArrayList<>();
            for (Integer catId : catIdsSet) {
                CategoriaResponseDTO cat = categoriaService.buscarPorId(catId);
                nomesCategorias.add(cat.nome());
            }

            String nomeLimpo = nome.trim();
            String descLimpa = (descricao != null && !descricao.trim().isBlank()) ? descricao.trim() : null;
            BigDecimal precoNormalizado = preco.setScale(2, RoundingMode.UNNECESSARY);

            List<String> detalhes = new ArrayList<>();
            detalhes.add("Nome: " + nomeLimpo);
            detalhes.add("Preço: " + formatarMoeda(precoNormalizado));
            detalhes.add("Categorias: " + String.join(", ", nomesCategorias));
            if (descLimpa != null) {
                detalhes.add("Descrição: " + descLimpa);
            }
            detalhes.add(String.format("Estoque inicial: %d un | Estoque mínimo: %d un", qtdFinal, minFinal));

            ProdutoRequestDTO dto = new ProdutoRequestDTO(
                    nomeLimpo, descLimpa, precoNormalizado, catIdsSet, minFinal, qtdFinal);

            return acaoPendenteService.proporAcao(
                    "Cadastrar novo produto",
                    String.format("%s (%s)", nomeLimpo, formatarMoeda(precoNormalizado)),
                    detalhes,
                    false,
                    () -> {
                        ProdutoResponseDTO criado = produtoService.criarProduto(dto);
                        return String.format("Produto #%d '%s' cadastrado com sucesso! Preço: %s | Estoque inicial: %d un.",
                                criado.id(), criado.nome(), formatarMoeda(criado.preco()), qtdFinal);
                    }
            );
        } catch (Exception e) {
            log.warn("Erro na tool criarProduto('{}'): {}", nome, e.getMessage());
            return "Erro ao preparar cadastro de produto: " + e.getMessage();
        }
    }

    @Tool(description = "Prepara a atualização de dados de um produto existente (nome, preço, descrição, categorias e/ou estoque mínimo) para confirmação do administrador. Informe apenas os campos que deseja alterar. Não grava no banco até confirmar.")
    public String atualizarProduto(
            @ToolParam(description = "ID do produto a ser atualizado") Integer produtoId,
            @ToolParam(required = false, description = "Novo nome do produto (opcional)") String novoNome,
            @ToolParam(required = false, description = "Novo preço unitário em reais (opcional)") BigDecimal novoPreco,
            @ToolParam(required = false, description = "Nova descrição do produto (opcional)") String novaDescricao,
            @ToolParam(required = false, description = "Nova lista de IDs de categorias (opcional)") List<Integer> novaCategoriaIds,
            @ToolParam(required = false, description = "Novo estoque mínimo (opcional)") Integer novoEstoqueMinimo) {
        try {
            ProdutoResponseDTO atual = produtoService.buscarPorId(produtoId);

            if (novoNome == null && novoPreco == null && novaDescricao == null
                    && novaCategoriaIds == null && novoEstoqueMinimo == null) {
                return "Erro: informe pelo menos um campo para atualizar no produto #" + produtoId + ".";
            }

            List<String> detalhes = new ArrayList<>();
            detalhes.add(String.format("Produto: %s (ID %d)", atual.nome(), atual.id()));

            String nomeFinal = null;
            if (novoNome != null) {
                if (novoNome.trim().isBlank()) {
                    return "Erro: o novo nome do produto não pode ser vazio.";
                }
                nomeFinal = novoNome.trim();
                detalhes.add(String.format("Nome: %s → %s", atual.nome(), nomeFinal));
            }

            BigDecimal precoFinal = null;
            if (novoPreco != null) {
                if (novoPreco.compareTo(BigDecimal.ZERO) < 0) {
                    return "Erro: o preço do produto não pode ser negativo.";
                }
                if (novoPreco.stripTrailingZeros().scale() > 2) {
                    return "Erro: o preço do produto deve ter no máximo 2 casas decimais.";
                }
                precoFinal = novoPreco.setScale(2, RoundingMode.UNNECESSARY);
                detalhes.add(String.format("Preço: %s → %s", formatarMoeda(atual.preco()), formatarMoeda(precoFinal)));
            }

            String descFinal = null;
            if (novaDescricao != null) {
                descFinal = novaDescricao.trim();
                detalhes.add("Descrição atualizada: " + (descFinal.isEmpty() ? "(vazia)" : descFinal));
            }

            Set<Integer> catIdsFinal = null;
            if (novaCategoriaIds != null) {
                if (novaCategoriaIds.isEmpty()) {
                    return "Erro: o produto deve permanecer vinculado a pelo menos uma categoria.";
                }
                catIdsFinal = new LinkedHashSet<>(novaCategoriaIds);
                List<String> nomesNovos = new ArrayList<>();
                for (Integer catId : catIdsFinal) {
                    nomesNovos.add(categoriaService.buscarPorId(catId).nome());
                }
                detalhes.add("Categorias: " + String.join(", ", nomesNovos));
            }

            if (novoEstoqueMinimo != null) {
                if (novoEstoqueMinimo < 0) {
                    return "Erro: o estoque mínimo não pode ser negativo.";
                }
                detalhes.add("Novo estoque mínimo: " + novoEstoqueMinimo + " un");
            }

            ProdutoRequestDTO dto = new ProdutoRequestDTO(
                    nomeFinal, descFinal, precoFinal, catIdsFinal, novoEstoqueMinimo, null);

            return acaoPendenteService.proporAcao(
                    "Atualizar produto #" + atual.id(),
                    "Editar " + atual.nome(),
                    detalhes,
                    false,
                    () -> {
                        ProdutoResponseDTO atualizado = produtoService.atualizarProduto(produtoId, dto);
                        return String.format("Produto #%d '%s' atualizado com sucesso! Preço atual: %s.",
                                atualizado.id(), atualizado.nome(), formatarMoeda(atualizado.preco()));
                    }
            );
        } catch (Exception e) {
            log.warn("Erro na tool atualizarProduto({}): {}", produtoId, e.getMessage());
            return "Erro ao preparar atualização de produto: " + e.getMessage();
        }
    }

    @Tool(description = "Prepara a remoção definitiva de um produto pelo ID para confirmação do administrador. Produtos vinculados a pedidos ou com histórico de movimentação não podem ser removidos. Não remove até confirmar.")
    public String removerProduto(
            @ToolParam(description = "ID do produto a ser removido") Integer produtoId) {
        try {
            ProdutoResponseDTO atual = produtoService.buscarPorId(produtoId);
            EstoqueResponseDTO estoque = estoqueService.buscarPorProdutoId(produtoId);

            List<String> detalhes = List.of(
                    String.format("Produto: %s (ID %d)", atual.nome(), atual.id()),
                    String.format("Preço: %s | Saldo atual: %d un", formatarMoeda(atual.preco()), estoque.quantidade()),
                    "Ação irreversível (bloqueada automaticamente se houver pedidos ou movimentações vinculadas)"
            );

            return acaoPendenteService.proporAcao(
                    "Remover produto #" + atual.id(),
                    "Excluir " + atual.nome(),
                    detalhes,
                    true,
                    () -> {
                        produtoService.removerProduto(produtoId);
                        return String.format("Produto #%d '%s' removido com sucesso.", atual.id(), atual.nome());
                    }
            );
        } catch (Exception e) {
            log.warn("Erro na tool removerProduto({}): {}", produtoId, e.getMessage());
            return "Erro ao preparar remoção de produto: " + e.getMessage();
        }
    }

    // ==========================================
    // 3. CRUD de Categorias (Exigem Confirmação do Administrador)
    // ==========================================

    @Tool(description = "Prepara o cadastro de uma nova categoria de produtos para confirmação do administrador. Não grava no banco até confirmar.")
    public String criarCategoria(
            @ToolParam(description = "Nome da nova categoria (ex: 'Bebidas Geladas', 'Salgados')") String nome,
            @ToolParam(required = false, description = "Cor em hexadecimal #RRGGBB (opcional, padrão #2BB673)") String corHex) {
        try {
            if (nome == null || nome.trim().isBlank()) {
                return "Erro: o nome da categoria é obrigatório.";
            }
            String nomeLimpo = nome.trim();

            boolean duplicada = categoriaService.listarTodas().stream()
                    .anyMatch(c -> c.nome() != null && c.nome().equalsIgnoreCase(nomeLimpo));
            if (duplicada) {
                return "Já existe uma categoria cadastrada com o nome: " + nomeLimpo;
            }

            String corFinal = validarCorOpcional(corHex);
            String corExibicao = corFinal != null ? corFinal : Categoria.COR_PADRAO;

            List<String> detalhes = List.of(
                    "Nome: " + nomeLimpo,
                    "Cor: " + corExibicao
            );

            CategoriaRequestDTO dto = new CategoriaRequestDTO(nomeLimpo, corFinal);

            return acaoPendenteService.proporAcao(
                    "Cadastrar nova categoria",
                    "Criar categoria '" + nomeLimpo + "'",
                    detalhes,
                    false,
                    () -> {
                        CategoriaResponseDTO criada = categoriaService.adicionarCategoria(dto);
                        return String.format("Categoria #%d '%s' (cor %s) cadastrada com sucesso!",
                                criada.id(), criada.nome(), criada.cor());
                    }
            );
        } catch (Exception e) {
            log.warn("Erro na tool criarCategoria('{}'): {}", nome, e.getMessage());
            return "Erro ao preparar cadastro de categoria: " + e.getMessage();
        }
    }

    @Tool(description = "Prepara a edição do nome e/ou cor de uma categoria existente para confirmação do administrador. Não grava no banco até confirmar.")
    public String editarCategoria(
            @ToolParam(description = "ID da categoria a ser editada") Integer categoriaId,
            @ToolParam(description = "Novo nome da categoria") String novoNome,
            @ToolParam(required = false, description = "Nova cor em hexadecimal #RRGGBB (opcional)") String novaCorHex) {
        try {
            CategoriaResponseDTO atual = categoriaService.buscarPorId(categoriaId);
            if (novoNome == null || novoNome.trim().isBlank()) {
                return "Erro: o novo nome da categoria é obrigatório.";
            }
            String nomeLimpo = novoNome.trim();

            boolean duplicada = categoriaService.listarTodas().stream()
                    .anyMatch(c -> !categoriaId.equals(c.id())
                            && c.nome() != null
                            && c.nome().equalsIgnoreCase(nomeLimpo));
            if (duplicada) {
                return "Já existe outra categoria cadastrada com o nome: " + nomeLimpo;
            }

            String corFinal = validarCorOpcional(novaCorHex);

            List<String> detalhes = new ArrayList<>();
            detalhes.add(String.format("Categoria ID: #%d", atual.id()));
            detalhes.add(String.format("Nome: %s → %s", atual.nome(), nomeLimpo));
            if (corFinal != null) {
                detalhes.add(String.format("Cor: %s → %s", atual.cor(), corFinal));
            }

            CategoriaRequestDTO dto = new CategoriaRequestDTO(nomeLimpo, corFinal);

            return acaoPendenteService.proporAcao(
                    "Editar categoria #" + atual.id(),
                    String.format("%s → %s", atual.nome(), nomeLimpo),
                    detalhes,
                    false,
                    () -> {
                        CategoriaResponseDTO editada = categoriaService.editarCategoria(categoriaId, dto);
                        return String.format("Categoria #%d atualizada para '%s' (cor %s) com sucesso!",
                                editada.id(), editada.nome(), editada.cor());
                    }
            );
        } catch (Exception e) {
            log.warn("Erro na tool editarCategoria({}): {}", categoriaId, e.getMessage());
            return "Erro ao preparar edição de categoria: " + e.getMessage();
        }
    }

    @Tool(description = "Prepara a remoção de uma categoria pelo ID para confirmação do administrador. Só permite remover categorias sem produtos vinculados. Não remove até confirmar.")
    public String removerCategoria(
            @ToolParam(description = "ID da categoria a ser removida") Integer categoriaId) {
        try {
            CategoriaResponseDTO atual = categoriaService.buscarPorId(categoriaId);

            long vinculados = produtoService.listarTodos().stream()
                    .filter(p -> p.categorias() != null
                            && p.categorias().stream().anyMatch(c -> categoriaId.equals(c.id())))
                    .count();
            if (vinculados > 0) {
                return String.format("Não é possível remover a categoria '%s' pois ela possui %d produto(s) vinculado(s).",
                        atual.nome(), vinculados);
            }

            List<String> detalhes = List.of(
                    String.format("Categoria: %s (ID %d)", atual.nome(), atual.id()),
                    "Nenhum produto vinculado"
            );

            return acaoPendenteService.proporAcao(
                    "Remover categoria #" + atual.id(),
                    "Excluir categoria '" + atual.nome() + "'",
                    detalhes,
                    true,
                    () -> {
                        categoriaService.removerCategoria(categoriaId);
                        return String.format("Categoria #%d '%s' removida com sucesso.", atual.id(), atual.nome());
                    }
            );
        } catch (Exception e) {
            log.warn("Erro na tool removerCategoria({}): {}", categoriaId, e.getMessage());
            return "Erro ao preparar remoção de categoria: " + e.getMessage();
        }
    }

    // ==========================================
    // 4. Movimentações de Estoque, Mínimo e Entrega (Exigem Confirmação)
    // ==========================================

    @Tool(description = "Prepara uma movimentação manual de estoque para confirmação do administrador. Tipos aceitos: 'COMPRA_FORNECEDOR' (entrada/reposição), 'USO_INTERNO' (saída para uso do clube) ou 'PERDA_DANO' (saída por quebra, validade ou perda). Não grava no banco até o administrador confirmar.")
    public String registrarMovimentacaoEstoque(
            @ToolParam(description = "ID do produto a movimentar") Integer produtoId,
            @ToolParam(description = "Tipo da movimentação: COMPRA_FORNECEDOR, USO_INTERNO ou PERDA_DANO") String tipo,
            @ToolParam(description = "Quantidade movimentada (inteiro maior que zero)") Integer quantidade,
            @ToolParam(required = false, description = "Observação ou motivo da movimentação (opcional)") String observacao) {
        try {
            if (tipo == null || tipo.isBlank()) {
                return "Erro: informe o tipo da movimentação (COMPRA_FORNECEDOR, USO_INTERNO ou PERDA_DANO).";
            }
            if (quantidade == null || quantidade <= 0) {
                return "Erro: a quantidade movimentada deve ser maior que zero.";
            }

            MovimentacaoEstoqueEnum tipoEnum;
            try {
                tipoEnum = MovimentacaoEstoqueEnum.valueOf(tipo.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return "Tipo inválido: '" + tipo + "'. Use COMPRA_FORNECEDOR, USO_INTERNO ou PERDA_DANO.";
            }

            if (tipoEnum == MovimentacaoEstoqueEnum.VENDA) {
                return "Movimentações do tipo VENDA são registradas apenas pelo fluxo de pedidos, não manualmente.";
            }

            EstoqueResponseDTO estoqueAtual = estoqueService.buscarPorProdutoId(produtoId);
            int saldoAtual = estoqueAtual.quantidade() != null ? estoqueAtual.quantidade() : 0;
            boolean entrada = (tipoEnum == MovimentacaoEstoqueEnum.COMPRA_FORNECEDOR);

            if (!entrada && saldoAtual < quantidade) {
                return String.format("Saldo insuficiente em estoque para '%s'. Saldo atual: %d un, saída solicitada: %d un.",
                        estoqueAtual.nomeProduto(), saldoAtual, quantidade);
            }

            int saldoProjetado = entrada ? saldoAtual + quantidade : saldoAtual - quantidade;
            String obsFinal = (observacao != null && !observacao.isBlank())
                    ? observacao.trim()
                    : "Movimentação registrada via Carollina AI";

            String direcao = entrada ? "Entrada (+" + quantidade + " un)" : "Saída (-" + quantidade + " un)";
            List<String> detalhes = List.of(
                    String.format("Produto: %s (ID %d)", estoqueAtual.nomeProduto(), produtoId),
                    String.format("Tipo: %s • %s", tipoEnum.name(), direcao),
                    String.format("Saldo: %d un → %d un", saldoAtual, saldoProjetado),
                    "Observação: " + obsFinal
            );

            boolean critica = (tipoEnum == MovimentacaoEstoqueEnum.PERDA_DANO);
            MovimentacaoEstoqueRequestDTO dto = new MovimentacaoEstoqueRequestDTO(
                    produtoId, tipoEnum, quantidade, obsFinal);

            return acaoPendenteService.proporAcao(
                    "Registrar movimentação de estoque",
                    String.format("%s em %s", direcao, estoqueAtual.nomeProduto()),
                    detalhes,
                    critica,
                    () -> {
                        MovimentacaoEstoqueResponseDTO registrada = estoqueService.registrarMovimentacao(dto);
                        EstoqueResponseDTO saldoAtualizado = estoqueService.buscarPorProdutoId(produtoId);
                        return String.format(
                                "Sucesso! Movimentação #%d (%s) registrada para '%s': %d un. Saldo atual: %d un.",
                                registrada.id(), registrada.tipo(), registrada.nomeProduto(),
                                registrada.quantidade(), saldoAtualizado.quantidade());
                    }
            );
        } catch (Exception e) {
            log.warn("Erro na tool registrarMovimentacaoEstoque(prod={}, tipo={}): {}", produtoId, tipo, e.getMessage());
            return "Erro ao preparar movimentação de estoque: " + e.getMessage();
        }
    }

    @Tool(description = "Prepara a atualização do valor de estoque mínimo de um produto para confirmação do administrador. Não grava no banco até o administrador confirmar.")
    public String atualizarEstoqueMinimo(
            @ToolParam(description = "ID do produto") Integer produtoId,
            @ToolParam(description = "Novo estoque mínimo (inteiro maior ou igual a zero)") Integer novoMinimo) {
        try {
            if (novoMinimo == null || novoMinimo < 0) {
                return "Erro: o estoque mínimo não pode ser nulo ou negativo.";
            }

            EstoqueResponseDTO estoqueAtual = estoqueService.buscarPorProdutoId(produtoId);
            int minimoAtual = estoqueAtual.estoqueMinimo() != null ? estoqueAtual.estoqueMinimo() : 0;

            List<String> detalhes = List.of(
                    String.format("Produto: %s (ID %d)", estoqueAtual.nomeProduto(), produtoId),
                    String.format("Estoque mínimo: %d un → %d un", minimoAtual, novoMinimo),
                    String.format("Saldo atual: %d un", estoqueAtual.quantidade())
            );

            return acaoPendenteService.proporAcao(
                    "Atualizar estoque mínimo",
                    String.format("%s: mínimo %d → %d un", estoqueAtual.nomeProduto(), minimoAtual, novoMinimo),
                    detalhes,
                    false,
                    () -> {
                        EstoqueResponseDTO atualizado = estoqueService.atualizarEstoqueMinimo(produtoId, novoMinimo);
                        return String.format(
                                "Sucesso! O estoque mínimo de '%s' (ID: %d) foi atualizado para %d un. Saldo atual: %d un.",
                                atualizado.nomeProduto(), atualizado.produtoId(),
                                atualizado.estoqueMinimo(), atualizado.quantidade());
                    }
            );
        } catch (Exception e) {
            log.warn("Erro na tool atualizarEstoqueMinimo(prod={}): {}", produtoId, e.getMessage());
            return "Erro ao preparar atualização de estoque mínimo: " + e.getMessage();
        }
    }

    @Tool(description = "Prepara a marcação de um pedido já PAGO como ENTREGUE para confirmação do administrador. Não altera o banco até o administrador confirmar.")
    public String marcarPedidoComoEntregue(
            @ToolParam(description = "ID do pedido a ser marcado como entregue") Integer pedidoId) {
        try {
            PedidoResponseDTO pedido = pedidoService.buscarPorId(pedidoId);
            if (pedido.statusPedido() != StatusTransacaoEnum.PAGO) {
                return "Só é permitido marcar como entregue um pedido com status PAGO. Status atual: " + pedido.statusPedido();
            }

            List<String> detalhes = List.of(
                    "Pedido: #" + pedido.id(),
                    "Status: PAGO → ENTREGUE",
                    "Total: " + formatarMoeda(pedido.total())
            );

            return acaoPendenteService.proporAcao(
                    "Marcar pedido #" + pedido.id() + " como entregue",
                    String.format("Entregar pedido #%d (%s)", pedido.id(), formatarMoeda(pedido.total())),
                    detalhes,
                    false,
                    () -> {
                        PedidoResponseDTO entregue = pedidoService.entregarPedido(pedidoId);
                        return String.format("Pedido #%d marcado como ENTREGUE com sucesso! Status atual: %s.",
                                entregue.id(), entregue.statusPedido());
                    }
            );
        } catch (Exception e) {
            log.warn("Erro na tool marcarPedidoComoEntregue({}): {}", pedidoId, e.getMessage());
            return "Erro ao preparar entrega do pedido: " + e.getMessage();
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

    private String validarCorOpcional(String corHex) {
        if (corHex == null || corHex.isBlank()) {
            return null;
        }
        String cor = corHex.trim().toUpperCase(Locale.ROOT);
        if (!HEX_COLOR_PATTERN.matcher(cor).matches()) {
            throw new IllegalArgumentException("A cor deve estar no formato hexadecimal #RRGGBB (ex: #2BB673).");
        }
        return cor;
    }

    private LocalDate parseDataOpcional(String valor, String nomeCampo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Formato inválido para " + nomeCampo + ": '" + valor + "'. Use o formato YYYY-MM-DD.");
        }
    }

    private String formatarMoeda(BigDecimal valor) {
        BigDecimal seguro = valor != null ? valor : BigDecimal.ZERO;
        return String.format(LOCALE_BR, "R$ %.2f", seguro);
    }

    private String formatarDataHora(Instant instante) {
        if (instante == null) return "-";
        return FORMATO_DATA_HORA.format(instante.atZone(FUSO_BRASIL));
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
