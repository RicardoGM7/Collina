import { Building2, Flame, PartyPopper, Trophy, type LucideIcon } from 'lucide-react'
import type { EstadoEspaco, SituacaoBloco, StatusReserva } from '../../services/espacosService'

// Datas trafegam como AAAA-MM-DD. "Hoje" é sempre o de Brasília, igual ao back-end (ReservaRegras.FUSO).
const FUSO = 'America/Sao_Paulo'

// Deve acompanhar o back-end (collina.reservas.max-dias-antecedencia / COLLINA_RESERVAS_MAX_DIAS).
// Só limita o calendário: o back-end é quem decide. Para mudar sem editar código: VITE_RESERVAS_MAX_DIAS no .env.local.
export const MAX_DIAS_ANTECEDENCIA = Number(import.meta.env.VITE_RESERVAS_MAX_DIAS) > 0 ? Number(import.meta.env.VITE_RESERVAS_MAX_DIAS) : 7

export function hojeISO(agora: Date = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: FUSO,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(agora)
}

export function somarDias(iso: string, dias: number): string {
  const [ano, mes, dia] = iso.split('-').map(Number)
  return new Date(Date.UTC(ano, mes - 1, dia + dias)).toISOString().slice(0, 10)
}

export function diasEntre(de: string, ate: string): number {
  const [a1, m1, d1] = de.split('-').map(Number)
  const [a2, m2, d2] = ate.split('-').map(Number)
  return Math.round((Date.UTC(a2, m2 - 1, d2) - Date.UTC(a1, m1 - 1, d1)) / 86_400_000)
}

/** 2026-11-21 -> 21/11/2026 */
export function formatarData(iso: string): string {
  const [ano, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${ano}`
}

/** 2026-11-21 -> sáb, 21/11 */
export function formatarDataComDia(iso: string): string {
  const [ano, mes, dia] = iso.split('-').map(Number)
  const semana = new Intl.DateTimeFormat('pt-BR', { weekday: 'short', timeZone: 'UTC' })
    .format(new Date(Date.UTC(ano, mes - 1, dia)))
    .replace('.', '')
  return `${semana}, ${String(dia).padStart(2, '0')}/${String(mes).padStart(2, '0')}`
}

/** RF-AS09: só dá para cancelar até o dia anterior à data agendada. */
export function podeCancelar(dataUso: string, hoje: string = hojeISO()): boolean {
  return diasEntre(hoje, dataUso) >= 1
}

/** Idade completa na data informada (RF-AS04: a elegibilidade do dependente vale na data de uso). */
export function idadeEm(nascimentoISO: string, dataISO: string): number {
  const [an, mn, dn] = nascimentoISO.split('-').map(Number)
  const [ar, mr, dr] = dataISO.split('-').map(Number)
  let anos = ar - an
  if (mr < mn || (mr === mn && dr < dn)) anos -= 1
  return anos
}

export const IDADE_LIMITE_DEPENDENTE = 21

export const ESTADO_LABEL: Record<EstadoEspaco, string> = {
  ATIVO: 'Ativo',
  EM_MANUTENCAO: 'Em manutenção',
  INATIVO: 'Inativo',
}

export const SITUACAO_LABEL: Record<SituacaoBloco, string> = {
  LIVRE: 'Livre',
  OCUPADO: 'Ocupado',
  TREINO: 'Treino',
  MANUTENCAO: 'Manutenção',
}

export const STATUS_RESERVA_LABEL: Record<StatusReserva, string> = {
  CONFIRMADA: 'Confirmada',
  CANCELADA: 'Cancelada',
}

/** Soma 2 horas a um HH:mm (blocos têm duração fixa de 2h). Vazio se passar de 23:59. */
export function maisDuasHoras(hhmm: string): string {
  const [h, m] = hhmm.split(':').map(Number)
  if (Number.isNaN(h) || Number.isNaN(m) || h + 2 > 23) return ''
  return `${String(h + 2).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

/** 2026-11-21 -> { semana: 'sáb', dia: '21', mes: 'nov' } (usado nos seletores de data e nos cartões de reserva) */
export function partesDaData(iso: string): { semana: string; dia: string; mes: string } {
  const [ano, mes, dia] = iso.split('-').map(Number)
  const d = new Date(Date.UTC(ano, mes - 1, dia))
  const limpar = (s: string) => s.replace('.', '')
  return {
    semana: limpar(new Intl.DateTimeFormat('pt-BR', { weekday: 'short', timeZone: 'UTC' }).format(d)),
    dia: String(dia).padStart(2, '0'),
    mes: limpar(new Intl.DateTimeFormat('pt-BR', { month: 'short', timeZone: 'UTC' }).format(d)),
  }
}

/** Lista de datas ISO de hoje até hoje + dias (inclusive). */
export function proximosDias(hoje: string, dias: number): string[] {
  return Array.from({ length: dias + 1 }, (_, i) => somarDias(hoje, i))
}

export const NOMES_MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro']

/** Soma meses a um 'AAAA-MM' (aceita valores negativos e atravessa anos). */
export function somarMeses(anoMes: string, meses: number): string {
  const [ano, mes] = anoMes.split('-').map(Number)
  const total = ano * 12 + (mes - 1) + meses
  return `${Math.floor(total / 12)}-${String((total % 12) + 1).padStart(2, '0')}`
}

/** Células de um mês para grade de calendário (domingo primeiro): null = espaço em branco antes do dia 1. */
export function celulasDoMes(anoMes: string): (string | null)[] {
  const [ano, mes] = anoMes.split('-').map(Number)
  const inicio = new Date(Date.UTC(ano, mes - 1, 1)).getUTCDay()
  const total = new Date(Date.UTC(ano, mes, 0)).getUTCDate()
  const celulas: (string | null)[] = Array(inicio).fill(null)
  for (let d = 1; d <= total; d++) celulas.push(`${anoMes}-${String(d).padStart(2, '0')}`)
  return celulas
}

/** Ícone sugerido pelo nome do espaço (quadras e campo, churrasqueira, salão). Só decoração. */
export function iconeDoEspaco(nome: string): LucideIcon {
  const n = nome.toLowerCase()
  if (n.includes('churras') || n.includes('quiosque')) return Flame
  if (n.includes('salão') || n.includes('salao') || n.includes('festa')) return PartyPopper
  if (n.includes('quadra') || n.includes('campo') || n.includes('tênis') || n.includes('tenis') || n.includes('society')) return Trophy
  return Building2
}

/** Classe CSS de cada situação de bloco (usada nos horários e na agenda). */
export const SITUACAO_CLASSE: Record<SituacaoBloco, string> = {
  LIVRE: 'livre',
  OCUPADO: 'ocupado',
  TREINO: 'treino',
  MANUTENCAO: 'manut',
}

/** Rótulo curto de convidados: "Sem convidados", "1 convidado", "4 convidados". */
export function rotuloConvidados(qtd: number): string {
  if (qtd === 0) return 'Sem convidados'
  return `${qtd} convidado${qtd > 1 ? 's' : ''}`
}
