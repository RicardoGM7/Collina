interface AvisoProps {
  tipo: 'erro' | 'sucesso' | 'info'
  mensagem: string | null
  onFechar?: () => void
}

export default function Aviso({ tipo, mensagem, onFechar }: AvisoProps) {
  if (!mensagem) return null
  return (
    <div className={`as-aviso as-aviso-${tipo}`} role={tipo === 'erro' ? 'alert' : 'status'}>
      <span>{mensagem}</span>
      {onFechar && (
        <button type="button" className="as-icon-btn" onClick={onFechar} aria-label="Dispensar">
          ×
        </button>
      )}
    </div>
  )
}
