import { isAxiosError } from 'axios'
import type {
  ApiError,
  CategoriaAssociado,
  MotivoInativacao,
  SituacaoCadastral,
  StatusVinculo,
  TipoPlano,
} from './types'

export const IDADE_LIMITE_DEPENDENTE = 21

export const TIPO_PLANO_LABEL: Record<TipoPlano, string> = {
  BASICO: 'Básico',
  PLUS: 'Plus',
  PREMIUM: 'Premium',
  PROPRIETARIO: 'Proprietário',
}

export const CATEGORIA_LABEL: Record<CategoriaAssociado, string> = {
  PROPRIETARIO: 'Proprietário',
  CONTRIBUINTE: 'Contribuinte',
}

export const SITUACAO_LABEL: Record<SituacaoCadastral, string> = {
  ATIVO: 'Ativo',
  SUSPENSO: 'Suspenso',
  INATIVO: 'Inativo',
}

export const STATUS_VINCULO_LABEL: Record<StatusVinculo, string> = {
  ATIVO: 'Ativo',
  INATIVO: 'Inativo',
}

export const MOTIVO_LABEL: Record<MotivoInativacao, string> = {
  IDADE: 'Atingiu 21 anos',
  INATIVACAO_TITULAR: 'Titular inativado',
  SOLICITACAO: 'Solicitação',
}

// RF-AS02: Contribuinte -> Básico/Plus/Premium; Proprietário -> Proprietário
export const PLANOS_POR_CATEGORIA: Record<CategoriaAssociado, TipoPlano[]> = {
  CONTRIBUINTE: ['BASICO', 'PLUS', 'PREMIUM'],
  PROPRIETARIO: ['PROPRIETARIO'],
}

export function somenteDigitos(valor: string): string {
  return valor.replace(/\D/g, '')
}

export function formatarCpf(cpf: string): string {
  return cpf.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4')
}

// Datas chegam como yyyy-MM-dd; formatamos pela string para evitar problema de fuso horário
export function formatarData(iso?: string | null): string {
  return iso ? iso.split('-').reverse().join('/') : '—'
}

export function formatarMoeda(valor: number): string {
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export function hojeISO(): string {
  const d = new Date()
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}

export function idadeEm(nascimentoISO: string, referencia: Date = new Date()): number {
  const [ano, mes, dia] = nascimentoISO.split('-').map(Number)
  let idade = referencia.getFullYear() - ano
  const mesAtual = referencia.getMonth() + 1
  if (mesAtual < mes || (mesAtual === mes && referencia.getDate() < dia)) {
    idade--
  }
  return idade
}

// Extrai uma mensagem legível de qualquer erro (resposta da API ou falha de conexão)
export function mensagemDeErro(erro: unknown): string {
  if (isAxiosError<ApiError>(erro)) {
    if (!erro.response) {
      return 'Não foi possível conectar ao servidor. Verifique se o backend está rodando em http://localhost:8080.'
    }
    const dados = erro.response.data
    if (dados?.fieldErrors && Object.keys(dados.fieldErrors).length > 0) {
      return Object.values(dados.fieldErrors).join(' ')
    }
    if (dados?.message) {
      return dados.message
    }
    return `Erro ${erro.response.status} ao processar a requisição.`
  }
  return erro instanceof Error ? erro.message : 'Ocorreu um erro inesperado.'
}
