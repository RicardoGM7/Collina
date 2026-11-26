import type { Mensalidade, Pagamento } from '../../types/financeiro'

export type SituacaoFinanceiraVariant = 'inadimplente' | 'pendente' | 'em-dia'

export interface SituacaoFinanceiraResumo {
  label: 'Inadimplente' | 'Pendente' | 'Em dia'
  variant: SituacaoFinanceiraVariant
  atrasadas: number
  pendentes: number
}

// Converte "AAAA-MM" para "MM/AA"
export function formatarCompetencia(competencia: string | null | undefined): string {
  if (!competencia) return '—'
  const match = /^(\d{4})-(\d{2})$/.exec(competencia.trim())
  if (!match) return competencia
  const [, ano, mes] = match
  return `${mes}/${ano.slice(-2)}`
}

export function situacaoFinanceira(lista: Mensalidade[]): SituacaoFinanceiraResumo {
  let atrasadas = 0
  let pendentes = 0

  for (const m of lista) {
    if (m.status === 'ATRASADA') atrasadas += 1
    else if (m.status === 'PENDENTE') pendentes += 1
  }

  if (atrasadas > 0) {
    return {
      label: 'Inadimplente',
      variant: 'inadimplente',
      atrasadas,
      pendentes,
    }
  }

  if (pendentes > 0) {
    return {
      label: 'Pendente',
      variant: 'pendente',
      atrasadas,
      pendentes,
    }
  }

  return {
    label: 'Em dia',
    variant: 'em-dia',
    atrasadas,
    pendentes,
  }
}

export function ultimaFechada(lista: Mensalidade[]): Mensalidade | null {
  const fechadas = lista.filter(
    (m) => m.status === 'PENDENTE' || m.status === 'ATRASADA' || m.status === 'PAGA',
  )
  if (fechadas.length === 0) return null

  return [...fechadas].sort((a, b) => {
    const compDiff = b.competencia.localeCompare(a.competencia)
    if (compDiff !== 0) return compDiff
    return b.id - a.id
  })[0]
}

export function emAberto(lista: Mensalidade[]): Mensalidade | null {
  return lista.find((m) => m.status === 'EM_ABERTO') ?? null
}

// Soma valores monetários em centavos para evitar imprecisão de ponto flutuante
export function agruparTotais(valores: number[]): number {
  const totalCentavos = valores.reduce((acc, val) => {
    const numero = Number(val)
    if (!Number.isFinite(numero)) return acc
    return acc + Math.round(numero * 100)
  }, 0)
  return totalCentavos / 100
}

export function competenciaDentroDosMeses(competencia: string, meses: number | 'todos'): boolean {
  if (meses === 'todos') return true
  const match = /^(\d{4})-(\d{2})$/.exec(competencia.trim())
  if (!match) return true

  const ano = Number(match[1])
  const mes = Number(match[2])
  const hoje = new Date()
  const atualTotal = hoje.getFullYear() * 12 + hoje.getMonth()
  const compTotal = ano * 12 + (mes - 1)
  return atualTotal - compTotal < meses
}

export function obterGatewayId(pagamento: Pagamento): string | null {
  return pagamento.gatewayId ?? pagamento.getwayId ?? null
}
