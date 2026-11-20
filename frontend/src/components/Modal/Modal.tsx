import {
  useEffect,
  useId,
  useRef,
  useState,
  type ReactNode,
  type TransitionEvent,
} from 'react'
import { X } from 'lucide-react'
import './Modal.css'

interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title: string
  headerBadge?: ReactNode
  subtitle?: string
  hideHeader?: boolean
  children: ReactNode
  footer?: ReactNode
  closeOnOverlayClick?: boolean
  maxWidth?: number
}

type ModalPhase = 'idle' | 'open' | 'closing'

// Controle global para bloquear scroll do body e manter foco correto em modais sequenciais
const activeOpenModalIds = new Set<string>()
let savedBodyOverflow: string | null = null
let pageTriggerElement: HTMLElement | null = null

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(', ')

const FIELD_SELECTOR =
  'input:not([disabled]):not([type="hidden"]), select:not([disabled]), textarea:not([disabled])'

export function Modal({
  isOpen,
  onClose,
  title,
  headerBadge,
  subtitle,
  hideHeader = false,
  children,
  footer,
  closeOnOverlayClick = true,
  maxWidth,
}: ModalProps) {
  const modalId = useId()
  const titleId = `${modalId}-title`

  const [isMounted, setIsMounted] = useState(isOpen)
  const [phase, setPhase] = useState<ModalPhase>('idle')
  const [isAnimating, setIsAnimating] = useState(false)

  const cardRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLElement | null>(null)
  const closeTimerRef = useRef<number | null>(null)
  const openTimerRef = useRef<number | null>(null)
  const onCloseRef = useRef(onClose)

  onCloseRef.current = onClose

  // Controla transições entre montado/aberto/fechando/desmontado
  useEffect(() => {
    if (isOpen) {
      if (closeTimerRef.current !== null) {
        window.clearTimeout(closeTimerRef.current)
        closeTimerRef.current = null
      }

      if (!activeOpenModalIds.has(modalId)) {
        const activeEl =
          document.activeElement instanceof HTMLElement
            ? document.activeElement
            : null
        triggerRef.current = activeEl

        if (activeEl && !activeEl.closest('[role="dialog"]')) {
          pageTriggerElement = activeEl
        }

        if (activeOpenModalIds.size === 0) {
          savedBodyOverflow = document.body.style.overflow
          document.body.style.overflow = 'hidden'
        }
        activeOpenModalIds.add(modalId)
      }

      if (!isMounted) {
        setIsMounted(true)
        setPhase('idle')
      } else if (phase === 'closing') {
        setPhase('open')
      }
      return
    }

    if (activeOpenModalIds.has(modalId)) {
      activeOpenModalIds.delete(modalId)
      if (activeOpenModalIds.size === 0) {
        document.body.style.overflow = savedBodyOverflow ?? ''
        savedBodyOverflow = null
      }
    }

    if (isMounted && phase !== 'closing') {
      if (openTimerRef.current !== null) {
        window.clearTimeout(openTimerRef.current)
        openTimerRef.current = null
      }
      setPhase('closing')
      setIsAnimating(true)

      const fallbackCloseMs = 180
      closeTimerRef.current = window.setTimeout(() => {
        finishClose()
      }, fallbackCloseMs)
    }
  }, [isOpen, isMounted, phase, modalId])

  // Dispara a animação de entrada após o frame inicial estar pintado no DOM
  useEffect(() => {
    if (!isOpen || !isMounted || phase !== 'idle') return

    setIsAnimating(true)
    let raf2 = 0
    const raf1 = window.requestAnimationFrame(() => {
      raf2 = window.requestAnimationFrame(() => {
        setPhase('open')
        openTimerRef.current = window.setTimeout(() => {
          setIsAnimating(false)
          openTimerRef.current = null
        }, 230)
      })
    })

    return () => {
      window.cancelAnimationFrame(raf1)
      window.cancelAnimationFrame(raf2)
    }
  }, [isOpen, isMounted, phase])

  // Move foco para o primeiro campo do modal (ou para o card se não houver campo)
  useEffect(() => {
    if (!isOpen || !isMounted || phase !== 'open') return
    const cardEl = cardRef.current
    if (!cardEl) return

    const firstField = cardEl.querySelector<HTMLElement>(FIELD_SELECTOR)
    if (firstField) {
      firstField.focus({ preventScroll: true })
    } else {
      cardEl.focus({ preventScroll: true })
    }
  }, [isOpen, isMounted, phase])

  // Esc para fechar + Focus Trap mantendo o Tab preso dentro do modal aberto
  useEffect(() => {
    if (!isOpen || !isMounted) return

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        event.preventDefault()
        event.stopPropagation()
        onCloseRef.current()
        return
      }

      if (event.key === 'Tab' && cardRef.current) {
        const focusables = Array.from(
          cardRef.current.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR),
        ).filter((el) => el.offsetParent !== null || el === document.activeElement)

        if (focusables.length === 0) {
          event.preventDefault()
          cardRef.current.focus()
          return
        }

        const firstEl = focusables[0]
        const lastEl = focusables[focusables.length - 1]
        const currentEl = document.activeElement

        if (event.shiftKey) {
          if (currentEl === firstEl || currentEl === cardRef.current) {
            event.preventDefault()
            lastEl.focus()
          }
        } else if (currentEl === lastEl) {
          event.preventDefault()
          firstEl.focus()
        }
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, isMounted])

  // Limpeza ao desmontar o componente pai
  useEffect(() => {
    return () => {
      if (closeTimerRef.current !== null) {
        window.clearTimeout(closeTimerRef.current)
      }
      if (openTimerRef.current !== null) {
        window.clearTimeout(openTimerRef.current)
      }
      if (activeOpenModalIds.has(modalId)) {
        activeOpenModalIds.delete(modalId)
        if (activeOpenModalIds.size === 0) {
          document.body.style.overflow = savedBodyOverflow ?? ''
          savedBodyOverflow = null
        }
      }
    }
  }, [modalId])

  function finishClose() {
    if (closeTimerRef.current !== null) {
      window.clearTimeout(closeTimerRef.current)
      closeTimerRef.current = null
    }
    setIsMounted(false)
    setIsAnimating(false)
    setPhase('idle')

    // Só devolve foco quando não há outro modal abrindo/aberto em sequência
    if (activeOpenModalIds.size === 0) {
      const target =
        triggerRef.current && triggerRef.current.isConnected
          ? triggerRef.current
          : pageTriggerElement && pageTriggerElement.isConnected
            ? pageTriggerElement
            : null

      pageTriggerElement = null
      if (target) {
        window.requestAnimationFrame(() => {
          target.focus({ preventScroll: true })
        })
      }
    }
  }

  function handleCardTransitionEnd(event: TransitionEvent<HTMLDivElement>) {
    if (event.target !== cardRef.current) return
    if (event.propertyName !== 'transform' && event.propertyName !== 'opacity') {
      return
    }

    if (phase === 'open') {
      if (openTimerRef.current !== null) {
        window.clearTimeout(openTimerRef.current)
        openTimerRef.current = null
      }
      setIsAnimating(false)
    } else if (phase === 'closing') {
      finishClose()
    }
  }

  if (!isMounted) return null

  const stateClass =
    phase === 'open'
      ? 'open'
      : phase === 'closing'
        ? 'closing'
        : 'idle'

  return (
    <div
      className={`collina-modal-backdrop collina-modal-backdrop--${stateClass} ${
        isAnimating ? 'collina-modal-backdrop--animating' : ''
      }`}
      onClick={() => {
        if (closeOnOverlayClick && phase === 'open') {
          onClose()
        }
      }}
    >
      <div
        ref={cardRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={hideHeader ? undefined : titleId}
        aria-label={hideHeader ? title : undefined}
        tabIndex={-1}
        className={`collina-modal-card collina-modal-card--${stateClass} ${
          isAnimating ? 'collina-modal-card--animating' : ''
        }`}
        style={maxWidth ? { maxWidth: `${maxWidth}px` } : undefined}
        onClick={(e) => e.stopPropagation()}
        onTransitionEnd={handleCardTransitionEnd}
      >
        {!hideHeader && (
          <div className="collina-modal-header">
            <div>
              <div className="collina-modal-title-row">
                <h3 id={titleId} className="collina-modal-title">
                  {title}
                </h3>
                {headerBadge}
              </div>
              {subtitle && (
                <span className="collina-modal-subtitle">{subtitle}</span>
              )}
            </div>
            <button
              type="button"
              className="collina-modal-close-btn"
              aria-label={`Fechar ${title}`}
              onClick={onClose}
            >
              <X size={14} />
            </button>
          </div>
        )}

        <div className="collina-modal-body">{children}</div>

        {footer && <div className="collina-modal-footer">{footer}</div>}
      </div>
    </div>
  )
}
