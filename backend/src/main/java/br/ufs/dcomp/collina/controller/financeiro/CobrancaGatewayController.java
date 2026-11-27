package br.ufs.dcomp.collina.controller.financeiro;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.ufs.dcomp.collina.dto.financeiro.CobrancaResponseDTO;
import br.ufs.dcomp.collina.dto.financeiro.IniciarCobrancaRequest;
import br.ufs.dcomp.collina.model.auth.UsuarioAutenticado;
import br.ufs.dcomp.collina.service.financeiro.CobrancaGatewayService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mensalidades")
public class CobrancaGatewayController {

    @Autowired
    private CobrancaGatewayService cobrancaService;

    // RF-F12: inicia o pagamento de uma mensalidade PENDENTE pelo gateway (201 Created).
    // Devolve o que o pagador precisa (código Pix ou link do checkout) e a validade. Se já existe uma
    // cobrança vigente da mesma forma, devolve a mesma. Erros (tratados pelo GlobalExceptionHandler):
    //   400 -> corpo inválido ou forma diferente de PIX/CARTAO
    //   403 -> associado tentando cobrar mensalidade de outro titular
    //   404 -> mensalidade inexistente
    //   409 -> mensalidade que não está PENDENTE
    @PostMapping("/{mensalidadeId}/cobrancas")
    public ResponseEntity<CobrancaResponseDTO> iniciar(
            @PathVariable Long mensalidadeId,
            @Valid @RequestBody IniciarCobrancaRequest dto,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        CobrancaResponseDTO cobranca = cobrancaService.iniciarCobranca(mensalidadeId, dto, usuario);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/pagamentos/{id}")
                .buildAndExpand(cobranca.pagamentoId())
                .toUri();
        return ResponseEntity.created(location).body(cobranca);
    }
}
