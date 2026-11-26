package br.ufs.dcomp.collina.controller.financeiro;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.ufs.dcomp.collina.dto.financeiro.PagamentoResponseDTO;
import br.ufs.dcomp.collina.service.financeiro.CobrancaGatewayService;
import br.ufs.dcomp.collina.service.financeiro.PagamentoService;
import br.ufs.dcomp.collina.model.financeiro.ResultadoNotificacao;
import br.ufs.dcomp.collina.service.financeiro.integracao.RequisicaoWebhook;
import br.ufs.dcomp.collina.service.financeiro.integracao.StatusNotificacaoGateway;
import br.ufs.dcomp.collina.service.financeiro.integracao.provisorio.GatewayPagamentoClientStub;

/**
 * SÓ PARA DESENVOLVIMENTO E DEMONSTRAÇÃO. Faz o papel do gateway: monta uma notificação assinada e a
 * entrega ao mesmo caminho do webhook real (validação de assinatura incluída). Existe apenas enquanto
 * o stub estiver ativo, isto é, em qualquer perfil exceto "gateway-real". ATENÇÃO: em produção o perfil
 * gateway-real DEVE estar ativo, senão este endpoint continua aberto.
 */
@RestController
@Profile("!gateway-real")
@RequestMapping("/api/stub-gateway")
public class StubGatewayController {

    @Autowired
    private GatewayPagamentoClientStub stub;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private CobrancaGatewayService cobrancaService;

    // Simula o aviso do gateway sobre um pagamento. Repita a chamada para simular notificação duplicada;
    // informe "valor" para simular valor divergente (RF-F22).
    @PostMapping("/pagamentos/{pagamentoId}/simular")
    public ResponseEntity<ResultadoNotificacao> simular(
            @PathVariable Long pagamentoId,
            @RequestParam("resultado") StatusNotificacaoGateway resultado,
            @RequestParam(value = "valor", required = false) BigDecimal valor) {
        PagamentoResponseDTO pagamento = pagamentoService.buscarPorId(pagamentoId);

        if (pagamento.getwayId() == null) {
            throw new IllegalStateException("O pagamento " + pagamentoId + " ainda não tem cobrança no gateway.");
        }

        BigDecimal valorNotificado = valor != null ? valor : pagamento.valor();
        RequisicaoWebhook requisicao = stub.montarNotificacao(pagamento.getwayId(), resultado, valorNotificado);

        return ResponseEntity.ok(cobrancaService.processarNotificacao(requisicao));
    }
}
