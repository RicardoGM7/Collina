import { ArrowRight, Info } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import espacoAssociado from '../../assets/EspacoAssociado.png'
import mensalidadeAssociado from '../../assets/MensalidadeAssociado.png'
import pedidosAssociado from '../../assets/PedidosAssociado.png'
import { Modal } from '../../components/Modal/Modal'
import { useAuth } from '../../context/AuthContext'
import { SITUACAO_LABEL, nomePlano } from '../../types/associado'
import { ProximasReservas } from '../espacos/ProximasReservas'
import { useMeuCadastro } from './MeuCadastroContext'
import { formatarMoeda, primeiroNome } from './associadoUtils'
import './AssociadoHomePage.css'

export function AssociadoHomePage() {
  const navigate = useNavigate()
  const { usuario } = useAuth()
  const { associado, carregando, naoVinculado, erro } = useMeuCadastro()

  const [modalModuloEmBreve, setModalModuloEmBreve] = useState<{
    titulo: string
    descricao: string
  } | null>(null)

  // Nome real do associado (cadastro do banco ou conta autenticada)
  const nomeCompleto =
    associado?.pessoa.nome ||
    usuario?.nome ||
    usuario?.email?.split('@')[0] ||
    'Associado'
  const saudacaoNome = primeiroNome(nomeCompleto) || nomeCompleto

  // Plano e valor reais vindos do back-end (/api/associados/me -> PlanoResponseDTO)
  const badgePlano = carregando
    ? 'Carregando plano...'
    : associado
      ? nomePlano(associado.plano)
      : 'Cadastro não vinculado'

  const valorMensalidade = associado
    ? formatarMoeda(associado.plano.valorPeriodico)
    : 'Em breve'

  return (
    <div className="associado-home">
      {/* 1. Hero Card Dinâmico */}
      <section className="associado-hero-card">
        <div className="associado-hero-top">
          <span className="associado-hero-plan-badge">{badgePlano}</span>
        </div>

        <h1 className="associado-hero-title">Olá, {saudacaoNome}!</h1>

        <div className="associado-hero-subtitle">
          {carregando ? (
            <p className="hero-secondary-text">
              Carregando informações do seu cadastro...
            </p>
          ) : erro ? (
            <p className="hero-secondary-text">{erro}</p>
          ) : naoVinculado || !associado ? (
            <>
              <p className="hero-highlight-text">
                Sua conta ainda não está vinculada a um cadastro de associado
                titular.
              </p>
              <p className="hero-secondary-text">
                Procure a recepção ou administração do clube para vincular seu
                título e liberar seu plano.
              </p>
            </>
          ) : (
            <>
              <p className="hero-highlight-text">
                Situação cadastral:{' '}
                <strong>{SITUACAO_LABEL[associado.situacaoCadastral]}</strong> •{' '}
                Até <strong>{associado.plano.limiteMensalReservas}</strong>{' '}
                reservas/mês e{' '}
                <strong>{associado.plano.maxDependentesAtivos}</strong>{' '}
                dependente(s) ativo(s).
              </p>
              <p className="hero-secondary-text">
                O clube está com as quadras do parque abertas até as 22h.
              </p>
            </>
          )}
        </div>
      </section>

      {/* 2. Ações Rápidas */}
      <section className="associado-section">
        <h2 className="associado-section-heading">Ações Rápidas</h2>

        <div className="associado-actions-grid">
          {/* Card 1: Reservar Espaço (módulo de Espaços) */}
          <div
            className="action-card card-green"
            onClick={() => navigate('/associado/reservas')}
            role="button"
            tabIndex={0}
          >
            <div className="action-card-header">
              <div>
                <span className="action-card-title">Reservar Espaço</span>
                <span className="action-card-sub">
                  Quadras, campos e quiosques
                </span>
              </div>
            </div>

            <div className="action-card-art">
              <img
                src={espacoAssociado}
                alt="Reservar Espaço"
                className="action-card-img"
              />
            </div>

            <div className="action-card-footer">
              <span className="action-footer-label">Nova reserva</span>
              <span className="action-circle-btn white">
                <ArrowRight size={16} strokeWidth={2.5} />
              </span>
            </div>
          </div>

          {/* Card 2: Mensalidades (valor real do plano; cobrança é placeholder até módulo Financeiro) */}
          <div
            className="action-card card-navy"
            onClick={() => navigate('/associado/mensalidades?aba=pagamentos')}
            role="button"
            tabIndex={0}
          >
            <div className="action-card-header">
              <div>
                <span className="action-card-title">Mensalidades</span>
                <span className="action-card-sub">Pagar via PIX ou Boleto</span>
              </div>
              <span className="action-badge-amount">{valorMensalidade}</span>
            </div>

            <div className="action-card-art">
              <img
                src={mensalidadeAssociado}
                alt="Mensalidades"
                className="action-card-img"
              />
            </div>

            <div className="action-card-footer">
              <span className="action-footer-label">Ver mensalidades</span>
              <span className="action-circle-btn green">
                <ArrowRight size={16} strokeWidth={2.5} />
              </span>
            </div>
          </div>

          {/* Card 3: Pedidos (integrado com /api/produtos e /api/pedidos) */}
          <div
            className="action-card card-white"
            onClick={() => navigate('/associado/pedidos')}
            role="button"
            tabIndex={0}
          >
            <div className="action-card-header">
              <div>
                <span className="action-card-title dark-text">Pedidos</span>
                <span className="action-card-sub">
                  Compre produtos do clube
                </span>
              </div>
            </div>

            <div className="action-card-art">
              <img
                src={pedidosAssociado}
                alt="Pedidos"
                className="action-card-img"
              />
            </div>

            <div className="action-card-footer">
              <span className="action-footer-label dark-text">Acessar</span>
              <span className="action-circle-btn slate">
                <ArrowRight size={16} strokeWidth={2.5} />
              </span>
            </div>
          </div>
        </div>
      </section>

      {/* 3. Minhas Próximas Reservas */}
      <section className="associado-section">
        <div className="associado-section-header-row">
          <div className="associado-heading-with-dot">
            <span className="green-indicator-dot" />
            <h2 className="associado-section-heading no-margin">
              Minhas Próximas Reservas
            </h2>
          </div>
        </div>

        <ProximasReservas />
      </section>

      {/* Modal informativa de módulo em desenvolvimento */}
      {modalModuloEmBreve && (
        <Modal
          isOpen={!!modalModuloEmBreve}
          onClose={() => setModalModuloEmBreve(null)}
          title={modalModuloEmBreve.titulo}
          subtitle="Módulo em desenvolvimento"
          maxWidth={460}
        >
          <div className="modulo-breve-body">
            <div className="modulo-breve-alert">
              <Info size={20} className="modulo-breve-icon" />
              <p className="modulo-breve-text">
                {modalModuloEmBreve.descricao}
              </p>
            </div>

            <div className="modulo-breve-actions">
              <button
                type="button"
                className="profile-btn-save"
                onClick={() => setModalModuloEmBreve(null)}
              >
                Entendi
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}
