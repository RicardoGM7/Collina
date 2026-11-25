import { useEffect, useState } from 'react'
import { Calendar, Clock } from 'lucide-react'
import { espacosService, type Reserva } from '../../services/espacosService'
import { hojeISO, partesDaData } from './espacosUtils'
import './espacos.css'

// Lista das próximas reservas confirmadas na home do associado (reaproveita estilos da home).
export function ProximasReservas() {
  const [reservas, setReservas] = useState<Reserva[] | null>(null)
  const [falhou, setFalhou] = useState(false)

  useEffect(() => {
    espacosService
      .minhasReservas()
      .then(setReservas)
      .catch(() => setFalhou(true))
  }, [])

  if (falhou) return <p className="esp-muted">Não foi possível carregar suas reservas agora.</p>
  if (reservas === null) return <p className="esp-muted">Carregando reservas…</p>

  const hoje = hojeISO()
  const proximas = reservas
    .filter((r) => r.status === 'CONFIRMADA' && r.dataUso >= hoje)
    .sort((a, b) => (a.dataUso + a.horaInicio).localeCompare(b.dataUso + b.horaInicio))
    .slice(0, 3)

  if (proximas.length === 0) {
    return (
      <div className="reservation-placeholder-box">
        <div className="placeholder-icon-wrap"><Calendar size={26} /></div>
        <div className="placeholder-text-wrap">
          <h3 className="placeholder-title">Nenhuma reserva agendada</h3>
          <p className="placeholder-desc">Reserve uma quadra, campo ou quiosque em “Nova Reserva”.</p>
        </div>
      </div>
    )
  }

  return (
    <ul className="esp-list">
      {proximas.map((r) => {
        const d = partesDaData(r.dataUso)
        return (
          <li key={r.id} className="esp-res">
            <div className="esp-res-data" aria-hidden="true">
              <span>{d.semana}</span>
              <strong>{d.dia}</strong>
              <span>{d.mes}</span>
            </div>
            <div className="esp-res-info">
              <strong>{r.espacoNome}</strong>
              <span className="esp-meta"><Clock size={14} /> {r.horaInicio} às {r.horaFim}</span>
            </div>
            <span className="esp-badge esp-badge--ok">Confirmada</span>
          </li>
        )
      })}
    </ul>
  )
}
