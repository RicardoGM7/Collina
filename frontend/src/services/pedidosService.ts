import { api } from './api'

export type PedidoStatusDTO =
  | 'AGUARDANDO_PAGAMENTO'
  | 'CONFIRMADO'
  | 'PAGO'
  | 'ENTREGUE'
  | 'EXPIRADO'
  | 'CANCELADO'

export interface ItemPedidoDTO {
  id: number
  quantidade: number
  preco: number
  subTotal: number
  produtoId: number
  nomeDoProduto: string
  descricao: string | null
}

export interface PedidoDTO {
  id: number
  data: string
  statusPedido: PedidoStatusDTO
  total: number
  itens: ItemPedidoDTO[]
}

export interface ItemPedidoPayload {
  produtoId: number
  quantidade: number
}

export interface PagamentoPedidoDTO {
  id: number
  data: string
  valor: number
  pedidoId: number
}

export interface ListarPedidosParams {
  status?: PedidoStatusDTO
  dataInicio?: string
  dataFim?: string
}

export const pedidosService = {
  async listar(params?: ListarPedidosParams): Promise<PedidoDTO[]> {
    const { data } = await api.get<PedidoDTO[]>('/api/pedidos', { params })
    return data
  },

  async buscarPorId(id: number): Promise<PedidoDTO> {
    const { data } = await api.get<PedidoDTO>(`/api/pedidos/${id}`)
    return data
  },

  async criar(itens: ItemPedidoPayload[]): Promise<PedidoDTO> {
    const { data } = await api.post<PedidoDTO>('/api/pedidos', { itens })
    return data
  },

  async cancelar(id: number): Promise<PedidoDTO> {
    const { data } = await api.patch<PedidoDTO>(`/api/pedidos/${id}/cancelar`)
    return data
  },

  async entregar(id: number): Promise<PedidoDTO> {
    const { data } = await api.patch<PedidoDTO>(`/api/pedidos/${id}/entregar`)
    return data
  },

  async adicionarItem(
    pedidoId: number,
    payload: ItemPedidoPayload
  ): Promise<ItemPedidoDTO> {
    const { data } = await api.post<ItemPedidoDTO>(
      `/api/pedidos/${pedidoId}/itens`,
      payload
    )
    return data
  },

  async alterarQuantidadeItem(
    pedidoId: number,
    itemId: number,
    novaQuantidade: number
  ): Promise<ItemPedidoDTO> {
    const { data } = await api.patch<ItemPedidoDTO>(
      `/api/pedidos/${pedidoId}/itens/${itemId}`,
      { novaQuantidade }
    )
    return data
  },

  async removerItem(pedidoId: number, itemId: number): Promise<void> {
    await api.delete(`/api/pedidos/${pedidoId}/itens/${itemId}`)
  },

  async pagar(
    pedidoId: number,
    valorPago: number
  ): Promise<PagamentoPedidoDTO> {
    const { data } = await api.post<PagamentoPedidoDTO>(
      `/api/pedidos/${pedidoId}/pagamento`,
      { valorPago }
    )
    return data
  },
}
