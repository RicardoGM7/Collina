package br.ufs.dcomp.collina.controller.financeiro;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.ufs.dcomp.collina.service.financeiro.CobrancaGatewayService;
import br.ufs.dcomp.collina.service.financeiro.ResultadoNotificacao;
import br.ufs.dcomp.collina.service.financeiro.integracao.RequisicaoWebhook;

@RestController
@RequestMapping("/api/webhooks/pagamentos")
public class WebhookPagamentoController {

    @Autowired
    private CobrancaGatewayService cobrancaService;

    // RF-F13: recebe as notificações do gateway. O corpo chega como TEXTO: a assinatura é calculada
    // sobre os bytes originais, e reserializar o JSON a invalidaria.
    //   200 -> recebida (processada ou ignorada: o gateway não deve reenviar nenhuma das duas)
    //   401 -> assinatura inválida
    //   400 -> notificação malformada
    // Quando houver autenticação, esta rota deve continuar pública: a proteção dela é a assinatura.
    @PostMapping
    public ResponseEntity<Void> receber(
            @RequestBody String corpo,
            @RequestHeader Map<String, String> cabecalhos,
            @RequestParam Map<String, String> parametros) {
        ResultadoNotificacao resultado = cobrancaService.processarNotificacao(
                new RequisicaoWebhook(corpo, cabecalhos, parametros));

        if (resultado == ResultadoNotificacao.ASSINATURA_INVALIDA) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok().build();
    }
}
