import { useMemo } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { NOMES_MESES, celulasDoMes, formatarData, formatarDataComDia, somarMeses } from './espacosUtils'

// 'minha' = o associado já tem reserva no dia; 'parcial' = outras pessoas já ocupam alguns horários;
// 'lotado' = não sobrou horário livre para o espaço escolhido.
export interface MarcaDia {
  minha?: boolean
  outros?: 'parcial' | 'lotado'
}

interface Props {
  mes: string // 'AAAA-MM' exibido
  onMesChange: (anoMes: string) => void
  selecionado: string
  onSelecionar: (iso: string) => void
  hoje: string
  maxData: string
  marcas: Record<string, MarcaDia>
}

const SEMANA = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S']

export function CalendarioReservas({ mes, onMesChange, selecionado, onSelecionar, hoje, maxData, marcas }: Props) {
  const celulas = useMemo(() => celulasDoMes(mes), [mes])
  const [ano, numMes] = mes.split('-').map(Number)
  const anoAtual = Number(hoje.slice(0, 4))
  const anos = useMemo(() => {
    const de = Math.min(anoAtual - 1, ano)
    const ate = Math.max(anoAtual + 15, ano)
    return Array.from({ length: ate - de + 1 }, (_, i) => de + i)
  }, [anoAtual, ano])

  const mesInteiroFora = celulas.every((iso) => iso === null || iso < hoje || iso > maxData)
  const mesDoHoje = hoje.slice(0, 7)

  function trocarAno(novo: number) {
    onMesChange(`${novo}-${String(numMes).padStart(2, '0')}`)
  }

  return (
    <div className="esp-cal">
      <div className="esp-cal-nav">
        <button type="button" className="esp-cal-seta" aria-label="Mês anterior" onClick={() => onMesChange(somarMeses(mes, -1))}>
          <ChevronLeft size={18} />
        </button>
        <div className="esp-cal-titulo">
          <select aria-label="Mês" value={numMes} onChange={(e) => onMesChange(`${ano}-${String(e.target.value).padStart(2, '0')}`)}>
            {NOMES_MESES.map((nome, i) => (
              <option key={nome} value={i + 1}>{nome}</option>
            ))}
          </select>
          <select aria-label="Ano" value={ano} onChange={(e) => trocarAno(Number(e.target.value))}>
            {anos.map((a) => (
              <option key={a} value={a}>{a}</option>
            ))}
          </select>
        </div>
        <button type="button" className="esp-cal-seta" aria-label="Próximo mês" onClick={() => onMesChange(somarMeses(mes, 1))}>
          <ChevronRight size={18} />
        </button>
      </div>

      {mes !== mesDoHoje && (
        <button type="button" className="esp-link esp-cal-hoje" onClick={() => onMesChange(mesDoHoje)}>Voltar para o mês atual</button>
      )}

      <div className="esp-cal-grade" role="group" aria-label={`${NOMES_MESES[numMes - 1]} de ${ano}`}>
        {SEMANA.map((l, i) => (
          <span key={i} className="esp-cal-semana" aria-hidden="true">{l}</span>
        ))}
        {celulas.map((iso, i) => {
          if (!iso) return <span key={`v${i}`} />
          const marca = marcas[iso] ?? {}
          const passado = iso < hoje
          const longe = iso > maxData
          const desabilitado = passado || longe
          const classes = [
            'esp-cal-dia',
            iso === hoje ? 'esp-cal-dia--hoje' : '',
            iso === selecionado ? 'esp-cal-dia--sel' : '',
            marca.minha ? 'esp-cal-dia--minha' : '',
            desabilitado ? 'esp-cal-dia--off' : '',
          ].filter(Boolean).join(' ')
          const partes = [formatarDataComDia(iso)]
          if (marca.minha) partes.push('você tem reserva')
          if (marca.outros === 'parcial') partes.push('alguns horários já reservados por outras pessoas')
          if (marca.outros === 'lotado') partes.push('sem horários livres')
          if (longe) partes.push(`reservas abertas até ${formatarData(maxData)}`)
          if (passado) partes.push('data passada')
          return (
            <button
              key={iso}
              type="button"
              className={classes}
              disabled={desabilitado}
              aria-pressed={iso === selecionado}
              aria-label={partes.join(', ')}
              title={partes.join(' · ')}
              onClick={() => onSelecionar(iso)}
            >
              <span className="esp-cal-num">{Number(iso.slice(8))}</span>
              {marca.outros && !desabilitado && (
                <span className={`esp-cal-ponto ${marca.outros === 'lotado' ? 'esp-cal-ponto--lotado' : 'esp-cal-ponto--parcial'}`} />
              )}
            </button>
          )
        })}
      </div>

      {mesInteiroFora && (
        <p className="esp-muted esp-cal-aviso">
          {mes < mesDoHoje ? 'Este mês já passou.' : `Reservas abertas até ${formatarData(maxData)}. Os dias seguintes ainda não podem ser reservados.`}
        </p>
      )}

      <ul className="esp-cal-legenda" aria-label="Legenda do calendário">
        <li><span className="esp-cal-amostra esp-cal-amostra--minha" /> Sua reserva</li>
        <li><span className="esp-cal-ponto esp-cal-ponto--parcial" /> Poucos horários</li>
        <li><span className="esp-cal-ponto esp-cal-ponto--lotado" /> Lotado</li>
      </ul>
    </div>
  )
}
