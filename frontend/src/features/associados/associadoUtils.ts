import axios from 'axios'

export function formatarCpf(cpf: string): string {
  const digits = cpf.replace(/\D/g, '').slice(0, 11)
  if (digits.length <= 3) return digits
  if (digits.length <= 6) return `${digits.slice(0, 3)}.${digits.slice(3)}`
  if (digits.length <= 9)
    return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6)}`
  return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9, 11)}`
}

// Mesmo padrão do CpfValidador do back-end (11 dígitos numéricos, bloqueando sequências repetidas)
export function validarCpf(cpf: string): boolean {
  const clean = cpf.replace(/\D/g, '')
  return clean.length === 11 && !/^(\d)\1{10}$/.test(clean)
}

export function formatarMoeda(valor: number | null | undefined): string {
  return Number(valor ?? 0).toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  })
}

export function calcularIdade(dataNascimento: string): number {
  if (!dataNascimento) return 0
  const [ano, mes, dia] = dataNascimento.split('-').map(Number)
  const hoje = new Date()
  let idade = hoje.getFullYear() - ano
  const m = hoje.getMonth() + 1 - mes
  if (m < 0 || (m === 0 && hoje.getDate() < dia)) idade--
  return idade
}

export function formatarDataBr(dataIso: string | null | undefined): string {
  if (!dataIso) return '—'
  const [ano, mes, dia] = dataIso.slice(0, 10).split('-')
  return dia && mes && ano ? `${dia}/${mes}/${ano}` : dataIso
}

export function primeiroNome(nome: string | null | undefined): string {
  return (nome ?? '').trim().split(/\s+/)[0] ?? ''
}

// Extrai a mensagem do ApiError do back-end ({ message, fieldErrors })
export function mensagemErroApi(erro: unknown, padrao = 'Não foi possível concluir a operação.'): string {
  if (axios.isAxiosError(erro)) {
    const data = erro.response?.data as
      | { message?: string; fieldErrors?: Record<string, string> }
      | undefined
    if (data?.fieldErrors) {
      const primeiro = Object.values(data.fieldErrors)[0]
      if (primeiro) return primeiro
    }
    if (data?.message) return data.message
    if (!erro.response) return 'Não foi possível conectar ao servidor.'
  }
  if (erro instanceof Error && erro.message) return erro.message
  return padrao
}

export function statusHttp(erro: unknown): number | undefined {
  return axios.isAxiosError(erro) ? erro.response?.status : undefined
}
