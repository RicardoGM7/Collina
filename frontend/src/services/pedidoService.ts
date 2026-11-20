// Catálogo e criação de pedidos (módulo de estoque) usados pelo portal do associado.
import { api } from './api'
import type { EstoqueProduto, PedidoCriado, Produto } from '../types/associado'

export const pedidoService = {
  async listarProdutos(): Promise<Produto[]> {
    const { data } = await api.get<Produto[]>('/api/produtos')
    return data.map((p) => ({ ...p, preco: Number(p.preco), categorias: p.categorias ?? [] }))
  },

  async listarEstoques(): Promise<EstoqueProduto[]> {
    const { data } = await api.get<EstoqueProduto[]>('/api/estoques')
    return data
  },

  // RF-E01: o pedido nasce AGUARDANDO_PAGAMENTO e é pago no caixa informando o número
  async criarPedido(itens: { produtoId: number; quantidade: number }[]): Promise<PedidoCriado> {
    const { data } = await api.post<PedidoCriado>('/api/pedidos', { itens })
    return { ...data, total: Number(data.total) }
  },
}
