import { formatCurrency } from '../../../utils/formatters'
import './mensalidadesShared.css'

interface ValorMonetarioProps {
  valor: number
  riscado?: boolean
  className?: string
}

export function ValorMonetario({ valor, riscado = false, className = '' }: ValorMonetarioProps) {
  const classes = ['mens-valor', riscado ? 'is-riscado' : '', className].filter(Boolean).join(' ')
  return <span className={classes}>{formatCurrency(Number(valor ?? 0))}</span>
}
