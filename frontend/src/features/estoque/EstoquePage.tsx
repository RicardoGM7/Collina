import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
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
import { getApiErrorMessage } from '../../services/api'
import {
  categoriasService,
  type CategoriaDTO,
} from '../../services/categoriasService'
import {
  estoqueService,
  type EstoqueDTO,
  type MovimentacaoEstoqueDTO,
  type MovimentacaoTipo,
} from '../../services/estoqueService'
import { produtosService, type ProdutoDTO } from '../../services/produtosService'
import {
  formatCurrency,
  formatDateShort,
  formatDecimalInput,
  getCategoryBadgeStyle,
  isIsoDateToday,
  isWithinDays,
  parseCurrencyInput,
} from '../../utils/formatters'
import './EstoquePage.css'

interface ProductWithStock extends ProdutoDTO {
  quantidade: number
  estoqueMinimo: number
  isLow: boolean
}

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

// RF-E05: VENDA é gerada automaticamente ao pagar pedidos; movimentação manual aceita os 3 tipos abaixo
const MANUAL_ENUM_OPTIONS: Array<{
  id: Exclude<MovimentacaoTipo, 'VENDA'>
  label: string
  sign: string
}> = [
  { id: 'COMPRA_FORNECEDOR', label: 'Compra', sign: '(+)' },
  { id: 'USO_INTERNO', label: 'Uso interno', sign: '(-)' },
  { id: 'PERDA_DANO', label: 'Perda / Dano', sign: '(-)' },
]

const MOV_TYPE_CLASS: Record<MovimentacaoTipo, string> = {
  COMPRA_FORNECEDOR: 'mov-compra',
  VENDA: 'mov-venda',
  USO_INTERNO: 'mov-uso',
  PERDA_DANO: 'mov-perda',
}

type ActiveModal =
  | null
  | 'produto'
  | 'movimentacao'
  | 'categorias'
  | 'editar-categoria'
  | 'historico'

const PRODUCTS_PER_PAGE = 6
const MOVEMENTS_PER_PAGE = 6

export function EstoquePage() {
  // Dados reais da API
  const [categories, setCategories] = useState<CategoriaDTO[]>([])
  const [rawProducts, setRawProducts] = useState<ProdutoDTO[]>([])
  const [estoques, setEstoques] = useState<EstoqueDTO[]>([])
  const [lowStockEstoques, setLowStockEstoques] = useState<EstoqueDTO[]>([])
  const [movements, setMovements] = useState<MovimentacaoEstoqueDTO[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Filtros e paginação
  const [search, setSearch] = useState('')
  const [selectedCatId, setSelectedCatId] = useState<string>('all')
  const [onlyLow, setOnlyLow] = useState(false)
  const [prodPage, setProdPage] = useState(1)

  const [movFilter, setMovFilter] = useState<string>('ALL')
  const [histSearch, setHistSearch] = useState('')
  const [histPeriod, setHistPeriod] = useState<'30d' | '7d' | 'today' | 'all'>(
    '30d'
  )
  const [movPage, setMovPage] = useState(1)

  const [activeModal, setActiveModal] = useState<ActiveModal>(null)

  // Estado do Modal Produto (Criar / Editar)
  const [editingProductId, setEditingProductId] = useState<number | null>(null)
  const [prodName, setProdName] = useState('')
  const [prodPrice, setProdPrice] = useState('')
  const [prodMin, setProdMin] = useState('0')
  const [prodInitialQty, setProdInitialQty] = useState('0')
  const [prodDesc, setProdDesc] = useState('')
  const [selectedModalCats, setSelectedModalCats] = useState<number[]>([])
  const [isProductDirty, setIsProductDirty] = useState(false)

  // Estado do Modal Movimentação
  const [movProductId, setMovProductId] = useState<number | ''>('')
  const [selectedMovType, setSelectedMovType] =
    useState<Exclude<MovimentacaoTipo, 'VENDA'>>('COMPRA_FORNECEDOR')
  const [movQty, setMovQty] = useState('1')
  const [movObs, setMovObs] = useState('')
  const [isMovDirty, setIsMovDirty] = useState(false)

  // Estado do Modal Histórico do Produto
  const [historyProductName, setHistoryProductName] = useState('')
  const [productHistory, setProductHistory] = useState<
    MovimentacaoEstoqueDTO[]
  >([])
  const [isHistoryLoading, setIsHistoryLoading] = useState(false)

  // Estado das categorias
  const [newCatName, setNewCatName] = useState('')
  const [newCatColor, setNewCatColor] = useState('#2BB673')
  const [showNewCatPalette, setShowNewCatPalette] = useState(false)
  const [editingCatId, setEditingCatId] = useState<number | null>(null)
  const [editCatName, setEditCatName] = useState('')
  const [editCatColor, setEditCatColor] = useState('#0284C7')
  const [isEditCatDirty, setIsEditCatDirty] = useState(false)

  const loadAll = useCallback(async (showErrorToast = true) => {
    try {
      const [cats, prods, ests, lowEsts, movs] = await Promise.all([
        categoriasService.listar(),
        produtosService.listar(),
        estoqueService.listar(),
        estoqueService.listarBaixo(),
        estoqueService.listarMovimentacoes(),
      ])

      const sortedMovs = [...movs].sort((a, b) => {
        const diff = new Date(b.data).getTime() - new Date(a.data).getTime()
        return diff !== 0 ? diff : b.id - a.id
      })

      setCategories(cats)
      setRawProducts(prods)
      setEstoques(ests)
      setLowStockEstoques(lowEsts)
      setMovements(sortedMovs)
    } catch (error) {
      if (showErrorToast) {
        toast.error(
          getApiErrorMessage(error, 'Erro ao carregar os dados de estoque.')
        )
      }
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadAll()
  }, [loadAll])

  // Junção Produto + Estoque pelo produtoId
  const products = useMemo<ProductWithStock[]>(() => {
    const estoqueByProdutoId = new Map<number, EstoqueDTO>()
    for (const est of estoques) {
      estoqueByProdutoId.set(est.produtoId, est)
    }

    return rawProducts.map((prod) => {
      const est = estoqueByProdutoId.get(prod.id)
      const quantidade = est?.quantidade ?? 0
      const estoqueMinimo = est?.estoqueMinimo ?? 0
      return {
        ...prod,
        quantidade,
        estoqueMinimo,
        isLow: quantidade <= estoqueMinimo,
      }
    })
  }, [rawProducts, estoques])

  // Contagem de produtos por categoria
  const categoryCounts = useMemo(() => {
    const counts = new Map<number, number>()
    for (const prod of rawProducts) {
      for (const cat of prod.categorias) {
        counts.set(cat.id, (counts.get(cat.id) ?? 0) + 1)
      }
    }
    return counts
  }, [rawProducts])

  // Produtos filtrados e paginados
  const filteredProducts = useMemo(() => {
    const q = search.trim().toLowerCase()
    return products.filter((p) => {
      const matchesName =
        !q ||
        p.nome.toLowerCase().includes(q) ||
        (p.descricao ?? '').toLowerCase().includes(q)
      const matchesCat =
        selectedCatId === 'all' ||
        p.categorias.some((c) => String(c.id) === selectedCatId)
      const matchesLow = !onlyLow || p.isLow
      return matchesName && matchesCat && matchesLow
    })
  }, [products, search, selectedCatId, onlyLow])

  const totalProdPages = Math.max(
    1,
    Math.ceil(filteredProducts.length / PRODUCTS_PER_PAGE)
  )
  const safeProdPage = Math.min(prodPage, totalProdPages)
  const paginatedProducts = useMemo(() => {
    const start = (safeProdPage - 1) * PRODUCTS_PER_PAGE
    return filteredProducts.slice(start, start + PRODUCTS_PER_PAGE)
  }, [filteredProducts, safeProdPage])

  // Movimentações filtradas e paginadas
  const filteredMovements = useMemo(() => {
    const q = histSearch.trim().toLowerCase()
    return movements.filter((m) => {
      const matchesType = movFilter === 'ALL' || m.tipo === movFilter
      const matchesQuery =
        !q ||
        (m.nomeProduto ?? '').toLowerCase().includes(q) ||
        (m.observacao ?? '').toLowerCase().includes(q)
      const matchesPeriod =
        histPeriod === 'all' ||
        (histPeriod === 'today' && isWithinDays(m.data, 0)) ||
        (histPeriod === '7d' && isWithinDays(m.data, 7)) ||
        (histPeriod === '30d' && isWithinDays(m.data, 30))

      return matchesType && matchesQuery && matchesPeriod
    })
  }, [movements, movFilter, histSearch, histPeriod])

  const totalMovPages = Math.max(
    1,
    Math.ceil(filteredMovements.length / MOVEMENTS_PER_PAGE)
  )
  const safeMovPage = Math.min(movPage, totalMovPages)
  const paginatedMovements = useMemo(() => {
    const start = (safeMovPage - 1) * MOVEMENTS_PER_PAGE
    return filteredMovements.slice(start, start + MOVEMENTS_PER_PAGE)
  }, [filteredMovements, safeMovPage])

  // KPIs reais
  const lowCount = useMemo(
    () => products.filter((p) => p.isLow).length,
    [products]
  )

  const movementsTodayCount = useMemo(
    () => movements.filter((m) => isIsoDateToday(m.data)).length,
    [movements]
  )

  // Abertura de modais
  function openProductModal(product?: ProductWithStock) {
    if (product) {
      setEditingProductId(product.id)
      setProdName(product.nome)
      setProdPrice(formatDecimalInput(Number(product.preco)))
      setProdMin(String(product.estoqueMinimo))
      setProdInitialQty(String(product.quantidade))
      setProdDesc(product.descricao ?? '')
      setSelectedModalCats(product.categorias.map((c) => c.id))
    } else {
      setEditingProductId(null)
      setProdName('')
      setProdPrice('')
      setProdMin('5')
      setProdInitialQty('0')
      setProdDesc('')
      setSelectedModalCats(categories.length > 0 ? [categories[0].id] : [])
    }
    setIsProductDirty(false)
    setActiveModal('produto')
  }

  function openMovementModal(
    defaultType: Exclude<MovimentacaoTipo, 'VENDA'>,
    preselectedProductId?: number
  ) {
    if (products.length === 0) {
      toast.error('Cadastre um produto antes de registrar uma movimentação.')
      return
    }
    setMovProductId(preselectedProductId ?? products[0].id)
    setSelectedMovType(defaultType)
    setMovQty('1')
    setMovObs('')
    setIsMovDirty(false)
    setActiveModal('movimentacao')
  }

  async function openHistoryModal(product: ProductWithStock) {
    setHistoryProductName(product.nome)
    setProductHistory([])
    setIsHistoryLoading(true)
    setActiveModal('historico')
    try {
      const list = await estoqueService.listarMovimentacoesPorProduto(
        product.id
      )
      setProductHistory(list)
    } catch (error) {
      toast.error(
        getApiErrorMessage(error, 'Erro ao carregar histórico do produto.')
      )
    } finally {
      setIsHistoryLoading(false)
    }
  }

  function toggleModalCategory(catId: number) {
    setIsProductDirty(true)
    setSelectedModalCats((prev) =>
      prev.includes(catId)
        ? prev.filter((id) => id !== catId)
        : [...prev, catId]
    )
  }

  // Salvar Produto (POST ou PUT)
  async function handleSaveProduct() {
    const trimmedName = prodName.trim()
    const parsedPrice = parseCurrencyInput(prodPrice)
    const parsedMin = Number.parseInt(prodMin, 10)
    const parsedInitQty = Number.parseInt(prodInitialQty, 10)

    if (!trimmedName) {
      toast.error('O nome do produto é obrigatório.')
      return
    }
    if (Number.isNaN(parsedPrice) || parsedPrice < 0) {
      toast.error('Informe um preço válido (ex: 45,00).')
      return
    }
    if (Number.isNaN(parsedMin) || parsedMin < 0) {
      toast.error('Informe um estoque mínimo válido (maior ou igual a 0).')
      return
    }
    if (
      editingProductId === null &&
      (Number.isNaN(parsedInitQty) || parsedInitQty < 0)
    ) {
      toast.error('Informe uma quantidade inicial válida (maior ou igual a 0).')
      return
    }
    if (selectedModalCats.length === 0) {
      toast.error('Selecione pelo menos uma categoria para o produto.')
      return
    }

    setIsSubmitting(true)
    try {
      const payload = {
        nome: trimmedName,
        descricao: prodDesc.trim() ? prodDesc.trim() : null,
        preco: Number(parsedPrice.toFixed(2)),
        categoriaIds: selectedModalCats,
        estoqueMinimo: parsedMin,
        ...(editingProductId === null
          ? { quantidadeInicial: parsedInitQty }
          : {}),
      }

      if (editingProductId === null) {
        await produtosService.criar(payload)
        toast.success('Produto cadastrado com sucesso!')
      } else {
        await produtosService.atualizar(editingProductId, payload)
        toast.success('Produto atualizado com sucesso!')
      }

      setIsProductDirty(false)
      setActiveModal(null)
      setMovPage(1)
      await loadAll(false)
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Erro ao salvar o produto.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  // Remover Produto (DELETE)
  async function handleDeleteProduct(product: ProductWithStock) {
    const confirmed = window.confirm(
      `Deseja realmente remover o produto "${product.nome}"?`
    )
    if (!confirmed) return

    setIsSubmitting(true)
    try {
      await produtosService.remover(product.id)
      toast.success('Produto removido com sucesso!')
      await loadAll(false)
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Não foi possível remover o produto.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  // Registrar Movimentação (POST)
  async function handleSaveMovement() {
    const parsedQty = Number.parseInt(movQty, 10)
    if (movProductId === '') {
      toast.error('Selecione um produto.')
      return
    }
    if (Number.isNaN(parsedQty) || parsedQty <= 0) {
      toast.error('A quantidade movimentada deve ser maior que zero.')
      return
    }

    setIsSubmitting(true)
    try {
      await estoqueService.registrarMovimentacao({
        produtoId: movProductId,
        tipo: selectedMovType,
        quantidade: parsedQty,
        observacao: movObs.trim() ? movObs.trim() : null,
      })
      toast.success('Movimentação registrada com sucesso!')
      setIsMovDirty(false)
      setActiveModal(null)
      setMovPage(1)
      await loadAll(false)
    } catch (error) {
      toast.error(
        getApiErrorMessage(error, 'Erro ao registrar movimentação de estoque.')
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  // CRUD Categorias
  async function handleAddCategory() {
    const trimmed = newCatName.trim()
    if (!trimmed) {
      toast.error('Informe o nome da nova categoria.')
      return
    }

    setIsSubmitting(true)
    try {
      await categoriasService.criar({
        nome: trimmed,
        cor: newCatColor.toUpperCase(),
      })
      toast.success('Categoria adicionada com sucesso!')
      setNewCatName('')
      setShowNewCatPalette(false)
      await loadAll(false)
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Erro ao cadastrar categoria.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  function handleOpenEditCategory(cat: CategoriaDTO) {
    setEditingCatId(cat.id)
    setEditCatName(cat.nome)
    setEditCatColor(cat.cor || '#2BB673')
    setIsEditCatDirty(false)
    setActiveModal('editar-categoria')
  }

  async function handleSaveEditCategory() {
    const trimmed = editCatName.trim()
    if (!trimmed || editingCatId === null) {
      toast.error('O nome da categoria é obrigatório.')
      return
    }

    setIsSubmitting(true)
    try {
      await categoriasService.atualizar(editingCatId, {
        nome: trimmed,
        cor: editCatColor.toUpperCase(),
      })
      toast.success('Categoria atualizada com sucesso!')
      setIsEditCatDirty(false)
      setActiveModal('categorias')
      await loadAll(false)
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Erro ao atualizar categoria.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleDeleteCategory(cat: CategoriaDTO) {
    const confirmed = window.confirm(
      `Deseja remover a categoria "${cat.nome}"?`
    )
    if (!confirmed) return

    setIsSubmitting(true)
    try {
      await categoriasService.remover(cat.id)
      toast.success('Categoria removida com sucesso!')
      await loadAll(false)
    } catch (error) {
      toast.error(
        getApiErrorMessage(error, 'Não foi possível remover a categoria.')
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  // Mapeia itens do card "Estoque baixo" com nomes de categorias
  const lowStockCards = useMemo(() => {
    const productMap = new Map<number, ProductWithStock>()
    for (const p of products) {
      productMap.set(p.id, p)
    }

    return lowStockEstoques.map((est) => {
      const prod = productMap.get(est.produtoId)
      const catNames =
        prod && prod.categorias.length > 0
          ? prod.categorias.map((c) => c.nome).join(', ')
          : 'Sem categoria'
      const isAtLimit = est.quantidade === est.estoqueMinimo
      const missing = Math.max(0, est.estoqueMinimo - est.quantidade)
      const pct =
        est.estoqueMinimo > 0
          ? Math.min(100, Math.round((est.quantidade / est.estoqueMinimo) * 100))
          : 100
      return {
        ...est,
        catNames,
        isAtLimit,
        missing,
        pct,
      }
    })
  }, [lowStockEstoques, products])

  const prodRangeStart =
    filteredProducts.length === 0 ? 0 : (safeProdPage - 1) * PRODUCTS_PER_PAGE + 1
  const prodRangeEnd = Math.min(
    safeProdPage * PRODUCTS_PER_PAGE,
    filteredProducts.length
  )

  const movRangeStart =
    filteredMovements.length === 0
      ? 0
      : (safeMovPage - 1) * MOVEMENTS_PER_PAGE + 1
  const movRangeEnd = Math.min(
    safeMovPage * MOVEMENTS_PER_PAGE,
    filteredMovements.length
  )

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
            <span className="kpi-value">{products.length}</span>
          </div>
          <div className="kpi-icon-box kpi-icon-green">
            <Archive size={20} />
          </div>
        </div>

        <button
          type="button"
          className="kpi-card kpi-danger"
          onClick={() => {
            setOnlyLow((prev) => !prev)
            setProdPage(1)
          }}
          title="Filtrar itens abaixo do mínimo"
        >
          <div className="kpi-info">
            <span className="kpi-label">Abaixo do mínimo</span>
            <span className="kpi-value">{lowCount}</span>
          </div>
          <div className="kpi-icon-box kpi-icon-red">
            <AlertTriangle size={20} />
          </div>
        </button>

        <div className="kpi-card kpi-white">
          <div className="kpi-info">
            <span className="kpi-label">Movimentações hoje</span>
            <span className="kpi-value">{movementsTodayCount}</span>
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
              onChange={(e) => {
                setSelectedCatId(e.target.value)
                setProdPage(1)
              }}
            >
              <option value="all">Todas categorias</option>
              {categories.map((cat) => (
                <option key={cat.id} value={String(cat.id)}>
                  {cat.nome}
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
                onChange={(e) => {
                  setSearch(e.target.value)
                  setProdPage(1)
                }}
              />
            </div>

            <button
              type="button"
              className="btn-buscar"
              onClick={() => setProdPage(1)}
            >
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
              onClick={() => openMovementModal('USO_INTERNO')}
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
              onClick={() => {
                setOnlyLow(false)
                setProdPage(1)
              }}
            >
              Todos ({products.length})
            </button>
            <button
              type="button"
              className={`estoque-tab estoque-tab-low ${onlyLow ? 'estoque-tab-active' : ''}`}
              onClick={() => {
                setOnlyLow(true)
                setProdPage(1)
              }}
            >
              Abaixo do mín. ({lowCount})
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
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="estoque-empty-state">
                    Carregando produtos do estoque...
                  </td>
                </tr>
              ) : paginatedProducts.length === 0 ? (
                <tr>
                  <td colSpan={6} className="estoque-empty-state">
                    Nenhum produto encontrado.
                  </td>
                </tr>
              ) : (
                paginatedProducts.map((prod) => (
                  <tr key={prod.id}>
                    <td className="col-item">
                      <span className="prod-name">{prod.nome}</span>
                      {prod.descricao && (
                        <span className="prod-desc">{prod.descricao}</span>
                      )}
                    </td>
                    <td className="col-cat">
                      <div className="cat-badges-wrap">
                        {prod.categorias.length > 0 ? (
                          prod.categorias.map((cat) => (
                            <span
                              key={cat.id}
                              className="cat-badge"
                              style={getCategoryBadgeStyle(cat.cor)}
                            >
                              {cat.nome}
                            </span>
                          ))
                        ) : (
                          <span
                            className="cat-badge"
                            style={getCategoryBadgeStyle('#64748B')}
                          >
                            Sem categoria
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="col-preco prod-price">
                      {formatCurrency(Number(prod.preco))}
                    </td>
                    <td className="col-qtd">
                      {prod.isLow ? (
                        <span className="qty-badge-low">
                          <span>{prod.quantidade}</span>
                          <span>baixo</span>
                        </span>
                      ) : (
                        <span className="qty-badge-ok">{prod.quantidade}</span>
                      )}
                    </td>
                    <td className="col-min">{prod.estoqueMinimo}</td>
                    <td className="col-acoes">
                      <div className="row-action-group">
                        <button
                          type="button"
                          className="row-icon-btn"
                          title={`Histórico — ${prod.nome}`}
                          onClick={() => void openHistoryModal(prod)}
                        >
                          <Clock size={14} />
                        </button>
                        <button
                          type="button"
                          className="row-icon-btn"
                          title={`Editar — ${prod.nome}`}
                          onClick={() => openProductModal(prod)}
                        >
                          <Pencil size={14} color="#F97316" />
                        </button>
                        <button
                          type="button"
                          className="row-icon-btn btn-trash"
                          title="Remover produto"
                          disabled={isSubmitting}
                          onClick={() => void handleDeleteProduct(prod)}
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="estoque-table-footer">
          <span>
            {filteredProducts.length === 0
              ? '0 itens'
              : `${prodRangeStart}–${prodRangeEnd} de ${filteredProducts.length}`}
          </span>
          <div className="pagination-group">
            <button
              type="button"
              className="page-btn"
              disabled={safeProdPage <= 1}
              onClick={() => setProdPage((p) => Math.max(1, p - 1))}
            >
              ‹
            </button>
            <button
              type="button"
              className="page-btn"
              disabled={safeProdPage >= totalProdPages}
              onClick={() =>
                setProdPage((p) => Math.min(totalProdPages, p + 1))
              }
            >
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
                onChange={(e) => {
                  setHistSearch(e.target.value)
                  setMovPage(1)
                }}
              />
            </div>

            <select
              className="estoque-select"
              value={histPeriod}
              onChange={(e) => {
                setHistPeriod(
                  e.target.value as '30d' | '7d' | 'today' | 'all'
                )
                setMovPage(1)
              }}
            >
              <option value="30d">Últimos 30 dias</option>
              <option value="7d">Últimos 7 dias</option>
              <option value="today">Hoje</option>
              <option value="all">Todo o período</option>
            </select>
          </div>

          <div className="hist-type-pills">
            <button
              type="button"
              className={`hist-pill ${movFilter === 'ALL' ? 'hist-pill-active' : ''}`}
              onClick={() => {
                setMovFilter('ALL')
                setMovPage(1)
              }}
            >
              Todos ({movements.length})
            </button>
            <button
              type="button"
              className={`hist-pill ${movFilter === 'COMPRA_FORNECEDOR' ? 'hist-pill-active' : ''}`}
              onClick={() => {
                setMovFilter('COMPRA_FORNECEDOR')
                setMovPage(1)
              }}
            >
              Compra
            </button>
            <button
              type="button"
              className={`hist-pill ${movFilter === 'VENDA' ? 'hist-pill-active' : ''}`}
              onClick={() => {
                setMovFilter('VENDA')
                setMovPage(1)
              }}
            >
              Venda
            </button>
            <button
              type="button"
              className={`hist-pill ${movFilter === 'USO_INTERNO' ? 'hist-pill-active' : ''}`}
              onClick={() => {
                setMovFilter('USO_INTERNO')
                setMovPage(1)
              }}
            >
              Uso interno
            </button>
            <button
              type="button"
              className={`hist-pill ${movFilter === 'PERDA_DANO' ? 'hist-pill-active' : ''}`}
              onClick={() => {
                setMovFilter('PERDA_DANO')
                setMovPage(1)
              }}
            >
              Perda / Dano
            </button>
          </div>

          <div className="hist-list">
            {isLoading ? (
              <div className="estoque-empty-state">
                Carregando movimentações...
              </div>
            ) : paginatedMovements.length === 0 ? (
              <div className="estoque-empty-state">
                Nenhuma movimentação encontrada para esse filtro.
              </div>
            ) : (
              paginatedMovements.map((mov) => {
                const isPositive = mov.tipo === 'COMPRA_FORNECEDOR'
                return (
                  <div key={mov.id} className="hist-row">
                    <div>
                      <span className="hist-item-title">
                        {mov.nomeProduto}
                      </span>
                      <div className="hist-meta-row">
                        <span
                          className={`badge-mov ${MOV_TYPE_CLASS[mov.tipo]}`}
                        >
                          {mov.tipo}
                        </span>
                        <span className="hist-date">
                          {formatDateShort(mov.data)}
                        </span>
                      </div>
                      {mov.observacao && (
                        <span className="hist-obs">Obs: {mov.observacao}</span>
                      )}
                    </div>
                    <span
                      className={isPositive ? 'mov-qty-plus' : 'mov-qty-minus'}
                    >
                      {isPositive ? `+${mov.quantidade}` : `-${mov.quantidade}`}
                    </span>
                  </div>
                )
              })
            )}
          </div>

          <div className="estoque-table-footer">
            <span>
              {filteredMovements.length === 0
                ? '0 movimentações'
                : `${movRangeStart}–${movRangeEnd} de ${filteredMovements.length} movimentações`}
            </span>
            <div className="pagination-group">
              <button
                type="button"
                className="page-btn"
                disabled={safeMovPage <= 1}
                onClick={() => setMovPage((p) => Math.max(1, p - 1))}
              >
                ‹
              </button>
              <button type="button" className="page-btn page-btn-active">
                {safeMovPage}
              </button>
              <button
                type="button"
                className="page-btn"
                disabled={safeMovPage >= totalMovPages}
                onClick={() =>
                  setMovPage((p) => Math.min(totalMovPages, p + 1))
                }
              >
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
            <span className="badge-attention">
              {lowStockCards.length} em atenção
            </span>
          </div>

          <div className="hist-list">
            {isLoading ? (
              <div className="estoque-empty-state">Verificando estoques...</div>
            ) : lowStockCards.length === 0 ? (
              <div className="estoque-empty-state">
                Todos os itens estão acima do estoque mínimo.
              </div>
            ) : (
              lowStockCards.map((item) => (
                <div
                  key={item.id}
                  className={`low-stock-card ${item.isAtLimit ? 'low-stock-card-warn' : ''}`}
                >
                  <div className="low-stock-top">
                    <div>
                      <span className="hist-item-title">
                        {item.nomeProduto}
                      </span>
                      <span className="bottom-panel-sub">
                        {item.catNames} · Mínimo: {item.estoqueMinimo} un.
                      </span>
                    </div>
                    <div className="low-stock-right">
                      <span
                        className={
                          item.isAtLimit ? 'pill-ratio-amber' : 'pill-ratio-red'
                        }
                      >
                        {item.quantidade} / {item.estoqueMinimo}
                      </span>
                      <button
                        type="button"
                        className="btn-quick-entrada"
                        onClick={() =>
                          openMovementModal('COMPRA_FORNECEDOR', item.produtoId)
                        }
                      >
                        + Entrada
                      </button>
                    </div>
                  </div>
                  <div className="low-progress-track">
                    <div
                      className={
                        item.isAtLimit ? 'low-progress-amber' : 'low-progress-red'
                      }
                      style={{ width: `${item.pct}%` }}
                    />
                  </div>
                  <span
                    className={
                      item.isAtLimit ? 'low-missing-amber' : 'low-missing-red'
                    }
                  >
                    {item.isAtLimit
                      ? 'No limite mínimo'
                      : `Faltam ${item.missing} ${item.missing === 1 ? 'unidade' : 'unidades'}`}
                  </span>
                </div>
              ))
            )}
          </div>
        </section>
      </div>

      {/* ======================================================== */}
      {/* MODAL 1: NOVO PRODUTO / EDITAR PRODUTO                   */}
      {/* ======================================================== */}
      <Modal
        isOpen={activeModal === 'produto'}
        onClose={() => setActiveModal(null)}
        title={editingProductId === null ? 'Novo Produto' : 'Editar Produto'}
        subtitle={
          editingProductId === null
            ? 'Cadastre um item no catálogo do estoque'
            : 'Altere os dados do produto (a quantidade é ajustada via Entrada/Saída)'
        }
        closeOnOverlayClick={!isProductDirty}
        footer={
          <>
            <button
              type="button"
              className="btn-modal-cancel"
              disabled={isSubmitting}
              onClick={() => setActiveModal(null)}
            >
              Cancelar
            </button>
            <button
              type="button"
              className="btn-modal-save"
              disabled={isSubmitting}
              onClick={() => void handleSaveProduct()}
            >
              {isSubmitting
                ? 'Salvando...'
                : editingProductId === null
                  ? 'Salvar produto'
                  : 'Salvar alterações'}
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
              placeholder="Ex: Boia de piscina"
              value={prodName}
              onChange={(e) => {
                setProdName(e.target.value)
                setIsProductDirty(true)
              }}
            />
          </div>

          <div className={editingProductId === null ? 'modal-row-3' : 'modal-row-2'}>
            <div>
              <label className="modal-label">Preço (R$) *</label>
              <input
                type="text"
                className="modal-input"
                placeholder="45,00"
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
                type="number"
                min={0}
                className="modal-input"
                value={prodMin}
                onChange={(e) => {
                  setProdMin(e.target.value)
                  setIsProductDirty(true)
                }}
              />
            </div>
            {editingProductId === null && (
              <div>
                <label className="modal-label">Qtd. inicial</label>
                <input
                  type="number"
                  min={0}
                  className="modal-input"
                  value={prodInitialQty}
                  onChange={(e) => {
                    setProdInitialQty(e.target.value)
                    setIsProductDirty(true)
                  }}
                />
              </div>
            )}
          </div>

          <div>
            <label className="modal-label">Categorias * (selecione uma ou mais)</label>
            {categories.length === 0 ? (
              <p className="bottom-panel-sub">
                Nenhuma categoria cadastrada. Crie uma categoria primeiro no botão
                &quot;Categorias&quot;.
              </p>
            ) : (
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
                      <span
                        className="modal-cat-dot-sm"
                        style={{ backgroundColor: cat.cor }}
                      />
                      <span>{cat.nome}</span>
                    </button>
                  )
                })}
              </div>
            )}
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
              disabled={isSubmitting}
              onClick={() => setActiveModal(null)}
            >
              Cancelar
            </button>
            <button
              type="button"
              className="btn-modal-save"
              disabled={isSubmitting}
              onClick={() => void handleSaveMovement()}
            >
              {isSubmitting ? 'Registrando...' : 'Confirmar movimentação'}
            </button>
          </>
        }
      >
        <div className="modal-form">
          <div>
            <label className="modal-label">Produto *</label>
            <select
              className="modal-input"
              value={movProductId}
              onChange={(e) => {
                setMovProductId(Number(e.target.value))
                setIsMovDirty(true)
              }}
            >
              {products.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nome} (Atual: {p.quantidade})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="modal-label">Tipo de movimentação *</label>
            <div className="modal-enum-grid">
              {MANUAL_ENUM_OPTIONS.map((opt) => {
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
              type="number"
              min={1}
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
              onKeyDown={(e) => {
                if (e.key === 'Enter') void handleAddCategory()
              }}
            />
            <button
              type="button"
              className="btn-modal-save"
              disabled={isSubmitting}
              onClick={() => void handleAddCategory()}
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
            {categories.length === 0 ? (
              <div className="estoque-empty-state">
                Nenhuma categoria cadastrada ainda.
              </div>
            ) : (
              categories.map((cat) => {
                const count = categoryCounts.get(cat.id) ?? 0
                return (
                  <div key={cat.id} className="modal-cat-row">
                    <div className="modal-cat-row-left">
                      <span
                        className="modal-cat-dot"
                        style={{ backgroundColor: cat.cor }}
                      />
                      <span>{cat.nome}</span>
                      <span className="modal-cat-count">
                        · {count} {count === 1 ? 'item' : 'itens'}
                      </span>
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
                        disabled={isSubmitting}
                        onClick={() => void handleDeleteCategory(cat)}
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                )
              })
            )}
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
              disabled={isSubmitting}
              onClick={() => setActiveModal('categorias')}
            >
              Voltar
            </button>
            <button
              type="button"
              className="btn-modal-save"
              disabled={isSubmitting}
              onClick={() => void handleSaveEditCategory()}
            >
              {isSubmitting ? 'Salvando...' : 'Salvar alterações'}
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
          {isHistoryLoading ? (
            <div className="estoque-empty-state">Carregando histórico...</div>
          ) : productHistory.length === 0 ? (
            <div className="estoque-empty-state">
              Nenhuma movimentação registrada para este item ainda.
            </div>
          ) : (
            productHistory.map((mov) => {
              const isPositive = mov.tipo === 'COMPRA_FORNECEDOR'
              return (
                <div key={mov.id} className="hist-row">
                  <div>
                    <div className="hist-meta-row">
                      <span
                        className={`badge-mov ${MOV_TYPE_CLASS[mov.tipo]}`}
                      >
                        {mov.tipo}
                      </span>
                      <span className="hist-date">
                        {formatDateShort(mov.data)}
                      </span>
                    </div>
                    {mov.observacao && (
                      <span className="hist-obs">Obs: {mov.observacao}</span>
                    )}
                  </div>
                  <span
                    className={isPositive ? 'mov-qty-plus' : 'mov-qty-minus'}
                  >
                    {isPositive ? `+${mov.quantidade}` : `-${mov.quantidade}`}
                  </span>
                </div>
              )
            })
          )}
        </div>
      </Modal>
    </div>
  )
}
