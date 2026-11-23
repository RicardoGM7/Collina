import {
  useEffect,
  useRef,
  useState,
  type CSSProperties,
  type FormEvent,
  type ReactNode,
  type TransitionEvent,
} from 'react'
import {
  ArrowRight,
  Check,
  ChevronLeft,
  ChevronRight,
  RotateCcw,
  ShieldAlert,
  ShieldCheck,
  Sparkles,
  X,
} from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { getApiErrorMessage } from '../../services/api'
import { chatService, type AcaoPendenteDTO } from '../../services/chatService'
import './ChatSidebar.css'

type StatusAcaoUi =
  | 'PENDENTE'
  | 'PROCESSANDO'
  | 'CONFIRMADA'
  | 'CANCELADA'
  | 'EXPIRADA'
  | 'ERRO'

interface AcaoPendenteUi extends AcaoPendenteDTO {
  status: StatusAcaoUi
  resultado?: string
}

interface ChatMessage {
  id: string
  role: 'ai' | 'user'
  text: string
  isError?: boolean
  acoes?: AcaoPendenteUi[]
}

const ADMIN_SUGGESTIONS = [
  'Quais itens estão acabando?',
  'Pedidos em aberto',
  'Ver cardápio disponível',
  'Listar todos os produtos',
]

function staggerStyle(index: number): CSSProperties {
  return { '--stagger-index': index } as CSSProperties
}

function criarMensagemInicial(nomeUsuario?: string | null): ChatMessage {
  const primeiroNome = nomeUsuario?.trim().split(/\s+/)[0]
  const saudacao = primeiroNome ? `Olá, ${primeiroNome}!` : 'Olá!'
  return {
    id: 'welcome',
    role: 'ai',
    text: `${saudacao} Sou a Carollina AI. Posso consultar itens com estoque baixo, exibir o cardápio, registrar movimentações de estoque e gerenciar pedidos. Como posso ajudar?`,
  }
}

/**
 * Renderiza trechos **em negrito** de forma segura (sem innerHTML) preservando quebras de linha via CSS.
 */
function renderFormattedText(text: string): ReactNode {
  const parts = text.split(/(\*\*[^*]+\*\*)/g)
  return parts.map((part, idx) => {
    if (part.startsWith('**') && part.endsWith('**') && part.length > 4) {
      return <strong key={idx}>{part.slice(2, -2)}</strong>
    }
    return part
  })
}

export function ChatSidebar() {
  const { usuario } = useAuth()
  const [isOpen, setIsOpen] = useState(true)
  const [isAnimating, setIsAnimating] = useState(false)
  const [input, setInput] = useState('')
  const [conversaId, setConversaId] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>(() => [
    criarMensagemInicial(usuario?.nome),
  ])

  const sidebarRef = useRef<HTMLElement>(null)
  const minimizedRef = useRef<HTMLDivElement>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const animTimerRef = useRef<number | null>(null)

  // Reinicia a conversa ao trocar de usuário logado
  useEffect(() => {
    setConversaId(null)
    setMessages([criarMensagemInicial(usuario?.nome)])
    setInput('')
    setIsLoading(false)
  }, [usuario?.uid, usuario?.nome])

  // Rolagem automática para a última mensagem
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isLoading])

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

  function handleResetConversation() {
    if (isLoading) return
    if (conversaId) {
      void chatService.limparConversa(conversaId).catch(() => undefined)
    }
    setConversaId(null)
    setMessages([criarMensagemInicial(usuario?.nome)])
    setInput('')
    inputRef.current?.focus()
  }

  function atualizarEstadoAcao(
    messageId: string,
    acaoId: string,
    patch: Partial<AcaoPendenteUi>,
  ) {
    setMessages((prev) =>
      prev.map((msg) => {
        if (msg.id !== messageId || !msg.acoes) return msg
        return {
          ...msg,
          acoes: msg.acoes.map((a) =>
            a.acaoId === acaoId ? { ...a, ...patch } : a,
          ),
        }
      }),
    )
  }

  async function handleDecisaoAcao(
    messageId: string,
    acaoId: string,
    decisao: 'confirmar' | 'cancelar',
  ) {
    atualizarEstadoAcao(messageId, acaoId, { status: 'PROCESSANDO' })

    try {
      const res =
        decisao === 'confirmar'
          ? await chatService.confirmarAcao(acaoId)
          : await chatService.cancelarAcao(acaoId)

      const novoStatus: StatusAcaoUi =
        res.status === 'CONFIRMADA'
          ? 'CONFIRMADA'
          : res.status === 'CANCELADA'
            ? 'CANCELADA'
            : res.status === 'EXPIRADA'
              ? 'EXPIRADA'
              : 'ERRO'

      atualizarEstadoAcao(messageId, acaoId, {
        status: novoStatus,
        resultado: res.mensagem,
      })
    } catch (error) {
      const msgErro = getApiErrorMessage(
        error,
        'Não foi possível processar sua confirmação.',
      )
      atualizarEstadoAcao(messageId, acaoId, {
        status: 'ERRO',
        resultado: msgErro,
      })
    }
  }

  async function enviarMensagem(textoBruto: string) {
    const texto = textoBruto.trim()
    if (!texto || isLoading) return

    const userMsg: ChatMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      text: texto,
    }

    setMessages((prev) => [...prev, userMsg])
    setInput('')
    setIsLoading(true)

    try {
      const resposta = await chatService.enviarMensagem(texto, conversaId)
      setConversaId(resposta.conversaId)

      const acoesUi: AcaoPendenteUi[] | undefined =
        resposta.acoesPendentes && resposta.acoesPendentes.length > 0
          ? resposta.acoesPendentes.map((a) => ({
              ...a,
              status: 'PENDENTE',
            }))
          : undefined

      setMessages((prev) => [
        ...prev,
        {
          id: `ai-${Date.now()}`,
          role: 'ai',
          text: resposta.mensagem,
          isError: Boolean(resposta.erro),
          acoes: acoesUi,
        },
      ])
    } catch (error) {
      const mensagemErro = getApiErrorMessage(
        error,
        'Não foi possível obter resposta da Carollina AI no momento.',
      )
      setMessages((prev) => [
        ...prev,
        {
          id: `err-${Date.now()}`,
          role: 'ai',
          text: mensagemErro,
          isError: true,
        },
      ])
    } finally {
      setIsLoading(false)
      window.setTimeout(() => {
        inputRef.current?.focus()
      }, 0)
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void enviarMensagem(input)
  }

  const mostrarSugestoes = messages.length === 1 && !isLoading

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
              {isLoading ? 'Pensando...' : 'Online'}
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
                {isLoading ? 'Pensando...' : 'Online'}
              </span>
            </div>
          </div>

          <div className="chat-header-actions">
            {messages.length > 1 && (
              <button
                type="button"
                className="chat-collapse-btn"
                aria-label="Iniciar nova conversa"
                onClick={handleResetConversation}
                disabled={isLoading}
                title="Nova conversa"
              >
                <RotateCcw size={14} />
              </button>
            )}

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
        </div>

        <div className="chat-messages">
          {messages.map((msg, idx) => (
            <div key={msg.id} className="chat-message-group">
              <div
                className={`chat-bubble ${
                  msg.role === 'user' ? 'chat-bubble-user' : 'chat-bubble-ai'
                } ${msg.isError ? 'chat-bubble-error' : ''} chat-stagger-item`}
                style={staggerStyle(Math.min(idx, 5))}
              >
                {msg.role === 'ai' ? renderFormattedText(msg.text) : msg.text}
              </div>

              {msg.acoes && msg.acoes.length > 0 && (
                <div className="chat-actions-stack">
                  {msg.acoes.map((acao) => (
                    <div
                      key={acao.acaoId}
                      className={`chat-action-card ${
                        acao.critica ? 'chat-action-card--critica' : ''
                      } chat-action-card--${acao.status.toLowerCase()}`}
                    >
                      <div className="chat-action-header">
                        <span className="chat-action-title">
                          {acao.critica ? (
                            <ShieldAlert size={14} />
                          ) : (
                            <ShieldCheck size={14} />
                          )}
                          {acao.titulo}
                        </span>

                        {acao.status !== 'PENDENTE' &&
                          acao.status !== 'PROCESSANDO' && (
                            <span
                              className={`chat-action-badge chat-action-badge--${acao.status.toLowerCase()}`}
                            >
                              {acao.status === 'CONFIRMADA'
                                ? 'Confirmada'
                                : acao.status === 'CANCELADA'
                                  ? 'Cancelada'
                                  : acao.status === 'EXPIRADA'
                                    ? 'Expirada'
                                    : 'Erro'}
                            </span>
                          )}
                      </div>

                      {acao.detalhes.length > 0 && (
                        <ul className="chat-action-details">
                          {acao.detalhes.map((linha, i) => (
                            <li key={i}>{linha}</li>
                          ))}
                        </ul>
                      )}

                      {(acao.status === 'PENDENTE' ||
                        acao.status === 'PROCESSANDO') && (
                        <div className="chat-action-buttons">
                          <button
                            type="button"
                            className={`chat-action-btn ${
                              acao.critica
                                ? 'chat-action-btn--danger'
                                : 'chat-action-btn--confirm'
                            }`}
                            disabled={acao.status === 'PROCESSANDO'}
                            onClick={() =>
                              void handleDecisaoAcao(
                                msg.id,
                                acao.acaoId,
                                'confirmar',
                              )
                            }
                          >
                            <Check size={14} />
                            {acao.status === 'PROCESSANDO'
                              ? 'Aplicando...'
                              : 'Confirmar'}
                          </button>

                          <button
                            type="button"
                            className="chat-action-btn chat-action-btn--cancel"
                            disabled={acao.status === 'PROCESSANDO'}
                            onClick={() =>
                              void handleDecisaoAcao(
                                msg.id,
                                acao.acaoId,
                                'cancelar',
                              )
                            }
                          >
                            <X size={14} />
                            Cancelar
                          </button>
                        </div>
                      )}

                      {acao.resultado && (
                        <p
                          className={`chat-action-result ${
                            acao.status === 'ERRO' || acao.status === 'EXPIRADA'
                              ? 'chat-action-result--error'
                              : ''
                          }`}
                        >
                          {acao.resultado}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}

          {isLoading && (
            <div
              className="chat-bubble chat-bubble-ai chat-bubble-loading"
              aria-live="polite"
            >
              <span
                className="chat-typing-dots"
                aria-label="Carollina AI está digitando"
              >
                <span />
                <span />
                <span />
              </span>
            </div>
          )}

          {mostrarSugestoes && (
            <div className="chat-suggestions">
              {ADMIN_SUGGESTIONS.map((item, idx) => (
                <button
                  key={item}
                  type="button"
                  className="chat-chip chat-stagger-item"
                  style={staggerStyle(1 + idx)}
                  onClick={() => void enviarMensagem(item)}
                >
                  {item}
                </button>
              ))}
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        <div className="chat-footer">
          <form className="chat-form" onSubmit={handleSubmit}>
            <input
              ref={inputRef}
              type="text"
              className="chat-input"
              maxLength={2000}
              placeholder={
                isLoading ? 'Aguardando resposta...' : 'Escreva sua pergunta...'
              }
              value={input}
              onChange={(e) => setInput(e.target.value)}
              disabled={isLoading}
            />
            <button
              type="submit"
              className="chat-send-btn"
              title="Enviar"
              disabled={isLoading || !input.trim()}
            >
              <ArrowRight size={17} />
            </button>
          </form>
        </div>
      </aside>
    </>
  )
}
