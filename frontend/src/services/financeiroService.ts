import { api } from './api'
import type {
  FiltroRequest,
  MovimentacaoRequest,
  MovimentacaoResponse,
  RelatorioFechamentoResponse,
  SaldoResponse,
  SolicitacaoRequest,
  SolicitacaoResponse,
} from '../types/financeiro'

const BASE = '/api/financeiro'

// PROVISÓRIO: as rotas de movimentação e de solicitação de gasto ainda exigem o header "funcionarioId"
const FUNCIONARIO_ID = String(import.meta.env.VITE_FUNCIONARIO_ID ?? '1')
const comFuncionario = { headers: { funcionarioId: FUNCIONARIO_ID } }

export const financeiroService = {
  // ConsultaOperacoesController
  async consultarSaldo(): Promise<SaldoResponse> {
    const { data } = await api.get<SaldoResponse>(`${BASE}/consulta-operacoes/saldo`)
    return data
  },

  async listarHistorico(filtro: FiltroRequest = {}): Promise<MovimentacaoResponse[]> {
    const { data } = await api.get<MovimentacaoResponse[]>(
      `${BASE}/consulta-operacoes/historico`,
      { params: filtro },
    )
    return data
  },

  // MovimentacaoFinanceiraController
  async registrarMovimentacao(dados: MovimentacaoRequest): Promise<MovimentacaoResponse> {
    const { data } = await api.post<MovimentacaoResponse>(
      `${BASE}/movimentacoes`,
      dados,
      comFuncionario,
    )
    return data
  },

  async editarMovimentacao(
    id: number,
    dados: MovimentacaoRequest,
  ): Promise<MovimentacaoResponse> {
    const { data } = await api.put<MovimentacaoResponse>(
      `${BASE}/movimentacoes/${id}`,
      dados,
      comFuncionario,
    )
    return data
  },

  // No back-end é DELETE /movimentacoes/{id}?motivo=... (cancelamento lógico com estorno no saldo)
  async cancelarMovimentacao(id: number, motivo: string): Promise<void> {
    await api.delete(`${BASE}/movimentacoes/${id}`, {
      params: { motivo },
      ...comFuncionario,
    })
  },

  // SolicitacaoGastoController
  async cadastrarSolicitacao(dados: SolicitacaoRequest): Promise<SolicitacaoResponse> {
    const { data } = await api.post<SolicitacaoResponse>(
      `${BASE}/solicitacoes-gasto`,
      dados,
      comFuncionario,
    )
    return data
  },

  async listarSolicitacoesPorPrioridade(): Promise<SolicitacaoResponse[]> {
    const { data } = await api.get<SolicitacaoResponse[]>(
      `${BASE}/solicitacoes-gasto/prioridade`,
    )
    return data
  },

  // RelatorioFinanceiroController
  async gerarFechamento(competencia: string): Promise<RelatorioFechamentoResponse> {
    const { data } = await api.get<RelatorioFechamentoResponse>(
      `${BASE}/relatorio/fechamento`,
      { params: { competencia } },
    )
    return data
  },
}
