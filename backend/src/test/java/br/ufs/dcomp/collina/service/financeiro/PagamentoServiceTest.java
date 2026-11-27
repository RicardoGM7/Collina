package br.ufs.dcomp.collina.service.financeiro;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.ufs.dcomp.collina.dto.financeiro.ConfirmacaoManualRequest;
import br.ufs.dcomp.collina.dto.financeiro.PagamentoResponseDTO;
import br.ufs.dcomp.collina.model.associados.Associado;
import br.ufs.dcomp.collina.model.financeiro.FormaPagamentoEnum;
import br.ufs.dcomp.collina.model.financeiro.Mensalidade;
import br.ufs.dcomp.collina.model.financeiro.Pagamento;
import br.ufs.dcomp.collina.model.financeiro.StatusMensalidadeEnum;
import br.ufs.dcomp.collina.model.financeiro.StatusTransacaoEnum;
import br.ufs.dcomp.collina.repository.financeiro.MensalidadeRepository;
import br.ufs.dcomp.collina.repository.financeiro.PagamentoRepository;
import br.ufs.dcomp.collina.service.financeiro.integracao.VerificadorDeFuncionario;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock
    private MensalidadeRepository mensalidadeRepository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private MovimentacaoFinanceiraService movimentacaoFinanceiraService;

    @Mock
    private VerificadorDeFuncionario verificadorDeFuncionario;

    @InjectMocks
    private PagamentoService pagamentoService;

    @Test
    @DisplayName("Lança exceção quando associadoId for nulo")
    void deveLancarExcecaoQuandoAssociadoIdNulo() {
        assertThrows(IllegalArgumentException.class, () -> pagamentoService.listarPorTitular(null));
    }

    @Test
    @DisplayName("Lista pagamentos do titular ordenados cronologicamente com dados mínimos requeridos")
    void deveListarPagamentosOrdenadosCronologicamente() {
        Associado titular = new Associado();
        titular.setId(1);

        Mensalidade mensalidade = new Mensalidade();
        mensalidade.setId(100L);
        mensalidade.setAssociadoTitular(titular);

        Pagamento p1 = new Pagamento(new BigDecimal("150.00"), FormaPagamentoEnum.PIX, mensalidade);
        p1.setId(1L);
        p1.setDataPagamento(LocalDateTime.of(2026, 3, 10, 14, 0));
        p1.setStatus(StatusTransacaoEnum.CONFIRMADO);
        p1.setComprovante("COMPROVANTE-001");

        Pagamento p2 = new Pagamento(new BigDecimal("150.00"), FormaPagamentoEnum.CARTAO, mensalidade);
        p2.setId(2L);
        p2.setDataPagamento(LocalDateTime.of(2026, 1, 5, 10, 30));
        p2.setStatus(StatusTransacaoEnum.CONFIRMADO);
        p2.setComprovante("COMPROVANTE-002");

        Pagamento p3 = new Pagamento(new BigDecimal("150.00"), FormaPagamentoEnum.BOLETO, mensalidade);
        p3.setId(3L);
        p3.setDataPagamento(null); // Pagamento ainda aguardando
        p3.setStatus(StatusTransacaoEnum.AGUARDANDO);

        // Repositório retorna fora de ordem propositalmente para validar a ordenação do serviço
        when(pagamentoRepository.buscarPorTitular(1)).thenReturn(List.of(p1, p3, p2));

        List<PagamentoResponseDTO> resultado = pagamentoService.listarPorTitular(1);

        assertEquals(3, resultado.size());
        // p2 é de janeiro (mais antigo)
        assertEquals(2L, resultado.get(0).id());
        assertEquals("MENSALIDADE", resultado.get(0).tipoGasto());
        assertEquals(100L, resultado.get(0).mensalidadeId());
        assertEquals(new BigDecimal("150.00"), resultado.get(0).valor());
        assertEquals(FormaPagamentoEnum.CARTAO, resultado.get(0).formaPagamento());
        assertEquals(LocalDateTime.of(2026, 1, 5, 10, 30), resultado.get(0).dataPagamento());
        assertEquals(StatusTransacaoEnum.CONFIRMADO, resultado.get(0).status());
        assertEquals("COMPROVANTE-002", resultado.get(0).comprovante());

        // p1 é de março
        assertEquals(1L, resultado.get(1).id());
        assertEquals(LocalDateTime.of(2026, 3, 10, 14, 0), resultado.get(1).dataPagamento());

        // p3 tem dataPagamento null (vai para o final)
        assertEquals(3L, resultado.get(2).id());
        assertNull(resultado.get(2).dataPagamento());
        assertEquals(StatusTransacaoEnum.AGUARDANDO, resultado.get(2).status());
    }

    // ── Ciclo 7: comprovante de confirmação manual ────────────────────────────

    @Test
    @DisplayName("Confirmação manual gera comprovante não-nulo com prefixo MANUAL-")
    void deveGerarComprovanteNaConfirmacaoManual() {
        Associado titular = new Associado();
        titular.setId(1);

        Mensalidade mensalidade = new Mensalidade("2026-05", new BigDecimal("200.00"), titular);
        mensalidade.setId(10L);
        // transitar para PENDENTE para que marcarComoPaga() seja válido
        mensalidade.fechar(java.time.LocalDate.of(2026, 5, 29));

        when(mensalidadeRepository.buscarPorIdParaAtualizar(10L)).thenReturn(Optional.of(mensalidade));

        // O repository devolve a própria instância recebida (simula save)
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(inv -> {
            Pagamento p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        ConfirmacaoManualRequest req = new ConfirmacaoManualRequest(new BigDecimal("200.00"), FormaPagamentoEnum.PIX);
        PagamentoResponseDTO resposta = pagamentoService.confirmarManualmente(10L, req, 1L);

        assertNotNull(resposta.comprovante(), "Comprovante não deve ser nulo");
        assertTrue(resposta.comprovante().startsWith("MANUAL-"), "Comprovante deve ter prefixo MANUAL-");
        verify(pagamentoRepository).save(any(Pagamento.class));
    }

    // ── Ciclo 8: validação do valor ANTES de marcar como paga ────────────────

    @Test
    @DisplayName("Valor errado não altera o status da mensalidade")
    void deveRejeitarValorErradoSemAlterarStatusDaMensalidade() {
        Associado titular = new Associado();
        titular.setId(1);

        Mensalidade mensalidade = new Mensalidade("2026-05", new BigDecimal("200.00"), titular);
        mensalidade.setId(11L);
        mensalidade.fechar(java.time.LocalDate.of(2026, 5, 29));

        StatusMensalidadeEnum statusAntes = mensalidade.getStatusMensalidade();

        when(mensalidadeRepository.buscarPorIdParaAtualizar(11L)).thenReturn(Optional.of(mensalidade));

        ConfirmacaoManualRequest req = new ConfirmacaoManualRequest(new BigDecimal("999.00"), FormaPagamentoEnum.PIX);

        assertThrows(IllegalArgumentException.class,
                () -> pagamentoService.confirmarManualmente(11L, req, 1L));

        // Status não deve ter mudado
        assertEquals(statusAntes, mensalidade.getStatusMensalidade(),
                "Status da mensalidade não deve ser alterado quando o valor é inválido");
        verify(pagamentoRepository, never()).save(any());
    }
}
