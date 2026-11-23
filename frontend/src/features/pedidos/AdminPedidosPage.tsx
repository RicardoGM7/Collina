import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Check,
  ChevronLeft,
  ChevronRight,
  Clock,
  DollarSign,
  Plus,
  Search,
  Trash2,
  User,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import { Modal } from '../../components/Modal/Modal'
import { getApiErrorMessage } from '../../services/api'
import { estoqueService, type EstoqueDTO } from '../../services/estoqueService'
import {
  pedidosService,
  type ClientePedidoDTO,
  type ItemPedidoDTO,
  type PedidoDTO,
  type PedidoStatusDTO,
} from '../../services/pedidosService'
import { produtosService } from '../../services/produtosService'
import {
  formatCurrency,
  formatDateFull,
  formatDateShort,
  getPeriodDateRange,
  isIsoDateToday,
  parseCurrencyInput,
} from '../../utils/formatters'
import './AdminPedidosPage.css'

interface CatalogProduct {
  id: number
  nome: string
  preco: number
  disponivel: number
}

interface DraftOrderItem {
  produtoId: number
  nome: string
  preco: number
  quantidade: number
}

type ActiveModalType = 'novo' | 'detalhes' | 'pagar' | null

const STATUS_META: Record<
  PedidoStatusDTO,
  { label: string; className: string; mutedTotal?: boolean }
> = {
  AGUARDANDO_PAGAMENTO: {
    label: 'AGUARDANDO PAGAMENTO',
    className: 'pedidos-badge--aguardando',
  },
  ENTREGUE: {
    label: 'ENTREGUE',
    className: 'pedidos-badge--entregue',
  },
  CONFIRMADO: {
    label: 'CONFIRMADO',
    className: 'pedidos-badge--entregue',
  },
  PAGO: {
    label: 'PAGO',
    className: 'pedidos-badge--pago',
  },
  EXPIRADO: {
    label: 'EXPIRADO',
    className: 'pedidos-badge--expirado',
    mutedTotal: true,
  },
  CANCELADO: {
    label: 'CANCELADO',
    className: 'pedidos-badge--cancelado',
    mutedTotal: true,
  },
}

const ORDERS_PER_PAGE = 6
const DEFAULT_CUSTOMER_LABEL = 'Balcão / Não vinculado'

export function AdminPedidosPage() {
  const [orders, setOrders] = useState<PedidoDTO[]>([])
  const [allOrdersForKpi, setAllOrdersForKpi] = useState<PedidoDTO[]>([])
  const [catalog, setCatalog] = useState<CatalogProduct[]>([])
  const [clientes, setClientes] = useState<ClientePedidoDTO[]>([])

  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const [periodFilter, setPeriodFilter] = useState<'Todos' | 'Hoje' | '7d' | '30d'>('Todos')
  const [searchInput, setSearchInput] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | PedidoStatusDTO>('ALL')
  const [moreStatusSelect, setMoreStatusSelect] = useState<string>('ALL')
  const [currentPage, setCurrentPage] = useState(1)

  const [activeModal, setActiveModal] = useState<ActiveModalType>(null)
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null)
  const lastSelectedOrderRef = useRef<PedidoDTO | null>(null)

  // Estado do Modal 1: Novo Pedido (Balcão)
  const [novoUsuarioId, setNovoUsuarioId] = useState<number | ''>('')
  const [novoProdutoId, setNovoProdutoId] = useState<number | ''>('')
  const [novoQtd, setNovoQtd] = useState<number>(1)
  const [novoItens, setNovoItens] = useState<DraftOrderItem[]>([])

  // Estado do Modal 2: Detalhes / Editar Pedido
  const [detalheProdutoId, setDetalheProdutoId] = useState<number | ''>('')
  const [detalheQtd, setDetalheQtd] = useState<number>(1)

  // Estado do Modal 3: Receber no Balcão
  const [valorPagoInput, setValorPagoInput] = useState<string>('')

  const loadData = useCallback(
    async (showErrorToast = true) => {
      try {
        const range = getPeriodDateRange(periodFilter)
        const [filteredList, fullList, prods, estoques, clientesList] = await Promise.all([
          pedidosService.listar(range),
          periodFilter === 'Todos'
            ? Promise.resolve<PedidoDTO[] | null>(null)
            : pedidosService.listar(),
          produtosService.listar(),
          estoqueService.listar(),
          pedidosService.listarClientes(),
        ])

        const sortedFiltered = [...filteredList].sort((a, b) => b.id - a.id)
        const sortedAll =
          fullList !== null
            ? [...fullList].sort((a, b) => b.id - a.id)
            : sortedFiltered

        const estoqueMap = new Map<number, EstoqueDTO>()
        for (const est of estoques) {
          estoqueMap.set(est.produtoId, est)
        }

        const mergedCatalog: CatalogProduct[] = prods.map((p) => ({
          id: p.id,
          nome: p.nome,
          preco: Number(p.preco),
          disponivel: estoqueMap.get(p.id)?.quantidade ?? 0,
        }))

        setOrders(sortedFiltered)
        setAllOrdersForKpi(sortedAll)
        setCatalog(mergedCatalog)
        setClientes(clientesList)
      } catch (error) {
        if (showErrorToast) {
          toast.error(
            getApiErrorMessage(error, 'Erro ao carregar os pedidos.')
          )
        }
      } finally {
        setIsLoading(false)
      }
    },
    [periodFilter]
  )

  useEffect(() => {
    void loadData()
  }, [loadData])

  const selectedOrder = useMemo<PedidoDTO | null>(() => {
    if (selectedOrderId !== null) {
      const found =
        orders.find((o) => o.id === selectedOrderId) ??
        allOrdersForKpi.find((o) => o.id === selectedOrderId)
      if (found) {
        lastSelectedOrderRef.current = found
        return found
      }
    }
    return lastSelectedOrderRef.current
  }, [orders, allOrdersForKpi, selectedOrderId])

  const getCustomerName = useCallback((order: PedidoDTO) => {
    if (order.usuarioNome?.trim()) return order.usuarioNome.trim()
    if (order.usuarioEmail?.trim()) return order.usuarioEmail.trim()
    return DEFAULT_CUSTOMER_LABEL
  }, [])

  // KPIs reais calculados sobre os pedidos do backend
  const kpis = useMemo(() => {
    const aguardando = allOrdersForKpi.filter(
      (o) => o.statusPedido === 'AGUARDANDO_PAGAMENTO'
    ).length

    const pagosHojeList = allOrdersForKpi.filter(
      (o) =>
        (o.statusPedido === 'PAGO' || o.statusPedido === 'ENTREGUE') &&
        isIsoDateToday(o.data)
    )

    const faturamentoHoje = pagosHojeList.reduce(
      (sum, o) => sum + Number(o.total ?? 0),
      0
    )

    return {
      aguardando,
      pagosHoje: pagosHojeList.length,
      faturamentoHoje,
    }
  }, [allOrdersForKpi])

  const filteredOrders = useMemo(() => {
    const query = appliedSearch.trim().toLowerCase().replace(/^#/, '')
    return orders.filter((order) => {
      const matchesStatus =
        statusFilter === 'ALL' || order.statusPedido === statusFilter
      const customerName = getCustomerName(order).toLowerCase()
      const customerEmail = (order.usuarioEmail ?? '').toLowerCase()
      const matchesQuery =
        !query ||
        String(order.id).includes(query) ||
        customerName.includes(query) ||
        customerEmail.includes(query) ||
        order.itens.some((i) =>
          (i.nomeDoProduto ?? '').toLowerCase().includes(query)
        )
      return matchesStatus && matchesQuery
    })
  }, [orders, statusFilter, appliedSearch, getCustomerName])

  const totalPages = Math.max(
    1,
    Math.ceil(filteredOrders.length / ORDERS_PER_PAGE)
  )
  const safePage = Math.min(currentPage, totalPages)
  const paginatedOrders = useMemo(() => {
    const start = (safePage - 1) * ORDERS_PER_PAGE
    return filteredOrders.slice(start, start + ORDERS_PER_PAGE)
  }, [filteredOrders, safePage])

  const handleSearchSubmit = () => {
    setAppliedSearch(searchInput)
    setCurrentPage(1)
  }

  const handleSelectTab = (status: 'ALL' | PedidoStatusDTO) => {
    setStatusFilter(status)
    setMoreStatusSelect('ALL')
    setCurrentPage(1)
  }

  const handleMoreStatusChange = (value: string) => {
    setMoreStatusSelect(value)
    setStatusFilter(value === 'ALL' ? 'ALL' : (value as PedidoStatusDTO))
    setCurrentPage(1)
  }

  // Abertura dos modais
  const openNovoPedidoModal = () => {
    if (catalog.length === 0) {
      toast.error('Cadastre produtos no estoque antes de criar um pedido.')
      return
    }
    setNovoUsuarioId('')
    const firstAvailable = catalog.find((p) => p.disponivel > 0) ?? catalog[0]
    setNovoProdutoId(firstAvailable.id)
    setNovoQtd(1)
    setNovoItens([])
    setActiveModal('novo')
  }

  const openDetalhesModal = (order: PedidoDTO) => {
    setSelectedOrderId(order.id)
    lastSelectedOrderRef.current = order
    const firstAvailable = catalog.find((p) => p.disponivel > 0) ?? catalog[0]
    setDetalheProdutoId(firstAvailable ? firstAvailable.id : '')
    setDetalheQtd(1)
    setActiveModal('detalhes')
  }

  const openPagarModal = (order: PedidoDTO) => {
    setSelectedOrderId(order.id)
    lastSelectedOrderRef.current = order
    const total = Number(order.total ?? 0)
    setValorPagoInput(total.toFixed(2).replace('.', ','))
    setActiveModal('pagar')
  }

  // Modal 1: Novo Pedido (Balcão)
  const handleAddNovoItem = () => {
    if (novoProdutoId === '') return
    const product = catalog.find((p) => p.id === novoProdutoId)
    if (!product) return
    const qtdValida = Math.max(1, novoQtd || 1)

    const currentQtyInDraft =
      novoItens.find((i) => i.produtoId === product.id)?.quantidade ?? 0
    if (currentQtyInDraft + qtdValida > product.disponivel) {
      toast.error(
        `Estoque insuficiente para "${product.nome}". Disponível: ${product.disponivel}.`
      )
      return
    }

    setNovoItens((prev) => {
      const existing = prev.find((item) => item.produtoId === product.id)
      if (existing) {
        return prev.map((item) =>
          item.produtoId === product.id
            ? { ...item, quantidade: item.quantidade + qtdValida }
            : item
        )
      }
      return [
        ...prev,
        {
          produtoId: product.id,
          nome: product.nome,
          preco: product.preco,
          quantidade: qtdValida,
        },
      ]
    })
    setNovoQtd(1)
  }

  const handleChangeNovoItemQty = (produtoId: number, delta: number) => {
    const product = catalog.find((p) => p.id === produtoId)
    setNovoItens((prev) =>
      prev
        .map((item) => {
          if (item.produtoId !== produtoId) return item
          const next = item.quantidade + delta
          if (product && next > product.disponivel) {
            toast.error(
              `Estoque máximo atingido para "${product.nome}" (${product.disponivel}).`
            )
            return item
          }
          return { ...item, quantidade: next }
        })
        .filter((item) => item.quantidade > 0)
    )
  }

  const handleRemoveNovoItem = (produtoId: number) => {
    setNovoItens((prev) => prev.filter((item) => item.produtoId !== produtoId))
  }

  const handleCreateOrder = async () => {
    if (novoItens.length === 0) {
      toast.error('Adicione pelo menos 1 item ao pedido.')
      return
    }

    setIsSubmitting(true)
    try {
      const created = await pedidosService.criar(
        novoItens.map((i) => ({
          produtoId: i.produtoId,
          quantidade: i.quantidade,
        })),
        novoUsuarioId === '' ? undefined : novoUsuarioId
      )

      toast.success(`Pedido #${created.id} criado com sucesso!`)
      setActiveModal(null)
      await loadData(false)
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Erro ao criar o pedido.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  // Modal 2: Editar Pedido #ID
  const handleAddDetalheItem = async () => {
    if (!selectedOrder || selectedOrder.statusPedido !== 'AGUARDANDO_PAGAMENTO') {
      return
    }
    if (detalheProdutoId === '') return
    const qtdValida = Math.max(1, detalheQtd || 1)

    setIsSubmitting(true)
    try {
      await pedidosService.adicionarItem(selectedOrder.id, {
        produtoId: detalheProdutoId,
        quantidade: qtdValida,
      })
      toast.success('Item adicionado ao pedido!')
      setDetalheQtd(1)
      await loadData(false)
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Erro ao adicionar item.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleChangeDetalheItemQty = async (
    item: ItemPedidoDTO,
    delta: number
  ) => {
    if (!selectedOrder || selectedOrder.statusPedido !== 'AGUARDANDO_PAGAMENTO') {
      return
    }
    const nextQty = item.quantidade + delta

    setIsSubmitting(true)
    try {
      if (nextQty <= 0) {
        await pedidosService.removerItem(selectedOrder.id, item.id)
        toast.success('Item removido do pedido.')
      } else {
        await pedidosService.alterarQuantidadeItem(
          selectedOrder.id,
          item.id,
          nextQty
        )
      }
      await loadData(false)
    } catch (error) {
      toast.error(
        getApiErrorMessage(error, 'Erro ao atualizar quantidade do item.')
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleRemoveDetalheItem = async (item: ItemPedidoDTO) => {
    if (!selectedOrder || selectedOrder.statusPedido !== 'AGUARDANDO_PAGAMENTO') {
      return
    }

    setIsSubmitting(true)
    try {
      await pedidosService.removerItem(selectedOrder.id, item.id)
      toast.success('Item removido do pedido.')
      await loadData(false)
    } catch (error) {
      toast.error(
        getApiErrorMessage(error, 'Não foi possível remover o item do pedido.')
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleCancelOrder = async () => {
    if (!selectedOrder) return

    setIsSubmitting(true)
    try {
      await pedidosService.cancelar(selectedOrder.id)
      toast.success(`Pedido #${selectedOrder.id} cancelado.`)
      setActiveModal(null)
      await loadData(false)
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Erro ao cancelar o pedido.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDeliverOrder = async (orderId: number) => {
    setIsSubmitting(true)
    try {
      await pedidosService.entregar(orderId)
      toast.success(`Pedido #${orderId} marcado como entregue!`)
      await loadData(false)
    } catch (error) {
      toast.error(
        getApiErrorMessage(error, 'Erro ao marcar o pedido como entregue.')
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  // Modal 3: Confirmar Pagamento no Balcão
  const selectedOrderTotal = Number(selectedOrder?.total ?? 0)
  const valorPagoNumero = parseCurrencyInput(valorPagoInput)
  const trocoCalculado =
    !Number.isNaN(valorPagoNumero) && valorPagoNumero > selectedOrderTotal
      ? valorPagoNumero - selectedOrderTotal
      : 0

  const handleConfirmPayment = async () => {
    if (!selectedOrder) return
    if (Number.isNaN(valorPagoNumero) || valorPagoNumero < selectedOrderTotal) {
      toast.error('O valor pago não pode ser menor que o total do pedido.')
      return
    }

    setIsSubmitting(true)
    try {
      await pedidosService.pagar(
        selectedOrder.id,
        Number(valorPagoNumero.toFixed(2))
      )
      toast.success(
        `Pagamento do pedido #${selectedOrder.id} confirmado! Estoque atualizado.`
      )
      setActiveModal(null)
      await loadData(false)
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Erro ao confirmar pagamento.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  const novoTotal = novoItens.reduce(
    (sum, item) => sum + item.preco * item.quantidade,
    0
  )
  const isSelectedEditable =
    selectedOrder?.statusPedido === 'AGUARDANDO_PAGAMENTO'
  const selectedStatusMeta = selectedOrder
    ? STATUS_META[selectedOrder.statusPedido]
    : STATUS_META.AGUARDANDO_PAGAMENTO

  const rangeStart =
    filteredOrders.length === 0 ? 0 : (safePage - 1) * ORDERS_PER_PAGE + 1
  const rangeEnd = Math.min(safePage * ORDERS_PER_PAGE, filteredOrders.length)

  return (
    <div className="admin-pedidos-page">
      {/* 1. Barra superior: Voltar ao Início + Breadcrumb */}
      <div className="pedidos-breadcrumb-bar">
        <Link to="/admin" className="pedidos-btn-back">
          <ChevronLeft size={14} strokeWidth={2.5} />
          <span>Voltar ao Início</span>
        </Link>

        <div className="pedidos-breadcrumb-trail">
          <span>Visão geral</span>
          <ChevronRight size={14} />
          <span className="pedidos-breadcrumb-active">Pedidos</span>
        </div>
      </div>

      {/* 2. Linha dos 3 Cards de KPI */}
      <div className="pedidos-kpi-grid">
        <div className="pedidos-kpi-card pedidos-kpi-amber">
          <div className="pedidos-kpi-info">
            <span className="pedidos-kpi-label">Aguardando pagamento</span>
            <strong className="pedidos-kpi-value">{kpis.aguardando}</strong>
          </div>
          <div className="pedidos-kpi-icon pedidos-kpi-icon--amber">
            <Clock size={20} strokeWidth={2.2} />
          </div>
        </div>

        <div className="pedidos-kpi-card pedidos-kpi-white">
          <div className="pedidos-kpi-info">
            <span className="pedidos-kpi-label">Pagos hoje</span>
            <strong className="pedidos-kpi-value">{kpis.pagosHoje}</strong>
          </div>
          <div className="pedidos-kpi-icon pedidos-kpi-icon--green">
            <Check size={20} strokeWidth={2.4} />
          </div>
        </div>

        <div className="pedidos-kpi-card pedidos-kpi-white">
          <div className="pedidos-kpi-info">
            <span className="pedidos-kpi-label">Faturamento hoje</span>
            <strong className="pedidos-kpi-value">
              {formatCurrency(kpis.faturamentoHoje)}
            </strong>
          </div>
          <div className="pedidos-kpi-icon pedidos-kpi-icon--navy">
            <DollarSign size={20} strokeWidth={2.2} />
          </div>
        </div>
      </div>

      {/* 3. Card Principal: Tabela de Pedidos */}
      <section className="pedidos-main-card">
        <div className="pedidos-card-header">
          <h2 className="pedidos-section-title">Pedidos</h2>

          <div className="pedidos-header-controls">
            <select
              className="pedidos-select"
              value={periodFilter}
              onChange={(e) => {
                setPeriodFilter(
                  e.target.value as 'Todos' | 'Hoje' | '7d' | '30d'
                )
                setCurrentPage(1)
              }}
              aria-label="Filtrar por período"
            >
              <option value="Todos">Todo o período</option>
              <option value="Hoje">Hoje</option>
              <option value="7d">Últimos 7 dias</option>
              <option value="30d">Últimos 30 dias</option>
            </select>

            <div className="pedidos-search-box">
              <Search size={13} className="pedidos-search-icon" />
              <input
                type="text"
                placeholder="Buscar ID ou nome..."
                value={searchInput}
                onChange={(e) => {
                  setSearchInput(e.target.value)
                  setAppliedSearch(e.target.value)
                  setCurrentPage(1)
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleSearchSubmit()
                }}
                className="pedidos-search-input"
              />
            </div>

            <button
              type="button"
              className="pedidos-btn-search"
              onClick={handleSearchSubmit}
            >
              Buscar
            </button>
          </div>
        </div>

        {/* Barra de ações (+ Novo pedido e filtros de status) */}
        <div className="pedidos-toolbar-row">
          <button
            type="button"
            className="pedidos-btn-new"
            onClick={openNovoPedidoModal}
          >
            <Plus size={14} strokeWidth={2.6} />
            <span>Novo pedido</span>
          </button>

          <div className="pedidos-filter-group">
            <div className="pedidos-status-tabs">
              <button
                type="button"
                className={`pedidos-tab-btn ${
                  statusFilter === 'ALL' ? 'active' : ''
                }`}
                onClick={() => handleSelectTab('ALL')}
              >
                Todos ({orders.length})
              </button>
              <button
                type="button"
                className={`pedidos-tab-btn ${
                  statusFilter === 'AGUARDANDO_PAGAMENTO' ? 'active' : ''
                }`}
                onClick={() => handleSelectTab('AGUARDANDO_PAGAMENTO')}
              >
                AGUARDANDO PAGAMENTO
              </button>
              <button
                type="button"
                className={`pedidos-tab-btn ${
                  statusFilter === 'ENTREGUE' ? 'active' : ''
                }`}
                onClick={() => handleSelectTab('ENTREGUE')}
              >
                ENTREGUE
              </button>
              <button
                type="button"
                className={`pedidos-tab-btn ${
                  statusFilter === 'PAGO' ? 'active' : ''
                }`}
                onClick={() => handleSelectTab('PAGO')}
              >
                PAGO
              </button>
            </div>

            <select
              className="pedidos-select pedidos-more-select"
              value={moreStatusSelect}
              onChange={(e) => handleMoreStatusChange(e.target.value)}
              aria-label="Mais status de pedido"
            >
              <option value="ALL">Mais status</option>
              <option value="EXPIRADO">EXPIRADO</option>
              <option value="CANCELADO">CANCELADO</option>
            </select>
          </div>
        </div>

        {/* Tabela de pedidos */}
        <div className="pedidos-table-wrapper">
          <table className="pedidos-table">
            <thead>
              <tr>
                <th>ID / Data</th>
                <th>Nome</th>
                <th className="text-center">Status do Pedido</th>
                <th className="text-right">Total</th>
                <th className="text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={5} className="pedidos-empty-row">
                    Carregando pedidos...
                  </td>
                </tr>
              ) : paginatedOrders.length === 0 ? (
                <tr>
                  <td colSpan={5} className="pedidos-empty-row">
                    Nenhum pedido encontrado para os filtros selecionados.
                  </td>
                </tr>
              ) : (
                paginatedOrders.map((order) => {
                  const meta = STATUS_META[order.statusPedido]
                  const canPay = order.statusPedido === 'AGUARDANDO_PAGAMENTO'
                  const canDeliver = order.statusPedido === 'PAGO'

                  return (
                    <tr key={order.id}>
                      <td>
                        <strong className="pedidos-id">#{order.id}</strong>
                        <span className="pedidos-date">
                          {formatDateShort(order.data)}
                        </span>
                      </td>
                      <td className="pedidos-customer-name">
                        {getCustomerName(order)}
                      </td>
                      <td className="text-center">
                        <span className={`pedidos-badge ${meta.className}`}>
                          {meta.label}
                        </span>
                      </td>
                      <td
                        className={`text-right pedidos-total-cell ${
                          meta.mutedTotal ? 'pedidos-total-cell--muted' : ''
                        }`}
                      >
                        {formatCurrency(Number(order.total ?? 0))}
                      </td>
                      <td className="text-right">
                        <div className="pedidos-row-actions">
                          {canPay && (
                            <button
                              type="button"
                              className="pedidos-btn-pay-sm"
                              disabled={isSubmitting}
                              onClick={() => openPagarModal(order)}
                            >
                              Pagar
                            </button>
                          )}
                          {canDeliver && (
                            <button
                              type="button"
                              className="pedidos-btn-deliver-sm"
                              disabled={isSubmitting}
                              onClick={() => void handleDeliverOrder(order.id)}
                            >
                              Entregar
                            </button>
                          )}
                          <button
                            type="button"
                            className="pedidos-btn-details-sm"
                            onClick={() => openDetalhesModal(order)}
                          >
                            Detalhes
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Rodapé da tabela */}
        <div className="pedidos-table-footer">
          <span>
            {filteredOrders.length === 0
              ? '0 pedidos'
              : `${rangeStart}–${rangeEnd} de ${filteredOrders.length} pedidos`}
          </span>
          <div className="pedidos-pagination">
            <button
              type="button"
              className="pedidos-page-btn"
              disabled={safePage <= 1}
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              aria-label="Página anterior"
            >
              <ChevronLeft size={14} />
            </button>
            <button
              type="button"
              className="pedidos-page-btn"
              disabled={safePage >= totalPages}
              onClick={() =>
                setCurrentPage((p) => Math.min(totalPages, p + 1))
              }
              aria-label="Próxima página"
            >
              <ChevronRight size={14} />
            </button>
          </div>
        </div>
      </section>

      {/* =========================================================
          MODAL 1: NOVO PEDIDO (BALCÃO)
         ========================================================= */}
      <Modal
        isOpen={activeModal === 'novo'}
        onClose={() => setActiveModal(null)}
        title="Novo Pedido (Balcão)"
        subtitle="Adicione os produtos e quantidades do pedido"
        maxWidth={460}
        closeOnOverlayClick={novoItens.length === 0 && novoUsuarioId === ''}
        footer={
          <div className="pedidos-modal-footer-split">
            <div className="pedidos-modal-total">
              <span>Total:</span>
              <strong>{formatCurrency(novoTotal)}</strong>
            </div>
            <div className="pedidos-modal-actions">
              <button
                type="button"
                className="pedidos-btn-secondary"
                disabled={isSubmitting}
                onClick={() => setActiveModal(null)}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="pedidos-btn-primary"
                disabled={isSubmitting || novoItens.length === 0}
                onClick={() => void handleCreateOrder()}
              >
                {isSubmitting ? 'Criando...' : 'Criar pedido'}
              </button>
            </div>
          </div>
        }
      >
        <div className="pedidos-modal-field">
          <label className="pedidos-field-label" htmlFor="novo-pedido-usuario">
            Cliente / Usuário (opcional)
          </label>
          <select
            id="novo-pedido-usuario"
            className="pedidos-text-input"
            value={novoUsuarioId}
            onChange={(e) =>
              setNovoUsuarioId(e.target.value === '' ? '' : Number(e.target.value))
            }
          >
            <option value="">Meu usuário (padrão)</option>
            {clientes.map((cliente) => (
              <option key={cliente.id} value={cliente.id}>
                {cliente.nome?.trim()
                  ? `${cliente.nome.trim()} (${cliente.email})`
                  : cliente.email}
              </option>
            ))}
          </select>
        </div>

        <div className="pedidos-modal-field">
          <label className="pedidos-field-label" htmlFor="novo-pedido-produto">
            Adicionar item *
          </label>
          <div className="pedidos-add-item-row">
            <select
              id="novo-pedido-produto"
              className="pedidos-item-select"
              value={novoProdutoId}
              onChange={(e) => setNovoProdutoId(Number(e.target.value))}
            >
              {catalog.map((p) => (
                <option key={p.id} value={p.id} disabled={p.disponivel <= 0}>
                  {p.nome} — {formatCurrency(p.preco)} (Disp: {p.disponivel})
                </option>
              ))}
            </select>
            <input
              type="number"
              min={1}
              className="pedidos-qty-input"
              value={novoQtd}
              onChange={(e) =>
                setNovoQtd(Math.max(1, Number(e.target.value) || 1))
              }
              aria-label="Quantidade"
            />
            <button
              type="button"
              className="pedidos-btn-dark"
              onClick={handleAddNovoItem}
            >
              + Item
            </button>
          </div>
        </div>

        <div className="pedidos-modal-field">
          <span className="pedidos-field-label">
            Itens no pedido ({novoItens.length})
          </span>
          <div className="pedidos-items-list">
            {novoItens.length === 0 ? (
              <div className="pedidos-empty-items">
                Nenhum item adicionado ainda.
              </div>
            ) : (
              novoItens.map((item) => (
                <div key={item.produtoId} className="pedidos-item-card">
                  <div className="pedidos-item-meta">
                    <strong className="pedidos-item-name">{item.nome}</strong>
                    <span className="pedidos-item-unit">
                      {item.quantidade}× {formatCurrency(item.preco)}
                    </span>
                  </div>
                  <div className="pedidos-item-controls">
                    <div className="pedidos-stepper">
                      <button
                        type="button"
                        onClick={() =>
                          handleChangeNovoItemQty(item.produtoId, -1)
                        }
                        aria-label={`Diminuir ${item.nome}`}
                      >
                        −
                      </button>
                      <span>{item.quantidade}</span>
                      <button
                        type="button"
                        onClick={() =>
                          handleChangeNovoItemQty(item.produtoId, 1)
                        }
                        aria-label={`Aumentar ${item.nome}`}
                      >
                        +
                      </button>
                    </div>
                    <strong className="pedidos-item-subtotal">
                      {formatCurrency(item.preco * item.quantidade)}
                    </strong>
                    <button
                      type="button"
                      className="pedidos-btn-trash"
                      onClick={() => handleRemoveNovoItem(item.produtoId)}
                      aria-label={`Remover ${item.nome}`}
                    >
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </Modal>

      {/* =========================================================
          MODAL 2 & 4: DETALHES DO PEDIDO (EDITÁVEL OU SOMENTE LEITURA)
         ========================================================= */}
      <Modal
        isOpen={activeModal === 'detalhes' && selectedOrder !== null}
        onClose={() => setActiveModal(null)}
        title={selectedOrder ? `Pedido #${selectedOrder.id}` : 'Pedido'}
        headerBadge={
          <span className={`pedidos-badge ${selectedStatusMeta.className}`}>
            {selectedStatusMeta.label}
          </span>
        }
        subtitle="Detalhes do Pedido"
        maxWidth={isSelectedEditable ? 500 : 460}
        footer={
          <div className="pedidos-modal-footer-split">
            <div className="pedidos-modal-total">
              <span>Total:</span>
              <strong>{formatCurrency(selectedOrderTotal)}</strong>
            </div>
            {isSelectedEditable && selectedOrder && (
              <div className="pedidos-modal-actions">
                <button
                  type="button"
                  className="pedidos-btn-danger-soft"
                  disabled={isSubmitting}
                  onClick={() => void handleCancelOrder()}
                >
                  Cancelar pedido
                </button>
                <button
                  type="button"
                  className="pedidos-btn-primary"
                  disabled={isSubmitting}
                  onClick={() => openPagarModal(selectedOrder)}
                >
                  Ir para pagamento
                </button>
              </div>
            )}
          </div>
        }
      >
        {selectedOrder && (
          <>
            <div className="pedidos-info-pills">
              <div className="pedidos-info-pill">
                <User size={14} className="pedidos-info-pill-icon" />
                <span>{getCustomerName(selectedOrder)}</span>
              </div>
              <div className="pedidos-info-pill">
                <Clock size={14} className="pedidos-info-pill-icon" />
                <span>Criado em {formatDateFull(selectedOrder.data)}</span>
              </div>
            </div>

            {isSelectedEditable && (
              <div className="pedidos-modal-field">
                <label
                  className="pedidos-field-label"
                  htmlFor="detalhe-pedido-produto"
                >
                  Adicionar novo item
                </label>
                <div className="pedidos-add-item-row">
                  <select
                    id="detalhe-pedido-produto"
                    className="pedidos-item-select"
                    value={detalheProdutoId}
                    onChange={(e) =>
                      setDetalheProdutoId(Number(e.target.value))
                    }
                  >
                    {catalog.map((p) => (
                      <option
                        key={p.id}
                        value={p.id}
                        disabled={p.disponivel <= 0}
                      >
                        {p.nome} — {formatCurrency(p.preco)} (Disp:{' '}
                        {p.disponivel})
                      </option>
                    ))}
                  </select>
                  <input
                    type="number"
                    min={1}
                    className="pedidos-qty-input"
                    value={detalheQtd}
                    onChange={(e) =>
                      setDetalheQtd(Math.max(1, Number(e.target.value) || 1))
                    }
                    aria-label="Quantidade"
                  />
                  <button
                    type="button"
                    className="pedidos-btn-dark"
                    disabled={isSubmitting || detalheProdutoId === ''}
                    onClick={() => void handleAddDetalheItem()}
                  >
                    + Item
                  </button>
                </div>
              </div>
            )}

            <div className="pedidos-items-list">
              {selectedOrder.itens.map((item) => (
                <div key={item.id} className="pedidos-item-card">
                  <div className="pedidos-item-meta">
                    <strong className="pedidos-item-name">
                      {item.nomeDoProduto}
                    </strong>
                    <span className="pedidos-item-unit">
                      {item.quantidade}× {formatCurrency(Number(item.preco))}
                    </span>
                  </div>

                  {isSelectedEditable ? (
                    <div className="pedidos-item-controls">
                      <div className="pedidos-stepper">
                        <button
                          type="button"
                          disabled={isSubmitting}
                          onClick={() =>
                            void handleChangeDetalheItemQty(item, -1)
                          }
                          aria-label={`Diminuir ${item.nomeDoProduto}`}
                        >
                          −
                        </button>
                        <span>{item.quantidade}</span>
                        <button
                          type="button"
                          disabled={isSubmitting}
                          onClick={() =>
                            void handleChangeDetalheItemQty(item, 1)
                          }
                          aria-label={`Aumentar ${item.nomeDoProduto}`}
                        >
                          +
                        </button>
                      </div>
                      <strong className="pedidos-item-subtotal">
                        {formatCurrency(Number(item.subTotal))}
                      </strong>
                      <button
                        type="button"
                        className="pedidos-btn-trash"
                        disabled={isSubmitting}
                        onClick={() => void handleRemoveDetalheItem(item)}
                        aria-label={`Remover ${item.nomeDoProduto}`}
                      >
                        <Trash2 size={15} />
                      </button>
                    </div>
                  ) : (
                    <strong className="pedidos-item-subtotal">
                      {formatCurrency(Number(item.subTotal))}
                    </strong>
                  )}
                </div>
              ))}
            </div>
          </>
        )}
      </Modal>

      {/* =========================================================
          MODAL 3: RECEBER NO BALCÃO
         ========================================================= */}
      <Modal
        isOpen={activeModal === 'pagar' && selectedOrder !== null}
        onClose={() => setActiveModal(null)}
        title={
          selectedOrder
            ? `Receber no Balcão — #${selectedOrder.id}`
            : 'Receber no Balcão'
        }
        subtitle="Baixa automática no estoque ao confirmar"
        maxWidth={410}
        footer={
          <div className="pedidos-modal-footer-end">
            <button
              type="button"
              className="pedidos-btn-secondary"
              disabled={isSubmitting}
              onClick={() => setActiveModal(null)}
            >
              Voltar
            </button>
            <button
              type="button"
              className="pedidos-btn-primary"
              disabled={isSubmitting}
              onClick={() => void handleConfirmPayment()}
            >
              {isSubmitting ? 'Confirmando...' : 'Confirmar pagamento'}
            </button>
          </div>
        }
      >
        {selectedOrder && (
          <>
            <div className="pedidos-info-pill">
              <User size={14} className="pedidos-info-pill-icon" />
              <span>{getCustomerName(selectedOrder)}</span>
            </div>

            <div className="pedidos-summary-box">
              <span>Total a pagar</span>
              <strong>{formatCurrency(selectedOrderTotal)}</strong>
            </div>

            <div className="pedidos-modal-field">
              <label
                className="pedidos-field-label"
                htmlFor="valor-pago-input"
              >
                Valor pago (R$) *
              </label>
              <input
                id="valor-pago-input"
                type="text"
                className="pedidos-text-input pedidos-money-input"
                value={valorPagoInput}
                onChange={(e) => setValorPagoInput(e.target.value)}
              />
            </div>

            <div className="pedidos-troco-box">
              <span>Troco</span>
              <strong>{formatCurrency(trocoCalculado)}</strong>
            </div>
          </>
        )}
      </Modal>
    </div>
  )
}
