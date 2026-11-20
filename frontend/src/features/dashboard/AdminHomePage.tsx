import { ArrowRight } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import iconeCadastros from '../../assets/iconeCadastros.png'
import iconeEscala from '../../assets/iconeEscala.png'
import iconeEstoque from '../../assets/iconeEstoque.png'
import iconeFinanceiro from '../../assets/iconeFinanceiro.png'
import iconePedidos from '../../assets/iconePedidos.png'
import iconeReservas from '../../assets/iconeReservas.png'
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

const ADMIN_CARDS: DashboardCardItem[] = [
  {
    id: 'estoque',
    path: '/admin/estoque',
    title: 'Estoque',
    subtitle: 'Itens e pedidos',
    badgeText: '2 baixos',
    badgeClass: 'badge-red',
    cardThemeClass: 'card-theme-green',
    arrowClass: 'arrow-white',
    image: iconeEstoque,
  },
  {
    id: 'escala',
    path: '/admin/escala',
    title: 'Escala de Hoje',
    subtitle: 'Turnos da equipe',
    badgeText: '4 turnos',
    badgeClass: 'badge-dark-green',
    cardThemeClass: 'card-theme-dark',
    arrowClass: 'arrow-green',
    image: iconeEscala,
  },
  {
    id: 'financeiro',
    path: '/admin/financeiro',
    title: 'Financeiro',
    subtitle: 'Receitas × despesas',
    badgeText: '+2.189',
    badgeClass: 'badge-mint',
    cardThemeClass: 'card-theme-white',
    arrowClass: 'arrow-slate',
    image: iconeFinanceiro,
  },
  {
    id: 'reservas',
    path: '/admin/reservas',
    title: 'Reservas',
    subtitle: 'Quadras e salões',
    badgeText: '4 hoje',
    badgeClass: 'badge-white',
    cardThemeClass: 'card-theme-navy',
    arrowClass: 'arrow-white',
    image: iconeReservas,
    imageClass: 'admin-card-img-reservas',
  },
  {
    id: 'cadastros',
    path: '/admin/cadastros',
    title: 'Cadastros',
    subtitle: 'Sócios e funcionários',
    badgeText: '+2.189',
    badgeClass: 'badge-mint',
    cardThemeClass: 'card-theme-white',
    arrowClass: 'arrow-green',
    image: iconeCadastros,
  },
  {
    id: 'pedidos',
    path: '/admin/pedidos',
    title: 'Pedidos',
    subtitle: 'Comandas e consumo',
    badgeText: '4 pendentes',
    badgeClass: 'badge-dark',
    cardThemeClass: 'card-theme-green',
    arrowClass: 'arrow-white',
    image: iconePedidos,
  },
]

export function AdminHomePage() {
  const navigate = useNavigate()

  return (
    <section className="admin-home">
      <h1 className="admin-home-title">Painel Administrativo</h1>

      <div className="admin-cards-grid">
        {ADMIN_CARDS.map((card) => (
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
