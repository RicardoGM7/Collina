import { Link } from 'react-router-dom'
import { ArrowRight, CalendarCheck, CreditCard, MapPin, Users } from 'lucide-react'
import './RecepcaoHomePage.css'

export function RecepcaoHomePage() {
  return (
    <div className="mens-rec-page">
      <header className="mens-rec-header">
        <h1>Painel da Recepção</h1>
        <p>Acesse os módulos de atendimento ao associado, mensalidades e reservas.</p>
      </header>

      <section className="mens-rec-grid">
        <Link to="/mensalidades" className="mens-rec-card">
          <div className="mens-rec-card-top">
            <span className="mens-rec-icon">
              <CreditCard size={20} />
            </span>
            <ArrowRight size={18} />
          </div>
          <div>
            <h2>Mensalidades</h2>
            <p>Consultar histórico por CPF, registrar pagamento manual e lançar gastos avulsos.</p>
          </div>
        </Link>

        <Link to="/cadastros" className="mens-rec-card">
          <div className="mens-rec-card-top">
            <span className="mens-rec-icon">
              <Users size={20} />
            </span>
            <ArrowRight size={18} />
          </div>
          <div>
            <h2>Associados</h2>
            <p>Gerenciar titulares, dependentes e situação cadastral.</p>
          </div>
        </Link>

        <Link to="/reservas" className="mens-rec-card">
          <div className="mens-rec-card-top">
            <span className="mens-rec-icon">
              <CalendarCheck size={20} />
            </span>
            <ArrowRight size={18} />
          </div>
          <div>
            <h2>Reservas</h2>
            <p>Acompanhar e gerenciar reservas de espaços do clube.</p>
          </div>
        </Link>

        <Link to="/espacos" className="mens-rec-card">
          <div className="mens-rec-card-top">
            <span className="mens-rec-icon">
              <MapPin size={20} />
            </span>
            <ArrowRight size={18} />
          </div>
          <div>
            <h2>Espaços</h2>
            <p>Consultar e configurar quadras, salões e blocos de horário.</p>
          </div>
        </Link>
      </section>
    </div>
  )
}
