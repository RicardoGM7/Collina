import type {
  OrigemMovimentacao,
  PrioridadeGasto,
  TipoMovimentacao,
} from '../../types/financeiro'

export const ORIGEM_LABEL: Record<OrigemMovimentacao, string> = {
  PAGAMENTO: 'Pagamento',
  RECEITA_AVULSA: 'Receita avulsa',
  CUSTO_OPERACIONAL: 'Custo operacional',
  SOLICITACAO_GASTO: 'Solicitação de gasto',
}

export const TIPO_LABEL: Record<TipoMovimentacao, string> = {
  ENTRADA: 'Entrada',
  SAIDA: 'Saída',
}

export const PRIORIDADE_LABEL: Record<PrioridadeGasto, string> = {
  ALTA: 'Alta',
  MEDIA: 'Média',
  BAIXA: 'Baixa',
}

export const PRIORIDADE_ORDEM: Record<PrioridadeGasto, number> = {
  ALTA: 0,
  MEDIA: 1,
  BAIXA: 2,
}

const pad = (n: number) => String(n).padStart(2, '0')

// "2026-11-12T10:00:00" (hora local, sem fuso), o formato que o back-end espera.
export function toApiDateTime(date: Date): string {
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

// Valor para <input type="datetime-local">: "2026-11-12T10:00".
export function toInputDateTime(date: Date): string {
  return toApiDateTime(date).slice(0, 16)
}

// Converte o valor do datetime-local para o formato da API.
export function inputToApiDateTime(value: string): string {
  return value.length === 16 ? `${value}:00` : value
}

export function monthKey(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`
}

// Meses terminando no mês de referência, do mais antigo ao mais recente.
export function lastMonths(count: number, ref: Date = new Date()) {
  const months: { key: string; label: string }[] = []
  for (let i = count - 1; i >= 0; i--) {
    const d = new Date(ref.getFullYear(), ref.getMonth() - i, 1)
    months.push({ key: monthKey(d), label: `${pad(d.getMonth() + 1)}/${String(d.getFullYear()).slice(2)}` })
  }
  return months
}

// Primeiro e último dia do mês, no formato AAAA-MM-DD. 
export function monthBounds(ref: Date) {
  const first = new Date(ref.getFullYear(), ref.getMonth(), 1)
  const last = new Date(ref.getFullYear(), ref.getMonth() + 1, 0)
  const fmt = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  return { inicio: fmt(first), fim: fmt(last) }
}
