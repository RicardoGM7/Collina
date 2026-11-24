import { useState } from 'react'
import toast from 'react-hot-toast'
import { AlertCircle, Clock, Users } from 'lucide-react'
import { Modal } from '../../components/Modal/Modal'
import { getApiErrorMessage } from '../../services/api'
import { espacosService, type Reserva } from '../../services/espacosService'
import { formatarData, partesDaData, rotuloConvidados } from './espacosUtils'
import './espacos.css'

interface Props {
  reserva: Reserva | null
  onClose: () => void
  /** Chamado depois do cancelamento bem-sucedido (recarregar a lista). */
  onCancelada: () => void
}

// RF-AS09 / UC-GE03: cancelamento sem penalidade, até o dia anterior (o back-end valida o prazo).
export function CancelarReservaModal({ reserva, onClose, onCancelada }: Props) {
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState('')

  async function confirmar() {
    if (!reserva) return
    setEnviando(true)
    setErro('')
    try {
      await espacosService.cancelarReserva(reserva.id)
      toast.success('Reserva cancelada. O horário foi liberado.')
      onCancelada()
      onClose()
    } catch (e) {
      setErro(getApiErrorMessage(e, 'Não foi possível cancelar a reserva.'))
    } finally {
      setEnviando(false)
    }
  }

  function fechar() {
    setErro('')
    onClose()
  }

  return (
    <Modal
      isOpen={reserva !== null}
      onClose={fechar}
      title="Cancelar reserva"
      subtitle="O cancelamento é sem penalidade até o dia anterior"
      maxWidth={440}
      footer={
        <div className="esp-modal-actions esp-modal-actions--manter-primeiro">
          <button type="button" className="esp-btn esp-btn--ghost" onClick={fechar} disabled={enviando}>
            Manter reserva
          </button>
          <button type="button" className="esp-btn esp-btn--danger" onClick={confirmar} disabled={enviando}>
            {enviando ? 'Cancelando…' : 'Cancelar reserva'}
          </button>
        </div>
      }
    >
      {reserva && (
        <div className="esp-modal-body">
          <div className="esp-cancel-resumo">
            <div className="esp-res-data" aria-hidden="true">
              <span>{partesDaData(reserva.dataUso).semana}</span>
              <strong>{partesDaData(reserva.dataUso).dia}</strong>
              <span>{partesDaData(reserva.dataUso).mes}</span>
            </div>
            <div className="esp-res-info">
              <strong>{reserva.espacoNome}</strong>
              <span className="esp-meta">{formatarData(reserva.dataUso)}</span>
              <div className="esp-res-linha">
                <span className="esp-meta"><Clock size={14} /> {reserva.horaInicio} às {reserva.horaFim}</span>
                <span className="esp-meta"><Users size={14} /> {rotuloConvidados(reserva.qtdConvidados)}</span>
              </div>
            </div>
          </div>
          {reserva.titularNome && (
            <p className="esp-muted">Associado: <strong>{reserva.titularNome}</strong></p>
          )}
          <p className="esp-muted">Ao cancelar, o horário volta a ficar livre para outros associados.</p>
          {erro && (
            <p className="esp-alert esp-alert--erro" role="alert">
              <AlertCircle size={16} />
              {erro}
            </p>
          )}
        </div>
      )}
    </Modal>
  )
}
