// Tipagens do módulo de associados, espelhando os DTOs do back-end
// (br.ufs.dcomp.collina.dto.associados). Valores e limites dos planos vêm sempre de /api/planos.

export type CategoriaAssociado = 'CONTRIBUINTE' | 'PROPRIETARIO'

export type TipoPlano = 'BASICO' | 'PLUS' | 'PREMIUM' | 'PROPRIETARIO'

export type SituacaoCadastral = 'ATIVO' | 'SUSPENSO' | 'INATIVO'

export type StatusVinculo = 'ATIVO' | 'INATIVO'

// Mesmos valores de MotivoInativacaoVinculoEnum (o front só exibe; não depende da lista completa)
export type MotivoInativacaoVinculo = string

// PessoaResponseDTO
export interface Pessoa {
  id: number
  nome: string
  cpf: string
  dataNascimento: string
}

// PlanoResponseDTO
export interface Plano {
  id: number
  tipo: TipoPlano
  valorPeriodico: number
  limiteMensalReservas: number
  maxDependentesAtivos: number
}

// AssociadoResponseDTO
export interface Associado {
  id: number
  pessoa: Pessoa
  categoria: CategoriaAssociado
  plano: Plano
  situacaoCadastral: SituacaoCadastral
  dataAdmissao: string
  usuarioId: string | null
}

// VinculoDependenciaResponseDTO
export interface VinculoDependencia {
  id: number
  titularId: number
  dependente: Pessoa
  parentesco: string
  dataInicio: string
  dataFim: string | null
  status: StatusVinculo
  motivoInativacao: MotivoInativacaoVinculo | null
}

// Nomes de exibição (o enum TipoPlanoEnum só tem o identificador; os números vêm do back)
export const PLANO_NOME: Record<TipoPlano, string> = {
  BASICO: 'Contribuinte Básico',
  PLUS: 'Contribuinte Plus',
  PREMIUM: 'Contribuinte Premium',
  PROPRIETARIO: 'Proprietário',
}

// Mesma regra de TipoPlanoEnum.getCategoriaPermitida()
export const CATEGORIA_DO_PLANO: Record<TipoPlano, CategoriaAssociado> = {
  BASICO: 'CONTRIBUINTE',
  PLUS: 'CONTRIBUINTE',
  PREMIUM: 'CONTRIBUINTE',
  PROPRIETARIO: 'PROPRIETARIO',
}

export const CATEGORIA_LABEL: Record<CategoriaAssociado, string> = {
  CONTRIBUINTE: 'Contribuinte',
  PROPRIETARIO: 'Proprietário',
}

export const SITUACAO_LABEL: Record<SituacaoCadastral, string> = {
  ATIVO: 'Ativo',
  SUSPENSO: 'Suspenso',
  INATIVO: 'Inativo',
}

export function nomePlano(plano: Pick<Plano, 'tipo'> | null | undefined): string {
  if (!plano) return '—'
  return `Plano ${PLANO_NOME[plano.tipo] ?? plano.tipo}`
}

// ===== Catálogo de produtos e pedidos (módulo de estoque) =====

// CategoriaResponseDTO
export interface CategoriaProduto {
  id: number
  nome: string
}

// ProdutoResponseDTO
export interface Produto {
  id: number
  nome: string
  descricao: string | null
  preco: number
  categorias: CategoriaProduto[]
}

// EstoqueResponseDTO
export interface EstoqueProduto {
  id: number
  quantidade: number
  estoqueMinimo: number
  produtoId: number
  nomeProduto: string
}

// PedidoResponseDTO
export interface PedidoCriado {
  id: number
  data: string
  statusPedido: string
  total: number
  itens: {
    id: number
    produtoId: number
    nomeDoProduto: string
    quantidade: number
    preco: number
    subTotal: number
  }[]
}
