import {
  AlertCircle,
  ArrowRight,
  ChevronLeft,
  Info,
  Minus,
  Package,
  Plus,
  Search,
  ShoppingBag,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Modal } from '../../components/Modal/Modal'
import { pedidoService } from '../../services/pedidoService'
import type { PedidoCriado, Produto } from '../../types/associado'
import { formatarMoeda, mensagemErroApi } from './associadoUtils'
import './AssociadoPedidosPage.css'

interface ProdutoComEstoque extends Produto {
  estoqueDisponivel: number
}

function classeCategoria(nome: string): string {
  const norm = nome
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
  if (norm.includes('quadra')) return 'cat-quadras'
  if (norm.includes('aquat') || norm.includes('piscin')) return 'cat-aquatico'
  if (norm.includes('lancho') || norm.includes('bebid')) return 'cat-lanchonete'
  if (norm.includes('recre')) return 'cat-recreacao'
  return 'cat-lanchonete'
}

export function AssociadoPedidosPage() {
  const [abaAtiva, setAbaAtiva] = useState<'produtos' | 'pedidos'>('produtos')
  const [categoriaAtiva, setCategoriaAtiva] = useState<string>('Todos')
  const [termoBusca, setTermoBusca] = useState('')

  const [produtos, setProdutos] = useState<ProdutoComEstoque[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erroCarga, setErroCarga] = useState('')

  const [carrinho, setCarrinho] = useState<Record<number, number>>({})
  const [enviandoPedido, setEnviandoPedido] = useState(false)
  const [erroPedido, setErroPedido] = useState('')

  // Pedidos criados nesta sessão (o modelo Pedido do back-end ainda não tem campo de associado)
  const [pedidosSessao, setPedidosSessao] = useState<PedidoCriado[]>([])
  const [pedidoCriadoModal, setPedidoCriadoModal] =
    useState<PedidoCriado | null>(null)
  const [detalhePedidoModal, setDetalhePedidoModal] =
    useState<PedidoCriado | null>(null)

  async function carregarCatalogo() {
    setCarregando(true)
    setErroCarga('')
    try {
      const [listaProdutos, listaEstoques] = await Promise.all([
        pedidoService.listarProdutos(),
        pedidoService.listarEstoques(),
      ])

      const mapaEstoque = new Map<number, number>()
      listaEstoques.forEach((e) => {
        if (e.produtoId != null) {
          mapaEstoque.set(e.produtoId, e.quantidade ?? 0)
        }
      })

      setProdutos(
        listaProdutos.map((p) => ({
          ...p,
          estoqueDisponivel: mapaEstoque.get(p.id) ?? 0,
        })),
      )
    } catch (e) {
      setErroCarga(
        mensagemErroApi(e, 'Não foi possível carregar o catálogo de produtos.'),
      )
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => {
    void carregarCatalogo()
  }, [])

  const categorias = useMemo(() => {
    const nomes = new Set<string>()
    produtos.forEach((p) => p.categorias.forEach((c) => nomes.add(c.nome)))
    return ['Todos', ...Array.from(nomes).sort()]
  }, [produtos])

  const produtosFiltrados = useMemo(() => {
    const buscaLower = termoBusca.trim().toLowerCase()
    return produtos.filter((p) => {
      const matchCat =
        categoriaAtiva === 'Todos' ||
        p.categorias.some((c) => c.nome === categoriaAtiva)
      const matchBusca =
        !buscaLower ||
        p.nome.toLowerCase().includes(buscaLower) ||
        (p.descricao ?? '').toLowerCase().includes(buscaLower)
      return matchCat && matchBusca
    })
  }, [produtos, categoriaAtiva, termoBusca])

  function alterarQuantidade(produtoId: number, delta: number) {
    setErroPedido('')
    setCarrinho((prev) => {
      const atual = prev[produtoId] ?? 0
      const nova = atual + delta
      const produto = produtos.find((p) => p.id === produtoId)
      const estoque = produto?.estoqueDisponivel ?? 0

      if (nova <= 0) {
        const copy = { ...prev }
        delete copy[produtoId]
        return copy
      }
      if (nova > estoque) return prev
      return { ...prev, [produtoId]: nova }
    })
  }

  const itensCarrinho = useMemo(() => {
    return Object.entries(carrinho)
      .map(([idStr, qty]) => {
        const product = produtos.find((p) => p.id === Number(idStr))
        if (!product) return null
        return { product, quantity: qty }
      })
      .filter((it): it is { product: ProdutoComEstoque; quantity: number } =>
        Boolean(it),
      )
  }, [carrinho, produtos])

  const totalCarrinho = itensCarrinho.reduce(
    (acc, item) => acc + item.product.preco * item.quantity,
    0,
  )

  const totalItensQtd = itensCarrinho.reduce(
    (acc, item) => acc + item.quantity,
    0,
  )

  async function handleCriarPedido() {
    if (itensCarrinho.length === 0) return

    setEnviandoPedido(true)
    setErroPedido('')
    try {
      const criado = await pedidoService.criarPedido(
        itensCarrinho.map((it) => ({
          produtoId: it.product.id,
          quantidade: it.quantity,
        })),
      )
      setPedidosSessao((prev) => [criado, ...prev])
      setPedidoCriadoModal(criado)
      setCarrinho({})
      void carregarCatalogo()
    } catch (e) {
      setErroPedido(
        mensagemErroApi(e, 'Não foi possível criar o pedido. Tente novamente.'),
      )
    } finally {
      setEnviandoPedido(false)
    }
  }

  return (
    <div className="associado-pedidos-page">
      {/* Top Breadcrumb & Switcher */}
      <div className="pedidos-top-bar">
        <div className="pedidos-breadcrumb-left">
          <Link to="/associado" className="pedidos-back-btn">
            <ChevronLeft size={16} />
            <span>Voltar ao Início</span>
          </Link>
          <div className="pedidos-breadcrumb-text">
            <span>Visão geral</span>
            <span className="crumb-sep">&gt;</span>
            <span className="crumb-curr">Pedidos</span>
          </div>
        </div>

        <div className="pedidos-tab-switcher">
          <button
            type="button"
            className={`pedidos-tab-pill ${abaAtiva === 'produtos' ? 'active' : ''}`}
            onClick={() => setAbaAtiva('produtos')}
          >
            Produtos
          </button>
          <button
            type="button"
            className={`pedidos-tab-pill ${abaAtiva === 'pedidos' ? 'active' : ''}`}
            onClick={() => setAbaAtiva('pedidos')}
          >
            Meus Pedidos
            {pedidosSessao.length > 0 ? ` (${pedidosSessao.length})` : ''}
          </button>
        </div>
      </div>

      {/* ABA PRODUTOS */}
      {abaAtiva === 'produtos' && (
        <div className="pedidos-main-grid">
          {/* Coluna Esquerda: Produtos Disponíveis */}
          <div className="pedidos-catalog-card">
            <div className="catalog-header">
              <div className="catalog-title-group">
                <h2 className="catalog-title">Produtos disponíveis</h2>
                <span className="catalog-count-badge">
                  {produtosFiltrados.length}{' '}
                  {produtosFiltrados.length === 1 ? 'item' : 'itens'}
                </span>
              </div>

              <div className="catalog-search-wrap">
                <Search size={16} className="search-icon" />
                <input
                  type="text"
                  placeholder="Buscar produto..."
                  value={termoBusca}
                  onChange={(e) => setTermoBusca(e.target.value)}
                  className="catalog-search-input"
                />
              </div>
            </div>

            {/* Categorias vindas do back-end */}
            {categorias.length > 1 && (
              <div className="catalog-categories-row">
                {categorias.map((cat) => (
                  <button
                    key={cat}
                    type="button"
                    className={`catalog-cat-pill ${categoriaAtiva === cat ? 'active' : ''}`}
                    onClick={() => setCategoriaAtiva(cat)}
                  >
                    {cat}
                  </button>
                ))}
              </div>
            )}

            {erroCarga && (
              <div className="profile-alert error">
                <AlertCircle size={16} />
                <span>{erroCarga}</span>
              </div>
            )}

            {/* Lista de produtos reais */}
            <div className="catalog-products-list">
              {carregando ? (
                <div className="empty-catalog-message">
                  Carregando produtos do estoque...
                </div>
              ) : produtosFiltrados.length > 0 ? (
                produtosFiltrados.map((prod) => {
                  const qtyNoCarrinho = carrinho[prod.id] ?? 0
                  const semEstoque = prod.estoqueDisponivel <= 0

                  return (
                    <div key={prod.id} className="product-row-card">
                      <div className="product-info-col">
                        <div className="product-name-cat-row">
                          <span className="product-name">{prod.nome}</span>
                          {prod.categorias.map((cat) => (
                            <span
                              key={cat.id}
                              className={`product-cat-tag ${classeCategoria(cat.nome)}`}
                            >
                              {cat.nome}
                            </span>
                          ))}
                        </div>
                        {prod.descricao && (
                          <span className="product-desc">{prod.descricao}</span>
                        )}
                      </div>

                      <div className="product-price-col">
                        <span className="product-price">
                          {formatarMoeda(prod.preco)}
                        </span>
                        <span className="product-stock">
                          {semEstoque
                            ? 'Sem estoque'
                            : `${prod.estoqueDisponivel} disponíveis`}
                        </span>
                      </div>

                      <div className="product-actions-col">
                        {qtyNoCarrinho > 0 ? (
                          <div className="quantity-counter">
                            <button
                              type="button"
                              className="qty-btn"
                              onClick={() => alterarQuantidade(prod.id, -1)}
                            >
                              <Minus size={14} />
                            </button>
                            <span className="qty-number">{qtyNoCarrinho}</span>
                            <button
                              type="button"
                              className="qty-btn"
                              onClick={() => alterarQuantidade(prod.id, 1)}
                              disabled={qtyNoCarrinho >= prod.estoqueDisponivel}
                            >
                              <Plus size={14} />
                            </button>
                          </div>
                        ) : (
                          <button
                            type="button"
                            className="btn-add-product"
                            disabled={semEstoque}
                            onClick={() => alterarQuantidade(prod.id, 1)}
                          >
                            {semEstoque ? 'Indisponível' : '+ Adicionar'}
                          </button>
                        )}
                      </div>
                    </div>
                  )
                })
              ) : (
                <div className="empty-catalog-message">
                  Nenhum produto cadastrado ou encontrado no estoque do clube.
                </div>
              )}
            </div>
          </div>

          {/* Coluna Direita: Meu Pedido */}
          <div className="pedidos-cart-card">
            <div className="cart-header">
              <div className="cart-title-row">
                <ShoppingBag size={18} className="cart-icon" />
                <h3 className="cart-title">Meu Pedido</h3>
              </div>
              <span className="cart-items-count-badge">
                {totalItensQtd} {totalItensQtd === 1 ? 'item' : 'itens'}
              </span>
            </div>

            <div className="cart-warning-banner">
              <Info size={16} className="warning-banner-icon" />
              <p className="warning-banner-text">
                Monte seu pedido aqui. O pagamento é feito somente no caixa
                informando o número gerado.
              </p>
            </div>

            {erroPedido && (
              <div className="profile-alert error">
                <AlertCircle size={16} />
                <span>{erroPedido}</span>
              </div>
            )}

            <div className="cart-items-list">
              {itensCarrinho.map((item) => (
                <div key={item.product.id} className="cart-item-row">
                  <div className="cart-item-left">
                    <span className="cart-item-name">{item.product.nome}</span>
                    <span className="cart-item-calc">
                      {item.quantity}× {formatarMoeda(item.product.preco)}
                    </span>
                  </div>

                  <div className="cart-item-right">
                    <div className="quantity-counter compact">
                      <button
                        type="button"
                        className="qty-btn"
                        onClick={() => alterarQuantidade(item.product.id, -1)}
                      >
                        <Minus size={12} />
                      </button>
                      <span className="qty-number">{item.quantity}</span>
                      <button
                        type="button"
                        className="qty-btn"
                        onClick={() => alterarQuantidade(item.product.id, 1)}
                        disabled={
                          item.quantity >= item.product.estoqueDisponivel
                        }
                      >
                        <Plus size={12} />
                      </button>
                    </div>

                    <span className="cart-item-subtotal">
                      {formatarMoeda(item.product.preco * item.quantity)}
                    </span>
                  </div>
                </div>
              ))}

              {itensCarrinho.length === 0 && (
                <div className="cart-empty-state">
                  <Package size={28} className="empty-icon" />
                  <p>Seu pedido está vazio.</p>
                  <span>Adicione produtos ao lado para criar um pedido.</span>
                </div>
              )}
            </div>

            <div className="cart-footer">
              <div className="cart-total-row">
                <span className="cart-total-label">Total a pagar no caixa</span>
                <span className="cart-total-value">
                  {formatarMoeda(totalCarrinho)}
                </span>
              </div>

              <button
                type="button"
                className="btn-create-order"
                disabled={itensCarrinho.length === 0 || enviandoPedido}
                onClick={() => void handleCriarPedido()}
              >
                <span>{enviandoPedido ? 'Criando...' : 'Criar pedido'}</span>
                <ArrowRight size={18} strokeWidth={2.5} />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ABA MEUS PEDIDOS */}
      {abaAtiva === 'pedidos' && (
        <div className="pedidos-history-view">
          <div className="history-header">
            <h2 className="history-title">Meus Pedidos</h2>
            <p className="history-sub">
              Pedidos gerados nesta sessão para pagamento no caixa
            </p>
          </div>

          <div className="cart-warning-banner">
            <Info size={16} className="warning-banner-icon" />
            <p className="warning-banner-text">
              O vínculo de histórico persistente de pedidos por associado ainda
              está em desenvolvimento no módulo de Estoque/Pedidos do back-end.
              Abaixo aparecem os pedidos criados nesta sessão.
            </p>
          </div>

          <div className="history-list">
            {pedidosSessao.length > 0 ? (
              pedidosSessao.map((ped) => {
                const isAguardando = ped.statusPedido === 'AGUARDANDO_PAGAMENTO'
                return (
                  <div key={ped.id} className="history-order-card">
                    <div className="order-card-header">
                      <div className="order-card-id-date">
                        <span className="order-number-tag">
                          #{String(ped.id).padStart(4, '0')}
                        </span>
                        <span className="order-date-text">
                          {new Date(ped.data).toLocaleString('pt-BR')}
                        </span>
                      </div>

                      <span
                        className={`order-status-badge ${isAguardando ? 'status-waiting' : 'status-paid'}`}
                      >
                        {isAguardando
                          ? 'Aguardando Pagamento no Caixa'
                          : ped.statusPedido}
                      </span>
                    </div>

                    <div className="order-card-items-summary">
                      {ped.itens.map((it) => (
                        <span key={it.id} className="order-summary-item-pill">
                          {it.quantidade}× {it.nomeDoProduto}
                        </span>
                      ))}
                    </div>

                    <div className="order-card-footer">
                      <div className="order-total-group">
                        <span className="order-total-label">Valor Total:</span>
                        <span className="order-total-val">
                          {formatarMoeda(ped.total)}
                        </span>
                      </div>

                      <button
                        type="button"
                        className="btn-order-details"
                        onClick={() => setDetalhePedidoModal(ped)}
                      >
                        Ver detalhes
                      </button>
                    </div>
                  </div>
                )
              })
            ) : (
              <div className="empty-catalog-message">
                Nenhum pedido criado nesta sessão. Monte um pedido na aba
                Produtos.
              </div>
            )}
          </div>
        </div>
      )}

      {/* Modal de Pedido Criado com Sucesso */}
      {pedidoCriadoModal && (
        <Modal
          isOpen={!!pedidoCriadoModal}
          onClose={() => setPedidoCriadoModal(null)}
          title="Pedido Criado com Sucesso!"
          subtitle="Apresente este número no caixa do clube para efetuar o pagamento"
          maxWidth={460}
        >
          <div className="pedido-criado-modal-body">
            <div className="pedido-number-big-box">
              <span className="pedido-number-title">Número do Pedido</span>
              <span className="pedido-number-big">
                #{String(pedidoCriadoModal.id).padStart(4, '0')}
              </span>
              <span className="pedido-number-tip">
                Informe este número no caixa para pagar
              </span>
            </div>

            <div className="pedido-items-summary-box">
              <span className="summary-box-title">Itens solicitados:</span>
              {pedidoCriadoModal.itens.map((item) => (
                <div key={item.id} className="summary-item-line">
                  <span>
                    {item.quantidade}× {item.nomeDoProduto}
                  </span>
                  <span>{formatarMoeda(item.subTotal)}</span>
                </div>
              ))}
              <div className="summary-total-line">
                <span>Total a Pagar no Caixa:</span>
                <strong>{formatarMoeda(pedidoCriadoModal.total)}</strong>
              </div>
            </div>

            <div className="pedido-modal-actions">
              <button
                type="button"
                className="btn-create-order"
                onClick={() => {
                  setPedidoCriadoModal(null)
                  setAbaAtiva('pedidos')
                }}
              >
                Ver Meus Pedidos
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* Modal de Detalhes de um Pedido */}
      {detalhePedidoModal && (
        <Modal
          isOpen={!!detalhePedidoModal}
          onClose={() => setDetalhePedidoModal(null)}
          title={`Detalhes do Pedido #${String(detalhePedidoModal.id).padStart(4, '0')}`}
          subtitle={new Date(detalhePedidoModal.data).toLocaleString('pt-BR')}
          maxWidth={460}
        >
          <div className="detalhe-pedido-modal-body">
            <div className="pedido-items-summary-box">
              {detalhePedidoModal.itens.map((it) => (
                <div key={it.id} className="summary-item-line">
                  <span>
                    {it.quantidade}× {it.nomeDoProduto}
                  </span>
                  <span>{formatarMoeda(it.subTotal)}</span>
                </div>
              ))}
              <div className="summary-total-line">
                <span>Total:</span>
                <strong>{formatarMoeda(detalhePedidoModal.total)}</strong>
              </div>
            </div>

            <div className="detalhes-actions">
              <button
                type="button"
                className="profile-btn-cancel"
                onClick={() => setDetalhePedidoModal(null)}
              >
                Fechar
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}
