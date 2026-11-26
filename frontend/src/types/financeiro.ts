// Tipagens do módulo financeiro (espelham os DTOs do back-end em /api/financeiro)

export type TipoMovimentacao = 'ENTRADA' | 'SAIDA'
export type OrigemMovimentacao =
  | 'PAGAMENTO'
  | 'RECEITA_AVULSA'
  | 'CUSTO_OPERACIONAL'
  | 'SOLICITACAO_GASTO'
export type SituacaoMovimentacao = 'ATIVO' | 'CANCELADO'

export type PrioridadeGasto = 'ALTA' | 'MEDIA' | 'BAIXA'
export type StatusSolicitacaoGasto = 'PENDENTE' | 'EFETIVADA'

export interface MovimentacaoResponse {
  id: number
  tipo: TipoMovimentacao
  origem: OrigemMovimentacao
  valor: number
  descricao: string | null
  motivoCancelamento?: string | null
  dataHora: string
  situacao: SituacaoMovimentacao
  pagamentoId: number | null
}

export interface MovimentacaoRequest {
  tipo: TipoMovimentacao
  origem: OrigemMovimentacao
  valor: number
  descricao?: string
  dataHora: string
  pagamentoId?: number
  solicitacaoId?: number
}

export interface SaldoResponse {
  valor: number
  dataAtualizacao: string
}

export interface FiltroRequest {
  tipo?: TipoMovimentacao
  origem?: OrigemMovimentacao
  situacao?: SituacaoMovimentacao
  dataInicio?: string // "AAAA-MM-DD"
  dataFim?: string // "AAAA-MM-DD"
}

export interface SolicitacaoRequest {
  descricao: string
  valor: number
  prioridade: PrioridadeGasto
  dataSolicitacao: string
}

export interface SolicitacaoResponse {
  id: number
  descricao: string
  valor: number
  prioridade: PrioridadeGasto
  status: StatusSolicitacaoGasto
  dataSolicitacao: string
  funcionarioId: number
}

export interface ItemCategoriaResponse {
  tipo: TipoMovimentacao
  origem: OrigemMovimentacao
  total: number
}

export interface RelatorioFechamentoResponse {
  competencia: string
  totalEntradas: number
  totalSaidas: number
  saldoPeriodo: number
  porCategoria: ItemCategoriaResponse[]
}
