import type { StatusPagamento } from '../../../types/financeiro'
import './mensalidadesShared.css'

const STATUS_META: Record<StatusPagamento, { label: string; className: string }> = {
  AGUARDANDO: { label: 'AGUARDANDO', className: 'mens-badge-aguardando' },
  CONFIRMADO: { label: 'CONFIRMADO', className: 'mens-badge-confirmado' },
  RECUSADO: { label: 'RECUSADO', className: 'mens-badge-recusado' },
  EXPIRADO: { label: 'EXPIRADO', className: 'mens-badge-expirado' },
}

interface StatusPagamentoBadgeProps {
  status: StatusPagamento
  sufixo?: string
}

export function StatusPagamentoBadge({ status, sufixo }: StatusPagamentoBadgeProps) {
  const meta = STATUS_META[status] ?? { label: status, className: 'mens-badge-expirado' }
  return (
    <span className={`mens-badge ${meta.className}`}>
      {meta.label}
      {sufixo ? ` - ${sufixo}` : ''}
    </span>
  )
}
