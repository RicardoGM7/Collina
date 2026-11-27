export type StatusMensalidade =
  | 'EM_ABERTO'
  | 'PENDENTE'
  | 'PAGA'
  | 'ATRASADA'
  | 'CANCELADA'

export type StatusPagamento =
  | 'AGUARDANDO'
  | 'CONFIRMADO'
  | 'RECUSADO'
  | 'EXPIRADO'

export type FormaPagamento = 'DINHEIRO' | 'PIX' | 'CARTAO' | 'BOLETO' | 'GATEWAY'

export type FormaPagamentoGateway = 'PIX' | 'CARTAO'

export type FormaPagamentoManual = 'DINHEIRO' | 'PIX' | 'CARTAO' | 'BOLETO'

export type ResultadoSimulacaoGateway =
  | 'APROVADO'
  | 'RECUSADO'
  | 'EXPIRADO'
  | 'PENDENTE'

export type RespostaSimulacaoGateway =
  | 'PROCESSADA'
  | 'IGNORADA'
  | 'ASSINATURA_INVALIDA'

export interface GastoAvulso {
  id: number
  descricao: string
  valor: number
  dataLancamento?: string | null
}

export interface Mensalidade {
  id: number
  competencia: string
  status: StatusMensalidade
  valor: number
  vencimento: string | null
  associadoId: number
  // Campos opcionais (B2)
  valorBase?: number
  gastosAvulsos?: GastoAvulso[]
  dataFechamento?: string | null
}

export interface Pagamento {
  id: number
  mensalidadeId: number | null
  tipoGasto: string
  valor: number
  dataPagamento: string | null
  formaPagamento: FormaPagamento
  status: StatusPagamento
  comprovante: string | null
  gatewayId?: string | null
  getwayId?: string | null
  // Campos opcionais (B3)
  dataCriacao?: string | null
  expiraEm?: string | null
}

export interface Cobranca {
  pagamentoId: number
  mensalidadeId: number
  formaPagamento: FormaPagamentoGateway
  valor: number
  status: StatusPagamento
  idExterno: string
  urlPagamento: string | null
  codigoPix: string | null
  expiraEm: string
}

export interface ResumoFechamento {
  competencia: string
  processados: number
  ignorados: number
  titularesComFalha: number[]
  dataExecucao?: string | null
}

export interface Inadimplente {
  associadoId: number
  nomeAssociado: string
  valor: number
  competencia: string
  dataVencimento: string
}

// ===== Caixa, movimentações, solicitações e fechamento contábil =====

export type TipoMovimentacao = 'ENTRADA' | 'SAIDA'

export type OrigemMovimentacao =
  | 'PAGAMENTO'
  | 'RECEITA_AVULSA'
  | 'CUSTO_OPERACIONAL'
  | 'SOLICITACAO_GASTO'

export type SituacaoMovimentacao = 'ATIVO' | 'CANCELADO'

export type PrioridadeGasto = 'ALTA' | 'MEDIA' | 'BAIXA'

export type StatusSolicitacaoGasto = 'PENDENTE' | 'EFETIVADA'

export interface SaldoResponse {
  valor: number
  dataAtualizacao: string
}

export interface MovimentacaoResponse {
  id: number
  tipo: TipoMovimentacao
  origem: OrigemMovimentacao
  valor: number
  descricao: string | null
  motivoCancelamento: string | null
  dataHora: string
  situacao: SituacaoMovimentacao
  pagamentoId: number | null
  funcionarioId: number | null
}

export interface SolicitacaoResponse {
  id: number
  descricao: string
  valor: number
  prioridade: PrioridadeGasto
  status: StatusSolicitacaoGasto
  dataSolicitacao: string
  funcionarioId: number | null
}

export interface RelatorioFechamentoCategoria {
  tipo: TipoMovimentacao
  origem: OrigemMovimentacao
  total: number
}

export interface RelatorioFechamentoResponse {
  competencia: string
  totalEntradas: number
  totalSaidas: number
  saldoPeriodo: number
  porCategoria: RelatorioFechamentoCategoria[]
}
