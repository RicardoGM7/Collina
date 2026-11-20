import { api } from './api'
import type { CategoriaDTO } from './categoriasService'

export interface ProdutoDTO {
  id: number
  nome: string
  descricao: string | null
  preco: number
  categorias: CategoriaDTO[]
}

export interface ProdutoPayload {
  nome: string
  descricao: string | null
  preco: number
  categoriaIds: number[]
  estoqueMinimo: number
  quantidadeInicial?: number
}

export const produtosService = {
  async listar(): Promise<ProdutoDTO[]> {
    const { data } = await api.get<ProdutoDTO[]>('/produtos')
    return data
  },

  async buscarPorId(id: number): Promise<ProdutoDTO> {
    const { data } = await api.get<ProdutoDTO>(`/produtos/${id}`)
    return data
  },

  async criar(payload: ProdutoPayload): Promise<ProdutoDTO> {
    const { data } = await api.post<ProdutoDTO>('/produtos', payload)
    return data
  },

  async atualizar(id: number, payload: ProdutoPayload): Promise<ProdutoDTO> {
    const { data } = await api.put<ProdutoDTO>(`/produtos/${id}`, payload)
    return data
  },

  async remover(id: number): Promise<void> {
    await api.delete(`/produtos/${id}`)
  },
}
