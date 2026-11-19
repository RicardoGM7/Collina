package br.ufs.dcomp.collina.controller.financeiro;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.ufs.dcomp.collina.dto.financeiro.ConfirmacaoManualRequest;
import br.ufs.dcomp.collina.dto.financeiro.PagamentoResponseDTO;
import br.ufs.dcomp.collina.service.financeiro.PagamentoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class PagamentoController {

    @Autowired
    private PagamentoService pagamentoService;

    // RF-F18 / RF-F22: confirma manualmente o pagamento de uma mensalidade PENDENTE ou ATRASADA
    // (201 Created). Cria o Pagamento, marca a mensalidade como PAGA e lança a entrada no caixa.
    // O funcionarioId no header é provisório, até haver autenticação. Erros possíveis
    // (tratados pelo GlobalExceptionHandler):
    //   400 -> corpo inválido (@Valid), header ausente ou valor diferente do valor da mensalidade
    //   404 -> mensalidade inexistente
    //   409 -> mensalidade em estado que não permite pagamento (EM_ABERTO, PAGA, CANCELADA)
    @PostMapping("/mensalidades/{mensalidadeId}/confirmar-pagamento")
    public ResponseEntity<PagamentoResponseDTO> confirmarManualmente(
            @PathVariable Long mensalidadeId,
            @Valid @RequestBody ConfirmacaoManualRequest dados,
            @RequestHeader("funcionarioId") Long funcionarioId) {

        PagamentoResponseDTO response = pagamentoService.confirmarManualmente(mensalidadeId, dados, funcionarioId);

        // O request é de /mensalidades/..., então o Location é montado a partir da raiz da aplicação.
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/pagamentos/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    // Consulta um pagamento (200 OK). 404 se não existir.
    @GetMapping("/pagamentos/{pagamentoId}")
    public ResponseEntity<PagamentoResponseDTO> buscarPorId(@PathVariable Long pagamentoId) {
        return ResponseEntity.ok(pagamentoService.buscarPorId(pagamentoId));
    }

    // Tentativas de pagamento de uma mensalidade (200 OK). 404 se a mensalidade não existir.
    @GetMapping("/mensalidades/{mensalidadeId}/pagamentos")
    public ResponseEntity<List<PagamentoResponseDTO>> listarPorMensalidade(@PathVariable Long mensalidadeId) {
        return ResponseEntity.ok(pagamentoService.listarPorMensalidade(mensalidadeId));
    }

    // Histórico de pagamentos de um titular (200 OK). associadoId é obrigatório (400 se ausente).
    @GetMapping("/pagamentos")
    public ResponseEntity<List<PagamentoResponseDTO>> listarPorTitular(@RequestParam Long associadoId) {
        return ResponseEntity.ok(pagamentoService.listarPorTitular(associadoId));
    }
}