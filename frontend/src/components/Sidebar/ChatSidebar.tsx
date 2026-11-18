import {
  useEffect,
  useRef,
  useState,
  type CSSProperties,
  type FormEvent,
  type TransitionEvent,
} from 'react'
import { ArrowRight, ChevronLeft, ChevronRight, Sparkles } from 'lucide-react'
import './ChatSidebar.css'

const SUGGESTIONS = ['Resumo do mês', 'Reservas de hoje', 'Registrar saída']

function staggerStyle(index: number): CSSProperties {
  return { '--stagger-index': index } as CSSProperties
}

export function ChatSidebar() {
  const [isOpen, setIsOpen] = useState(true)
  const [isAnimating, setIsAnimating] = useState(false)
  const [input, setInput] = useState('')

  const sidebarRef = useRef<HTMLElement>(null)
  const minimizedRef = useRef<HTMLDivElement>(null)
  const animTimerRef = useRef<number | null>(null)

  function handleToggle(nextOpen: boolean) {
    if (animTimerRef.current !== null) {
      window.clearTimeout(animTimerRef.current)
    }
    setIsAnimating(true)
    setIsOpen(nextOpen)

    const fallbackDuration = nextOpen ? 320 : 220
    animTimerRef.current = window.setTimeout(() => {
      setIsAnimating(false)
      animTimerRef.current = null
    }, fallbackDuration)
  }

  function handleSidebarTransitionEnd(event: TransitionEvent<HTMLElement>) {
    if (event.target !== sidebarRef.current) return
    if (event.propertyName === 'transform' || event.propertyName === 'opacity') {
      if (animTimerRef.current !== null) {
        window.clearTimeout(animTimerRef.current)
        animTimerRef.current = null
      }
      setIsAnimating(false)
    }
  }

  // Aplica inert via DOM ref para impedir foco no painel fechado e no card oculto
  useEffect(() => {
    if (sidebarRef.current) {
      sidebarRef.current.inert = !isOpen && !isAnimating
    }
    if (minimizedRef.current) {
      minimizedRef.current.inert = isOpen
    }
  }, [isOpen, isAnimating])

  useEffect(() => {
    return () => {
      if (animTimerRef.current !== null) {
        window.clearTimeout(animTimerRef.current)
      }
    }
  }, [])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!input.trim()) return
    setInput('')
  }

  return (
    <>
      {/* Card flutuante visível quando o chat está fechado */}
      <div
        ref={minimizedRef}
        className={`chat-minimized-card ${
          isOpen ? 'chat-minimized-card--hidden' : 'chat-minimized-card--visible'
        }`}
        aria-hidden={isOpen}
        onClick={() => handleToggle(true)}
        title="Maximizar Carollina AI"
      >
        <button
          type="button"
          className="chat-collapse-btn"
          aria-label="Abrir chat Carollina AI"
          onClick={(e) => {
            e.stopPropagation()
            handleToggle(true)
          }}
          title="Maximizar"
        >
          <ChevronLeft size={16} className="chat-minimized-arrow" />
        </button>

        <div className="chat-brand">
          <div className="chat-logo">
            <Sparkles size={17} />
          </div>
          <div>
            <span className="chat-name">Carollina AI</span>
            <span className="chat-status">
              <span className="chat-status-dot" />
              Online
            </span>
          </div>
        </div>
      </div>

      {/* Painel lateral animado com slide horizontal + fade */}
      <aside
        ref={sidebarRef}
        className={`chat-sidebar ${
          isOpen ? 'chat-sidebar--open' : 'chat-sidebar--closed'
        } ${isAnimating ? 'chat-sidebar--animating' : ''}`}
        aria-hidden={!isOpen}
        onTransitionEnd={handleSidebarTransitionEnd}
      >
        <div className="chat-header">
          <div className="chat-brand">
            <div className="chat-logo">
              <Sparkles size={17} />
            </div>
            <div>
              <span className="chat-name">Carollina AI</span>
              <span className="chat-status">
                <span className="chat-status-dot" />
                Online
              </span>
            </div>
          </div>

          <button
            type="button"
            className="chat-collapse-btn"
            aria-label="Fechar chat Carollina AI"
            onClick={() => handleToggle(false)}
            title="Minimizar"
          >
            <ChevronRight size={16} className="chat-arrow-icon" />
          </button>
        </div>

        <div className="chat-messages">
          <div
            className="chat-bubble chat-bubble-ai chat-stagger-item"
            style={staggerStyle(0)}
          >
            Olá! Posso consultar reservas, estoque e finanças, e registrar
            movimentações. Como ajudo?
          </div>

          <div
            className="chat-bubble chat-bubble-user chat-stagger-item"
            style={staggerStyle(1)}
          >
            Quais itens estão acabando?
          </div>

          <div
            className="chat-bubble chat-bubble-ai chat-stagger-item"
            style={staggerStyle(2)}
          >
            2 itens estão abaixo do mínimo: <strong>Boia de piscina</strong> (4
            de 5) e <strong>Bolas de basquete</strong> (1 de 3). Quer registrar
            uma entrada?
          </div>

          <div className="chat-suggestions">
            {SUGGESTIONS.map((item, idx) => (
              <button
                key={item}
                type="button"
                className="chat-chip chat-stagger-item"
                style={staggerStyle(3 + idx)}
              >
                {item}
              </button>
            ))}
          </div>
        </div>

        <div className="chat-footer">
          <form className="chat-form" onSubmit={handleSubmit}>
            <input
              type="text"
              className="chat-input"
              placeholder="Escreva sua pergunta..."
              value={input}
              onChange={(e) => setInput(e.target.value)}
            />
            <button type="submit" className="chat-send-btn" title="Enviar">
              <ArrowRight size={17} />
            </button>
          </form>
        </div>
      </aside>
    </>
  )
}
