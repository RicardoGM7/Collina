import { useState, type FormEvent } from 'react'
import { ChevronRight, Send, Sparkles } from 'lucide-react'
import './ChatSidebar.css'

interface Message {
  id: number
  sender: 'ai' | 'user'
  text: string
}

const INITIAL_MESSAGES: Message[] = [
  {
    id: 1,
    sender: 'ai',
    text: 'Olá! Posso consultar reservas, estoque e finanças, e registrar movimentações. Como ajudo?',
  },
]

const SUGGESTIONS = ['Resumo do mês', 'Reservas de hoje', 'Itens em baixa']

export function ChatSidebar() {
  const [messages, setMessages] = useState<Message[]>(INITIAL_MESSAGES)
  const [input, setInput] = useState('')

  function sendMessage(text: string) {
    const trimmed = text.trim()
    if (!trimmed) return

    const userMsg: Message = { id: Date.now(), sender: 'user', text: trimmed }
    const replyMsg: Message = {
      id: Date.now() + 1,
      sender: 'ai',
      text: `Recebi "${trimmed}". Depois vamos conectar esta resposta ao backend!`,
    }

    setMessages((prev) => [...prev, userMsg, replyMsg])
    setInput('')
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    sendMessage(input)
  }

  return (
    <aside className="chat-sidebar">
      <div className="chat-header">
        <div className="chat-brand">
          <div className="chat-logo">
            <Sparkles size={18} />
          </div>
          <div>
            <span className="chat-name">Carollina AI</span>
            <span className="chat-status">
              <span className="chat-status-dot" />
              Online
            </span>
          </div>
        </div>

        <button type="button" className="chat-collapse-btn" title="Recolher">
          <ChevronRight size={16} />
        </button>
      </div>

      <div className="chat-messages">
        {messages.map((msg) => (
          <div
            key={msg.id}
            className={`chat-bubble ${msg.sender === 'user' ? 'chat-bubble-user' : 'chat-bubble-ai'}`}
          >
            {msg.text}
          </div>
        ))}

        <div className="chat-suggestions">
          {SUGGESTIONS.map((item) => (
            <button
              key={item}
              type="button"
              className="chat-chip"
              onClick={() => sendMessage(item)}
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
            <Send size={15} />
          </button>
        </form>
      </div>
    </aside>
  )
}
