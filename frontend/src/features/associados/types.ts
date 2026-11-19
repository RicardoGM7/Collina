// Tipos espelhando os DTOs do backend (pacote dto.associados)

export type CategoriaAssociado = 'PROPRIETARIO' | 'CONTRIBUINTE'
export type TipoPlano = 'BASICO' | 'PLUS' | 'PREMIUM' | 'PROPRIETARIO'
export type SituacaoCadastral = 'ATIVO' | 'SUSPENSO' | 'INATIVO'
export type StatusVinculo = 'ATIVO' | 'INATIVO'
export type MotivoInativacao = 'IDADE' | 'INATIVACAO_TITULAR' | 'SOLICITACAO'

export interface Pessoa {
  id: number
  nome: string
  cpf: string
  dataNascimento: string // yyyy-MM-dd
}

export interface Plano {
  id: number
  tipo: TipoPlano
  valorPeriodico: number
  limiteMensalReservas: number
  maxDependentesAtivos: number
}

export interface Associado {
  id: number
  pessoa: Pessoa
  categoria: CategoriaAssociado
  plano: Plano
  situacaoCadastral: SituacaoCadastral
  dataAdmissao: string | null
  usuarioId: string | null
}

export interface VinculoDependencia {
  id: number
  titularId: number
  dependente: Pessoa
  parentesco: string
  dataInicio: string
  dataFim: string | null
  status: StatusVinculo
  motivoInativacao: MotivoInativacao | null
}

export interface AssociadoRequest {
  nome: string
  cpf: string
  dataNascimento: string
  categoria: CategoriaAssociado
  tipoPlano: TipoPlano
}

export interface AssociadoUpdateRequest {
  nome: string
  dataNascimento: string
}

export interface DependenteRequest {
  nome: string
  cpf: string
  dataNascimento: string
  parentesco: string
}

export interface DependenteUpdateRequest {
  nome: string
  dataNascimento: string
  parentesco: string
}

export interface PlanoRequest {
  valorPeriodico: number
  limiteMensalReservas: number
  maxDependentesAtivos: number
}

// Formato de erro devolvido pelo GlobalExceptionHandler
export interface ApiError {
  status: number
  error: string
  message: string
  path: string
  fieldErrors?: Record<string, string>
}
