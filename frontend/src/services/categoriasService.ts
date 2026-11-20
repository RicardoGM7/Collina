import { api } from './api'

export interface CategoriaDTO {
  id: number
  nome: string
  cor: string
}

export interface CategoriaPayload {
  nome: string
  cor: string
}

export const categoriasService = {
  async listar(): Promise<CategoriaDTO[]> {
    const { data } = await api.get<CategoriaDTO[]>('/categorias')
    return data
  },

  async criar(payload: CategoriaPayload): Promise<CategoriaDTO> {
    const { data } = await api.post<CategoriaDTO>('/categorias', payload)
    return data
  },

  async atualizar(id: number, payload: CategoriaPayload): Promise<CategoriaDTO> {
    const { data } = await api.put<CategoriaDTO>(`/categorias/${id}`, payload)
    return data
  },

  async remover(id: number): Promise<void> {
    await api.delete(`/categorias/${id}`)
  },
}
