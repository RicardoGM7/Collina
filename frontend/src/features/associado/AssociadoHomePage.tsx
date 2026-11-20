import { ArrowRight, Trophy } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import espacoAssociado from '../../assets/EspacoAssociado.png'
import mensalidadeAssociado from '../../assets/MensalidadeAssociado.png'
import pedidosAssociado from '../../assets/PedidosAssociado.png'
import './AssociadoHomePage.css'

interface QuickActionCard {
  id: string
  path: string
  title: string
  subtitle: string
  badgeText?: string
  badgeClass?: string
  cardThemeClass: string
  footerText: string
  arrowClass: string
  image: string
}

const QUICK_ACTION_CARDS: QuickActionCard[] = [
  {
    id: 'reservar-espaco',
    path: '/associado/reservas',
    title: 'Reservar Espaço',
    subtitle: 'Quadras, campos e quiosques',
    badgeText: '1 hoje',
    badgeClass: 'assoc-badge-dark',
    cardThemeClass: 'assoc-card-green',
    footerText: 'Nova reserva',
    arrowClass: 'assoc-arrow-white',
    image: espacoAssociado,
  },
  {
    id: 'mensalidades',
    path: '/associado/financeiro',
    title: 'Mensalidades',
    subtitle: 'Pagar via PIX ou Boleto',
    badgeText: 'R$ 430,00',
    badgeClass: 'assoc-badge-rose',
    cardThemeClass: 'assoc-card-dark',
    footerText: 'Pagar mensalidade',
    arrowClass: 'assoc-arrow-green',
    image: mensalidadeAssociado,
  },
  {
    id: 'pedidos',
    path: '/associado/pedidos',
    title: 'Pedidos',
    subtitle: 'Compre produtos do clube',
    cardThemeClass: 'assoc-card-white',
    footerText: 'Acessar',
    arrowClass: 'assoc-arrow-slate',
    image: pedidosAssociado,
  },
]

export function AssociadoHomePage() {
  const navigate = useNavigate()

  return (
    <section className="assoc-home">
      <div className="assoc-home-container">
        {/* 1. Banner de Boas-Vindas */}
        <div className="assoc-hero-card">
          <div className="assoc-hero-content">
            <span className="assoc-plan-badge">Plano Familiar Ouro</span>
            <h1 className="assoc-hero-title">Olá, Nome do Titular!</h1>
            <p className="assoc-hero-highlight">
              <strong>1 reserva confirmada</strong> para hoje no clube.
            </p>
            <p className="assoc-hero-sub">
              O clube está com as quadras do parque abertas até as 22h.
            </p>
          </div>
          <div className="assoc-hero-glow" aria-hidden="true" />
        </div>

        {/* 2. Ações Rápidas (Mesmo tamanho dos cards da página de Admin) */}
        <div className="assoc-section-block">
          <h2 className="assoc-section-heading">Ações Rápidas</h2>

          <div className="assoc-cards-grid">
            {QUICK_ACTION_CARDS.map((card) => (
              <button
                key={card.id}
                type="button"
                className={`assoc-action-card ${card.cardThemeClass}`}
                onClick={() => navigate(card.path)}
              >
                <div className="assoc-card-top">
                  <div>
                    <span className="assoc-card-title">{card.title}</span>
                    <span className="assoc-card-subtitle">{card.subtitle}</span>
                  </div>
                  {card.badgeText && (
                    <span
                      className={`assoc-card-badge ${card.badgeClass ?? ''}`}
                    >
                      {card.badgeText}
                    </span>
                  )}
                </div>

                <div className="assoc-card-illustration">
                  <img
                    src={card.image}
                    alt={card.title}
                    className="assoc-card-img"
                  />
                </div>

                <div className="assoc-card-footer">
                  <span>{card.footerText}</span>
                  <span className={`assoc-card-arrow ${card.arrowClass}`}>
                    <ArrowRight size={16} strokeWidth={2.5} />
                  </span>
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* 3. Minhas Próximas Reservas */}
        <div className="assoc-reservations-card">
          <div className="assoc-panel-header">
            <div className="assoc-panel-title-wrap">
              <span className="assoc-green-dot" aria-hidden="true" />
              <h2 className="assoc-panel-title">Minhas Próximas Reservas</h2>
            </div>
            <button
              type="button"
              className="assoc-panel-link"
              onClick={() => navigate('/associado/reservas')}
            >
              Nova Reserva +
            </button>
          </div>

          <div className="assoc-reservations-list">
            {/* Reserva 1 — Hoje */}
            <div className="assoc-reservation-row assoc-reservation-row--today">
              <div className="assoc-reservation-left">
                <div className="assoc-date-badge assoc-date-badge--green">
                  <span className="assoc-date-top">HOJE</span>
                  <span className="assoc-date-bottom">18:00</span>
                </div>
                <div className="assoc-reservation-info">
                  <div className="assoc-reservation-title-line">
                    <strong className="assoc-reservation-name">
                      Quadra de Tênis Saibro 2
                    </strong>
                    <span className="assoc-res-status assoc-res-status--confirmed">
                      Confirmada
                    </span>
                  </div>
                  <p className="assoc-reservation-meta">
                    18:00 às 19:30 • Com dependente:{' '}
                    <strong>Lucas Gomes</strong>
                  </p>
                </div>
              </div>

              <div className="assoc-reservation-actions">
                <button type="button" className="assoc-btn-outline">
                  Detalhes
                </button>
                <button type="button" className="assoc-btn-cancel-soft">
                  Cancelar
                </button>
              </div>
            </div>

            {/* Reserva 2 — Agendada */}
            <div className="assoc-reservation-row assoc-reservation-row--upcoming">
              <div className="assoc-reservation-left">
                <div className="assoc-date-badge assoc-date-badge--slate">
                  <span className="assoc-date-day">15</span>
                  <span className="assoc-date-month">NOV</span>
                </div>
                <div className="assoc-reservation-info">
                  <div className="assoc-reservation-title-line">
                    <strong className="assoc-reservation-name">
                      Quiosque da Churrasqueira 4
                    </strong>
                    <span className="assoc-res-status assoc-res-status--scheduled">
                      Agendada
                    </span>
                  </div>
                  <p className="assoc-reservation-meta">
                    Sábado das 11:00 às 17:00 • 8 convidados cadastrados
                  </p>
                </div>
              </div>

              <div className="assoc-reservation-actions">
                <button type="button" className="assoc-btn-outline">
                  Ver Convites
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* 4. Acontecendo no Clube */}
        <div className="assoc-events-card">
          <div className="assoc-panel-header">
            <div className="assoc-panel-title-wrap">
              <Trophy size={15} className="assoc-trophy-icon" />
              <h2 className="assoc-panel-title">Acontecendo no Clube</h2>
            </div>
            <button type="button" className="assoc-panel-link">
              Ver todos
            </button>
          </div>

          <div className="assoc-events-list">
            <div className="assoc-event-box">
              <span className="assoc-event-tag assoc-event-tag--green">
                Inscrições Abertas
              </span>
              <strong className="assoc-event-title">
                Torneio de Beach Tennis Collina
              </strong>
              <p className="assoc-event-desc">
                Sábado e Domingo, dias 22 e 23/11.
                <br />
                Categorias A, B e Mista.
              </p>
              <button type="button" className="assoc-btn-event">
                Inscrever-se
              </button>
            </div>

            <div className="assoc-event-box">
              <span className="assoc-event-tag assoc-event-tag--blue">
                Social
              </span>
              <strong className="assoc-event-title">
                Música ao Vivo no Deck da Piscina
              </strong>
              <p className="assoc-event-desc">
                Sexta-feira a partir das 19h.
                <br />
                Entrada gratuita para sócios e dependentes.
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
