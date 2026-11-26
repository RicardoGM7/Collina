import { ArrowRight } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import iconeCadastros from '../../assets/iconeCadastros.png'
import iconeEscala from '../../assets/iconeEscala.png'
import iconeEstoque from '../../assets/iconeEstoque.png'
import iconeFinanceiro from '../../assets/iconeFinanceiro.png'
import iconePedidos from '../../assets/iconePedidos.png'
import iconeReservas from '../../assets/iconeReservas.png'
import { associadoService } from '../../services/associadoService'
import { estoqueService } from '../../services/estoqueService'
import { pedidosService } from '../../services/pedidosService'
import './AdminHomePage.css'

interface DashboardCardItem {
  id: string
  path: string
  title: string
  subtitle: string
  badgeText: string
  badgeClass: string
  cardThemeClass: string
  arrowClass: string
  image: string
  imageClass?: string
}

interface MetricState {
  loading: boolean
  value: number | null
}

export function AdminHomePage() {
  const navigate = useNavigate()

  const [estoqueBaixo, setEstoqueBaixo] = useState<MetricState>({
    loading: true,
    value: null,
  })
  const [totalSocios, setTotalSocios] = useState<MetricState>({
    loading: true,
    value: null,
  })
  const [pedidosPendentes, setPedidosPendentes] = useState<MetricState>({
    loading: true,
    value: null,
  })

  useEffect(() => {
    let ativo = true

    Promise.allSettled([
      estoqueService.listarBaixo(),
      associadoService.listar(),
      pedidosService.listar(),
    ]).then(([resEstoque, resAssociados, resPedidos]) => {
      if (!ativo) return

      setEstoqueBaixo({
        loading: false,
        value: resEstoque.status === 'fulfilled' ? resEstoque.value.length : null,
      })

      setTotalSocios({
        loading: false,
        value:
          resAssociados.status === 'fulfilled'
            ? resAssociados.value.length
            : null,
      })

      if (resPedidos.status === 'fulfilled') {
        const pendentes = resPedidos.value.filter(
          (p) =>
            p.statusPedido === 'AGUARDANDO_PAGAMENTO' ||
            p.statusPedido === 'CONFIRMADO',
        ).length
        setPedidosPendentes({ loading: false, value: pendentes })
      } else {
        setPedidosPendentes({ loading: false, value: null })
      }
    })

    return () => {
      ativo = false
    }
  }, [])

  const estoqueBadgeText = estoqueBaixo.loading
    ? '...'
    : estoqueBaixo.value === null
      ? 'Indisponível'
      : estoqueBaixo.value === 0
        ? 'Em dia'
        : `${estoqueBaixo.value} ${estoqueBaixo.value === 1 ? 'baixo' : 'baixos'}`

  const estoqueBadgeClass =
    estoqueBaixo.value !== null && estoqueBaixo.value > 0
      ? 'badge-red'
      : 'badge-dark'

  const cadastrosBadgeText = totalSocios.loading
    ? '...'
    : totalSocios.value === null
      ? 'Indisponível'
      : `${totalSocios.value} ${totalSocios.value === 1 ? 'sócio' : 'sócios'}`

  const pedidosBadgeText = pedidosPendentes.loading
    ? '...'
    : pedidosPendentes.value === null
      ? 'Indisponível'
      : pedidosPendentes.value === 0
        ? 'Em dia'
        : `${pedidosPendentes.value} ${pedidosPendentes.value === 1 ? 'pendente' : 'pendentes'}`

  const cards: DashboardCardItem[] = [
    {
      id: 'estoque',
      path: '/estoque',
      title: 'Estoque',
      subtitle: 'Itens e pedidos',
      badgeText: estoqueBadgeText,
      badgeClass: estoqueBadgeClass,
      cardThemeClass: 'card-theme-green',
      arrowClass: 'arrow-white',
      image: iconeEstoque,
    },
    {
      id: 'escala',
      path: '/escala',
      title: 'Escala de Hoje',
      subtitle: 'Turnos da equipe',
      badgeText: 'Em breve',
      badgeClass: 'badge-dark-green',
      cardThemeClass: 'card-theme-dark',
      arrowClass: 'arrow-green',
      image: iconeEscala,
    },
    {
      id: 'financeiro',
      path: '/financeiro',
      title: 'Financeiro',
      subtitle: 'Receitas × despesas',
      badgeText: 'Disponível',
      badgeClass: 'badge-dark-green',
      cardThemeClass: 'card-theme-white',
      arrowClass: 'arrow-slate',
      image: iconeFinanceiro,
    },
    {
      id: 'reservas',
      path: '/reservas',
      title: 'Reservas',
      subtitle: 'Quadras e salões',
      badgeText: 'Disponível',
      badgeClass: 'badge-white',
      cardThemeClass: 'card-theme-navy',
      arrowClass: 'arrow-white',
      image: iconeReservas,
      imageClass: 'admin-card-img-reservas',
    },
    {
      id: 'cadastros',
      path: '/cadastros',
      title: 'Cadastros',
      subtitle: 'Sócios e funcionários',
      badgeText: cadastrosBadgeText,
      badgeClass: 'badge-mint',
      cardThemeClass: 'card-theme-white',
      arrowClass: 'arrow-green',
      image: iconeCadastros,
    },
    {
      id: 'pedidos',
      path: '/pedidos',
      title: 'Pedidos',
      subtitle: 'Comandas e consumo',
      badgeText: pedidosBadgeText,
      badgeClass: 'badge-dark',
      cardThemeClass: 'card-theme-green',
      arrowClass: 'arrow-white',
      image: iconePedidos,
    },
  ]

  return (
    <section className="admin-home">
      <h1 className="admin-home-title">Painel Administrativo</h1>

      <div className="admin-cards-grid">
        {cards.map((card) => (
          <button
            key={card.id}
            type="button"
            className={`admin-card ${card.cardThemeClass}`}
            onClick={() => navigate(card.path)}
          >
            <div className="admin-card-top">
              <div>
                <span className="admin-card-title">{card.title}</span>
                <span className="admin-card-subtitle">{card.subtitle}</span>
              </div>
              <span className={`admin-card-badge ${card.badgeClass}`}>
                {card.badgeText}
              </span>
            </div>

            <div className="admin-card-illustration">
              <img
                src={card.image}
                alt={card.title}
                className={`admin-card-img ${card.imageClass ?? ''}`}
              />
            </div>

            <div className="admin-card-footer">
              <span>Abrir módulo</span>
              <span className={`admin-card-arrow ${card.arrowClass}`}>
                <ArrowRight size={16} strokeWidth={2.5} />
              </span>
            </div>
          </button>
        ))}
      </div>
    </section>
  )
}
