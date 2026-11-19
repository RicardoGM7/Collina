import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  AlertTriangle,
  Archive,
  Check,
  ChevronLeft,
  ChevronRight,
  Clock,
  Pencil,
  Search,
  Tag,
  Trash2,
  TrendingUp,
} from 'lucide-react'
import { Modal } from '../../components/Modal/Modal'
import './EstoquePage.css'

interface ProductItem {
  id: number
  name: string
  description: string
  categoryId: number
  price: string
  qty: number
  min: number
  isLow: boolean
}

interface MovementItem {
  id: number
  productName: string
  type: 'COMPRA_FORNECEDOR' | 'VENDA' | 'USO_INTERNO' | 'PERDA_DANO'
  typeClass: string
  date: string
  obs: string
  delta: string
  isPositive: boolean
}

interface CategoryItem {
  id: number
  name: string
  color: string
  countText: string
}

const INITIAL_CATEGORIES: CategoryItem[] = [
  { id: 1, name: 'Aquático', color: '#0284C7', countText: '· 1 item' },
  { id: 2, name: 'Quadras', color: '#D97706', countText: '· 2 itens' },
  { id: 3, name: 'Lanchonete', color: '#2BB673', countText: '· 1 item' },
  { id: 4, name: 'Recreação', color: '#7E22CE', countText: '· 1 item' },
]

const PRODUCTS: ProductItem[] = [
  {
    id: 1,
    name: 'Boia de piscina',
    description: 'Boia circular infantil reforçada',
    categoryId: 1,
    price: 'R$ 45,00',
    qty: 4,
    min: 5,
    isLow: true,
  },
  {
    id: 2,
    name: 'Bolas de basquete',
    description: 'Tamanho oficial nº 7 indoor/outdoor',
    categoryId: 2,
    price: 'R$ 120,00',
    qty: 1,
    min: 3,
    isLow: true,
  },
  {
    id: 3,
    name: 'Raquetes de tênis',
    description: 'Locação para sócios nas quadras de saibro',
    categoryId: 2,
    price: 'R$ 280,00',
    qty: 2,
    min: 2,
    isLow: false,
  },
  {
    id: 4,
    name: 'Arma de água',
    description: 'Brinquedo recreativo para área infantil',
    categoryId: 4,
    price: 'R$ 35,00',
    qty: 5,
    min: 2,
    isLow: false,
  },
]

const MOVEMENTS: MovementItem[] = [
  {
    id: 1,
    productName: 'Boia de piscina',
    type: 'COMPRA_FORNECEDOR',
    typeClass: 'mov-compra',
    date: '24/09 09:12',
    obs: 'Obs: NF 4021 — Reposição piscina',
    delta: '+10',
    isPositive: true,
  },
  {
    id: 2,
    productName: 'Bolas de basquete',
    type: 'VENDA',
    typeClass: 'mov-venda',
    date: '23/09 15:40',
    obs: 'Obs: Baixa automática Pedido #103',
    delta: '-2',
    isPositive: false,
  },
  {
    id: 3,
    productName: 'Boia de piscina',
    type: 'USO_INTERNO',
    typeClass: 'mov-uso',
    date: '18/09 14:00',
    obs: 'Obs: Aula de natação infantil',
    delta: '-2',
    isPositive: false,
  },
  {
    id: 4,
    productName: 'Raquetes de tênis',
    type: 'PERDA_DANO',
    typeClass: 'mov-perda',
    date: '12/09 16:45',
    obs: 'Obs: Encordoamento rompido na quadra 2',
    delta: '-1',
    isPositive: false,
  },
  {
    id: 5,
    productName: 'Coca-Cola 350ml',
    type: 'VENDA',
    typeClass: 'mov-venda',
    date: '11/09 19:20',
    obs: 'Obs: Baixa automática Pedido #98',
    delta: '-6',
    isPositive: false,
  },
  {
    id: 6,
    productName: 'Arma de água',
    type: 'COMPRA_FORNECEDOR',
    typeClass: 'mov-compra',
    date: '08/09 10:05',
    obs: 'Obs: NF 3980 — Área recreativa',
    delta: '+5',
    isPositive: true,
  },
]

const PRESET_COLORS = [
  '#0284C7',
  '#D97706',
  '#2BB673',
  '#7E22CE',
  '#E11D48',
  '#0D9488',
  '#4F46E5',
  '#0B1724',
]

const ENUM_OPTIONS = [
  { id: 'COMPRA_FORNECEDOR', label: 'Compra', sign: '(+)' },
  { id: 'VENDA', label: 'Venda', sign: '(-)' },
  { id: 'USO_INTERNO', label: 'Uso interno', sign: '(-)' },
  { id: 'PERDA_DANO', label: 'Perda / Dano', sign: '(-)' },
] as const

type ActiveModal =
  | null
  | 'produto'
  | 'movimentacao'
  | 'categorias'
  | 'editar-categoria'
  | 'historico'

/**
 * Gera o fundo suave (14% de opacidade) e a cor do texto da badge na tabela
 * a partir de qualquer cor hexadecimal escolhida no frontend.
 */
function getCategoryBadgeStyle(hexColor: string) {
  return {
    backgroundColor: `${hexColor}22`,
    color: hexColor,
  }
}

export function EstoquePage() {
  const [search, setSearch] = useState('')
  const [selectedCatId, setSelectedCatId] = useState<string>('all')
  const [onlyLow, setOnlyLow] = useState(false)
  const [movFilter, setMovFilter] = useState<string>('ALL')
  const [histSearch, setHistSearch] = useState('')
  const [activeModal, setActiveModal] = useState<ActiveModal>(null)

  // Estado do Modal Produto
  const [prodName, setProdName] = useState('Boia de piscina')
  const [prodPrice, setProdPrice] = useState('45,00')
  const [prodMin, setProdMin] = useState('5')
  const [prodDesc, setProdDesc] = useState('')
  const [selectedModalCats, setSelectedModalCats] = useState<number[]>([1])
  const [isProductDirty, setIsProductDirty] = useState(false)

  // Estado do Modal Movimentação
  const [selectedMovType, setSelectedMovType] =
    useState<string>('COMPRA_FORNECEDOR')
  const [movQty, setMovQty] = useState('10')
  const [movObs, setMovObs] = useState('')
  const [isMovDirty, setIsMovDirty] = useState(false)

  // Estado do Modal Histórico
  const [historyProductName, setHistoryProductName] =
    useState('Boia de piscina')

  // Estado das categorias compartilhado com a tabela geral
  const [categories, setCategories] =
    useState<CategoryItem[]>(INITIAL_CATEGORIES)
  const [newCatName, setNewCatName] = useState('')
  const [newCatColor, setNewCatColor] = useState('#2BB673')
  const [showNewCatPalette, setShowNewCatPalette] = useState(false)
  const [editingCatId, setEditingCatId] = useState<number | null>(null)
  const [editCatName, setEditCatName] = useState('')
  const [editCatColor, setEditCatColor] = useState('#0284C7')
  const [isEditCatDirty, setIsEditCatDirty] = useState(false)

  function getCategoryById(id: number): CategoryItem {
    return (
      categories.find((c) => c.id === id) ?? {
        id: 0,
        name: 'Sem categoria',
        color: '#64748B',
        countText: '',
      }
    )
  }

  const filteredProducts = PRODUCTS.filter((p) => {
    const matchesName = p.name.toLowerCase().includes(search.toLowerCase())
    const matchesCat =
      selectedCatId === 'all' || String(p.categoryId) === selectedCatId
    const matchesLow = !onlyLow || p.isLow
    return matchesName && matchesCat && matchesLow
  })

  const filteredMovements = MOVEMENTS.filter((m) => {
    const matchesType = movFilter === 'ALL' || m.type === movFilter
    const matchesQuery =
      m.productName.toLowerCase().includes(histSearch.toLowerCase()) ||
      m.obs.toLowerCase().includes(histSearch.toLowerCase())
    return matchesType && matchesQuery
  })

  function openProductModal(product?: ProductItem) {
    setProdName(product?.name ?? 'Boia de piscina')
    setProdPrice(product ? product.price.replace('R$ ', '') : '45,00')
    setProdMin(product ? String(product.min) : '5')
    setProdDesc(product?.description ?? '')
    setSelectedModalCats(product ? [product.categoryId] : [1])
    setIsProductDirty(false)
    setActiveModal('produto')
  }

  function openMovementModal(defaultType: string) {
    setSelectedMovType(defaultType)
    setMovQty('10')
    setMovObs('')
    setIsMovDirty(false)
    setActiveModal('movimentacao')
  }

  function openHistoryModal(name: string) {
    setHistoryProductName(name)
    setActiveModal('historico')
  }

  function toggleModalCategory(catId: number) {
    setIsProductDirty(true)
    setSelectedModalCats((prev) =>
      prev.includes(catId)
        ? prev.filter((id) => id !== catId)
        : [...prev, catId],
    )
  }

  function handleAddCategory() {
    const trimmed = newCatName.trim()
    if (!trimmed) return
    setCategories((prev) => [
      ...prev,
      {
        id: Date.now(),
        name: trimmed,
        color: newCatColor,
        countText: '· 0 itens',
      },
    ])
    setNewCatName('')
    setShowNewCatPalette(false)
  }

  function handleOpenEditCategory(cat: CategoryItem) {
    setEditingCatId(cat.id)
    setEditCatName(cat.name)
    setEditCatColor(cat.color)
    setIsEditCatDirty(false)
    setActiveModal('editar-categoria')
  }

  function handleSaveEditCategory() {
    const trimmed = editCatName.trim()
    if (!trimmed || editingCatId === null) return
    setCategories((prev) =>
      prev.map((cat) =>
        cat.id === editingCatId
          ? { ...cat, name: trimmed, color: editCatColor }
          : cat,
      ),
    )
    setIsEditCatDirty(false)
    setActiveModal('categorias')
  }

  function handleDeleteCategory(id: number) {
    setCategories((prev) => prev.filter((cat) => cat.id !== id))
  }

  return (
    <div className="estoque-page">
      {/* 1. Barra Voltar ao Início + Breadcrumb */}
      <div className="estoque-breadcrumb-bar">
        <Link to="/admin" className="btn-back-home">
          <ChevronLeft size={14} color="#2BB673" strokeWidth={2.8} />
          <span>Voltar ao Início</span>
        </Link>

        <div className="breadcrumb-trail">
          <span>Visão geral</span>
          <ChevronRight size={14} color="#94A3B8" />
          <span className="breadcrumb-active">Estoque</span>
        </div>
      </div>

      {/* 2. Linha de 3 KPIs */}
      <div className="estoque-kpi-grid">
        <div className="kpi-card kpi-white">
          <div className="kpi-info">
            <span className="kpi-label">Itens cadastrados</span>
            <span className="kpi-value">5</span>
          </div>
          <div className="kpi-icon-box kpi-icon-green">
            <Archive size={20} />
          </div>
        </div>

        <button
          type="button"
          className="kpi-card kpi-danger"
          onClick={() => setOnlyLow((prev) => !prev)}
          title="Filtrar itens abaixo do mínimo"
        >
          <div className="kpi-info">
            <span className="kpi-label">Abaixo do mínimo</span>
            <span className="kpi-value">2</span>
          </div>
          <div className="kpi-icon-box kpi-icon-red">
            <AlertTriangle size={20} />
          </div>
        </button>

        <div className="kpi-card kpi-white">
          <div className="kpi-info">
            <span className="kpi-label">Movimentações hoje</span>
            <span className="kpi-value">1</span>
          </div>
          <div className="kpi-icon-box kpi-icon-amber">
            <TrendingUp size={20} />
          </div>
        </div>
      </div>

      {/* 3. Card Principal: Produtos em estoque */}
      <section className="estoque-main-card">
        <div className="estoque-card-header">
          <h2 className="estoque-section-title">Produtos em estoque</h2>

          <div className="estoque-header-filters">
            <select
              className="estoque-select"
              value={selectedCatId}
              onChange={(e) => setSelectedCatId(e.target.value)}
            >
              <option value="all">Todas categorias</option>
              {categories.map((cat) => (
                <option key={cat.id} value={String(cat.id)}>
                  {cat.name}
                </option>
              ))}
            </select>

            <div className="estoque-search-wrap">
              <Search size={14} className="estoque-search-icon" />
              <input
                type="text"
                className="estoque-search-input"
                placeholder="Buscar por nome"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>

            <button type="button" className="btn-buscar">
              Buscar
            </button>
          </div>
        </div>

        <div className="estoque-actions-row">
          <div className="estoque-action-btns">
            <button
              type="button"
              className="btn-action btn-entrada"
              onClick={() => openMovementModal('COMPRA_FORNECEDOR')}
            >
              <span>↓</span>
              <span>Entrada</span>
            </button>

            <button
              type="button"
              className="btn-action btn-saida"
              onClick={() => openMovementModal('VENDA')}
            >
              <span>↑</span>
              <span>Saída</span>
            </button>

            <button
              type="button"
              className="btn-action btn-novo-produto"
              onClick={() => openProductModal()}
            >
              <span>+</span>
              <span>Novo produto</span>
            </button>

            <button
              type="button"
              className="btn-action btn-categorias"
              onClick={() => setActiveModal('categorias')}
            >
              <Tag size={13} color="#F59E0B" />
              <span>Categorias</span>
            </button>
          </div>

          <div className="estoque-tabs-pill">
            <button
              type="button"
              className={`estoque-tab ${!onlyLow ? 'estoque-tab-active' : ''}`}
              onClick={() => setOnlyLow(false)}
            >
              Todos (5)
            </button>
            <button
              type="button"
              className={`estoque-tab estoque-tab-low ${onlyLow ? 'estoque-tab-active' : ''}`}
              onClick={() => setOnlyLow(true)}
            >
              Abaixo do mín. (2)
            </button>
          </div>
        </div>

        <div className="estoque-table-wrap">
          <table className="estoque-table">
            <thead>
              <tr>
                <th className="col-item">Item</th>
                <th className="col-cat">Categoria</th>
                <th className="col-preco">Preço</th>
                <th className="col-qtd">Qtd</th>
                <th className="col-min">Mín</th>
                <th className="col-acoes">Ações</th>
              </tr>
            </thead>
            <tbody>
              {filteredProducts.map((prod) => {
                const cat = getCategoryById(prod.categoryId)
                return (
                  <tr key={prod.id}>
                    <td className="col-item">
                      <span className="prod-name">{prod.name}</span>
                      <span className="prod-desc">{prod.description}</span>
                    </td>
                    <td className="col-cat">
                      <span
                        className="cat-badge"
                        style={getCategoryBadgeStyle(cat.color)}
                      >
                        {cat.name}
                      </span>
                    </td>
                    <td className="col-preco prod-price">{prod.price}</td>
                    <td className="col-qtd">
                      {prod.isLow ? (
                        <span className="qty-badge-low">
                          <span>{prod.qty}</span>
                          <span>baixo</span>
                        </span>
                      ) : (
                        <span className="qty-badge-ok">{prod.qty}</span>
                      )}
                    </td>
                    <td className="col-min">{prod.min}</td>
                    <td className="col-acoes">
                      <div className="row-action-group">
                        <button
                          type="button"
                          className="row-icon-btn"
                          title={`Histórico — ${prod.name}`}
                          onClick={() => openHistoryModal(prod.name)}
                        >
                          <Clock size={14} />
                        </button>
                        <button
                          type="button"
                          className="row-icon-btn"
                          title={`Editar — ${prod.name}`}
                          onClick={() => openProductModal(prod)}
                        >
                          <Pencil size={14} color="#F97316" />
                        </button>
                        <button
                          type="button"
                          className="row-icon-btn btn-trash"
                          title="Remover produto"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>

        <div className="estoque-table-footer">
          <span>1–{filteredProducts.length} de 5</span>
          <div className="pagination-group">
            <button type="button" className="page-btn">
              ‹
            </button>
            <button type="button" className="page-btn">
              ›
            </button>
          </div>
        </div>
      </section>

      {/* 4. Grade Inferior: Histórico completo + Estoque baixo */}
      <div className="estoque-bottom-grid">
        {/* Card Esquerdo: Histórico completo */}
        <section className="bottom-panel-card">
          <div className="bottom-panel-header">
            <div>
              <h3 className="estoque-section-title">Histórico completo</h3>
              <span className="bottom-panel-sub">
                Todas as movimentações de entradas e saídas
              </span>
            </div>
          </div>

          <div className="hist-filter-row">
            <div className="hist-search-wrap">
              <Search size={14} className="estoque-search-icon" />
              <input
                type="text"
                className="hist-search-input"
                placeholder="Buscar por produto ou observação..."
                value={histSearch}
                onChange={(e) => setHistSearch(e.target.value)}
              />
            </div>

            <select className="estoque-select">
              <option>Últimos 30 dias</option>
              <option>Últimos 7 dias</option>
              <option>Hoje</option>
            </select>
          </div>

          <div className="hist-type-pills">
            <button
              type="button"
              className={`hist-pill ${movFilter === 'ALL' ? 'hist-pill-active' : ''}`}
              onClick={() => setMovFilter('ALL')}
            >
              Todos (24)
            </button>
            <button
              type="button"
              className={`hist-pill ${movFilter === 'COMPRA_FORNECEDOR' ? 'hist-pill-active' : ''}`}
              onClick={() => setMovFilter('COMPRA_FORNECEDOR')}
            >
              Compra
            </button>
            <button
              type="button"
              className={`hist-pill ${movFilter === 'VENDA' ? 'hist-pill-active' : ''}`}
              onClick={() => setMovFilter('VENDA')}
            >
              Venda
            </button>
            <button
              type="button"
              className={`hist-pill ${movFilter === 'USO_INTERNO' ? 'hist-pill-active' : ''}`}
              onClick={() => setMovFilter('USO_INTERNO')}
            >
              Uso interno
            </button>
            <button
              type="button"
              className={`hist-pill ${movFilter === 'PERDA_DANO' ? 'hist-pill-active' : ''}`}
              onClick={() => setMovFilter('PERDA_DANO')}
            >
              Perda / Dano
            </button>
          </div>

          <div className="hist-list">
            {filteredMovements.map((mov) => (
              <div key={mov.id} className="hist-row">
                <div>
                  <span className="hist-item-title">{mov.productName}</span>
                  <div className="hist-meta-row">
                    <span className={`badge-mov ${mov.typeClass}`}>
                      {mov.type}
                    </span>
                    <span className="hist-date">{mov.date}</span>
                  </div>
                  <span className="hist-obs">{mov.obs}</span>
                </div>
                <span
                  className={
                    mov.isPositive ? 'mov-qty-plus' : 'mov-qty-minus'
                  }
                >
                  {mov.delta}
                </span>
              </div>
            ))}
          </div>

          <div className="estoque-table-footer">
            <span>1–6 de 24 movimentações</span>
            <div className="pagination-group">
              <button type="button" className="page-btn">
                ‹
              </button>
              <button type="button" className="page-btn page-btn-active">
                1
              </button>
              <button type="button" className="page-btn">
                2
              </button>
              <button type="button" className="page-btn">
                3
              </button>
              <button type="button" className="page-btn">
                ›
              </button>
            </div>
          </div>
        </section>

        {/* Card Direito: Estoque baixo */}
        <section className="bottom-panel-card">
          <div className="bottom-panel-header">
            <div>
              <h3 className="estoque-section-title">Estoque baixo</h3>
              <span className="bottom-panel-sub">
                Todos os itens abaixo ou no limite mínimo
              </span>
            </div>
            <span className="badge-attention">4 em atenção</span>
          </div>

          <div className="hist-list">
            <div className="low-stock-card">
              <div className="low-stock-top">
                <div>
                  <span className="hist-item-title">Bolas de basquete</span>
                  <span className="bottom-panel-sub">
                    {getCategoryById(2).name} · Mínimo: 3 un.
                  </span>
                </div>
                <div className="low-stock-right">
                  <span className="pill-ratio-red">1 / 3</span>
                  <button
                    type="button"
                    className="btn-quick-entrada"
                    onClick={() => openMovementModal('COMPRA_FORNECEDOR')}
                  >
                    + Entrada
                  </button>
                </div>
              </div>
              <div className="low-progress-track">
                <div className="low-progress-red" style={{ width: '33%' }} />
              </div>
              <span className="low-missing-red">Faltam 2 unidades</span>
            </div>

            <div className="low-stock-card">
              <div className="low-stock-top">
                <div>
                  <span className="hist-item-title">Coca-Cola 350ml</span>
                  <span className="bottom-panel-sub">
                    {getCategoryById(3).name} · Mínimo: 12 un.
                  </span>
                </div>
                <div className="low-stock-right">
                  <span className="pill-ratio-red">3 / 12</span>
                  <button
                    type="button"
                    className="btn-quick-entrada"
                    onClick={() => openMovementModal('COMPRA_FORNECEDOR')}
                  >
                    + Entrada
                  </button>
                </div>
              </div>
              <div className="low-progress-track">
                <div className="low-progress-red" style={{ width: '25%' }} />
              </div>
              <span className="low-missing-red">Faltam 9 unidades</span>
            </div>

            <div className="low-stock-card low-stock-card-warn">
              <div className="low-stock-top">
                <div>
                  <span className="hist-item-title">Raquetes de tênis</span>
                  <span className="bottom-panel-sub">
                    {getCategoryById(2).name} · Mínimo: 2 un.
                  </span>
                </div>
                <div className="low-stock-right">
                  <span className="pill-ratio-amber">2 / 2</span>
                  <button
                    type="button"
                    className="btn-quick-entrada"
                    onClick={() => openMovementModal('COMPRA_FORNECEDOR')}
                  >
                    + Entrada
                  </button>
                </div>
              </div>
              <div className="low-progress-track">
                <div className="low-progress-amber" style={{ width: '100%' }} />
              </div>
              <span className="low-missing-amber">No limite mínimo</span>
            </div>
          </div>
        </section>
      </div>

      {/* ======================================================== */}
      {/* MODAL 1: NOVO PRODUTO                                    */}
      {/* ======================================================== */}
      <Modal
        isOpen={activeModal === 'produto'}
        onClose={() => setActiveModal(null)}
        title="Novo Produto"
        subtitle="Cadastre um item no catálogo do estoque"
        closeOnOverlayClick={!isProductDirty}
        footer={
          <>
            <button
              type="button"
              className="btn-modal-cancel"
              onClick={() => setActiveModal(null)}
            >
              Cancelar
            </button>
            <button
              type="button"
              className="btn-modal-save"
              onClick={() => setActiveModal(null)}
            >
              Salvar produto
            </button>
          </>
        }
      >
        <div className="modal-form">
          <div>
            <label className="modal-label">Nome do produto *</label>
            <input
              type="text"
              className="modal-input"
              value={prodName}
              onChange={(e) => {
                setProdName(e.target.value)
                setIsProductDirty(true)
              }}
            />
          </div>

          <div className="modal-row-2">
            <div>
              <label className="modal-label">Preço (R$) *</label>
              <input
                type="text"
                className="modal-input"
                value={prodPrice}
                onChange={(e) => {
                  setProdPrice(e.target.value)
                  setIsProductDirty(true)
                }}
              />
            </div>
            <div>
              <label className="modal-label">Estoque mínimo *</label>
              <input
                type="text"
                className="modal-input"
                value={prodMin}
                onChange={(e) => {
                  setProdMin(e.target.value)
                  setIsProductDirty(true)
                }}
              />
            </div>
          </div>

          <div>
            <label className="modal-label">Categorias</label>
            <div className="modal-cat-chips">
              {categories.map((cat) => {
                const isChecked = selectedModalCats.includes(cat.id)
                return (
                  <button
                    key={cat.id}
                    type="button"
                    className={`modal-cat-chip ${isChecked ? 'modal-cat-chip-active' : ''}`}
                    onClick={() => toggleModalCategory(cat.id)}
                  >
                    <span
                      className={`modal-check-box ${isChecked ? 'modal-check-box-active' : ''}`}
                    >
                      {isChecked && (
                        <Check size={10} color="#FFFFFF" strokeWidth={3} />
                      )}
                    </span>
                    <span>{cat.name}</span>
                  </button>
                )
              })}
            </div>
          </div>

          <div>
            <label className="modal-label">Descrição</label>
            <textarea
              rows={2}
              className="modal-textarea"
              value={prodDesc}
              onChange={(e) => {
                setProdDesc(e.target.value)
                setIsProductDirty(true)
              }}
              placeholder="Boia circular infantil reforçada para piscina recreativa."
            />
          </div>
        </div>
      </Modal>

      {/* ======================================================== */}
      {/* MODAL 2: REGISTRAR MOVIMENTAÇÃO                          */}
      {/* ======================================================== */}
      <Modal
        isOpen={activeModal === 'movimentacao'}
        onClose={() => setActiveModal(null)}
        title="Registrar Movimentação"
        subtitle="Atualize o saldo com histórico e motivo"
        closeOnOverlayClick={!isMovDirty}
        footer={
          <>
            <button
              type="button"
              className="btn-modal-cancel"
              onClick={() => setActiveModal(null)}
            >
              Cancelar
            </button>
            <button
              type="button"
              className="btn-modal-save"
              onClick={() => setActiveModal(null)}
            >
              Confirmar movimentação
            </button>
          </>
        }
      >
        <div className="modal-form">
          <div>
            <label className="modal-label">Produto *</label>
            <select
              className="modal-input"
              onChange={() => setIsMovDirty(true)}
            >
              <option>Boia de piscina (Atual: 4)</option>
              <option>Bolas de basquete (Atual: 1)</option>
              <option>Raquetes de tênis (Atual: 2)</option>
              <option>Arma de água (Atual: 5)</option>
            </select>
          </div>

          <div>
            <label className="modal-label">Tipo de movimentação *</label>
            <div className="modal-enum-grid">
              {ENUM_OPTIONS.map((opt) => {
                const isSelected = selectedMovType === opt.id
                return (
                  <button
                    key={opt.id}
                    type="button"
                    className={`modal-enum-btn ${isSelected ? 'modal-enum-btn-active' : ''}`}
                    onClick={() => {
                      setSelectedMovType(opt.id)
                      setIsMovDirty(true)
                    }}
                  >
                    <span>{opt.label}</span>
                    <span>{opt.sign}</span>
                  </button>
                )
              })}
            </div>
          </div>

          <div>
            <label className="modal-label">Quantidade *</label>
            <input
              type="text"
              className="modal-input"
              value={movQty}
              onChange={(e) => {
                setMovQty(e.target.value)
                setIsMovDirty(true)
              }}
            />
          </div>

          <div>
            <label className="modal-label">Observação</label>
            <textarea
              rows={2}
              className="modal-textarea"
              value={movObs}
              onChange={(e) => {
                setMovObs(e.target.value)
                setIsMovDirty(true)
              }}
              placeholder="NF 4021 — Reposição piscina adulto e infantil"
            />
          </div>
        </div>
      </Modal>

      {/* ======================================================== */}
      {/* MODAL 3: GERENCIAR CATEGORIAS                            */}
      {/* ======================================================== */}
      <Modal
        isOpen={activeModal === 'categorias'}
        onClose={() => setActiveModal(null)}
        title="Gerenciar Categorias"
        subtitle="Organize produtos e cardápio do clube"
        closeOnOverlayClick={newCatName.trim().length === 0}
        footer={
          <button
            type="button"
            className="btn-modal-cancel"
            onClick={() => setActiveModal(null)}
          >
            Fechar
          </button>
        }
      >
        <div className="modal-form">
          <div className="modal-add-cat-row">
            <button
              type="button"
              className={`new-cat-color-btn ${showNewCatPalette ? 'new-cat-color-btn-open' : ''}`}
              onClick={() => setShowNewCatPalette((prev) => !prev)}
              title="Escolher cor da nova categoria"
            >
              <span
                className="new-cat-color-dot"
                style={{ backgroundColor: newCatColor }}
              />
            </button>

            <input
              type="text"
              className="modal-input"
              placeholder="Nova categoria (ex: Bebidas)"
              value={newCatName}
              onChange={(e) => setNewCatName(e.target.value)}
            />
            <button
              type="button"
              className="btn-modal-save"
              onClick={handleAddCategory}
            >
              + Adicionar
            </button>
          </div>

          {showNewCatPalette && (
            <div className="new-cat-palette-popover">
              <div className="color-swatches-grid">
                {PRESET_COLORS.map((hex) => {
                  const isSelected =
                    newCatColor.toLowerCase() === hex.toLowerCase()
                  return (
                    <button
                      key={hex}
                      type="button"
                      className={`color-swatch-btn-sm ${isSelected ? 'color-swatch-active' : ''}`}
                      style={{ backgroundColor: hex }}
                      onClick={() => {
                        setNewCatColor(hex)
                        setShowNewCatPalette(false)
                      }}
                      title={hex}
                    >
                      {isSelected && (
                        <Check size={12} color="#FFFFFF" strokeWidth={3} />
                      )}
                    </button>
                  )
                })}
              </div>

              <label className="custom-color-label">
                <input
                  type="color"
                  className="custom-color-input"
                  value={newCatColor}
                  onChange={(e) => setNewCatColor(e.target.value)}
                />
                <span>Outra cor</span>
              </label>
            </div>
          )}

          <div className="modal-cat-list">
            {categories.map((cat) => (
              <div key={cat.id} className="modal-cat-row">
                <div className="modal-cat-row-left">
                  <span
                    className="modal-cat-dot"
                    style={{ backgroundColor: cat.color }}
                  />
                  <span>{cat.name}</span>
                  <span className="modal-cat-count">{cat.countText}</span>
                </div>
                <div className="row-action-group">
                  <button
                    type="button"
                    className="row-icon-btn"
                    title="Editar categoria"
                    onClick={() => handleOpenEditCategory(cat)}
                  >
                    <Pencil size={14} color="#F97316" />
                  </button>
                  <button
                    type="button"
                    className="row-icon-btn btn-trash"
                    title="Excluir categoria"
                    onClick={() => handleDeleteCategory(cat.id)}
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </Modal>

      {/* ======================================================== */}
      {/* MODAL 4: EDITAR CATEGORIA (COR E NOME)                   */}
      {/* ======================================================== */}
      <Modal
        isOpen={activeModal === 'editar-categoria'}
        onClose={() => setActiveModal('categorias')}
        title="Editar Categoria"
        subtitle="Altere o nome e a cor de identificação da categoria"
        closeOnOverlayClick={!isEditCatDirty}
        footer={
          <>
            <button
              type="button"
              className="btn-modal-cancel"
              onClick={() => setActiveModal('categorias')}
            >
              Voltar
            </button>
            <button
              type="button"
              className="btn-modal-save"
              onClick={handleSaveEditCategory}
            >
              Salvar alterações
            </button>
          </>
        }
      >
        <div className="modal-form">
          <div>
            <label className="modal-label">Nome da categoria *</label>
            <input
              type="text"
              className="modal-input"
              value={editCatName}
              onChange={(e) => {
                setEditCatName(e.target.value)
                setIsEditCatDirty(true)
              }}
              placeholder="Ex: Aquático"
            />
          </div>

          <div>
            <label className="modal-label">Cor da categoria *</label>
            <div className="color-swatches-grid">
              {PRESET_COLORS.map((hex) => {
                const isSelected =
                  editCatColor.toLowerCase() === hex.toLowerCase()
                return (
                  <button
                    key={hex}
                    type="button"
                    className={`color-swatch-btn ${isSelected ? 'color-swatch-active' : ''}`}
                    style={{ backgroundColor: hex }}
                    onClick={() => {
                      setEditCatColor(hex)
                      setIsEditCatDirty(true)
                    }}
                    title={hex}
                  >
                    {isSelected && (
                      <Check size={14} color="#FFFFFF" strokeWidth={3} />
                    )}
                  </button>
                )
              })}

              <label className="custom-color-label">
                <input
                  type="color"
                  className="custom-color-input"
                  value={editCatColor}
                  onChange={(e) => {
                    setEditCatColor(e.target.value)
                    setIsEditCatDirty(true)
                  }}
                />
                <span>Outra cor</span>
              </label>
            </div>
          </div>

          <div>
            <label className="modal-label">
              Pré-visualização (na lista e na tabela de estoque)
            </label>
            <div className="cat-preview-box">
              <div className="modal-cat-row-left">
                <span
                  className="modal-cat-dot"
                  style={{ backgroundColor: editCatColor }}
                />
                <span>{editCatName || 'Nome da categoria'}</span>
              </div>
              <span
                className="cat-badge"
                style={getCategoryBadgeStyle(editCatColor)}
              >
                {editCatName || 'Categoria'}
              </span>
            </div>
          </div>
        </div>
      </Modal>

      {/* ======================================================== */}
      {/* MODAL 5: HISTÓRICO DO ITEM                               */}
      {/* ======================================================== */}
      <Modal
        isOpen={activeModal === 'historico'}
        onClose={() => setActiveModal(null)}
        title={`Histórico — ${historyProductName}`}
        subtitle="Registro completo de entradas e saídas deste item"
        footer={
          <button
            type="button"
            className="btn-modal-cancel"
            onClick={() => setActiveModal(null)}
          >
            Fechar
          </button>
        }
      >
        <div className="hist-list">
          {MOVEMENTS.slice(0, 3).map((mov) => (
            <div key={mov.id} className="hist-row">
              <div>
                <div className="hist-meta-row">
                  <span className={`badge-mov ${mov.typeClass}`}>
                    {mov.type}
                  </span>
                  <span className="hist-date">{mov.date}</span>
                </div>
                <span className="hist-obs">{mov.obs}</span>
              </div>
              <span
                className={
                  mov.isPositive ? 'mov-qty-plus' : 'mov-qty-minus'
                }
              >
                {mov.delta}
              </span>
            </div>
          ))}
        </div>
      </Modal>
    </div>
  )
}
