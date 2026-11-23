package br.ufs.dcomp.collina.service.financeiro;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ufs.dcomp.collina.dto.financeiro.ConfirmacaoManualRequest;
import br.ufs.dcomp.collina.dto.financeiro.PagamentoResponseDTO;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.model.financeiro.Pagamento;
import br.ufs.dcomp.collina.repository.financeiro.MensalidadeRepository;
import br.ufs.dcomp.collina.repository.financeiro.PagamentoRepository;
import br.ufs.dcomp.collina.service.financeiro.integracao.VerificadorDeFuncionario;

@Service
public class PagamentoService {

    @Autowired
    private MensalidadeRepository mensalidadeRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private MovimentacaoFinanceiraService movimentacaoFinanceiraService;

    @Autowired
    private VerificadorDeFuncionario verificadorDeFuncionario;

    // RF-F18 / RF-F22: confirmação manual + validação do valor + entrada automática,
    // tudo numa única transação (se a entrada falhar, o pagamento e o status voltam).
    @Transactional
    public PagamentoResponseDTO confirmarManualmente(Long mensalidadeId, ConfirmacaoManualRequest dados, Long funcionarioId) {
        if (mensalidadeId == null) { throw new IllegalArgumentException("Id da mensalidade não informado"); }
        if (funcionarioId == null) { throw new IllegalArgumentException("Id do funcionario não informado"); }
        if (dados == null) { throw new IllegalArgumentException("Dados da requisição não informados"); }
        if (dados.valor() == null) { throw new IllegalArgumentException("Valor não informado"); }
        if (dados.formaPagamento() == null) { throw new IllegalArgumentException("Forma de pagamento não informada"); }

        verificadorDeFuncionario.exigirFuncionarioHabilitado(funcionarioId);

        Mensalidade mensalidade = mensalidadeRepository.buscarPorIdParaAtualizar(mensalidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensalidade não encontrada com o ID: " + mensalidadeId));

        mensalidade.marcarComoPaga();
        validarValor(mensalidade, dados.valor());

        LocalDateTime agora = LocalDateTime.now();

        Pagamento pagamento = new Pagamento(dados.valor(), dados.formaPagamento(), mensalidade);
        pagamento.confirmar(agora);
        Pagamento salvo = pagamentoRepository.save(pagamento);

       movimentacaoFinanceiraService.registrarEntradaAutomatica(salvo);

        return new PagamentoResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public PagamentoResponseDTO buscarPorId(Long pagamentoId) {
        if (pagamentoId == null) {
            throw new IllegalArgumentException("Id do pagamento não informado");
        }

        Pagamento pagamento = pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com o ID: " + pagamentoId));

        return new PagamentoResponseDTO(pagamento);
    }

    // Tentativas de uma mensalidade (recusadas, expiradas e confirmada), da mais antiga à mais nova.
    @Transactional(readOnly = true)
    public List<PagamentoResponseDTO> listarPorMensalidade(Long mensalidadeId) {
        if (mensalidadeId == null) {
            throw new IllegalArgumentException("Id da mensalidade não informado");
        }

        // Sem esta busca, um id inexistente devolveria lista vazia em vez de 404.
        Mensalidade mensalidade = mensalidadeRepository.findById(mensalidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Mensalidade não encontrada com o ID: " + mensalidadeId));

        return pagamentoRepository.findByMensalidade(mensalidade)
                .stream()
                .sorted(Comparator.comparing(Pagamento::getId))
                .map(PagamentoResponseDTO::new)
                .toList();
    }

    // Histórico de pagamentos de um titular. Id inexistente devolve lista vazia: o
    // financeiro ainda não tem como confirmar a existência do associado.
    @Transactional(readOnly = true)
    public List<PagamentoResponseDTO> listarPorTitular(Long associadoId) {
        if (associadoId == null) {
            throw new IllegalArgumentException("Id do associado não informado");
        }

        return pagamentoRepository.buscarPorTitular(associadoId)
                .stream()
                .sorted(Comparator.comparing(Pagamento::getId))
                .map(PagamentoResponseDTO::new)
                .toList();
    }

    // RF-F22: o valor pago deve ser exatamente o valor final congelado da mensalidade.
    // getValorFinal() e não calcularValorTotal(): este só vale para EM_ABERTO.
    private void validarValor(Mensalidade mensalidade, BigDecimal valor) {
        if (valor.compareTo(mensalidade.getValorFinal()) != 0) {
            throw new IllegalArgumentException("Valor informado (" + valor
                    + ") não corresponde ao valor da mensalidade (" + mensalidade.getValorFinal() + ").");
        }
    }
}