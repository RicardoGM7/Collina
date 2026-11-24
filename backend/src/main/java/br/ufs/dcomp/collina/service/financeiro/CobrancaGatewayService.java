package br.ufs.dcomp.collina.service.financeiro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.ufs.dcomp.collina.dto.financeiro.CobrancaResponseDTO;
import br.ufs.dcomp.collina.dto.financeiro.IniciarCobrancaRequest;
import br.ufs.dcomp.collina.model.financeiro.FormaPagamentoEnum;
import br.ufs.dcomp.collina.service.financeiro.CobrancaTransacaoService.Reserva;
import br.ufs.dcomp.collina.service.financeiro.integracao.CobrancaGateway;
import br.ufs.dcomp.collina.service.financeiro.integracao.GatewayPagamentoClient;
import br.ufs.dcomp.collina.service.financeiro.integracao.NotificacaoGateway;
import br.ufs.dcomp.collina.service.financeiro.integracao.RequisicaoWebhook;

/**
 * Orquestra o pagamento de mensalidades pelo gateway (Pix e cartão). NÃO é transacional de propósito:
 * a chamada ao gateway fica entre duas transações curtas, executadas em {@link CobrancaTransacaoService}.
 * Segurar uma transação aberta durante uma chamada de rede travaria a mensalidade e uma conexão do banco.
 *
 * Mensalidade e Pagamento continuam sendo alterados só pelos métodos das próprias entidades; aqui
 * só há sequência e decisões que dependem de mais de um objeto.
 */
@Service
public class CobrancaGatewayService {

    private static final Logger log = LoggerFactory.getLogger(CobrancaGatewayService.class);

    @Autowired
    private GatewayPagamentoClient gateway;

    @Autowired
    private CobrancaTransacaoService transacoes;

    // RF-F12: inicia o pagamento de uma mensalidade PENDENTE pelo gateway.
    public CobrancaResponseDTO iniciarCobranca(Long mensalidadeId, IniciarCobrancaRequest dto) {
        if (mensalidadeId == null) {
            throw new IllegalArgumentException("Id da mensalidade não informado");
        }
        if (dto == null || dto.formaPagamento() == null) {
            throw new IllegalArgumentException("A forma de pagamento é obrigatória.");
        }
        FormaPagamentoEnum forma = dto.formaPagamento();
        // Validado ANTES de reservar, para uma forma inválida não deixar um pagamento recusado para trás.
        if (forma != FormaPagamentoEnum.PIX && forma != FormaPagamentoEnum.CARTAO) {
            throw new IllegalArgumentException("O gateway só atende PIX e CARTAO.");
        }

        // Transação 1: reserva o pagamento (ou encontra a cobrança vigente).
        Reserva reserva = transacoes.reservarPagamento(mensalidadeId, forma);
        if (reserva.cobrancaExistente() != null) {
            return reserva.cobrancaExistente();
        }

        // Fora de transação: chamada ao gateway. A referência é o id do nosso Pagamento.
        CobrancaGateway cobranca;
        try {
            cobranca = gateway.criarCobranca(reserva.valor(), forma, String.valueOf(reserva.pagamentoId()));
        } catch (RuntimeException e) {
            log.error("Falha ao criar a cobrança do pagamento {} no gateway: {}", reserva.pagamentoId(), e.getMessage());
            transacoes.registrarFalhaNaCriacao(reserva.pagamentoId());
            throw e;
        }

        // Transação 2: guarda os dados da cobrança.
        return transacoes.registrarCobranca(reserva.pagamentoId(), cobranca);
    }

    // RF-F13: processa uma notificação do gateway.
    public ResultadoNotificacao processarNotificacao(RequisicaoWebhook requisicao) {
        if (!gateway.validarAssinatura(requisicao)) {
            log.warn("Notificação com assinatura inválida descartada.");
            return ResultadoNotificacao.ASSINATURA_INVALIDA;
        }

        NotificacaoGateway notificacao = gateway.interpretarNotificacao(requisicao);
        return transacoes.processarNotificacao(notificacao);
    }
}
