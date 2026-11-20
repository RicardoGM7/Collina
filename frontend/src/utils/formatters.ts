export function formatCurrency(value: number): string {
  return value.toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  })
}

export function formatDecimalInput(value: number): string {
  return value.toFixed(2).replace('.', ',')
}

export function parseCurrencyInput(raw: string): number {
  const trimmed = raw.trim()
  if (!trimmed) return Number.NaN
  // Remove símbolo de moeda e espaços; trata padrão pt-BR (1.234,56) ou ponto decimal (45.50)
  const sanitized = trimmed.replace(/[R$\s]/g, '')
  const normalized = sanitized.includes(',')
    ? sanitized.replace(/\./g, '').replace(',', '.')
    : sanitized
  const parsed = Number.parseFloat(normalized)
  return Number.isFinite(parsed) ? parsed : Number.NaN
}

export function formatDateShort(isoString: string): string {
  const date = new Date(isoString)
  if (Number.isNaN(date.getTime())) return isoString
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${day}/${month} ${hours}:${minutes}`
}

export function formatDateFull(isoString: string): string {
  const date = new Date(isoString)
  if (Number.isNaN(date.getTime())) return isoString
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = date.getFullYear()
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${day}/${month}/${year} às ${hours}:${minutes}`
}

export function formatDateRelative(isoString: string): string {
  const date = new Date(isoString)
  if (Number.isNaN(date.getTime())) return isoString
  const now = new Date()
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')

  const isSameDay = (a: Date, b: Date) =>
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()

  if (isSameDay(date, now)) {
    return `Hoje, ${hours}:${minutes}`
  }

  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (isSameDay(date, yesterday)) {
    return `Ontem, ${hours}:${minutes}`
  }

  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  return `${day}/${month}, ${hours}:${minutes}`
}

export function isIsoDateToday(isoString: string): boolean {
  const date = new Date(isoString)
  if (Number.isNaN(date.getTime())) return false
  const now = new Date()
  return (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  )
}

export function isWithinDays(isoString: string, days: number): boolean {
  const date = new Date(isoString)
  if (Number.isNaN(date.getTime())) return false
  const now = new Date()
  if (days === 0) return isIsoDateToday(isoString)
  const cutoff = new Date(now)
  cutoff.setHours(0, 0, 0, 0)
  cutoff.setDate(cutoff.getDate() - days)
  return date >= cutoff
}

export function toIsoLocalDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function getPeriodDateRange(period: string): {
  dataInicio?: string
  dataFim?: string
} {
  if (period === 'Todos') return {}
  const today = new Date()
  const dataFim = toIsoLocalDate(today)

  if (period === 'Hoje') {
    return { dataInicio: dataFim, dataFim }
  }

  const start = new Date(today)
  if (period === '7d') {
    start.setDate(today.getDate() - 7)
  } else if (period === '30d') {
    start.setDate(today.getDate() - 30)
  }

  return { dataInicio: toIsoLocalDate(start), dataFim }
}

export function getCategoryBadgeStyle(hexColor?: string | null) {
  const safeHex =
    hexColor && /^#[0-9A-Fa-f]{6}$/.test(hexColor.trim())
      ? hexColor.trim()
      : '#2BB673'
  return {
    backgroundColor: `${safeHex}22`,
    color: safeHex,
  }
}
