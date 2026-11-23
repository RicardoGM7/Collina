package br.ufs.dcomp.collina.controller.estoque;

import br.ufs.dcomp.collina.dto.estoque.ClientePedidoResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.PagamentoPedidoRequestDTO;
import br.ufs.dcomp.collina.dto.estoque.PagamentoPedidoResponseDTO;
import br.ufs.dcomp.collina.dto.estoque.PedidoRequestDTO;
import br.ufs.dcomp.collina.dto.estoque.PedidoResponseDTO;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import br.ufs.dcomp.collina.model.estoque.StatusTransacaoEnum;
import br.ufs.dcomp.collina.service.estoque.PedidoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    // ==========================================
    // 1. Ciclo de Vida do Pedido
    // ==========================================

    // RF-E01: Criar novo pedido (201 Created)
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criar(
            @Valid @RequestBody PedidoRequestDTO dto,
            @AuthenticationPrincipal UsuarioAutenticado solicitante) {
        PedidoResponseDTO novoPedido = pedidoService.criarPedido(dto, solicitante);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(novoPedido.id())
                .toUri();

        return ResponseEntity.created(location).body(novoPedido);
    }

    // Listar pedidos do próprio usuário autenticado (200 OK)
    @GetMapping("/meus")
    public ResponseEntity<List<PedidoResponseDTO>> listarMeus(
            @AuthenticationPrincipal UsuarioAutenticado solicitante) {
        return ResponseEntity.ok(pedidoService.listarMeusPedidos(solicitante));
    }

    // Listar usuários ativos para seleção de cliente no balcão (ADMIN / ESTOQUE)
    @GetMapping("/clientes")
    public ResponseEntity<List<ClientePedidoResponseDTO>> listarClientes() {
        return ResponseEntity.ok(pedidoService.listarClientesParaPedido());
    }

    // RF-E16 e RF-E17: Listar pedidos com filtros opcionais de status e período (200 OK)
    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listarTodos(
            @RequestParam(required = false) StatusTransacaoEnum status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(pedidoService.listarComFiltro(status, dataInicio, dataFim));
    }

    // Buscar pedido por ID (200 OK)
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    // RF-E07: Cancelar pedido (200 OK)
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponseDTO> cancelar(@PathVariable Integer id) {
        return ResponseEntity.ok(pedidoService.cancelarPedido(id));
    }

    // Marcar pedido como entregue (200 OK)
    @PatchMapping("/{id}/entregar")
    public ResponseEntity<PedidoResponseDTO> entregar(@PathVariable Integer id) {
        return ResponseEntity.ok(pedidoService.entregarPedido(id));
    }

    // ==========================================
    // 2. Gerenciamento de Itens do Pedido
    // ==========================================

    // RF-E02: Adicionar item ao pedido (201 Created)
    @PostMapping("/{id}/itens")
    public ResponseEntity<PedidoResponseDTO.ItemPedidoResponseDTO> adicionarItem(
            @PathVariable Integer id,
            @Valid @RequestBody PedidoRequestDTO.ItemPedidoRequestDTO dto) {
        PedidoResponseDTO.ItemPedidoResponseDTO itemSalvo = pedidoService.adicionarItemAoPedido(id, dto.produtoId(), dto.quantidade());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{itemId}")
                .buildAndExpand(itemSalvo.id())
                .toUri();

        return ResponseEntity.created(location).body(itemSalvo);
    }

    // RF-E14: Alterar quantidade de um item do pedido (200 OK)
    @PatchMapping("/{id}/itens/{itemId}")
    public ResponseEntity<PedidoResponseDTO.ItemPedidoResponseDTO> alterarQuantidadeItem(
            @PathVariable Integer id,
            @PathVariable Integer itemId,
            @Valid @RequestBody PedidoRequestDTO.AtualizarQuantidadeItemRequestDTO dto) {
        return ResponseEntity.ok(pedidoService.alterarQuantidadeItem(id, itemId, dto.novaQuantidade()));
    }

    // RF-E15: Remover item de um pedido (204 No Content)
    @DeleteMapping("/{id}/itens/{itemId}")
    public ResponseEntity<Void> removerItem(
            @PathVariable Integer id,
            @PathVariable Integer itemId) {
        pedidoService.removerItem(id, itemId);
        return ResponseEntity.noContent().build();
    }

    // Listar itens de um pedido (200 OK)
    @GetMapping("/{id}/itens")
    public ResponseEntity<List<PedidoResponseDTO.ItemPedidoResponseDTO>> listarItens(@PathVariable Integer id) {
        return ResponseEntity.ok(pedidoService.listarItensPorPedido(id));
    }

    // ==========================================
    // 3. Pagamento do Pedido
    // ==========================================

    // RF-E05, RF-E06 e RF-F22: Confirmar pagamento e dar baixa automática de estoque (200 OK)
    @PostMapping("/{id}/pagamento")
    public ResponseEntity<PagamentoPedidoResponseDTO> pagar(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) PagamentoPedidoRequestDTO dto) {
        BigDecimal valorPago = dto != null ? dto.valorPago() : null;
        return ResponseEntity.ok(pedidoService.confirmarPagamento(id, valorPago));
    }

    // Consultar dados do pagamento do pedido (200 OK)
    @GetMapping("/{id}/pagamento")
    public ResponseEntity<PagamentoPedidoResponseDTO> buscarPagamento(@PathVariable Integer id) {
        return ResponseEntity.ok(pedidoService.buscarPagamentoPorPedidoId(id));
    }
}
