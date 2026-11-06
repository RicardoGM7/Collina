package br.ufs.dcomp.collina.service.estoque;

import br.ufs.dcomp.collina.dto.estoque.PagamentoPedidoResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.PedidoRequestDTO;
import br.ufs.dcomp.collina.dto.estoque.PedidoResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.estoque.*;
import br.ufs.dcomp.collina.repository.estoque.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ItemPedidoRepository itemPedidoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Autowired
    private PagamentoPedidoRepository pagamentoPedidoRepository;

    // ==========================================
    // 1. Ciclo de Vida do Pedido
    // ==========================================

    // RF-E01: Criar novo pedido
    @Transactional
    public PedidoResponseDTO criarPedido(PedidoRequestDTO dto) {
        if (dto == null || dto.itens() == null || dto.itens().isEmpty()) {
            throw new IllegalArgumentException("O pedido deve conter pelo menos um item.");
        }

        Pedido pedido = new Pedido();
        pedido.setData(Instant.now());
        pedido.setStatus(StatusTransacaoEnum.AGUARDANDO_PAGAMENTO);
        pedido.setItens(new ArrayList<>());

        // Agrupa as quantidades por produtoId para evitar duplicações e burlar validação de estoque
        Map<Integer, Integer> quantidadesPorProduto = new LinkedHashMap<>();
        for (PedidoRequestDTO.ItemPedidoRequestDTO itemDto : dto.itens()) {
            if (itemDto == null || itemDto.produtoId() == null) {
                throw new IllegalArgumentException("O ID do produto do item é obrigatório.");
            }
            if (itemDto.quantidade() == null || itemDto.quantidade() <= 0) {
                throw new IllegalArgumentException("A quantidade do item deve ser maior que zero.");
            }
            quantidadesPorProduto.merge(itemDto.produtoId(), itemDto.quantidade(), Integer::sum);
        }

        // Processa e valida cada produto agrupado
        for (Map.Entry<Integer, Integer> entry : quantidadesPorProduto.entrySet()) {
            Integer produtoId = entry.getKey();
            Integer quantidadeTotal = entry.getValue();

            Produto produto = produtoRepository.findById(produtoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + produtoId));

            // RF-E04: Verifica disponibilidade em estoque
            Estoque estoque = estoqueRepository.findByProdutoId(produto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o produto: " + produto.getNome()));

            int saldoDisponivel = estoque.getQuantidade() != null ? estoque.getQuantidade() : 0;
            if (saldoDisponivel < quantidadeTotal) {
                throw new IllegalArgumentException("Estoque insuficiente para o produto '" + produto.getNome()
                        + "'. Saldo disponível: " + saldoDisponivel + ", solicitado: " + quantidadeTotal);
            }

            // RF-E02: Congela o preço unitário praticado no momento da compra
            ItemPedido item = new ItemPedido();
            item.setProduto(produto);
            item.setQuantidade(quantidadeTotal);
            item.setPreco(produto.getPreco());
            item.setPedido(pedido);

            pedido.getItens().add(item);
        }

        Pedido pedidoSalvo = pedidoRepository.save(pedido);
        return new PedidoResponseDTO(pedidoSalvo);
    }

    // RF-E16 e RF-E17: Listar todos os pedidos
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarTodos() {
        return pedidoRepository.findAll()
                .stream()
                .map(PedidoResponseDTO::new)
                .toList();
    }

    // Buscar pedido por ID
    @Transactional(readOnly = true)
    public PedidoResponseDTO buscarPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do pedido é obrigatório.");
        }

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com o ID: " + id));

        return new PedidoResponseDTO(pedido);
    }

    // RF-E07: Cancelar pedido (somente permitido para status AGUARDANDO_PAGAMENTO)
    @Transactional
    public PedidoResponseDTO cancelarPedido(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID do pedido é obrigatório.");
        }

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com o ID: " + id));

        if (pedido.getStatus() != StatusTransacaoEnum.AGUARDANDO_PAGAMENTO) {
            throw new IllegalStateException("Não é permitido cancelar pedido com status: " + pedido.getStatus());
        }

        pedido.setStatus(StatusTransacaoEnum.CANCELADO);
        Pedido atualizado = pedidoRepository.save(pedido);

        return new PedidoResponseDTO(atualizado);
    }

    // ==========================================
    // 2. Gerenciamento de Itens do Pedido (ItemPedido)
    // ==========================================

    // RF-E02: Adicionar item a um pedido já aberto
    @Transactional
    public PedidoResponseDTO.ItemPedidoResponseDTO adicionarItemAoPedido(Integer pedidoId, Integer produtoId, Integer quantidade) {
        if (pedidoId == null) {
            throw new IllegalArgumentException("O ID do pedido é obrigatório.");
        }
        if (produtoId == null) {
            throw new IllegalArgumentException("O ID do produto é obrigatório.");
        }
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com o ID: " + pedidoId));

        if (pedido.getStatus() != StatusTransacaoEnum.AGUARDANDO_PAGAMENTO) {
            throw new IllegalStateException("Não é possível alterar itens de um pedido com status: " + pedido.getStatus());
        }

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + produtoId));

        Estoque estoque = estoqueRepository.findByProdutoId(produto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o produto: " + produto.getNome()));

        int saldoEstoque = estoque.getQuantidade() != null ? estoque.getQuantidade() : 0;

        if (pedido.getItens() == null) {
            pedido.setItens(new ArrayList<>());
        }

        // Se o produto já estiver no pedido, soma a quantidade sem duplicar registros
        Optional<ItemPedido> itemExistenteOpt = pedido.getItens().stream()
                .filter(i -> i.getProduto() != null && i.getProduto().getId().equals(produtoId))
                .findFirst();

        ItemPedido itemParaSalvar;
        if (itemExistenteOpt.isPresent()) {
            itemParaSalvar = itemExistenteOpt.get();
            int novaQuantidadeTotal = itemParaSalvar.getQuantidade() + quantidade;

            // RF-E04: Valida disponibilidade no estoque
            if (saldoEstoque < novaQuantidadeTotal) {
                throw new IllegalArgumentException("Estoque insuficiente para o produto '" + produto.getNome()
                        + "'. Saldo disponível: " + saldoEstoque + ", total solicitado: " + novaQuantidadeTotal);
            }

            itemParaSalvar.setQuantidade(novaQuantidadeTotal);
        } else {
            // RF-E04: Valida disponibilidade no estoque
            if (saldoEstoque < quantidade) {
                throw new IllegalArgumentException("Estoque insuficiente para o produto '" + produto.getNome()
                        + "'. Saldo disponível: " + saldoEstoque + ", solicitado: " + quantidade);
            }

            itemParaSalvar = new ItemPedido();
            itemParaSalvar.setPedido(pedido);
            itemParaSalvar.setProduto(produto);
            itemParaSalvar.setQuantidade(quantidade);
            itemParaSalvar.setPreco(produto.getPreco());

            pedido.getItens().add(itemParaSalvar);
        }

        ItemPedido itemSalvo = itemPedidoRepository.save(itemParaSalvar);
        return new PedidoResponseDTO.ItemPedidoResponseDTO(itemSalvo);
    }

    // RF-E14: Alterar quantidade de um item do pedido
    @Transactional
    public PedidoResponseDTO.ItemPedidoResponseDTO alterarQuantidadeItem(Integer pedidoId, Integer itemId, Integer novaQuantidade) {
        if (pedidoId == null) {
            throw new IllegalArgumentException("O ID do pedido é obrigatório.");
        }
        if (itemId == null) {
            throw new IllegalArgumentException("O ID do item é obrigatório.");
        }
        if (novaQuantidade == null || novaQuantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }

        ItemPedido item = itemPedidoRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item do pedido não encontrado com ID: " + itemId));

        if (!item.getPedido().getId().equals(pedidoId)) {
            throw new IllegalArgumentException("O item informado não pertence ao pedido ID: " + pedidoId);
        }

        if (item.getPedido().getStatus() != StatusTransacaoEnum.AGUARDANDO_PAGAMENTO) {
            throw new IllegalStateException("Não é permitido alterar itens de pedidos que não estejam aguardando pagamento.");
        }

        Estoque estoque = estoqueRepository.findByProdutoId(item.getProduto().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o produto associado."));

        int saldoEstoque = estoque.getQuantidade() != null ? estoque.getQuantidade() : 0;
        if (saldoEstoque < novaQuantidade) {
            throw new IllegalArgumentException("Estoque insuficiente para a quantidade solicitada: " + novaQuantidade
                    + ". Saldo disponível: " + saldoEstoque);
        }

        item.setQuantidade(novaQuantidade);
        ItemPedido itemAtualizado = itemPedidoRepository.save(item);

        return new PedidoResponseDTO.ItemPedidoResponseDTO(itemAtualizado);
    }

    // RF-E15: Remover item de um pedido
    @Transactional
    public void removerItem(Integer pedidoId, Integer itemId) {
        if (pedidoId == null) {
            throw new IllegalArgumentException("O ID do pedido é obrigatório.");
        }
        if (itemId == null) {
            throw new IllegalArgumentException("O ID do item é obrigatório.");
        }

        ItemPedido item = itemPedidoRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item do pedido não encontrado com ID: " + itemId));

        if (!item.getPedido().getId().equals(pedidoId)) {
            throw new IllegalArgumentException("O item informado não pertence ao pedido ID: " + pedidoId);
        }

        if (item.getPedido().getStatus() != StatusTransacaoEnum.AGUARDANDO_PAGAMENTO) {
            throw new IllegalStateException("Não é permitido remover itens de pedidos que não estejam aguardando pagamento.");
        }

        Pedido pedido = item.getPedido();
        if (pedido.getItens() != null) {
            pedido.getItens().removeIf(i -> i.getId() != null && i.getId().equals(itemId));
        }

        itemPedidoRepository.delete(item);
    }

    // Listar itens por pedido
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO.ItemPedidoResponseDTO> listarItensPorPedido(Integer pedidoId) {
        if (pedidoId == null) {
            throw new IllegalArgumentException("O ID do pedido é obrigatório.");
        }

        return itemPedidoRepository.findByPedidoId(pedidoId)
                .stream()
                .map(PedidoResponseDTO.ItemPedidoResponseDTO::new)
                .toList();
    }

    // ==========================================
    // 3. Pagamento do Pedido (PagamentoPedido)
    // ==========================================

    // RF-E05 e RF-E06: Confirmar pagamento do pedido e baixar estoque automaticamente
    @Transactional
    public PagamentoPedidoResponseDTO confirmarPagamento(Integer pedidoId) {
        if (pedidoId == null) {
            throw new IllegalArgumentException("O ID do pedido é obrigatório.");
        }

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com o ID: " + pedidoId));

        if (pedido.getStatus() != StatusTransacaoEnum.AGUARDANDO_PAGAMENTO) {
            throw new IllegalStateException("Apenas pedidos com status 'AGUARDANDO_PAGAMENTO' podem ser pagos. Status atual: " + pedido.getStatus());
        }

        if (pagamentoPedidoRepository.findByPedidoId(pedidoId).isPresent()) {
            throw new IllegalStateException("Este pedido já possui um pagamento registrado.");
        }

        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new IllegalStateException("Não é possível confirmar pagamento de um pedido sem itens.");
        }

        // FASE 1: Validação prévia de disponibilidade de estoque
        Map<Integer, Integer> quantidadesTotaisPorProduto = new HashMap<>();
        for (ItemPedido item : pedido.getItens()) {
            if (item.getProduto() == null || item.getProduto().getId() == null) {
                throw new IllegalStateException("Item do pedido inválido sem produto associado.");
            }
            quantidadesTotaisPorProduto.merge(item.getProduto().getId(), item.getQuantidade(), Integer::sum);
        }

        for (Map.Entry<Integer, Integer> entry : quantidadesTotaisPorProduto.entrySet()) {
            Integer produtoId = entry.getKey();
            Integer qtdNecessaria = entry.getValue();

            Estoque estoque = estoqueRepository.findByProdutoId(produtoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o produto ID: " + produtoId));

            int saldoAtual = estoque.getQuantidade() != null ? estoque.getQuantidade() : 0;
            if (saldoAtual < qtdNecessaria) {
                String nomeProduto = estoque.getProduto() != null ? estoque.getProduto().getNome() : "ID #" + produtoId;
                throw new IllegalStateException("Estoque insuficiente para o produto '" + nomeProduto
                        + "'. Saldo disponível: " + saldoAtual + ", total necessário: " + qtdNecessaria);
            }
        }

        // FASE 2: Baixa do estoque e registro de auditoria
        for (ItemPedido item : pedido.getItens()) {
            Estoque estoque = estoqueRepository.findByProdutoId(item.getProduto().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o produto ID: " + item.getProduto().getId()));

            int saldoAtual = estoque.getQuantidade() != null ? estoque.getQuantidade() : 0;
            estoque.setQuantidade(saldoAtual - item.getQuantidade());
            estoqueRepository.save(estoque);

            MovimentacaoEstoque mov = new MovimentacaoEstoque();
            mov.setData(Instant.now());
            mov.setTipo(MovimentacaoEstoqueEnum.VENDA);
            mov.setQuantidade(item.getQuantidade());
            mov.setObservacao("Venda confirmada no Pedido #" + pedido.getId());
            mov.setEstoque(estoque);
            movimentacaoEstoqueRepository.save(mov);
        }

        // FASE 3: Criação do registro de pagamento e atualização do pedido
        PagamentoPedido pagamento = new PagamentoPedido();
        pagamento.setData(Instant.now());
        pagamento.setPedido(pedido);
        PagamentoPedido pagamentoSalvo = pagamentoPedidoRepository.save(pagamento);

        pedido.setStatus(StatusTransacaoEnum.PAGO);
        pedido.setPagamentoPedido(pagamentoSalvo);
        pedidoRepository.save(pedido);

        return new PagamentoPedidoResponseDTO(pagamentoSalvo);
    }

    // Buscar dados do pagamento pelo ID do Pedido
    @Transactional(readOnly = true)
    public PagamentoPedidoResponseDTO buscarPagamentoPorPedidoId(Integer pedidoId) {
        if (pedidoId == null) {
            throw new IllegalArgumentException("O ID do pedido é obrigatório.");
        }

        PagamentoPedido pagamento = pagamentoPedidoRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum pagamento encontrado para o pedido ID: " + pedidoId));

        return new PagamentoPedidoResponseDTO(pagamento);
    }
}
