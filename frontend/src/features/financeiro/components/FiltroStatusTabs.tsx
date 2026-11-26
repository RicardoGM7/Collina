import './mensalidadesShared.css'

export interface FiltroStatusTabOption<T extends string> {
  valor: T
  label: string
  quantidade?: number
}

interface FiltroStatusTabsProps<T extends string> {
  opcoes: FiltroStatusTabOption<T>[]
  ativo: T
  onChange: (valor: T) => void
}

export function FiltroStatusTabs<T extends string>({
  opcoes,
  ativo,
  onChange,
}: FiltroStatusTabsProps<T>) {
  return (
    <div className="mens-tabs" role="tablist">
      {opcoes.map((opt) => {
        const isActive = opt.valor === ativo
        return (
          <button
            key={opt.valor}
            type="button"
            role="tab"
            aria-selected={isActive}
            className={`mens-tab-btn${isActive ? ' is-active' : ''}`}
            onClick={() => onChange(opt.valor)}
          >
            <span>{opt.label}</span>
            {opt.quantidade !== undefined && <span>({opt.quantidade})</span>}
          </button>
        )
      })}
    </div>
  )
}
