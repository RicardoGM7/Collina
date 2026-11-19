import type { ReactNode } from 'react'

interface ModalProps {
  titulo: string
  onFechar: () => void
  children: ReactNode
}

export default function Modal({ titulo, onFechar, children }: ModalProps) {
  return (
    <div className="as-modal-overlay" onClick={onFechar}>
      <div
        className="as-modal"
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="as-modal-header">
          <h2>{titulo}</h2>
          <button type="button" className="as-icon-btn" onClick={onFechar} aria-label="Fechar">
            ×
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
