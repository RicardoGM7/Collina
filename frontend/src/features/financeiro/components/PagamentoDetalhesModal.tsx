import { Modal } from '../../../components/Modal/Modal'
import type { Pagamento } from '../../../types/financeiro'
import { formatDateFull } from '../../../utils/formatters'
import { obterGatewayId } from '../mensalidadesUtils'
import { StatusPagamentoBadge } from './StatusPagamentoBadge'
import { ValorMonetario } from './ValorMonetario'
import './mensalidadesShared.css'

interface PagamentoDetalhesModalProps {
  pagamento: Pagamento | null
  onClose: () => void
}

export function PagamentoDetalhesModal({ pagamento, onClose }: PagamentoDetalhesModalProps) {
  if (!pagamento) return null
  const gatewayId = obterGatewayId(pagamento)

  return (
    <Modal
      isOpen={pagamento !== null}
      onClose={onClose}
      title={`Pagamento #${pagamento.id}`}
      headerBadge={<StatusPagamentoBadge status={pagamento.status} />}
      maxWidth={480}
    >
      <div className="mens-modal-body">
        <div className="mens-detalhes-grid">
          <div className="mens-detalhe-item">
            <span>Forma de pagamento</span>
            <strong>{pagamento.formaPagamento}</strong>
          </div>
          <div className="mens-detalhe-item">
            <span>Valor</span>
            <strong>
              <ValorMonetario
                valor={pagamento.valor}
                riscado={pagamento.status === 'RECUSADO' || pagamento.status === 'EXPIRADO'}
              />
            </strong>
          </div>
          <div className="mens-detalhe-item">
            <span>Data do pagamento</span>
            <strong>
              {pagamento.dataPagamento ? formatDateFull(pagamento.dataPagamento) : '—'}
            </strong>
          </div>
          <div className="mens-detalhe-item">
            <span>Mensalidade</span>
            <strong>{pagamento.mensalidadeId ? `#${pagamento.mensalidadeId}` : '—'}</strong>
          </div>
          {gatewayId && (
            <div className="mens-detalhe-item mens-detalhe-full">
              <span>ID no gateway</span>
              <strong>{gatewayId}</strong>
            </div>
          )}
          {pagamento.comprovante && (
            <div className="mens-detalhe-item mens-detalhe-full">
              <span>Comprovante</span>
              <strong>{pagamento.comprovante}</strong>
            </div>
          )}
        </div>

        <div className="mens-modal-actions">
          <button type="button" className="mens-btn-secondary" onClick={onClose}>
            Fechar
          </button>
        </div>
      </div>
    </Modal>
  )
}
