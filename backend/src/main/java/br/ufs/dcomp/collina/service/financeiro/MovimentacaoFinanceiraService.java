package br.ufs.dcomp.collina.service.financeiro;

import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import br.ufs.dcomp.collina.exception.ResourceNotFoundException;
import br.ufs.dcomp.collina.dto.financeiro.FiltroMovimentacaoRequestDTO;
import br.ufs.dcomp.collina.dto.financeiro.MovimentacaoFinanceiraRequestDTO;
import br.ufs.dcomp.collina.dto.financeiro.MovimentacaoFinanceiraResponseDTO;
import br.ufs.dcomp.collina.dto.financeiro.SaldoClubeResponseDTO;
import br.ufs.dcomp.collina.model.financeiro.MovimentacaoFinanceira;
import br.ufs.dcomp.collina.model.financeiro.OrigemMovimentacaoEnum;
import br.ufs.dcomp.collina.model.financeiro.Pagamento;
import br.ufs.dcomp.collina.model.financeiro.SaldoClube;
import br.ufs.dcomp.collina.model.financeiro.SituacaoMovimentacaoEnum;
import br.ufs.dcomp.collina.model.financeiro.TipoMovimentacaoEnum;
import br.ufs.dcomp.collina.repository.financeiro.MovimentacaoFinanceiraRepository;
import br.ufs.dcomp.collina.repository.financeiro.SaldoClubeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimentacaoFinanceiraService {

    @Autowired
    private MovimentacaoFinanceiraRepository movimentacaoRepository;

    @Autowired
    private SaldoClubeRepository saldoRepository;

    @Autowired
    private SolicitacaoGastoService solicitacaoService;

    /* Preciso de mais classes para utilizar este
    @Autowired
    private FuncionarioService funcionarioService; 
    */

    private static final LocalDateTime DATA_MINIMA = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime DATA_MAXIMA = LocalDateTime.of(9999, 12, 31, 0, 0);

    // RF-F01/RF-F02/RF-F17 - Cadastro e realização de movimentações financeiras
    @Transactional
    public MovimentacaoFinanceiraResponseDTO registrar(
            MovimentacaoFinanceiraRequestDTO dados, Long funcionarioId) {

        // Verificação dos dados
        validarDados(dados);
        if (funcionarioId == null) {
            throw new IllegalArgumentException("O id do funcionário é obrigatório.");
        }
        if (dados.origem() == OrigemMovimentacaoEnum.PAGAMENTO) {
            throw new IllegalArgumentException("Uma operação de pagamento não pode ser registrada.");
        }
        // RF-F01: lançamentos manuais não são vinculados a pagamento de mensalidade ou pedido
        if (dados.pagamentoId() != null) {
            throw new IllegalArgumentException("Movimentações manuais não podem ser vinculadas a um pagamento.");
        }
        if (dados.origem() == OrigemMovimentacaoEnum.SOLICITACAO_GASTO && dados.solicitacaoId() == null) {
            throw new IllegalArgumentException("Movimentações de Solicitação de Gasto exigem o ID da solicitação.");
        }

        // Saída só é feita se houver saldo suficiente
        SaldoClube saldo = obterSaldoParaAtualizar();
        BigDecimal variacao = efeito(dados.tipo(), dados.valor());
        validarSaldoSuficiente(saldo, variacao);

        // Efetiva uma solicitação de gasto, se houver (RF-F19)
        if (dados.solicitacaoId() != null) {
            if (dados.tipo() != TipoMovimentacaoEnum.SAIDA
                    || dados.origem() != OrigemMovimentacaoEnum.SOLICITACAO_GASTO) {
                throw new IllegalArgumentException(
                        "Para efetivar uma solicitação de gasto, a movimentação deve ser uma SAIDA.");
            }
            solicitacaoService.marcarComoEfetivada(dados.solicitacaoId(), dados.valor());
        }

        aplicarVariacao(saldo, variacao);

        MovimentacaoFinanceira mov = criarMovimentacao(dados);
        mov.setFuncionarioId(funcionarioId);

        return new MovimentacaoFinanceiraResponseDTO(movimentacaoRepository.save(mov));
    }

    // RF-F18 - Registro Automático de Entrada
    @Transactional
    public MovimentacaoFinanceiraResponseDTO registrarEntradaAutomatica(Pagamento pagamento) {

        if (pagamento == null || pagamento.getId() == null) {
            throw new IllegalArgumentException("O pagamento é obrigatório e precisa estar salvo.");
        }
        if (!pagamento.foiConfirmado()) {
            throw new IllegalStateException("Somente pagamentos confirmados geram entrada financeira.");
        }
        // Relação 1:1, um pagamento gera no máximo uma movimentação
        if (movimentacaoRepository.existsByPagamentoId(pagamento.getId())) {
            throw new IllegalStateException("Já existe uma movimentação financeira para este pagamento.");
        }

        BigDecimal valor = pagamento.getValor().setScale(2, RoundingMode.HALF_UP);
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor do pagamento deve ser maior que zero.");
        }

        // Entrada: soma ao saldo do clube
        aplicarVariacao(obterSaldoParaAtualizar(), valor);

        // Registro da operação, ligada ao pagamento
        MovimentacaoFinanceira mov = new MovimentacaoFinanceira();
        mov.setTipo(TipoMovimentacaoEnum.ENTRADA);
        mov.setOrigem(OrigemMovimentacaoEnum.PAGAMENTO);
        mov.setValor(valor);
        mov.setDescricao(descricaoDoPagamento(pagamento));
        mov.setDataHora(pagamento.getDataPagamento() != null
                ? pagamento.getDataPagamento() : LocalDateTime.now());
        mov.setSituacao(SituacaoMovimentacaoEnum.ATIVO);
        mov.setPagamento(pagamento);

        return new MovimentacaoFinanceiraResponseDTO(movimentacaoRepository.save(mov));
    }

    // RF-F23 - Edição de Movimentação Financeira
    @Transactional
    public MovimentacaoFinanceiraResponseDTO editar(
        Long id, MovimentacaoFinanceiraRequestDTO dados, Long funcionarioId) {

        // Verificação dos dados
        validarDados(dados);
        if (id == null) {
            throw new IllegalArgumentException("O id da movimentação é obrigatório.");
        }
        if (funcionarioId == null) {
            throw new IllegalArgumentException("O id do funcionário é obrigatório.");
        }

        MovimentacaoFinanceira mov = movimentacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentacao financeira não encontrada."));

        // Verifica se a transação está ativa
        if (mov.getSituacao() == SituacaoMovimentacaoEnum.CANCELADO) {
            throw new IllegalStateException("A transação deve estar ativa para poder ser editada.");
        }
        // Verifica se a origem é um pagamento
        if (mov.getOrigem() == OrigemMovimentacaoEnum.PAGAMENTO) {
            throw new IllegalStateException("Um pagamento confirmado não pode ser editado.");
        }

        // RF-F23: o tipo não pode ser alterado 
        if (dados.tipo() != mov.getTipo() || dados.origem() != mov.getOrigem()) {
            throw new IllegalArgumentException(
                    "O tipo e a origem da movimentação não podem ser alterados. Cancele e registre novamente.");
        }

        // Só o valor muda o saldo: desfaz o efeito do valor antigo e aplica o do novo.
        BigDecimal variacao = efeito(mov.getTipo(), dados.valor())
                .subtract(efeito(mov.getTipo(), mov.getValor()));

        if (variacao.signum() != 0) {
            aplicarVariacao(obterSaldoParaAtualizar(), variacao);
        }

        mov.atualizar(dados.descricao(), dados.valor().setScale(2, RoundingMode.UNNECESSARY));
        mov.setFuncionarioId(funcionarioId);

        return new MovimentacaoFinanceiraResponseDTO(movimentacaoRepository.save(mov));
    }

    // RF-F24 - Remoção de Movimentação Financeira
    @Transactional
    public MovimentacaoFinanceiraResponseDTO cancelar(
        Long id, String motivo, Long funcionarioId) {

        if (id == null) {
            throw new IllegalArgumentException("O id da movimentação é obrigatório");
        }
        if (funcionarioId == null) {
            throw new IllegalArgumentException("O id do funcionário é obrigatório.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("O motivo do cancelamento é obrigatório.");
        }

        MovimentacaoFinanceira mov = movimentacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentacao financeira não encontrada."));

        if (mov.getSituacao() == SituacaoMovimentacaoEnum.CANCELADO) {
            throw new IllegalStateException("Esta movimentação já foi cancelada");
        }
        if (mov.getOrigem() == OrigemMovimentacaoEnum.PAGAMENTO) {
            throw new IllegalStateException("Movimentações geradas por pagamentos não podem ser canceladas.");
        }

        // Aplica o efeito contrário ao da movimentação
        BigDecimal estorno = efeito(mov.getTipo(), mov.getValor()).negate();
        aplicarVariacao(obterSaldoParaAtualizar(), estorno);

        mov.setFuncionarioId(funcionarioId);
        mov.cancelar(motivo);

        return new MovimentacaoFinanceiraResponseDTO(movimentacaoRepository.save(mov));
    }

    // RF-F04 - Histórico de Operações do Clube
    @Transactional(readOnly = true)
    public List<MovimentacaoFinanceiraResponseDTO> listarHistorico(FiltroMovimentacaoRequestDTO filtro) {
        if (filtro.dataInicio() != null && filtro.dataFim() != null
                && filtro.dataFim().isBefore(filtro.dataInicio())) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial");
        }

        // Datas mínima e máxima para evitar erros
        LocalDateTime inicio = filtro.dataInicio() != null
                ? filtro.dataInicio().atStartOfDay() : DATA_MINIMA;
        LocalDateTime fim = filtro.dataFim() != null
                ? filtro.dataFim().plusDays(1).atStartOfDay() : DATA_MAXIMA;   

        return movimentacaoRepository
                .buscarPorFiltro(filtro.tipo(), filtro.origem(), filtro.situacao(), inicio, fim)
                .stream()
                .map(MovimentacaoFinanceiraResponseDTO::new)
                .toList();
    }

    // RF-F16 - Acesso ao Saldo do Clube
    @Transactional(readOnly = true)
    public SaldoClubeResponseDTO consultarSaldo() {
        // Sem nenhuma movimentação ainda, o saldo é zero
        return saldoRepository.findFirstByOrderByIdAsc()
                .map(SaldoClubeResponseDTO::new)
                .orElseGet(() -> new SaldoClubeResponseDTO(BigDecimal.ZERO, LocalDateTime.now()));
    }

    
    // Métodos auxiliares

    // O saldo do clube é um registro único em tb_saldo_clube. 
    private SaldoClube obterSaldoParaAtualizar() {
        return saldoRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> {
                    SaldoClube inicial = new SaldoClube();
                    inicial.setValor(BigDecimal.ZERO);
                    inicial.setDataAtualizacao(LocalDateTime.now());
                    return saldoRepository.save(inicial);
                });
    }

    // Efeito da movimentação no saldo
    private BigDecimal efeito(TipoMovimentacaoEnum tipo, BigDecimal valor) {
        BigDecimal v = valor.setScale(2, RoundingMode.UNNECESSARY);
        return switch (tipo) {
            case ENTRADA -> v;
            case SAIDA -> v.negate();
        };
    }

    // O saldo NUNCA pode ficar negativo (RF-F17)
    private void validarSaldoSuficiente(SaldoClube saldo, BigDecimal variacao) {
        if (variacao.signum() < 0 && !saldo.possuiSaldoPara(variacao.negate())) {
            throw new IllegalStateException("Valor insuficiente para realizar a operação");
        }
    }

    // Atualiza o registro único do saldo
    private void aplicarVariacao(SaldoClube saldo, BigDecimal variacao) {
        validarSaldoSuficiente(saldo, variacao);

        if (variacao.signum() >= 0) {
            saldo.creditar(variacao);
        } else {
            saldo.debitar(variacao.negate());
        }
        saldoRepository.save(saldo);
    }

    private void validarDados(MovimentacaoFinanceiraRequestDTO dados) {
        if (dados == null) {
            throw new IllegalArgumentException("Os dados da movimentação são obrigatórios.");
        }
        if (dados.tipo() == null || dados.origem() == null) {
            throw new IllegalArgumentException("Tipo e origem são obrigatórios.");
        }
        if (dados.valor() == null || dados.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
        // Garantia do número certo de casas decimais
        if (dados.valor().stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("O valor deve ter no máximo 2 casas decimais.");
        }
        if (dados.dataHora() == null) {
            throw new IllegalArgumentException("A data e hora da movimentação são obrigatórias.");
        }

        // Coerência entre tipo e origem
        boolean coerente = dados.tipo() == TipoMovimentacaoEnum.ENTRADA
                ? (dados.origem() == OrigemMovimentacaoEnum.PAGAMENTO
                    || dados.origem() == OrigemMovimentacaoEnum.RECEITA_AVULSA)
                : (dados.origem() == OrigemMovimentacaoEnum.CUSTO_OPERACIONAL
                    || dados.origem() == OrigemMovimentacaoEnum.SOLICITACAO_GASTO);
        if (!coerente) {
            throw new IllegalArgumentException("A origem " + dados.origem()
                    + " não é compatível com uma movimentação do tipo " + dados.tipo() + ".");
        }
    }

    private String descricaoDoPagamento(Pagamento pagamento) {
        return pagamento.getMensalidade() != null
                ? "Pagamento de mensalidade " + pagamento.getMensalidade().getCompetencia()
                : "Pagamento " + pagamento.getId();
    }

    private MovimentacaoFinanceira criarMovimentacao(MovimentacaoFinanceiraRequestDTO dados) {
        MovimentacaoFinanceira mov = new MovimentacaoFinanceira();
        mov.setTipo(dados.tipo());
        mov.setOrigem(dados.origem());
        mov.setValor(dados.valor().setScale(2, RoundingMode.UNNECESSARY));
        mov.setDescricao(dados.descricao());
        mov.setDataHora(dados.dataHora());
        mov.setSituacao(SituacaoMovimentacaoEnum.ATIVO);
        return mov;
    }
}