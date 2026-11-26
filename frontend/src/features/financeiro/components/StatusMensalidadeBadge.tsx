import type { StatusMensalidade } from '../../../types/financeiro'
import './mensalidadesShared.css'

const STATUS_META: Record<StatusMensalidade, { label: string; className: string }> = {
  EM_ABERTO: { label: 'EM ABERTO', className: 'mens-badge-em-aberto' },
  PENDENTE: { label: 'PENDENTE', className: 'mens-badge-pendente' },
  PAGA: { label: 'PAGA', className: 'mens-badge-paga' },
  ATRASADA: { label: 'ATRASADA', className: 'mens-badge-atrasada' },
  CANCELADA: { label: 'CANCELADA', className: 'mens-badge-cancelada' },
}

interface StatusMensalidadeBadgeProps {
  status: StatusMensalidade
}

export function StatusMensalidadeBadge({ status }: StatusMensalidadeBadgeProps) {
  const meta = STATUS_META[status] ?? { label: status, className: 'mens-badge-cancelada' }
  return <span className={`mens-badge ${meta.className}`}>{meta.label}</span>
}
