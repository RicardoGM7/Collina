import axios from 'axios'
import { api } from './api'
import type {
  Cobranca,
  FormaPagamentoGateway,
  FormaPagamentoManual,
  Inadimplente,
  Mensalidade,
  MovimentacaoResponse,
  OrigemMovimentacao,
  Pagamento,
  PrioridadeGasto,
  RelatorioFechamentoResponse,
  RespostaSimulacaoGateway,
  ResultadoSimulacaoGateway,
  ResumoFechamento,
  SaldoResponse,
  SituacaoMovimentacao,
  SolicitacaoResponse,
  TipoMovimentacao,
} from '../types/financeiro'

export interface ConfirmarPagamentoDTO {
  valor: number
  formaPagamento: FormaPagamentoManual
  idGateway?: string
}

export interface CancelarEReemitirDTO {
  justificativa: string
  novoVencimento: string
  novoValor: number
}

export interface LancarGastoAvulsoDTO {
  pessoaId: number
  valor: number
  descricao: string
}

function normalizarMensalidade(m: Mensalidade): Mensalidade {
  return {
    ...m,
    valor: Number(m.valor),
    valorBase: m.valorBase !== undefined ? Number(m.valorBase) : undefined,
    gastosAvulsos: m.gastosAvulsos?.map((g) => ({
      ...g,
      valor: Number(g.valor),
    })),
  }
}

function normalizarPagamento(p: Pagamento): Pagamento {
  return {
    ...p,
    valor: Number(p.valor),
    gatewayId: p.gatewayId ?? p.getwayId ?? null,
  }
}

function normalizarCobranca(c: Cobranca): Cobranca {
  return {
    ...c,
    valor: Number(c.valor),
  }
}

function normalizarInadimplente(i: Inadimplente): Inadimplente {
  return {
    ...i,
    valor: Number(i.valor),
  }
}

function ultimasCompetencias(quantidade = 12): string[] {
  const hoje = new Date()
  const lista: string[] = []
  for (let i = 0; i < quantidade; i++) {
    const data = new Date(hoje.getFullYear(), hoje.getMonth() - i, 1)
    const ano = data.getFullYear()
    const mes = String(data.getMonth() + 1).padStart(2, '0')
    lista.push(`${ano}-${mes}`)
  }
  return lista
}

export const financeiroService = {
  // ===== Portal do associado (perfil ASSOCIADO) =====

  async minhasMensalidades(competencia?: string): Promise<Mensalidade[]> {
    const { data } = await api.get<Mensalidade[]>('/api/associados/me/mensalidades', {
      params: competencia ? { competencia } : undefined,
    })
    return data.map(normalizarMensalidade)
  },

  async meusPagamentos(): Promise<Pagamento[]> {
    const { data } = await api.get<Pagamento[]>('/api/associados/me/pagamentos')
    return data.map(normalizarPagamento)
  },

  async iniciarCobranca(
    mensalidadeId: number,
    formaPagamento: FormaPagamentoGateway,
  ): Promise<Cobranca> {
    const { data } = await api.post<Cobranca>(
      `/api/mensalidades/${mensalidadeId}/cobrancas`,
      { formaPagamento },
    )
    return normalizarCobranca(data)
  },

  // ===== Atendimento e financeiro =====

  async listarMensalidades(associadoId: number, competencia?: string): Promise<Mensalidade[]> {
    try {
      const { data } = await api.get<Mensalidade[]>('/api/mensalidades', {
        params: competencia ? { associadoId, competencia } : { associadoId },
      })
      return data.map(normalizarMensalidade)
    } catch (erro) {
      // Fallback B1 caso o back-end ainda exija "competencia" obrigatória
      if (!competencia && axios.isAxiosError(erro) && erro.response?.status === 400) {
        const resultados = await Promise.allSettled(
          ultimasCompetencias(12).map((comp) =>
            api.get<Mensalidade[]>('/api/mensalidades', {
              params: { associadoId, competencia: comp },
            }),
          ),
        )
        const mapa = new Map<number, Mensalidade>()
        for (const res of resultados) {
          if (res.status === 'fulfilled') {
            for (const item of res.value.data) {
              mapa.set(item.id, normalizarMensalidade(item))
            }
          }
        }
        return Array.from(mapa.values()).sort((a, b) =>
          b.competencia.localeCompare(a.competencia),
        )
      }
      throw erro
    }
  },

  async confirmarPagamento(
    mensalidadeId: number,
    dto: ConfirmarPagamentoDTO,
  ): Promise<Pagamento> {
    const { data } = await api.post<Pagamento>(
      `/api/mensalidades/${mensalidadeId}/confirmar-pagamento`,
      {
        ...dto,
        valor: Number(dto.valor.toFixed(2)),
      },
    )
    return normalizarPagamento(data)
  },

  async cancelarEReemitir(
    mensalidadeId: number,
    dto: CancelarEReemitirDTO,
  ): Promise<Mensalidade> {
    const { data } = await api.post<Mensalidade>(
      `/api/mensalidades/${mensalidadeId}/cancelar-e-reemitir`,
      {
        ...dto,
        novoValor: Number(dto.novoValor.toFixed(2)),
      },
    )
    return normalizarMensalidade(data)
  },

  async lancarGastoAvulso(dto: LancarGastoAvulsoDTO): Promise<Mensalidade> {
    const { data } = await api.post<Mensalidade>('/api/mensalidades/gastos-avulsos', {
      ...dto,
      valor: Number(dto.valor.toFixed(2)),
    })
    return normalizarMensalidade(data)
  },

  async pagamentosDaMensalidade(mensalidadeId: number): Promise<Pagamento[]> {
    const { data } = await api.get<Pagamento[]>(
      `/api/mensalidades/${mensalidadeId}/pagamentos`,
    )
    return data.map(normalizarPagamento)
  },

  async pagamentosDoTitular(associadoId: number): Promise<Pagamento[]> {
    const { data } = await api.get<Pagamento[]>('/api/pagamentos', {
      params: { associadoId },
    })
    return data.map(normalizarPagamento)
  },

  // ===== Fechamento e relatórios =====

  async executarFechamento(): Promise<ResumoFechamento> {
    const { data } = await api.post<ResumoFechamento>('/api/financeiro/fechamento')
    return data
  },

  async ultimoFechamento(): Promise<ResumoFechamento | null> {
    const response = await api.get<ResumoFechamento>('/api/financeiro/fechamento/ultimo', {
      validateStatus: (status) => status === 200 || status === 204,
    })
    if (response.status === 204 || !response.data) {
      return null
    }
    return response.data
  },

  async inadimplentes(): Promise<Inadimplente[]> {
    const { data } = await api.get<Inadimplente[]>('/api/financeiro/relatorio/inadimplentes')
    return data.map(normalizarInadimplente)
  },

  // ===== Stub de gateway (somente desenvolvimento) =====

  async simularGateway(
    pagamentoId: number,
    resultado: ResultadoSimulacaoGateway,
    valor?: number,
  ): Promise<RespostaSimulacaoGateway> {
    const { data } = await api.post<RespostaSimulacaoGateway>(
      `/api/stub-gateway/pagamentos/${pagamentoId}/simular`,
      null,
      {
        params: valor !== undefined ? { resultado, valor: Number(valor.toFixed(2)) } : { resultado },
      },
    )
    return data
  },

  // ===== Caixa, movimentações e solicitações de gasto =====

  async consultarSaldo(): Promise<SaldoResponse> {
    const { data } = await api.get<SaldoResponse>('/api/financeiro/consulta-operacoes/saldo')
    return { ...data, valor: Number(data.valor) }
  },

  async listarHistorico(filtros?: {
    tipo?: TipoMovimentacao
    origem?: OrigemMovimentacao
    situacao?: SituacaoMovimentacao
    dataInicio?: string
    dataFim?: string
  }): Promise<MovimentacaoResponse[]> {
    const { data } = await api.get<MovimentacaoResponse[]>(
      '/api/financeiro/consulta-operacoes/historico',
      { params: filtros },
    )
    return data.map((m) => ({ ...m, valor: Number(m.valor) }))
  },

  async registrarMovimentacao(dto: {
    tipo: TipoMovimentacao
    origem: OrigemMovimentacao
    valor: number
    descricao?: string
    dataHora: string
    solicitacaoId?: number
  }): Promise<MovimentacaoResponse> {
    const { data } = await api.post<MovimentacaoResponse>('/api/financeiro/movimentacoes', {
      ...dto,
      valor: Number(dto.valor.toFixed(2)),
    })
    return { ...data, valor: Number(data.valor) }
  },

  async editarMovimentacao(
    id: number,
    dto: {
      tipo: TipoMovimentacao
      origem: OrigemMovimentacao
      valor: number
      descricao?: string
      dataHora: string
    },
  ): Promise<MovimentacaoResponse> {
    const { data } = await api.put<MovimentacaoResponse>(`/api/financeiro/movimentacoes/${id}`, {
      ...dto,
      valor: Number(dto.valor.toFixed(2)),
    })
    return { ...data, valor: Number(data.valor) }
  },

  async cancelarMovimentacao(id: number, motivo: string): Promise<void> {
    await api.delete(`/api/financeiro/movimentacoes/${id}`, { params: { motivo } })
  },

  async listarSolicitacoesPorPrioridade(): Promise<SolicitacaoResponse[]> {
    const { data } = await api.get<SolicitacaoResponse[]>(
      '/api/financeiro/solicitacoes-gasto/prioridade',
    )
    return data.map((s) => ({ ...s, valor: Number(s.valor) }))
  },

  async cadastrarSolicitacao(dto: {
    descricao: string
    valor: number
    prioridade: PrioridadeGasto
    dataSolicitacao: string
  }): Promise<SolicitacaoResponse> {
    const { data } = await api.post<SolicitacaoResponse>('/api/financeiro/solicitacoes-gasto', {
      ...dto,
      valor: Number(dto.valor.toFixed(2)),
    })
    return { ...data, valor: Number(data.valor) }
  },

  async gerarFechamento(competencia: string): Promise<RelatorioFechamentoResponse> {
    const { data } = await api.get<RelatorioFechamentoResponse>(
      '/api/financeiro/relatorio/fechamento',
      { params: { competencia } },
    )
    return {
      ...data,
      totalEntradas: Number(data.totalEntradas),
      totalSaidas: Number(data.totalSaidas),
      saldoPeriodo: Number(data.saldoPeriodo),
      porCategoria: data.porCategoria.map((c) => ({ ...c, total: Number(c.total) })),
    }
  },
}
