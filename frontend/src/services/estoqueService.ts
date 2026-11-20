import { api } from './api'

export type MovimentacaoTipo =
  | 'COMPRA_FORNECEDOR'
  | 'VENDA'
  | 'USO_INTERNO'
  | 'PERDA_DANO'

export interface EstoqueDTO {
  id: number
  quantidade: number
  estoqueMinimo: number
  produtoId: number
  nomeProduto: string
}

export interface MovimentacaoEstoqueDTO {
  id: number
  produtoId: number
  nomeProduto: string
  data: string
  tipo: MovimentacaoTipo
  quantidade: number
  observacao: string | null
}

export interface MovimentacaoPayload {
  produtoId: number
  tipo: MovimentacaoTipo
  quantidade: number
  observacao?: string | null
}

export const estoqueService = {
  async listar(): Promise<EstoqueDTO[]> {
    const { data } = await api.get<EstoqueDTO[]>('/api/estoques')
    return data
  },

  async listarBaixo(): Promise<EstoqueDTO[]> {
    const { data } = await api.get<EstoqueDTO[]>('/api/estoques/baixo')
    return data
  },

  async atualizarMinimo(
    produtoId: number,
    novoMinimo: number
  ): Promise<EstoqueDTO> {
    const { data } = await api.patch<EstoqueDTO>(
      `/api/estoques/produto/${produtoId}/minimo`,
      { novoMinimo }
    )
    return data
  },

  async registrarMovimentacao(
    payload: MovimentacaoPayload
  ): Promise<MovimentacaoEstoqueDTO> {
    const { data } = await api.post<MovimentacaoEstoqueDTO>(
      '/api/estoques/movimentacoes',
      payload
    )
    return data
  },

  async listarMovimentacoes(
    tipo?: MovimentacaoTipo
  ): Promise<MovimentacaoEstoqueDTO[]> {
    const { data } = await api.get<MovimentacaoEstoqueDTO[]>(
      '/api/estoques/movimentacoes',
      { params: tipo ? { tipo } : undefined }
    )
    return data
  },

  async listarMovimentacoesPorProduto(
    produtoId: number
  ): Promise<MovimentacaoEstoqueDTO[]> {
    const { data } = await api.get<MovimentacaoEstoqueDTO[]>(
      `/api/estoques/produto/${produtoId}/movimentacoes`
    )
    return data
  },
}
