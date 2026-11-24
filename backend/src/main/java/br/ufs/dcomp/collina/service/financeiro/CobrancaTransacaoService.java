package br.ufs.dcomp.collina.service.financeiro;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ufs.dcomp.collina.dto.financeiro.CobrancaResponseDTO;
import br.ufs.dcomp.collina.dto.financeiro.MovimentacaoFinanceiraRequestDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.financeiro.FormaPagamentoEnum;
import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.model.financeiro.OrigemMovimentacaoEnum;
import br.ufs.dcomp.collina.model.financeiro.Pagamento;
import br.ufs.dcomp.collina.model.financeiro.StatusTransacaoEnum;
import br.ufs.dcomp.collina.model.financeiro.TipoMovimentacaoEnum;
import br.ufs.dcomp.collina.repository.financeiro.MensalidadeRepository;
import br.ufs.dcomp.collina.repository.financeiro.PagamentoRepository;
import br.ufs.dcomp.collina.service.financeiro.integracao.CobrancaGateway;
import br.ufs.dcomp.collina.service.financeiro.integracao.NotificacaoGateway;

/**
 * Os passos TRANSACIONAIS da cobrança por gateway, um por transação. Fica em um bean separado do
 * {@link CobrancaGatewayService} porque o @Transactional só vale quando a chamada passa pelo proxy
 * do Spring, e porque a chamada ao gateway precisa ficar ENTRE duas transações, nunca dentro de uma.
 *
 * Ordem de travas (igual à da confirmação manual): mensalidade, depois pagamento, depois saldo do
 * clube. Todo código que altera o estado de um pagamento trava a mensalidade primeiro; é isso que
 * serializa um pedido de cobrança, um webhook duplicado e uma confirmação manual da recepção sobre
 * a mesma mensalidade, sem risco de impasse (deadlock).
 */
@Service
public class CobrancaTransacaoService {

    private static final Logger log = LoggerFactory.getLogger(CobrancaTransacaoService.class);

    /** Resultado do primeiro passo: ou uma cobrança vigente a reaproveitar, ou o pagamento recém-reservado. */
    public record Reserva(Long pagamentoId, BigDecimal valor, CobrancaResponseDTO cobrancaExistente) {}

    @Autowired
    private MensalidadeRepository mensalidadeRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private MovimentacaoFinanceiraService movimentacaoFinanceiraService;

    @Autowired
    private Clock clock;

    // PASSO 1 (transação 1): trava a mensalidade, decide entre reaproveitar e criar, e reserva o pagamento.
    @Transactional
    public Reserva reservarPagamento(Long mensalidadeId, FormaPagamentoEnum forma) {
        Mensalidade mensalidade = mensalidadeRepository.buscarPorIdParaAtualizar(mensalidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensalidade não encontrada com o ID: " + mensalidadeId));

        // O gateway só atende PENDENTE. Mensalidade ATRASADA é quitada na recepção (confirmação manual).
        if (!mensalidade.estaPendente()) {
            throw new IllegalStateException("Apenas mensalidades PENDENTES podem ser pagas pelo gateway. Status atual: "
                    + mensalidade.getStatusMensalidade());
        }

        LocalDateTime agora = LocalDateTime.now(clock);

        Optional<Pagamento> emAndamento = pagamentoRepository
                .findFirstByMensalidadeAndStatusOrderByIdDesc(mensalidade, StatusTransacaoEnum.AGUARDANDO);

        if (emAndamento.isPresent()) {
            Pagamento atual = emAndamento.get();
            boolean mesmaForma = atual.getFormaPagamento() == forma;
            boolean temCobranca = atual.getIdExternoGateway() != null;

            if (mesmaForma && temCobranca && !atual.cobrancaVencida(agora)) {
                // Decisão 1: devolve a mesma cobrança em vez de criar outra.
                return new Reserva(atual.getId(), atual.getValor(), new CobrancaResponseDTO(atual));
            }
            // Venceu, ou o pagador mudou de forma, ou ficou uma reserva sem cobrança (falha anterior):
            // encerra a antiga para liberar uma nova tentativa.
            atual.expirar();
        }

        Pagamento novo = pagamentoRepository.save(new Pagamento(mensalidade.getValorFinal(), forma, mensalidade));
        return new Reserva(novo.getId(), novo.getValor(), null);
    }

    // PASSO 3 (transação 2): guarda no pagamento os dados da cobrança criada no gateway.
    @Transactional
    public CobrancaResponseDTO registrarCobranca(Long pagamentoId, CobrancaGateway cobranca) {
        Pagamento pagamento = buscarPagamento(pagamentoId);

        // Lança IllegalStateException se outra solicitação encerrou esta reserva enquanto o gateway respondia.
        pagamento.vincularCobranca(cobranca.idExterno(), cobranca.urlPagamento(), cobranca.codigoPix(), cobranca.expiraEm());

        return new CobrancaResponseDTO(pagamento);
    }

    // Se o gateway falhou ao criar a cobrança, nada foi cobrado: libera a mensalidade para outra tentativa.
    @Transactional
    public void registrarFalhaNaCriacao(Long pagamentoId) {
        Pagamento pagamento = buscarPagamento(pagamentoId);
        if (pagamento.estaAguardando()) {
            pagamento.recusar();
        }
    }

    // Processa uma notificação JÁ VALIDADA (assinatura conferida). Idempotente: repetir não muda nada.
    @Transactional
    public ResultadoNotificacao processarNotificacao(NotificacaoGateway notificacao) {
        // 1. Descobre a mensalidade e a TRAVA antes de ler o pagamento (ver ordem de travas acima).
        Optional<Long> mensalidadeId = pagamentoRepository.buscarMensalidadeIdPorIdExterno(notificacao.idExterno());
        if (mensalidadeId.isEmpty()) {
            log.warn("Notificação para cobrança desconhecida {}; ignorada.", notificacao.idExterno());
            return ResultadoNotificacao.IGNORADA;
        }

        Mensalidade mensalidade = mensalidadeRepository.buscarPorIdParaAtualizar(mensalidadeId.get())
                .orElseThrow(() -> new ResourceNotFoundException("Mensalidade não encontrada com o ID: " + mensalidadeId.get()));
        Pagamento pagamento = pagamentoRepository.buscarPorIdExterno(notificacao.idExterno())
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado para a cobrança " + notificacao.idExterno()));

        // 2. Idempotência: só um pagamento AGUARDANDO pode mudar. Duplicadas e fora de ordem
        //    (por exemplo, EXPIRADO chegando depois de APROVADO) caem aqui e não fazem nada.
        if (!pagamento.estaAguardando()) {
            log.info("Notificação {} para o pagamento {} já {}; ignorada.",
                    notificacao.status(), pagamento.getId(), pagamento.getStatus());
            return ResultadoNotificacao.IGNORADA;
        }

        LocalDateTime agora = LocalDateTime.now(clock);

        switch (notificacao.status()) {
            case PENDENTE -> {
                return ResultadoNotificacao.IGNORADA;
            }
            case RECUSADO -> pagamento.recusar();
            case EXPIRADO -> pagamento.expirar();
            case APROVADO -> {
                // Decisão 2 (RF-F22): valor diferente do cobrado ou da mensalidade não confirma nada.
                if (notificacao.valor().compareTo(pagamento.getValor()) != 0 || notificacao.valor().compareTo(mensalidade.getValorFinal()) != 0) {
                    log.error("Pagamento {} aprovado com valor {} divergente (cobrado: {}, mensalidade: {}); NÃO confirmado, conferir manualmente.",
                            pagamento.getId(), notificacao.valor(), pagamento.getValor(), mensalidade.getValorFinal());
                    return ResultadoNotificacao.IGNORADA;
                }

                // Decisão 3: PENDENTE ou ATRASADA são quitadas (o dinheiro entrou). Outro estado (por
                // exemplo, cancelada enquanto o Pix estava em aberto) exige conferência manual.
                if (!(mensalidade.estaPendente() || mensalidade.estaAtrasada())) {
                    log.error("Pagamento {} aprovado, mas a mensalidade {} está {}; NÃO confirmado, conciliar manualmente.",
                            pagamento.getId(), mensalidade.getId(), mensalidade.getStatusMensalidade());
                    return ResultadoNotificacao.IGNORADA;
                }

                mensalidade.marcarComoPaga();
                pagamento.confirmar(agora);
                if (pagamento.getComprovante() == null) {
                    pagamento.setComprovante("GATEWAY-" + notificacao.idExterno());
                }

                // Mesma entrada automática da confirmação manual (RF-F18), na mesma transação:
                // se o caixa recusar, a mensalidade e o pagamento voltam ao estado anterior.
                movimentacaoFinanceiraService.registrarEntradaAutomatica(pagamento);
            }
        }

        return ResultadoNotificacao.PROCESSADA;
    }

    private Pagamento buscarPagamento(Long pagamentoId) {
        return pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com o ID: " + pagamentoId));
    }
}
