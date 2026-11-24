package br.ufs.dcomp.collina.service.financeiro.integracao;

import java.math.BigDecimal;

import br.ufs.dcomp.collina.model.financeiro.FormaPagamentoEnum;

/**
 * Contrato do financeiro com o gateway de pagamentos (Pix e cartão).
 *
 * Existem duas implementações, escolhidas por perfil do Spring:
 *  - {@code GatewayPagamentoClientStub} (padrão): simula o gateway, sem rede nem conta;
 *  - a implementação real (perfil {@code gateway-real}), que fala com o provedor.
 *
 * ATENÇÃO: {@link #criarCobranca} é uma chamada de rede na implementação real. Nunca a faça
 * dentro de uma transação de banco aberta: grave o Pagamento AGUARDANDO, chame o gateway e só
 * depois grave o id externo.
 *
 * Exceções: argumento inválido ou notificação malformada => {@code IllegalArgumentException} (400).
 */
public interface GatewayPagamentoClient {

    /**
     * Cria uma cobrança no gateway.
     *
     * @param valor      valor a cobrar (positivo)
     * @param forma      apenas PIX ou CARTAO
     * @param referencia referência do nosso lado (o id do Pagamento), para reencontrá-lo depois
     */
    CobrancaGateway criarCobranca(BigDecimal valor, FormaPagamentoEnum forma, String referencia);

    /** Confere a autenticidade da requisição do webhook. Falsa => responder 401 e não processar. */
    boolean validarAssinatura(RequisicaoWebhook requisicao);

    /** Traduz a notificação do provedor para o nosso formato. Só chamar depois de validar a assinatura. */
    NotificacaoGateway interpretarNotificacao(RequisicaoWebhook requisicao);
}
