import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  AlertCircle,
  ArrowRight,
  Check,
  ChevronLeft,
  ChevronRight,
  Search,
  ShoppingCart,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import { Modal } from '../../components/Modal/Modal'
import { useAuth } from '../../context/AuthContext'
import { getApiErrorMessage } from '../../services/api'
import {
  categoriasService,
  type CategoriaDTO,
} from '../../services/categoriasService'
import { estoqueService, type EstoqueDTO } from '../../services/estoqueService'
import {
  pedidosService,
  type PedidoDTO,
  type PedidoStatusDTO,
} from '../../services/pedidosService'
import { produtosService, type ProdutoDTO } from '../../services/produtosService'
import {
  formatCurrency,
  formatDateRelative,
  getCategoryBadgeStyle,
} from '../../utils/formatters'
import './AssociadoPedidosPage.css'

interface CatalogProduct extends ProdutoDTO {
  disponivel: number
}

const ORDER_STATUS_META: Record<
  PedidoStatusDTO,
  {
    label: string
    className: string
    note: string
    noteTone: 'muted' | 'success' | 'danger'
    strikeTotal?: boolean
  }
> = {
  AGUARDANDO_PAGAMENTO: {
    label: 'AGUARDANDO PAGAMENTO',
    className: 'assoc-order-badge--aguardando',
    note: 'Pagamento pendente no caixa',
    noteTone: 'muted',
  },
  CONFIRMADO: {
    label: 'CONFIRMADO',
    className: 'assoc-order-badge--confirmado',
    note: 'Pedido confirmado no balcão',
    noteTone: 'muted',
  },
  PAGO: {
    label: 'PAGO',
    className: 'assoc-order-badge--pago',
    note: 'Pago — pronto para retirada no balcão',
    noteTone: 'success',
  },
  ENTREGUE: {
    label: 'ENTREGUE',
    className: 'assoc-order-badge--confirmado',
    note: 'Pago e retirado no balcão',
    noteTone: 'success',
  },
  CANCELADO: {
    label: 'CANCELADO',
    className: 'assoc-order-badge--cancelado',
    note: 'Pedido cancelado',
    noteTone: 'danger',
  },
  EXPIRADO: {
    label: 'EXPIRADO',
    className: 'assoc-order-badge--expirado',
    note: 'Prazo de retirada expirado',
    noteTone: 'muted',
    strikeTotal: true,
  },
}

export function AssociadoPedidosPage() {
  const { usuario } = useAuth()
  const storageKey = `collina_meus_pedidos_${usuario?.uid ?? 'anon'}`

  const [activeTab, setActiveTab] = useState<'produtos' | 'pedidos'>('produtos')
  const [selectedCategoryId, setSelectedCategoryId] = useState<'all' | number>(
    'all'
  )
  const [searchQuery, setSearchQuery] = useState('')

  const [categories, setCategories] = useState<CategoriaDTO[]>([])
  const [products, setProducts] = useState<CatalogProduct[]>([])
  const [myOrders, setMyOrders] = useState<PedidoDTO[]>(() => {
    try {
      const saved = sessionStorage.getItem(storageKey)
      return saved ? (JSON.parse(saved) as PedidoDTO[]) : []
    } catch {
      return []
    }
  })
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const [cart, setCart] = useState<Record<number, number>>({})

  // Modal de pedido criado com sucesso
  const [isSuccessModalOpen, setIsSuccessModalOpen] = useState(false)
  const [lastCreatedOrder, setLastCreatedOrder] = useState<PedidoDTO | null>(
    null
  )

  // Modal ao clicar em "Pagar" num pedido AGUARDANDO_PAGAMENTO em Meus Pedidos
  const [isPayInfoModalOpen, setIsPayInfoModalOpen] = useState(false)
  const [selectedPayOrder, setSelectedPayOrder] = useState<PedidoDTO | null>(
    null
  )

  const loadData = useCallback(async (showErrorToast = true) => {
    try {
      // GET /api/pedidos é restrito a ADMIN/ESTOQUE no SecurityConfig (pedidos ainda não possuem dono no backend).
      // Por isso o associado consulta apenas catálogo + saldo, e mantém seus pedidos da sessão localmente.
      const [cats, prods, estoques] = await Promise.all([
        categoriasService.listar(),
        produtosService.listar(),
        estoqueService.listar(),
      ])

      const estoqueMap = new Map<number, EstoqueDTO>()
      for (const est of estoques) {
        estoqueMap.set(est.produtoId, est)
      }

      const mergedProducts: CatalogProduct[] = prods.map((p) => ({
        ...p,
        preco: Number(p.preco),
        disponivel: estoqueMap.get(p.id)?.quantidade ?? 0,
      }))

      setCategories(cats)
      setProducts(mergedProducts)
    } catch (error) {
      if (showErrorToast) {
        toast.error(
          getApiErrorMessage(error, 'Erro ao carregar os produtos disponíveis.')
        )
      }
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadData()
  }, [loadData])

  const filteredProducts = useMemo(() => {
    const q = searchQuery.trim().toLowerCase()
    return products.filter((product) => {
      const matchesCat =
        selectedCategoryId === 'all' ||
        product.categorias.some((c) => c.id === selectedCategoryId)
      const matchesSearch =
        !q ||
        product.nome.toLowerCase().includes(q) ||
        (product.descricao ?? '').toLowerCase().includes(q)
      return matchesCat && matchesSearch
    })
  }, [products, selectedCategoryId, searchQuery])

  const cartItems = useMemo(() => {
    return Object.entries(cart)
      .map(([idStr, qty]) => {
        const prod = products.find((p) => p.id === Number(idStr))
        if (!prod || qty <= 0) return null
        return {
          produtoId: prod.id,
          nome: prod.nome,
          preco: prod.preco,
          quantidade: qty,
        }
      })
      .filter((item): item is NonNullable<typeof item> => item !== null)
  }, [cart, products])

  const totalItemsCount = useMemo(
    () => cartItems.reduce((sum, item) => sum + item.quantidade, 0),
    [cartItems]
  )

  const cartTotal = useMemo(
    () =>
      cartItems.reduce((sum, item) => sum + item.preco * item.quantidade, 0),
    [cartItems]
  )

  const handleChangeQty = (productId: number, delta: number) => {
    const product = products.find((p) => p.id === productId)
    if (!product) return

    setCart((prev) => {
      const current = prev[productId] ?? 0
      const target = current + delta
      if (target > product.disponivel) {
        toast.error(
          `Quantidade máxima disponível para "${product.nome}": ${product.disponivel}.`
        )
        return prev
      }
      if (target <= 0) {
        const copy = { ...prev }
        delete copy[productId]
        return copy
      }
      return { ...prev, [productId]: target }
    })
  }

  const handleCreateOrder = async () => {
    if (cartItems.length === 0) return

    setIsSubmitting(true)
    try {
      const created = await pedidosService.criar(
        cartItems.map((i) => ({
          produtoId: i.produtoId,
          quantidade: i.quantidade,
        }))
      )

      setLastCreatedOrder(created)
      setMyOrders((prev) => {
        const updated = [created, ...prev.filter((p) => p.id !== created.id)]
        try {
          sessionStorage.setItem(storageKey, JSON.stringify(updated))
        } catch {
          // ignora erro de quota do sessionStorage
        }
        return updated
      })
      setCart({})
      setIsSuccessModalOpen(true)
      toast.success(`Pedido #${created.id} criado com sucesso!`)
      await loadData(false)
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Não foi possível criar o pedido.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="assoc-pedidos-page">
      {/* 1. Barra superior: Voltar + Breadcrumb + Abas */}
      <div className="assoc-pedidos-topbar">
        <div className="assoc-pedidos-top-left">
          <Link to="/associado" className="assoc-pedidos-btn-back">
            <ChevronLeft size={14} strokeWidth={2.5} />
            <span>Voltar ao Início</span>
          </Link>

          <div className="assoc-pedidos-breadcrumb">
            <span>Visão geral</span>
            <ChevronRight size={14} />
            <span className="assoc-pedidos-breadcrumb-active">Pedidos</span>
          </div>
        </div>

        <div className="assoc-pedidos-seg-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'produtos'}
            className={`assoc-seg-btn ${
              activeTab === 'produtos' ? 'active' : ''
            }`}
            onClick={() => setActiveTab('produtos')}
          >
            Produtos
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === 'pedidos'}
            className={`assoc-seg-btn ${
              activeTab === 'pedidos' ? 'active' : ''
            }`}
            onClick={() => setActiveTab('pedidos')}
          >
            Meus Pedidos ({myOrders.length})
          </button>
        </div>
      </div>

      {/* =========================================================
          ABA 1: PRODUTOS DISPONÍVEIS + MEU PEDIDO
         ========================================================= */}
      {activeTab === 'produtos' ? (
        <div className="assoc-pedidos-split">
          {/* Cardápio Unificado (sem ícones por produto) */}
          <section className="assoc-catalog-card">
            <div className="assoc-catalog-head">
              <div className="assoc-catalog-head-top">
                <div className="assoc-catalog-title-group">
                  <h2 className="assoc-catalog-title">Produtos disponíveis</h2>
                  <span className="assoc-catalog-count">
                    {filteredProducts.length}{' '}
                    {filteredProducts.length === 1 ? 'item' : 'itens'}
                  </span>
                </div>

                <div className="assoc-catalog-search">
                  <Search size={13} className="assoc-catalog-search-icon" />
                  <input
                    type="text"
                    placeholder="Buscar produto..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="assoc-catalog-search-input"
                  />
                </div>
              </div>

              <div className="assoc-category-chips">
                <button
                  type="button"
                  className={`assoc-cat-chip ${
                    selectedCategoryId === 'all' ? 'active' : ''
                  }`}
                  onClick={() => setSelectedCategoryId('all')}
                >
                  Todos
                </button>
                {categories.map((category) => (
                  <button
                    key={category.id}
                    type="button"
                    className={`assoc-cat-chip ${
                      selectedCategoryId === category.id ? 'active' : ''
                    }`}
                    onClick={() => setSelectedCategoryId(category.id)}
                  >
                    {category.nome}
                  </button>
                ))}
              </div>
            </div>

            <div className="assoc-products-list">
              {isLoading ? (
                <div className="assoc-empty-products">
                  Carregando catálogo do clube...
                </div>
              ) : filteredProducts.length === 0 ? (
                <div className="assoc-empty-products">
                  Nenhum produto encontrado para essa busca.
                </div>
              ) : (
                filteredProducts.map((product) => {
                  const qtyInCart = cart[product.id] ?? 0
                  const isOutOfStock = product.disponivel <= 0

                  return (
                    <div key={product.id} className="assoc-product-row">
                      <div className="assoc-product-info">
                        <div className="assoc-product-title-line">
                          <strong className="assoc-product-name">
                            {product.nome}
                          </strong>
                          {product.categorias.map((cat) => (
                            <span
                              key={cat.id}
                              className="assoc-cat-badge"
                              style={getCategoryBadgeStyle(cat.cor)}
                            >
                              {cat.nome}
                            </span>
                          ))}
                        </div>
                        {product.descricao && (
                          <span className="assoc-product-desc">
                            {product.descricao}
                          </span>
                        )}
                      </div>

                      <div className="assoc-product-right">
                        <div className="assoc-product-price-col">
                          <strong className="assoc-product-price">
                            {formatCurrency(product.preco)}
                          </strong>
                          <span className="assoc-product-stock">
                            {isOutOfStock
                              ? 'Indisponível'
                              : `${product.disponivel} ${product.disponivel === 1 ? 'disponível' : 'disponíveis'}`}
                          </span>
                        </div>

                        {qtyInCart > 0 ? (
                          <div className="assoc-dark-stepper">
                            <button
                              type="button"
                              onClick={() => handleChangeQty(product.id, -1)}
                              aria-label={`Diminuir ${product.nome}`}
                            >
                              −
                            </button>
                            <span>{qtyInCart}</span>
                            <button
                              type="button"
                              onClick={() => handleChangeQty(product.id, 1)}
                              aria-label={`Aumentar ${product.nome}`}
                            >
                              +
                            </button>
                          </div>
                        ) : (
                          <button
                            type="button"
                            className="assoc-btn-add-pill"
                            disabled={isOutOfStock}
                            onClick={() => handleChangeQty(product.id, 1)}
                          >
                            {isOutOfStock ? 'Esgotado' : '+ Adicionar'}
                          </button>
                        )}
                      </div>
                    </div>
                  )
                })
              )}
            </div>
          </section>

          {/* Painel Lateral: Meu Pedido */}
          <aside className="assoc-cart-card">
            <div className="assoc-cart-header">
              <div className="assoc-cart-header-title">
                <ShoppingCart
                  size={16}
                  strokeWidth={2.4}
                  className="assoc-cart-icon"
                />
                <span>Meu Pedido</span>
              </div>
              <span className="assoc-cart-badge">
                {totalItemsCount} {totalItemsCount === 1 ? 'item' : 'itens'}
              </span>
            </div>

            <div className="assoc-cart-body">
              <div className="assoc-caixa-notice">
                <AlertCircle
                  size={15}
                  strokeWidth={2.2}
                  className="assoc-caixa-notice-icon"
                />
                <span>
                  Monte seu pedido aqui. O{' '}
                  <strong>pagamento é feito somente no caixa</strong> informando
                  o número gerado.
                </span>
              </div>

              <div className="assoc-cart-items">
                {cartItems.length === 0 ? (
                  <div className="assoc-cart-empty">
                    Seu pedido está vazio.
                    <br />
                    Adicione produtos da lista ao lado.
                  </div>
                ) : (
                  cartItems.map((item) => (
                    <div key={item.produtoId} className="assoc-cart-item">
                      <div className="assoc-cart-item-meta">
                        <strong className="assoc-cart-item-name">
                          {item.nome}
                        </strong>
                        <span className="assoc-cart-item-unit">
                          {item.quantidade}× {formatCurrency(item.preco)}
                        </span>
                      </div>

                      <div className="assoc-cart-item-right">
                        <div className="assoc-light-stepper">
                          <button
                            type="button"
                            onClick={() => handleChangeQty(item.produtoId, -1)}
                            aria-label={`Diminuir ${item.nome}`}
                          >
                            −
                          </button>
                          <span>{item.quantidade}</span>
                          <button
                            type="button"
                            onClick={() => handleChangeQty(item.produtoId, 1)}
                            aria-label={`Aumentar ${item.nome}`}
                          >
                            +
                          </button>
                        </div>

                        <strong className="assoc-cart-item-subtotal">
                          {formatCurrency(item.preco * item.quantidade)}
                        </strong>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="assoc-cart-footer">
              <div className="assoc-cart-total-row">
                <span className="assoc-cart-total-label">
                  Total a pagar no caixa
                </span>
                <strong className="assoc-cart-total-value">
                  {formatCurrency(cartTotal)}
                </strong>
              </div>

              <button
                type="button"
                className="assoc-btn-create-order"
                disabled={cartItems.length === 0 || isSubmitting}
                onClick={() => void handleCreateOrder()}
              >
                <span>{isSubmitting ? 'Criando pedido...' : 'Criar pedido'}</span>
                <ArrowRight size={15} strokeWidth={2.5} />
              </button>
            </div>
          </aside>
        </div>
      ) : (
        /* =========================================================
           ABA 2: MEUS PEDIDOS (CARDS EM GRID 2 COLUNAS)
          ========================================================= */
        <section className="assoc-orders-section">
          <div className="assoc-orders-header">
            <h2 className="assoc-orders-title">Pedidos do Clube</h2>
            <p className="assoc-orders-subtitle">
              Acompanhe seus pedidos e apresente o código no caixa para pagar
            </p>
          </div>

          {isLoading ? (
            <div className="assoc-empty-products">Carregando pedidos...</div>
          ) : myOrders.length === 0 ? (
            <div className="assoc-empty-products">
              Você ainda não possui pedidos registrados.
            </div>
          ) : (
            <div className="assoc-orders-grid">
              {myOrders.map((order) => {
                const statusMeta = ORDER_STATUS_META[order.statusPedido]
                const resumoItens = order.itens
                  .map((i) => `${i.quantidade}× ${i.nomeDoProduto}`)
                  .join(', ')

                return (
                  <div key={order.id} className="assoc-order-card">
                    <div className="assoc-order-card-top">
                      <div className="assoc-order-id-group">
                        <strong className="assoc-order-id">
                          Pedido #{order.id}
                        </strong>
                        <span className="assoc-order-date">
                          {formatDateRelative(order.data)}
                        </span>
                      </div>
                      <span
                        className={`assoc-order-badge ${statusMeta.className}`}
                      >
                        {statusMeta.label}
                      </span>
                    </div>

                    <p className="assoc-order-items-text">{resumoItens}</p>

                    <div className="assoc-order-card-bottom">
                      <span
                        className={`assoc-order-note assoc-order-note--${statusMeta.noteTone}`}
                      >
                        {statusMeta.note}
                      </span>
                      <div className="assoc-order-bottom-right">
                        <strong
                          className={`assoc-order-total ${
                            statusMeta.strikeTotal
                              ? 'assoc-order-total--strike'
                              : ''
                          } ${
                            order.statusPedido === 'CANCELADO'
                              ? 'assoc-order-total--muted'
                              : ''
                          }`}
                        >
                          {formatCurrency(Number(order.total ?? 0))}
                        </strong>
                        {order.statusPedido === 'AGUARDANDO_PAGAMENTO' && (
                          <button
                            type="button"
                            className="assoc-btn-pay-card"
                            onClick={() => {
                              setSelectedPayOrder(order)
                              setIsPayInfoModalOpen(true)
                            }}
                          >
                            Pagar
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </section>
      )}

      {/* =========================================================
          MODAL: PEDIDO CRIADO COM SUCESSO (CÓDIGO PARA O CAIXA)
         ========================================================= */}
      <Modal
        isOpen={isSuccessModalOpen && lastCreatedOrder !== null}
        onClose={() => setIsSuccessModalOpen(false)}
        title="Pedido criado com sucesso!"
        hideHeader
        maxWidth={430}
      >
        {lastCreatedOrder && (
          <div className="assoc-confirm-modal">
            <div className="assoc-confirm-check">
              <Check size={28} strokeWidth={2.8} />
            </div>

            <div className="assoc-confirm-head">
              <h3 className="assoc-confirm-title">
                Pedido criado com sucesso!
              </h3>
              <span className="assoc-confirm-subtitle">
                Seu pedido foi enviado para o balcão do clube
              </span>
            </div>

            <span className="assoc-order-badge assoc-order-badge--aguardando">
              AGUARDANDO PAGAMENTO
            </span>

            <div className="assoc-ticket-pass">
              <span className="assoc-ticket-label">
                SEU CÓDIGO PARA O CAIXA
              </span>
              <strong className="assoc-ticket-code">
                #{lastCreatedOrder.id}
              </strong>
              <p className="assoc-ticket-note">
                Apresente este código no caixa para efetuar o pagamento e
                retirar seus itens.
              </p>
            </div>

            <div className="assoc-confirm-summary">
              {lastCreatedOrder.itens.map((item) => (
                <div key={item.id} className="assoc-confirm-line">
                  <span>
                    {item.quantidade}× {item.nomeDoProduto}
                  </span>
                  <strong>{formatCurrency(Number(item.subTotal))}</strong>
                </div>
              ))}
              <div className="assoc-confirm-divider" />
              <div className="assoc-confirm-total">
                <span>Total a pagar no caixa</span>
                <strong>{formatCurrency(Number(lastCreatedOrder.total))}</strong>
              </div>
            </div>

            <div className="assoc-confirm-actions">
              <button
                type="button"
                className="assoc-btn-modal-outline"
                onClick={() => setIsSuccessModalOpen(false)}
              >
                Continuar
              </button>
              <button
                type="button"
                className="assoc-btn-modal-green"
                onClick={() => {
                  setIsSuccessModalOpen(false)
                  setActiveTab('pedidos')
                }}
              >
                Ver Meus Pedidos
              </button>
            </div>
          </div>
        )}
      </Modal>

      {/* =========================================================
          MODAL: PAGAR PEDIDO (EXIBE CÓDIGO PARA O CAIXA + ITENS)
         ========================================================= */}
      <Modal
        isOpen={isPayInfoModalOpen && selectedPayOrder !== null}
        onClose={() => setIsPayInfoModalOpen(false)}
        title={
          selectedPayOrder
            ? `Código para o Caixa — Pedido #${selectedPayOrder.id}`
            : 'Código para o Caixa'
        }
        hideHeader
        maxWidth={420}
      >
        {selectedPayOrder && (
          <div className="assoc-confirm-modal">
            <div className="assoc-ticket-pass">
              <span className="assoc-ticket-label">
                SEU CÓDIGO PARA O CAIXA
              </span>
              <strong className="assoc-ticket-code">
                #{selectedPayOrder.id}
              </strong>
              <p className="assoc-ticket-note">
                Apresente este código no caixa para efetuar o pagamento e
                retirar seus itens.
              </p>
            </div>

            <div className="assoc-confirm-summary">
              {selectedPayOrder.itens.map((item) => (
                <div key={item.id} className="assoc-confirm-line">
                  <span>
                    {item.quantidade}× {item.nomeDoProduto}
                  </span>
                  <strong>{formatCurrency(Number(item.subTotal))}</strong>
                </div>
              ))}
              <div className="assoc-confirm-divider" />
              <div className="assoc-confirm-total">
                <span>Total a pagar no caixa</span>
                <strong>{formatCurrency(Number(selectedPayOrder.total))}</strong>
              </div>
            </div>

            <div className="assoc-confirm-actions">
              <button
                type="button"
                className="assoc-btn-modal-outline"
                onClick={() => setIsPayInfoModalOpen(false)}
              >
                Fechar
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
