import type { ReactNode } from 'react'
import { Inbox } from 'lucide-react'
import './mensalidadesShared.css'

interface EstadoVazioProps {
  titulo: string
  descricao?: string
  icone?: ReactNode
  acao?: ReactNode
}

export function EstadoVazio({ titulo, descricao, icone, acao }: EstadoVazioProps) {
  return (
    <div className="mens-estado-vazio">
      <div className="mens-estado-vazio-icone">{icone ?? <Inbox size={28} />}</div>
      <p className="mens-estado-vazio-titulo">{titulo}</p>
      {descricao && <p className="mens-estado-vazio-desc">{descricao}</p>}
      {acao && <div>{acao}</div>}
    </div>
  )
}
